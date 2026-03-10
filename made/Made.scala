package made

import made.annotation.*

import scala.annotation.{implicitNotFound, Annotation}
import scala.deriving.Mirror
import scala.quoted.*

/**
 * Extended mirror for Scala types, providing annotation metadata, element-level detail,
 * and generated member support beyond standard `scala.deriving.Mirror`.
 *
 * A `Made` instance describes the structure of a type `T` at both the type level and runtime.
 * Unlike the standard library `Mirror`, `Made` carries per-element metadata (annotations,
 * default values, labels) and supports `@generated` members that compute derived values.
 *
 * == Basic usage ==
 * {{{
 * import made.*
 *
 * case class User(name: String, age: Int)
 *
 * val mirror = Made.derived[User]
 * val (nameFld, ageFld) = mirror.elems
 * val user = mirror.fromUnsafeArray(Array("Alice", 30))
 * }}}
 *
 * == Compile-time label access ==
 *
 * Labels are resolved at compile time and preserve singleton string types.
 *
 * '''Mirror label:'''
 * {{{
 * val mirror = Made.derived[User]
 * val name: "User" = mirror.label  // compile-time singleton type
 * }}}
 *
 * '''All element labels as a typed tuple:'''
 * {{{
 * val labels: ("name", "age") = mirror.elemLabels
 * }}}
 *
 * '''Per-element label via tuple destructuring:'''
 * {{{
 * val nameField *: ageField *: EmptyTuple = mirror.elems
 * val n: "name" = nameField.label
 * val a: "age"  = ageField.label
 * }}}
 *
 * '''Inline recursive collection (advanced):'''
 * {{{
 * inline def collectLabels[Tup <: Tuple]: Tuple.Map[Tup, Made.ExtractLabel] =
 *   inline compiletime.erasedValue[Tup] match
 *     case _: EmptyTuple => EmptyTuple
 *     case _: (m *: t)   => compiletime.constValue[Made.ExtractLabel[m]] *: collectLabels[t]
 *
 * val labels = collectLabels[mirror.Elems]  // ("name", "age")
 * }}}
 *
 * == Compile-time annotation queries ==
 * {{{
 * val nameField *: _ *: EmptyTuple = mirror.elems
 * nameField.hasAnnotation[MyAnnotation]           // Boolean, resolved at compile time
 * nameField.getAnnotation[MyAnnotation]           // Option[MyAnnotation]
 * }}}
 *
 * == Compile-time singleton value access ==
 *
 * For enum types with singleton cases, access values via tuple destructuring:
 * {{{
 * enum Color:
 *   case Red, Green, Blue
 *
 * val mirror = Made.derived[Color]
 * val r *: g *: b *: EmptyTuple = mirror.elems
 * r.value  // Color.Red
 * g.value  // Color.Green
 * b.value  // Color.Blue
 * }}}
 *
 * @note Most label and annotation operations require concrete element types known
 *       at compile time. Erasing elements (e.g. via `toList`) loses type information.
 *       Use `elemLabels.toList` for runtime-compatible label access.
 *
 * @see [[Made.Product]]
 * @see [[Made.Sum]]
 * @see [[Made.Singleton]]
 * @see [[Made.Transparent]]
 * @see [[MadeElem]]
 * @see [[Made.derived]]
 */
@implicitNotFound("No Made could be generated.\nDiagnose any issues by calling Made.derived directly")
sealed trait Made:
  /** Tuple of element types, extracted from [[Elems]] via type-level mapping. */
  final type ElemTypes = Tuple.Map[Elems, MadeElem.ExtractOf]

  /** Tuple of element labels, extracted from [[Elems]] via type-level mapping. */
  final type ElemLabels = Tuple.Map[Elems, MadeElem.ExtractLabel]

  /** The mirrored type `T`. */
  type Type

  /** The simple name of `T` (or the override provided by `@name`). */
  type Label <: String

  /**
   * Annotation metadata on `T`, represented as an `AnnotatedType` chain wrapping the [[Meta]]
   * base type. When no `MetaAnnotation` annotations are present, `Metadata = Meta`. When
   * annotations are present, `Metadata` becomes `Meta @Ann1 @Ann2 ...`.
   * Query at runtime via [[hasAnnotation]] and [[getAnnotation]].
   */
  type Metadata <: Meta

  /** Tuple of [[MadeElem]] subtypes representing constructor fields (for products) or subtypes (for sums). */
  type Elems <: Tuple
  val elems: Elems

  /** Tuple of [[GeneratedMadeElem]] for members annotated with `@generated`. */
  type GeneratedElems <: Tuple
  val generatedElems: GeneratedElems

/**
 * Base type for elements within a [[Made]] mirror's [[Elems]] tuple.
 *
 * Each element in the [[Elems]] tuple is a subtype of `MadeElem`,
 * carrying the element's type, label, and annotation metadata. The concrete
 * subtype depends on the mirror kind:
 *
 *  - [[MadeFieldElem]] - constructor parameters in product mirrors
 *  - [[MadeSubElem]] - non-singleton subtypes in sum mirrors
 *  - [[MadeSubSingletonElem]] - singleton subtypes in sum mirrors
 *  - [[GeneratedMadeElem]] - `@generated` members (in `GeneratedElems`)
 *
 * @example
 * {{{
 * import made.*
 *
 * case class User(name: String, age: Int)
 *
 * val mirror = Made.derived[User]
 * val (nameFld, ageFld) = mirror.elems
 * // nameFld: MadeFieldElem { type Type = String; type Label = "name" }
 * // ageFld:  MadeFieldElem { type Type = Int;    type Label = "age"  }
 * }}}
 *
 * @see [[MadeFieldElem]]
 * @see [[MadeSubElem]]
 * @see [[MadeSubSingletonElem]]
 * @see [[GeneratedMadeElem]]
 */
sealed trait MadeElem:
  /** The element's type (field type or subtype). */
  type Type

  /** The element's label (field name or subtype name, or the override provided by `@name`). */
  type Label <: String

  /**
   * Annotation metadata on `T`, represented as an `AnnotatedType` chain wrapping the [[Meta]]
   * base type. When no `MetaAnnotation` annotations are present, `Metadata = Meta`. When
   * annotations are present, `Metadata` becomes `Meta @Ann1 @Ann2 ...`.
   * Query at runtime via [[hasAnnotation]] and [[getAnnotation]].
   */
  type Metadata <: Meta

/**
 * Element representing a constructor parameter in a product type mirror.
 *
 * Each entry in [[Made.Product]]'s [[Elems]] tuple is a `MadeFieldElem`,
 * providing the field's type, label, metadata, and default value.
 *
 * @see [[MadeElem]]
 * @see [[MadeSubElem]]
 * @see [[GeneratedMadeElem]]
 * @see [[Made.Product]]
 */
sealed trait MadeFieldElem extends MadeElem

sealed trait MadeFieldElemWithDefault extends MadeFieldElem:
  /**
   * Resolves a default value for this field using the following priority chain (first match wins):
   *
   *  1. `@whenAbsent(value)` - explicit default from annotation (highest priority)
   *  2. `@optionalParam` - uses `Default[T]` for option-like types
   *  3. Constructor default - the Scala-level default parameter value
   *  4. `None` - no default available
   *
   * @return the default value
   */
  def default: Type

object MadeFieldElem:
  type Of[T] = MadeFieldElem { type Type = T }
  type LabelOf[l <: String] = MadeElem { type Label = l }
  type MetaOf[m <: Meta] = MadeElem { type Metadata = m }

  type ExtractType[M /* <: MadeFieldElem */ ] = M match
    case Of[t] => t

  type ExtractLabel[M /* <: MadeFieldElem */ ] <: String = M match
    case LabelOf[label] => label

  type ExtractMeta[M /* <: MadeFieldElem */ ] <: Meta = M match
    case MetaOf[meta] => meta

/**
 * Element representing a non-singleton subtype in a sum type mirror.
 *
 * Used in [[Made.Sum]]'s [[Elems]] for subtypes that are not
 * singleton types (e.g., case classes with parameters). For singleton
 * subtypes (case objects, parameterless enum cases), see
 * [[MadeSubSingletonElem]].
 *
 * @see [[MadeElem]]
 * @see [[MadeFieldElem]]
 * @see [[MadeSubSingletonElem]]
 * @see [[Made.Sum]]
 */
sealed trait MadeSubElem extends MadeElem

object MadeSubElem:
  type Of[T] = MadeSubElem { type Type = T }

/**
 * Element representing a singleton subtype in a sum type mirror.
 *
 * Extends [[MadeSubElem]]. Used in [[Made.Sum]]'s [[Elems]] for
 * case objects and parameterless enum cases. Provides access to the
 * singleton instance via the [[value]] method.
 *
 * @see [[MadeSubElem]]
 * @see [[Made.Sum]]
 * @see [[Made.Singleton]]
 */
sealed trait MadeSubSingletonElem extends MadeSubElem

object MadeSubSingletonElem:
  type Of[T] = MadeSubSingletonElem { type Type = T }

/**
 * Element representing a [[generated]] val or def.
 *
 * Extends [[MadeFieldElem]]. Lives in [[Made]]'s [[GeneratedElems]] tuple
 * (separate from [[Elems]]). A generated element computes a derived
 * value from an instance of the outer type.
 *
 * @see [[MadeFieldElem]]
 * @see [[MadeElem]]
 * @see [[made.annotation.generated]]
 */
sealed trait GeneratedMadeElem extends MadeFieldElem:
  /** The type that declares the [[generated]] member. */
  type OuterType

  /** Computes the generated value from an instance of the declaring type. */
  def apply(outer: OuterType): Type

object GeneratedMadeElem:
  type Of[T] = GeneratedMadeElem { type Type = T }
  type OuterOf[Outer] = GeneratedMadeElem { type OuterType = Outer }

// workaround for https://github.com/scala/scala3/issues/25245
private sealed trait GeneratedMadeElemWorkaround[Outer, Elem] extends GeneratedMadeElem:
  final type OuterType = Outer
  final type Type = Elem

object MadeElem:
  type Of[T] = MadeElem { type Type = T }
  type LabelOf[l <: String] = MadeElem { type Label = l }
  type MetaOf[m <: Meta] = MadeElem { type Metadata = m }

  type ExtractOf[M /* <: MadeElem */ ] = M match
    case Of[t] => t

  type ExtractLabel[M /* <: MadeElem */ ] <: String = M match
    case LabelOf[label] => label

  type ExtractMeta[M /* <: MadeElem */ ] <: Meta = M match
    case MetaOf[meta] => meta
private trait Meta

object Made:
  type Of[T] = Made { type Type = T }
  type ProductOf[T] = Made.Product { type Type = T }
  type SumOf[T] = Made.Sum { type Type = T }
  type SingletonOf[T] = Made.Singleton { type Type = T }
  type TransparentOf[T] = Made.Transparent { type Type = T; }

  type LabelOf[l <: String] = MadeElem { type Label = l }
  type MetaOf[m <: Meta] = MadeElem { type Metadata = m }

  type ExtractType[M /* <: MadeElem */ ] = M match
    case Of[t] => t

  type ExtractLabel[M /* <: MadeElem */ ] <: String = M match
    case LabelOf[label] => label

  type ExtractMeta[M /* <: MadeElem */ ] <: Meta = M match
    case MetaOf[meta] => meta

  /**
   * Derives a [[Made]] mirror for `T` at compile time.
   *
   * The concrete subtype of the returned mirror is determined by the
   * following derivation priority (first match wins):
   *
   *  1. [[Singleton]] - `T` is an object, `Unit`, or a singleton type
   *  2. [[Transparent]] - `T` is annotated with `@transparent`
   *     (must have exactly one constructor field; `@generated` members
   *     are not allowed)
   *  3. [[Product]] - `T` is a value class (extends `AnyVal`)
   *  4. [[Product]] - `T` has a `Mirror.ProductOf[T]` (case classes)
   *  5. [[Sum]] - `T` has a `Mirror.SumOf[T]` (sealed traits, enums)
   *
   * The return type is `Made.Of[T]` but the actual runtime type is the
   * more specific subtype listed above.
   *
   * @see [[Made.Product]]
   * @see [[Made.Sum]]
   * @see [[Made.Singleton]]
   * @see [[Made.Transparent]]
   */
  transparent inline given derived[T]: Of[T] = ${ derivedImpl[T] }

  private def derivedImpl[T: Type](using quotes: Quotes): Expr[Made.Of[T]] =
    import quotes.reflect.*

    val tTpe = TypeRepr.of[T]
    val tSymbol = tTpe.typeSymbol

    def metaTypeOf(symbol: Symbol): Type[? <: Meta] =
      val annotations = symbol.annotations.filter(_.tpe <:< TypeRepr.of[MetaAnnotation])
      annotations
        .foldRight(TypeRepr.of[Meta])((annot, tpe) => AnnotatedType(tpe, annot))
        .asType
        .asInstanceOf[Type[? <: Meta]]

    extension (symbol: Symbol)
      def hasAnnotationOf[AT <: Annotation: Type] =
        symbol.hasAnnotation(TypeRepr.of[AT].typeSymbol)

      def getAnnotationOf[AT <: Annotation: Type] =
        symbol.getAnnotation(TypeRepr.of[AT].typeSymbol).map(_.asExprOf[AT])

    def labelTypeOf(sym: Symbol, fallback: String): Type[? <: String] =
      val syms = Iterator(sym) ++ sym.allOverriddenSymbols
      val res = syms.find(_.hasAnnotationOf[name]).flatMap(_.getAnnotationOf[name])
      stringToType(res match
        case Some('{ new `name`($value) }) => value.valueOrAbort
        case _ => fallback)

    val generatedElems = for
      member <- tSymbol.fieldMembers ++ tSymbol.declaredMethods
      if member.hasAnnotationOf[generated]
      _ = if !(member.isValDef || member.isDefDef) then
        report.errorAndAbort(
          "@generated can only be applied to vals and defs.",
          member.pos.getOrElse(Position.ofMacroExpansion),
        )
      _ = member.paramSymss match
        case Nil => // no parameters, it's a val or a def without parameters
        case List(Nil) => // a def with empty parameter list
        case paramLists =>
          for
            paramList <- paramLists
            param <- paramList
          do if !param.flags.is(Flags.EmptyFlags) then symbolInfo(param).dbg // todo
    yield
      val elemTpe = tTpe.memberType(member).widen

      (elemTpe.asType, labelTypeOf(member, member.name), metaTypeOf(member)).runtimeChecked match
        case ('[elemTpe], '[type elemLabel <: String; elemLabel], '[type meta <: Meta; meta]) =>
          '{
            new GeneratedMadeElemWorkaround[T, elemTpe]:
              override type Label = elemLabel
              override type Metadata = meta
              override def apply(outer: T): elemTpe = ${ '{ outer }.asTerm.select(member).asExprOf[elemTpe] }
            : GeneratedMadeElem {
              type Type = elemTpe
              type Label = elemLabel
              type Metadata = meta
              type OuterType = T
            }
          }

    def singleCaseFieldOf(symbol: Symbol): Symbol = symbol.caseFields match
      case field :: Nil => field
      case _ => report.errorAndAbort(s"Expected a single case field for ${symbol.name}")

    def madeFieldOf(field: Symbol): Expr[MadeFieldElem] =
      (field.termRef.widen.asType, labelTypeOf(field, field.name), metaTypeOf(field)).runtimeChecked match
        case ('[fieldType], '[type elemLabel <: String; elemLabel], '[type fieldMeta <: Meta; fieldMeta]) =>
          defaultOf[fieldType](0, field) match
            case Some(defaultExpr) =>
              '{
                new MadeFieldElemWithDefault:
                  override type Type = fieldType
                  override type Label = elemLabel
                  override type Metadata = fieldMeta

                  override def default: Type = $defaultExpr
              }
            case None =>
              '{
                new MadeFieldElem:
                  override type Type = fieldType
                  override type Label = elemLabel
                  override type Metadata = fieldMeta
              }

    def defaultOf[E: Type](index: Int, symbol: Symbol): Option[Expr[E]] =
      def fromWhenAbsent = symbol
        .getAnnotationOf[whenAbsent[?]]
        .map:
          case '{ `whenAbsent`($value: E) } => value
          case '{ `whenAbsent`($_ : e) } =>
            report.error(s"whenAbsent should have value with type ${Type.show[e]}")
            '{ ??? }

      def fromOptionalParam = Option.when(symbol.hasAnnotationOf[optionalParam]) {
        Expr.summon[Default[E]] match
          case Some(impl) => '{ $impl() }
          case None =>
            report.error(s"optionalParam should be used only for types with Default defined")
            '{ ??? }
      }
      def fromDefaultValue = tSymbol.companionModule.methodMembers.collectFirst:
        case m if m.name.startsWith("$lessinit$greater$default$" + (index + 1)) =>
          val ref = Ref(m)
          val applied = tTpe.typeArgs match
            case Nil => ref
            case args => ref.appliedToTypes(args)
          applied.asExprOf[E]

      fromWhenAbsent orElse fromOptionalParam orElse fromDefaultValue

    def newTFrom(args: List[Expr[?]]): Expr[T] =
      New(TypeTree.of[T])
        .select(tSymbol.primaryConstructor)
        .appliedToArgs(args.map(_.asTerm))
        .asExprOf[T]

    (
      metaTypeOf(tSymbol),
      labelTypeOf(tSymbol, tSymbol.name.stripSuffix("$")), // find a better way than stripping $
      Expr.ofTupleFromSeq(generatedElems),
    ).runtimeChecked match
      case (
            '[type meta <: Meta; meta],
            '[type label <: String; label],
            '{ type generatedElems <: Tuple; $generatedElemsExpr: generatedElems },
          ) =>
        def deriveSingleton = Option.when(tTpe.isSingleton || tTpe <:< TypeRepr.of[Unit]) {
          Type.of[T] match
            case '[type s <: scala.Singleton; s] =>
              '{
                new Made.Singleton:
                  override type Type = s
                  override type Label = label
                  override type Metadata = meta
                  override type GeneratedElems = generatedElems

                  override val generatedElems: GeneratedElems = $generatedElemsExpr
                .asInstanceOf[
                  Made.SingletonOf[T] {
                    type Label = label
                    type Metadata = meta
                    type GeneratedElems = generatedElems
                  },
                ]
              }
            case '[Unit] =>
              '{
                new Made.Singleton:
                  override type Type = Unit
                  override type Label = label
                  override type Metadata = meta
                  override type GeneratedElems = generatedElems

                  override val generatedElems: GeneratedElems = $generatedElemsExpr
                .asInstanceOf[
                  Made.SingletonOf[T] {
                    type Label = label
                    type Metadata = meta
                    type GeneratedElems = generatedElems
                  },
                ]
              }
        }

        def deriveTransparent = Option.when(tSymbol.hasAnnotation(TypeRepr.of[transparent].typeSymbol)) {
          if generatedElems.nonEmpty then
            report.errorAndAbort(
              "@generated members are not supported in transparent mirrors",
              tSymbol.pos.getOrElse(Position.ofMacroExpansion),
            )

          madeFieldOf(singleCaseFieldOf(tSymbol)) match
            case '{
                  type fieldType
                  type madeFieldElem <: MadeFieldElem.Of[fieldType]
                  $madeFieldExpr: madeFieldElem
                } =>
              '{
                val tw = TransparentWrapping.derived[fieldType, T]

                new TransparentWorkaround[T, fieldType]:
                  override type Label = label
                  override type Metadata = meta

                  override type Elems = madeFieldElem *: EmptyTuple
                  override val elems: Elems = $madeFieldExpr *: EmptyTuple

                  override def unwrap(value: Type): ElemType = tw.unwrap(value)
                  override def wrap(value: ElemType): Type = tw.wrap(value)
                : Made.TransparentOf[T] {
                  type Label = label
                  type ElemType = fieldType
                  type Metadata = meta
                  type Elems = madeFieldElem *: EmptyTuple
                }
              }
        }

        def deriveValueClass = Option.when(tTpe <:< TypeRepr.of[AnyVal]) {
          madeFieldOf(singleCaseFieldOf(tSymbol)) match
            case '{
                  type fieldType
                  type madeFieldElem <: MadeFieldElem.Of[fieldType]
                  $madeFieldExpr: madeFieldElem
                } =>
              '{
                new Made.Product:
                  override type Label = label
                  override type Type = T
                  override type Metadata = meta

                  override type Elems = madeFieldElem *: EmptyTuple
                  override val elems: Elems = $madeFieldExpr *: EmptyTuple

                  override type GeneratedElems = generatedElems
                  override val generatedElems: GeneratedElems = $generatedElemsExpr

                  override def fromUnsafeArray(product: Array[Any]): T =
                    ${ newTFrom(List('{ product(0).asInstanceOf[fieldType] })) }
                : Made.ProductOf[T] {
                  type Label = label
                  type Metadata = meta
                  type Elems = madeFieldElem *: EmptyTuple
                  type GeneratedElems = generatedElems
                }
              }
        }

        def deriveProduct = Expr.summon[Mirror.ProductOf[T]].map {
          case '{
                type mirroredElemTypes <: Tuple
                type label <: String;

                $m: Mirror.ProductOf[T] {
                  type MirroredLabel = label
                  type MirroredElemTypes = mirroredElemTypes
                }
              } =>

            val (exprs, names) = tSymbol.caseFields.zipWithIndex
              .zip(traverseTuple(Type.of[mirroredElemTypes]))
              .foldLeft((Vector.empty[Expr[?]], Vector.empty[(label: String, original: String)])):
                case ((exprs, names), ((fieldSymbol, index), '[fieldTpe])) =>
                  (labelTypeOf(fieldSymbol, fieldSymbol.name), metaTypeOf(fieldSymbol)).runtimeChecked match
                    case ('[type elemLabel <: String; elemLabel], '[type meta <: Meta; meta]) =>
                      val expr = defaultOf[fieldTpe](index, fieldSymbol) match
                        case Some(defaultExpr) =>
                          '{
                            new MadeFieldElemWithDefault:
                              override type Type = fieldTpe
                              override type Label = elemLabel
                              override type Metadata = meta

                              override def default: Type = $defaultExpr
                          }
                        case None =>
                          '{
                            new MadeFieldElem:
                              type Type = fieldTpe
                              type Label = elemLabel
                              type Metadata = meta
                          }

                      (exprs :+ expr, names :+ (typeToString[elemLabel], fieldSymbol.name))
                case _ => wontHappen

            reportOnDuplicates(names)

            Expr.ofTupleFromSeq(exprs) match
              case '{ type mirroredElems <: Tuple; $mirroredElemsExpr: mirroredElems } =>
                '{
                  new Made.Product:
                    override type Type = T
                    override type Label = label
                    override type Metadata = meta
                    override type Elems = mirroredElems

                    override val elems: Elems = $mirroredElemsExpr
                    override def fromUnsafeArray(product: Array[Any]): T = $m.fromProduct(Tuple.fromArray(product))

                    override type GeneratedElems = generatedElems
                    override val generatedElems: GeneratedElems = $generatedElemsExpr
                  : Made.ProductOf[T] {
                    type Label = label
                    type Metadata = meta
                    type Elems = mirroredElems
                    type GeneratedElems = generatedElems
                  }
                }
        }

        def deriveSum = Expr.summon[Mirror.SumOf[T]].map {
          case '{
                type mirroredElemTypes <: Tuple
                type label <: String;

                $_ : Mirror.SumOf[T] {
                  type MirroredLabel = label
                  type MirroredElemTypes = mirroredElemTypes
                }
              } =>

            val (exprs, names) = traverseTuple(Type.of[mirroredElemTypes])
              .foldLeft((Vector.empty[Expr[?]], Vector.empty[(label: String, original: String)])):
                case ((exprs, names), '[subType]) =>
                  val subType = TypeRepr.of[subType]
                  val subSymbol = if subType.termSymbol.isNoSymbol then subType.typeSymbol else subType.termSymbol

                  (labelTypeOf(subSymbol, subSymbol.name), metaTypeOf(subSymbol)).runtimeChecked match
                    case ('[type elemLabel <: String; elemLabel], '[type meta <: Meta; meta]) =>
                      val expr = Type.of[subType] match
                        case '[type s <: scala.Singleton; s] =>
                          '{
                            new MadeSubSingletonElem:
                              type Type = s
                              type Label = elemLabel
                              type Metadata = meta
                          }
                        case '[s] =>
                          '{
                            new MadeSubElem:
                              override type Type = subType
                              override type Label = elemLabel
                              override type Metadata = meta
                          }
                      (exprs :+ expr, names :+ (typeToString[elemLabel], subSymbol.name))
                case _ => wontHappen

            reportOnDuplicates(names)

            Expr.ofTupleFromSeq(exprs) match
              case '{ type mirroredElems <: Tuple; $mirroredElemsExpr: mirroredElems } =>
                '{
                  new Made.Sum:
                    override type Type = T
                    override type Label = label
                    override type Metadata = meta
                    override type Elems = mirroredElems
                    override val elems: Elems = $mirroredElemsExpr

                    override type GeneratedElems = generatedElems
                    override val generatedElems: GeneratedElems = $generatedElemsExpr
                  : Made.SumOf[T] {
                    type Label = label
                    type Metadata = meta
                    type Elems = mirroredElems
                    type GeneratedElems = generatedElems
                  }
                }
              case '{ $_ : x } => report.errorAndAbort(s"Unexpected Mirror type: ${Type.show[x]}")

          case x => report.errorAndAbort(s"Unexpected Mirror type: ${x.show}")
        }

        deriveSingleton orElse deriveTransparent orElse deriveValueClass orElse deriveProduct orElse deriveSum getOrElse {
          report.errorAndAbort(s"Unsupported Mirror type for ${tTpe.show}")
        }

  /**
   * Mirror for product types (case classes and value classes).
   *
   * Produced by [[Made.derived]] when `T` is a case class, a zero-field
   * case class, or a value class (extends `AnyVal`).
   *
   * [[Elems]] is a tuple of [[MadeFieldElem]] representing each
   * constructor parameter. `GeneratedElems` is a tuple of
   * [[GeneratedMadeElem]] for any `@generated` members.
   *
   * @see [[Made]]
   * @see [[Made.Sum]]
   * @see [[Made.Singleton]]
   * @see [[Made.Transparent]]
   * @see [[MadeFieldElem]]
   * @see [[GeneratedMadeElem]]
   */
  sealed trait Product extends Made:
    /** Constructs an instance of `Type` from an untyped array of field values. */
    def fromUnsafeArray(product: Array[Any]): Type

  /**
   * Mirror for sum types (sealed traits and enums).
   *
   * Produced by [[Made.derived]] when `T` is a sealed trait or enum.
   *
   * [[Elems]] is a tuple of [[MadeSubElem]] and
   * [[MadeSubSingletonElem]] representing the subtypes.
   *
   * @see [[Made]]
   * @see [[Made.Product]]
   * @see [[Made.Singleton]]
   * @see [[MadeSubElem]]
   * @see [[MadeSubSingletonElem]]
   */
  sealed trait Sum extends Made

  /**
   * Mirror for singleton types (objects and Unit).
   *
   * Produced by [[Made.derived]] when `T` is an object, `Unit`, or a
   * singleton type.
   *
   * [[Elems]] is fixed to `EmptyTuple` since singletons have no
   * elements. The singleton instance is available via `value`.
   *
   * @see [[Made]]
   * @see [[Made.Sum]]
   * @see [[Made.Product]]
   */
  sealed trait Singleton extends Made:
    override final type Elems = EmptyTuple

    override final val elems: Elems = EmptyTuple

  /**
   * Mirror for transparent wrapper types (single-field case classes
   * annotated with `@transparent`).
   *
   * Produced by [[Made.derived]] when `T` is a case class annotated
   * with `@transparent` having exactly one constructor field.
   * `@generated` members are not supported on transparent types and
   * will cause a compile error.
   *
   * @see [[Made]]
   * @see [[Made.Product]]
   * @see [[TransparentWrapping]]
   */
  sealed trait Transparent extends Made:
    /** `@generated` members are not supported on transparent types. */
    final type GeneratedElems = EmptyTuple

    /** The single wrapped element's type. */
    type ElemType
    type Elems <: MadeElem.Of[ElemType] *: EmptyTuple

    /** Extracts the wrapped value. */
    def unwrap(value: Type): ElemType

    /** Wraps a value into the transparent type. */
    def wrap(value: ElemType): Type

    final val generatedElems: GeneratedElems = EmptyTuple

  // workaround for https://github.com/scala/scala3/issues/25245
  private sealed trait TransparentWorkaround[T, U] extends Made.Transparent:
    final type Type = T
    final type ElemType = U

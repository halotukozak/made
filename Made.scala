package made

import made.annotation.*

import scala.annotation.{implicitNotFound, Annotation}
import scala.deriving.Mirror
import scala.quoted.*

@implicitNotFound("No Made could be generated.\nDiagnose any issues by calling Made.derived directly")
sealed trait Made:
  final type MirroredElemTypes = Tuple.Map[
    MirroredElems,
    [E] =>> E match
      case MadeElem.Of[t] => t,
  ]
  final type MirroredElemLabels = Tuple.Map[
    MirroredElems,
    [E] =>> E match
      case MadeElem.LabelOf[l] => l,
  ]
  type MirroredType
  type MirroredLabel <: String
  type Metadata <: Meta
  type MirroredElems <: Tuple
  type GeneratedElems <: Tuple
  def mirroredElems: MirroredElems
  def generatedElems: GeneratedElems

sealed trait MadeElem:
  type MirroredType
  type MirroredLabel <: String
  type Metadata <: Meta

sealed trait MadeFieldElem extends MadeElem:
  def default: Option[MirroredType]

object MadeFieldElem:
  type Of[T] = MadeFieldElem { type MirroredType = T }

sealed trait MadeSubElem extends MadeElem
object MadeSubElem:
  type Of[T] = MadeSubElem { type MirroredType = T }

sealed trait MadeSubSingletonElem extends MadeSubElem:
  def value: MirroredType

object MadeSubSingletonElem:
  type Of[T] = MadeSubSingletonElem { type MirroredType = T }

sealed trait GeneratedMadeElem extends MadeFieldElem:
  type OuterMirroredType
  def apply(outer: OuterMirroredType): MirroredType

  final def default: Option[MirroredType] = None

object GeneratedMadeElem:
  type Of[T] = GeneratedMadeElem { type MirroredType = T }

  type OuterOf[Outer] = GeneratedMadeElem { type OuterMirroredType = Outer }

// workaround for https://github.com/scala/scala3/issues/25245
sealed trait GeneratedMadeElemWorkaround[Outer, Elem] extends GeneratedMadeElem:
  final type OuterMirroredType = Outer
  final type MirroredType = Elem

object MadeElem:
  type Of[T] = MadeElem { type MirroredType = T }
  type LabelOf[l <: String] = MadeElem { type MirroredLabel = l }
  type MetaOf[m <: Meta] = MadeElem { type Metadata = m }

private trait Meta

object Made:
  type Of[T] = Made { type MirroredType = T }
  type ProductOf[T] = Made.Product { type MirroredType = T }
  type SumOf[T] = Made.Sum { type MirroredType = T }
  type SingletonOf[T] = Made.Singleton { type MirroredType = T }
  type TransparentOf[T, U] = Made.Transparent { type MirroredType = T; type MirroredElemType = U }

  type LabelOf[l <: String] = MadeElem { type MirroredLabel = l }
  type MetaOf[m <: Meta] = MadeElem { type Metadata = m }

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
              type MirroredLabel = elemLabel
              type Metadata = meta
              def apply(outer: T): elemTpe = ${ '{ outer }.asTerm.select(member).asExprOf[elemTpe] }
            : GeneratedMadeElem {
              type MirroredType = elemTpe
              type MirroredLabel = elemLabel
              type Metadata = meta
              type OuterMirroredType = T
            }
          }

    def singleCaseFieldOf(symbol: Symbol): Symbol = symbol.caseFields match
      case field :: Nil => field
      case _ => report.errorAndAbort(s"Expected a single case field for ${symbol.name}")

    def madeFieldOf(field: Symbol): Expr[MadeFieldElem] =
      (field.termRef.widen.asType, labelTypeOf(field, field.name), metaTypeOf(field)).runtimeChecked match
        case ('[fieldType], '[type elemLabel <: String; elemLabel], '[type fieldMeta <: Meta; fieldMeta]) =>
          '{
            new MadeFieldElem:
              type MirroredType = fieldType
              type MirroredLabel = elemLabel
              type Metadata = fieldMeta

              def default = ${ defaultOf[fieldType](0, field) }
          }

    def defaultOf[E: Type](index: Int, symbol: Symbol): Expr[Option[E]] = Expr.ofOption {
      def fromWhenAbsent = symbol.getAnnotationOf[whenAbsent[?]].map {
        case '{ `whenAbsent`($value: E) } => value
        case '{ `whenAbsent`($_ : e) } =>
          report.error(s"whenAbsent should have value with type ${Type.show[e]}")
          '{ ??? }
      }
      def fromOptionalParam = Option.when(symbol.hasAnnotationOf[optionalParam]) {
        Expr.summon[OptionLike[E]] match
          case Some(impl) => '{ $impl.none }
          case None =>
            report.error(s"optionalParam should be used only for types with OptionLike defined")
            '{ ??? }
      }
      def fromDefaultValue = tSymbol.companionModule.methodMembers.collectFirst {
        case m if m.name.startsWith("$lessinit$greater$default$" + (index + 1)) =>
          // todo: generics
          Ref(m).asExprOf[E]
      }
      fromWhenAbsent orElse fromOptionalParam orElse fromDefaultValue
    }

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
                  type MirroredType = s
                  type MirroredLabel = label
                  type Metadata = meta
                  type GeneratedElems = generatedElems

                  def generatedElems: GeneratedElems = $generatedElemsExpr
                  def value: s = singleValueOf[s]
                .asInstanceOf[
                  Made.SingletonOf[T] {
                    type MirroredLabel = label
                    type Metadata = meta
                    type GeneratedElems = generatedElems
                  },
                ]
              }
            case '[Unit] =>
              '{
                new Made.Singleton:
                  type MirroredType = Unit
                  type MirroredLabel = label
                  type Metadata = meta
                  type GeneratedElems = generatedElems

                  def generatedElems: GeneratedElems = $generatedElemsExpr
                  def value: Unit = ()
                .asInstanceOf[
                  Made.SingletonOf[T] {
                    type MirroredLabel = label
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
                  type MirroredLabel = label
                  type Metadata = meta

                  type MirroredElems = madeFieldElem *: EmptyTuple
                  def mirroredElems = $madeFieldExpr *: EmptyTuple

                  def unwrap(value: MirroredType): MirroredElemType = tw.unwrap(value)
                  def wrap(value: MirroredElemType): MirroredType = tw.wrap(value)
                : Made.TransparentOf[T, fieldType] {
                  type MirroredLabel = label
                  type Metadata = meta
                  type MirroredElems = madeFieldElem *: EmptyTuple
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
                  type MirroredLabel = label
                  type MirroredType = T
                  type Metadata = meta

                  type MirroredElems = madeFieldElem *: EmptyTuple
                  def mirroredElems: MirroredElems = $madeFieldExpr *: EmptyTuple

                  type GeneratedElems = generatedElems
                  def generatedElems: GeneratedElems = $generatedElemsExpr

                  def fromUnsafeArray(product: Array[Any]): T =
                    ${ newTFrom(List('{ product(0).asInstanceOf[fieldType] })) }
                : Made.ProductOf[T] {
                  type MirroredLabel = label
                  type Metadata = meta
                  type MirroredElems = madeFieldElem *: EmptyTuple
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

            val elems = Expr.ofTupleFromSeq(
              tSymbol.caseFields.zipWithIndex
                .zip(traverseTuple(Type.of[mirroredElemTypes]))
                .map {
                  case ((fieldSymbol, index), '[fieldTpe]) =>
                    (labelTypeOf(fieldSymbol, fieldSymbol.name), metaTypeOf(fieldSymbol)).runtimeChecked match
                      case ('[type elemLabel <: String; elemLabel], '[type meta <: Meta; meta]) =>
                        '{
                          new MadeFieldElem:
                            type MirroredType = fieldTpe
                            type MirroredLabel = elemLabel
                            type Metadata = meta

                            def default = ${ defaultOf[fieldTpe](index, fieldSymbol) }
                        }
                  case (_, _) => wontHappen
                },
            )

            elems match
              case '{ type mirroredElems <: Tuple; $mirroredElemsExpr: mirroredElems } =>
                '{
                  new Made.Product:
                    type MirroredType = T
                    type MirroredLabel = label
                    type Metadata = meta
                    type MirroredElems = mirroredElems

                    def mirroredElems: MirroredElems = $mirroredElemsExpr
                    def fromUnsafeArray(product: Array[Any]): T = $m.fromProduct(Tuple.fromArray(product))

                    type GeneratedElems = generatedElems
                    def generatedElems: GeneratedElems = $generatedElemsExpr
                  : Made.ProductOf[T] {
                    type MirroredLabel = label
                    type Metadata = meta
                    type MirroredElems = mirroredElems
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

            val elems = Expr.ofTupleFromSeq(traverseTuple(Type.of[mirroredElemTypes]).map { case '[subType] =>
              val subType = TypeRepr.of[subType]
              val subSymbol = if subType.termSymbol.isNoSymbol then subType.typeSymbol else subType.termSymbol

              (labelTypeOf(subSymbol, subSymbol.name), metaTypeOf(subSymbol)).runtimeChecked match
                case ('[type elemLabel <: String; elemLabel], '[type meta <: Meta; meta]) =>
                  Type.of[subType] match
                    case '[type s <: scala.Singleton; s] =>
                      '{
                        new MadeSubSingletonElem:
                          type MirroredType = s
                          type MirroredLabel = elemLabel
                          type Metadata = meta

                          def value: s = singleValueOf[s]
                      }
                    case '[s] =>
                      '{
                        new MadeSubElem:
                          type MirroredType = subType
                          type MirroredLabel = elemLabel
                          type Metadata = meta
                      }
            })

            elems match
              case '{
                    type mirroredElems <: Tuple; $mirroredElemsExpr: mirroredElems
                  } =>
                '{
                  new Made.Sum:
                    type MirroredType = T
                    type MirroredLabel = label
                    type Metadata = meta
                    type MirroredElems = mirroredElems
                    def mirroredElems: MirroredElems = $mirroredElemsExpr

                    type GeneratedElems = generatedElems
                    def generatedElems: GeneratedElems = $generatedElemsExpr
                  : Made.SumOf[T] {
                    type MirroredLabel = label
                    type Metadata = meta
                    type MirroredElems = mirroredElems
                    type GeneratedElems = generatedElems
                  }
                }
              case '{ $_ : x } => report.errorAndAbort(s"Unexpected Mirror type: ${Type.show[x]}")

          case x => report.errorAndAbort(s"Unexpected Mirror type: ${x.show}")
        }

        deriveSingleton orElse deriveTransparent orElse deriveValueClass orElse deriveProduct orElse deriveSum getOrElse {
          report.errorAndAbort(s"Unsupported Mirror type for ${tTpe.show}")
        }

  sealed trait Product extends Made:
    def fromUnsafeArray(product: Array[Any]): MirroredType
  sealed trait Sum extends Made
  sealed trait Singleton extends Made:
    final type MirroredElems = EmptyTuple
    def value: MirroredType
    final def mirroredElems: MirroredElems = EmptyTuple

  sealed trait Transparent extends Made:
    final type GeneratedElems = EmptyTuple
    type MirroredElemType
    type MirroredElems <: MadeElem.Of[MirroredElemType] *: EmptyTuple
    def unwrap(value: MirroredType): MirroredElemType
    def wrap(value: MirroredElemType): MirroredType
    final def generatedElems: GeneratedElems = EmptyTuple

  // workaround for https://github.com/scala/scala3/issues/25245
  sealed trait TransparentWorkaround[T, U] extends Made.Transparent:
    final type MirroredType = T
    final type MirroredElemType = U

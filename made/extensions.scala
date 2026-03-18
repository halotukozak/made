//noinspection UnitMethodIsParameterless
package made

import made.annotation.MetaAnnotation

import scala.annotation.{publicInBinary, tailrec}
import scala.quoted.*

extension [M <: Meta](self: { type Metadata = M })
  /**
   * Returns `true` if the mirror's `Metadata` type member contains an annotation of type `A`.
   *
   * Transparent inline - resolved entirely at compile time, no runtime cost.
   * `A` must extend [[made.annotation.MetaAnnotation]].
   */
  transparent inline def hasAnnotation[A <: MetaAnnotation]: Boolean = ${ hasAnnotationImpl[A, M] }

  /**
   * Returns `Some(annotation)` if the mirror's `Metadata` type member contains an annotation
   * of type `A`, `None` otherwise.
   *
   * The returned annotation instance provides access to annotation parameters
   * (e.g., `getAnnotation[JsonName].get.value`). Inline - resolved at compile time.
   * `A` must extend [[made.annotation.MetaAnnotation]`.
   */
  inline def getAnnotation[A <: MetaAnnotation]: Option[A] = ${ getAnnotationImpl[A, M] }

extension [L <: String](l: { type Label = L })
  /**
   * Returns the label of the mirror or element as a singleton string type.
   *
   * Resolved entirely at compile time via `compiletime.constValue`.
   *
   * Works on both mirrors and individual elements from the [[Elems]] tuple:
   * {{{
   * // Mirror label
   * val mirror = Made.derived[User]
   * val name: "User" = mirror.label
   *
   * // Per-element label via tuple destructuring
   * val nameField *: ageField *: EmptyTuple = mirror.elems
   * val fieldName: "name" = nameField.label
   * }}}
   *
   * For collecting all element labels at once, use [[elemLabels]] instead.
   *
   * @note This is a '''compile-time''' operation. When element types are erased
   *       (e.g. via `toList`), the concrete `Label` type is lost and `.label`
   *       will not compile. Use [[elemLabels]] or `elemLabels.toList` for
   *       runtime-compatible label access.
   */
  inline def label: L = compiletime.constValue[L]

extension [Ls <: Tuple](l: { type ElemLabels = Ls })
  /**
   * Returns all element labels as a typed tuple of singleton strings.
   *
   * Resolved at compile time via `compiletime.constValueTuple`.
   * The result preserves literal string types.
   *
   * {{{
   * val mirror = Made.derived[User]
   *
   * // Typed tuple with literal types
   * val labels: ("name", "age") = mirror.elemLabels
   *
   * // Convert to List[String] for runtime use
   * val labelList: List[String] = mirror.elemLabels.toList.asInstanceOf[List[String]]
   * // labelList == List("name", "age")
   * }}}
   *
   * @note Respects `@name` overrides on fields and enum cases.
   */
  inline def elemLabels: Ls = compiletime.constValueTuple[Ls]

@publicInBinary private def getAnnotationImpl[A <: MetaAnnotation: Type, M <: Meta: Type](using quotes: Quotes)
  : Expr[Option[A]] =
  import quotes.reflect.*

  @tailrec def loop(tpe: TypeRepr): Option[Expr[A]] = tpe match
    case AnnotatedType(_, annot) if annot.tpe <:< TypeRepr.of[A] => Some(annot.asExprOf[A])
    case AnnotatedType(underlying, _) => loop(underlying)
    case _ => None

  Expr.ofOption(loop(TypeRepr.of[M]))

@publicInBinary private def hasAnnotationImpl[A <: MetaAnnotation: Type, M <: Meta: Type](using quotes: Quotes)
  : Expr[Boolean] =
  Expr(getAnnotationImpl[A, M].isExprOf[Some[A]])

extension [T <: Singleton](self: Made.SingletonOf[T]) inline def value: T = singleValueOf[T]
extension (self: Made.SingletonOf[Unit]) inline def value: Unit = ()
extension [T <: Singleton](self: MadeSubSingletonElem.Of[T]) inline def value: T = singleValueOf[T]
extension (self: MadeSubSingletonElem.Of[Unit]) inline def value: Unit = ()

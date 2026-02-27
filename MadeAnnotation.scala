package made
import made.annotation.MetaAnnotation

import scala.annotation.tailrec
import scala.quoted.*

/**
 * Extension methods for querying annotation metadata on a [[Made]] mirror instance.
 *
 * These methods inspect the `Metadata` type member at compile time, walking the
 * `AnnotatedType` chain to find annotations of the requested type. Both methods
 * are inline and resolved entirely at compile time.
 *
 * IMPORTANT: These methods work ONLY on [[Made]] instances, NOT on [[MadeElem]].
 * Element-level metadata is accessible as the `Metadata` type member on each
 * element but has no convenience query methods.
 *
 * ```scala
 * import made.*
 * import made.annotation.*
 *
 * class JsonName(val value: String) extends MetaAnnotation
 *
 * @JsonName("user_record")
 * case class User(name: String, age: Int)
 *
 * val mirror = Made.derived[User]
 * mirror.hasAnnotation[JsonName]          // true
 * mirror.getAnnotation[JsonName].get.value // "user_record"
 * ```
 *
 * @see [[made.Made]]
 * @see [[made.annotation.MetaAnnotation]]
 * @see [[made.Meta]]
 */
extension (m: Made)

  /**
   * Returns `true` if the mirror's `Metadata` type member contains an annotation of type `A`.
   *
   * Transparent inline -- resolved entirely at compile time, no runtime cost.
   * `A` must extend [[made.annotation.MetaAnnotation]].
   */
  transparent inline def hasAnnotation[A <: MetaAnnotation]: Boolean = ${ hasAnnotationImpl[A, m.type] }

  /**
   * Returns `Some(annotation)` if the mirror's `Metadata` type member contains an annotation
   * of type `A`, `None` otherwise.
   *
   * The returned annotation instance provides access to annotation parameters
   * (e.g., `getAnnotation[JsonName].get.value`). Inline -- resolved at compile time.
   * `A` must extend [[made.annotation.MetaAnnotation]].
   */
  inline def getAnnotation[A <: MetaAnnotation]: Option[A] = ${ getAnnotationImpl[A, m.type] }

private def getAnnotationImpl[A <: MetaAnnotation: Type, DM <: Made: Type](using quotes: Quotes): Expr[Option[A]] =
  import quotes.reflect.*

  @tailrec def loop(tpe: TypeRepr): Option[Expr[A]] = tpe match
    case AnnotatedType(_, annot) if annot.tpe <:< TypeRepr.of[A] => Some(annot.asExprOf[A])
    case AnnotatedType(underlying, _) => loop(underlying)
    case _ => None

  Expr.ofOption(loop(TypeRepr.of(using metaOf[DM])))
private def hasAnnotationImpl[A <: MetaAnnotation: Type, DM <: Made: Type](using quotes: Quotes): Expr[Boolean] =
  import quotes.reflect.*

  @tailrec def loop(tpe: TypeRepr): Boolean = tpe match
    case AnnotatedType(_, annot) if annot.tpe <:< TypeRepr.of[A] => true
    case AnnotatedType(underlying, _) => loop(underlying)
    case _ => false

  Expr(loop(TypeRepr.of(using metaOf[DM])))

private def metaOf[DM <: Made: Type](using quotes: Quotes): Type[? <: Meta] = Type.of[DM] match
  case '[type meta <: Meta; Made { type Metadata <: meta }] => Type.of[meta]

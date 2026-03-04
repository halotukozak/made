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
   * Returns the label of the mirror.
   */
  inline def label: L = compiletime.constValue[L]
extension [Ls <: Tuple ](l: { type ElemLabels = Ls })
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

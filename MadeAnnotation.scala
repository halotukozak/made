package made
import made.annotation.MetaAnnotation

import scala.annotation.tailrec
import scala.quoted.*

extension (m: Made)
  transparent inline def hasAnnotation[A <: MetaAnnotation]: Boolean = ${ hasAnnotationImpl[A, m.type] }
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

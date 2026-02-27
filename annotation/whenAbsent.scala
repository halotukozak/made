package made.annotation

import scala.annotation.{Annotation, RefiningAnnotation}
import scala.quoted.*

class whenAbsent[+T](v: => T) extends RefiningAnnotation:
  def value: T = v

object whenAbsent:
  inline def value[T]: T = ${ valueImpl[T] }

  private def valueImpl[T: Type](using quotes: Quotes): Expr[T] =
    import quotes.reflect.*

    object DefaultValueMethod:
      private val DefaultValueMethodName = """(.*)\$default\$(\d+)$""".r

      def unapply(s: Symbol): Option[Symbol] = s match
        case ms if ms.isDefDef =>
          ms.name match
            case DefaultValueMethodName(actualMethodName: String, idx: String) =>
              val method = actualMethodName match
                case "$lessinit$greater" =>
                  ms.owner.companionModule.companionClass.primaryConstructor
                case name =>
                  ms.owner.methodMember(name).headOption getOrElse report.errorAndAbort(
                    s"whenAbsent.value macro could not find method '$name' in ${ms.owner.fullName}",
                  )

              method.paramSymss.flatten.lift(idx.toInt - 1)
            case _ => None
        case _ => None

    val owner = Symbol.spliceOwner.owner match
      case DefaultValueMethod(paramSymbol) => paramSymbol
      case other => other

    owner.getAnnotation(TypeRepr.of[whenAbsent[T]].typeSymbol) match
      case Some(annot) => '{ (${ annot.asExprOf[whenAbsent[T]] }).value }
      case _ =>
        report.error("whenAbsent.value can only be used inside a parameter annotated with @whenAbsent")
        '{ ??? }

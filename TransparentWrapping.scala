package made

import scala.quoted.*

trait TransparentWrapping[R, T]:
  def wrap(r: R): T
  def unwrap(t: T): R

object TransparentWrapping:
  private val reusableIdentity = new TransparentWrapping[Any, Any]:
    def wrap(r: Any): Any = r
    def unwrap(t: Any): Any = t

  def identity[T]: TransparentWrapping[T, T] =
    reusableIdentity.asInstanceOf[TransparentWrapping[T, T]]

  inline def derived[R, T]: TransparentWrapping[R, T] = ${ derivedImpl[R, T] }
  private def derivedImpl[R: Type, T: Type](using quotes: Quotes): Expr[TransparentWrapping[R, T]] =
    import quotes.reflect.*

    val symbol = TypeRepr.of[T].typeSymbol
    val field = symbol.caseFields match
      case field :: Nil => field
      case _ => report.errorAndAbort(s"Expected a single case field for ${symbol.name}")

    field.termRef.widen.asType match
      case '[R] =>
        '{
          new TransparentWrapping[R, T]:
            def unwrap(value: T): R =
              ${ '{ value }.asTerm.select(field).asExprOf[R] }

            def wrap(v: R): T =
              ${
                New(TypeTree.of[T])
                  .select(symbol.primaryConstructor)
                  .appliedToArgs(List('{ v }.asTerm))
                  .asExprOf[T]
              }
        }
      case '[fieldType] =>
        report.errorAndAbort(s"Expected a single case field of type ${TypeRepr.of[fieldType]} for ${symbol.name}")

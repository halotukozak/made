package made

import scala.quoted.*

/**
 * Bidirectional conversion between a transparent wrapper type `T` and its single wrapped field type `R`.
 *
 * Used by [[Made.Transparent]] to implement `wrap` and `unwrap`. Derived at compile time
 * via `TransparentWrapping.derived[R, T]` for single-field case classes annotated with `@transparent`.
 *
 * @tparam R the wrapped field type
 * @tparam T the transparent wrapper type
 * @see [[Made.Transparent]]
 * @see [[made.annotation.transparent]]
 */
trait TransparentWrapping[R, T]:
  /** Wraps a value of the field type into the transparent wrapper type. */
  def wrap(r: R): T

  /** Extracts the field value from a transparent wrapper instance. */
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

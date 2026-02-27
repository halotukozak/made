package made

/**
 * Type class providing a default "empty" value for optional types.
 *
 * Used by `@optionalParam` to supply a default when no explicit default is provided.
 * Instances are defined for `Option[A]` (returns `None`) and `A | Null` (returns `null`).
 *
 * @tparam O the type for which a default value is provided
 * @see [[made.annotation.optionalParam]]
 * @see [[MadeFieldElem]]
 */
trait Default[O] extends (() => O)

object Default:
  given [A] => Default[Option[A]] = () => None
  given [A <: AnyRef] => Default[A | Null] = () => null

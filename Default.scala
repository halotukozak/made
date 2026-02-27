package made

trait Default[O] extends (() => O)

object Default:
  given [A] => Default[Option[A]] = () => None
  given [A <: AnyRef] => Default[A | Null] = () => null

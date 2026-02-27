package made

sealed trait OptionLike[O]:
  type Value

  def none: O

  def some(value: Value): O

  def isDefined(opt: O): Boolean

  def get(opt: O): Value

  def ignoreNulls: Boolean

  def getOrElse[B >: Value](opt: O, default: => B): B = if isDefined(opt) then get(opt) else default

  def foreach(opt: O, f: Value => Unit): Unit = if isDefined(opt) then f(get(opt))

  final def convert[OO, V](opt: O, into: OptionLike.Aux[OO, V])(fun: Value => V): OO =
    fold(opt, into.none)(v => into.some(fun(v)))

  def fold[B](opt: O, ifEmpty: => B)(f: Value => B): B = if isDefined(opt) then f(get(opt)) else ifEmpty

  final def apply(value: Value): O =
    if ignoreNulls && (value.asInstanceOf[AnyRef] eq null) then none else some(value)

object OptionLike:
  type Aux[O, V] = OptionLike[O] { type Value = V }

  given [A] => OptionLike.Aux[Option[A], A] = new OptionLike[Option[A]]:
    override type Value = A

    override val none: None.type = None

    override def some(value: A): Some[A] = Some(value)

    override def isDefined(opt: Option[A]): Boolean = opt.isDefined

    override def get(opt: Option[A]): A = opt.get

    override def ignoreNulls: Boolean = true

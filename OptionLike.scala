package made

trait OptionLike[O]:
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

    override val ignoreNulls: Boolean = true

  given [A <: AnyRef] => OptionLike.Aux[A | Null, A] = new OptionLike[A | Null]:
    override type Value = A

    override val none: Null = null

    override def some(value: A): A = value

    override def isDefined(opt: A | Null): Boolean = opt ne null

    override def get(opt: A | Null): A = opt.nn

    override val ignoreNulls: Boolean = false

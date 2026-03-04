package made
package example.show

import scala.reflect.ClassTag

trait Show[T]:
  def show(value: T): String

object Show:
  given Show[String] = (value: String) => value
  given Show[Int] = (value: Int) => value.toString
  given Show[Long] = (value: Long) => value.toString
  given Show[Double] = (value: Double) => value.toString
  given Show[Boolean] = (value: Boolean) => value.toString

  inline def derived[T](using m: Made.Of[T]): Show[T] = inline m match
    case m: Made.ProductOf[T & Product] => deriveProduct(m).asInstanceOf[Show[T]]
    case m: Made.SumOf[T] => deriveSum(m)
    case m: Made.SingletonOf[T] => deriveSingleton(m)
    case m: Made.TransparentOf[T] => deriveTransparent(m)

  inline def deriveProduct[T <: Product](m: Made.ProductOf[T]): Show[T] = value =>
    val typeName = compiletime.constValue[m.Label]
    val labels = compiletime.constValueTuple[m.MirroredElemLabels].toList.asInstanceOf[List[String]]
    val values = value.productIterator.toList
    val fieldShows = compiletime.summonAll[Tuple.Map[m.MirroredElemTypes, Show]].toList.asInstanceOf[List[Show[Any]]]
    val fields = labels.lazyZip(values).lazyZip(fieldShows).map((label, value, s) => s"$label = ${s.show(value)}")

    s"$typeName(${fields.mkString(", ")})"

  inline def deriveTransparent[T](m: Made.TransparentOf[T]): Show[T] = value =>
    val underlyingShow = compiletime.summonInline[Show[m.MirroredElemType]]
    val inner = m.unwrap(value)
    underlyingShow.show(inner)

  inline def deriveSum[T](m: Made.SumOf[T]): Show[T] = value =>
    val subtypeClasses =
      compiletime.summonAll[Tuple.Map[m.MirroredElemTypes, ClassTag]].toList.asInstanceOf[List[ClassTag[?]]]
    val subtypeShows = compiletime.summonAll[Tuple.Map[m.MirroredElemTypes, Show]].toList.asInstanceOf[List[Show[Any]]]

    subtypeClasses
      .lazyZip(subtypeShows)
      .collectFirst:
        case (clazz, s) if clazz.unapply(value).isDefined => s.show(value)
      .getOrElse(throw IllegalStateException("Unable to find subtype"))

  inline def deriveSingleton[T](m: Made.SingletonOf[T]): Show[T] = _ => compiletime.constValue[m.Label]

package made
package example.fromMap

trait FromMap[T]:
  def fromMap(map: Map[String, Any]): T

object FromMap:
  inline given derived[T](using m: Made.Of[T]): FromMap[T] = inline m match
    case given Made.ProductOf[T] => derivedProduct[T]
    case _ => compiletime.error("Cannot derive FromMap")

  inline private def derivedProduct[T](using m: Made.ProductOf[T]): FromMap[T] = source =>
    val labels = compiletime.constValueTuple[m.ElemLabels].toList.asInstanceOf[List[String]]
    val elems = m.elems.toList.asInstanceOf[List[MadeFieldElem]]

    val values = labels
      .zip(elems)
      .map: (label, elem) =>
        val default = elem match
          case e: MadeFieldElemWithDefault => Some(e.default)
          case _ => None
        source
          .get(label)
          .orElse(default)
          .getOrElse(throw IllegalArgumentException(s"Missing key '$label' with no default"))

    m.fromUnsafeArray(values.toArray)

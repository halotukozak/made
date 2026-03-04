package made
package example.generated

import made.annotation.generated

class GeneratedTest extends munit.FunSuite:
  test("measurement") {
    case class Measurement(value: Double, unit: String):
      @generated def display: String = s"$value $unit"

    val mirror = Made.derived[Measurement]
    val displayGen *: EmptyTuple = mirror.generatedElems

    val m = Measurement(9.81, "m/s")
    assert(displayGen(m) == "9.81 m/s")
  }

  test("prod") {
    case class Prod(a: Int, b: String):
      @generated def ab: String = s"$a-$b"

      @generated def len: Int = b.length

    val mirror = Made.derived[Prod]
    val gAb *: gLen *: EmptyTuple = mirror.generatedElems

    val p = Prod(2, "x")
    assert(gAb(p) == "2-x")
    assert(gLen(p) == 1)
    assert(gAb.default.isEmpty)
    assert(gLen.default.isEmpty)
  }

  test("shape") {
    sealed trait Shape:
      @generated val description: String = "a shape"

    case class Circle(radius: Double) extends Shape

    case class Square(side: Double) extends Shape

    val mirror = Made.derived[Shape]
    val desc *: EmptyTuple = mirror.generatedElems
    assert(desc(Circle(1.0)) == "a shape")
    assert(desc(Square(2.0)) == "a shape")
  }

  test("describe") {
    case class SensorReading(id: String, value: Double):
      @generated def summary: String = s"$id=$value"

      @generated def isValid: Boolean = value >= 0

    trait Describe[T]:
      def apply(instance: T): String

    object Describe:
      inline given derived[T](using mirror: Made.Of[T]): Describe[T] = inline mirror match
        case given Made.ProductOf[T] => derivedProduct[T]
        case _ => compiletime.error("some error")

      inline private def derivedProduct[T](using mirror: Made.ProductOf[T]): Describe[T] = instance =>
        val fieldLabels = compiletime
          .constValueTuple[mirror.ElemLabels]
          .toList
          .asInstanceOf[List[String]]

        val genElems = mirror.generatedElems.toList

        val fieldValues = instance.asInstanceOf[Product].productIterator.toList
        val fieldEntries = fieldLabels.zip(fieldValues).map((label, value) => s"$label = $value")
        val genEntries = genElems.map: elem =>
          val gen = elem.asInstanceOf[GeneratedMadeElem.OuterOf[T]]
          gen(instance)

        val typeName = compiletime.constValue[mirror.Label]

        (fieldEntries ++ genEntries).mkString(s"$typeName(\n", ",\n", "\n)")

    val describe = Describe.derived[SensorReading]
    val output = describe(SensorReading("temp", 23.5))
    assert(output.contains("id = temp"))
    assert(output.contains("value = 23.5"))
    assert(output.contains("temp=23.5"))
    assert(output.contains("true"))
  }

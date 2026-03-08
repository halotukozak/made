package made

import made.annotation.*

class ElemTypesTest extends munit.FunSuite:

  // --- ElemTypes type-level verification ---

  test("ElemTypes for simple product") {
    val m = Made.derived[ETProduct]
    summon[m.ElemTypes =:= (Int, String, Boolean)]
  }

  test("ElemTypes for generic product") {
    val m = Made.derived[ETGeneric[Double]]
    summon[m.ElemTypes =:= (Double, String)]
  }

  test("ElemTypes for no-field product") {
    val m = Made.derived[ETEmpty]
    summon[m.ElemTypes =:= EmptyTuple]
  }

  test("ElemTypes for value class") {
    val m = Made.derived[ETValueClass]
    summon[m.ElemTypes =:= (String *: EmptyTuple)]
  }

  test("ElemTypes for transparent class") {
    val m = Made.derived[ETTransparent]
    summon[m.ElemTypes =:= (Int *: EmptyTuple)]
  }

  test("ElemTypes for singleton") {
    val m = Made.derived[ETObject.type]
    summon[m.ElemTypes =:= EmptyTuple]
  }

  // --- ElemLabels type-level verification ---

  test("ElemLabels for simple product") {
    val m = Made.derived[ETProduct]
    summon[m.ElemLabels =:= ("a", "b", "c")]
  }

  test("ElemLabels for product with @name") {
    val m = Made.derived[ETNamed]
    summon[m.ElemLabels =:= ("renamed", "y")]
  }

  test("ElemLabels for enum") {
    val m = Made.derived[ETEnum]
    summon[m.ElemLabels =:= ("X", "Y")]
  }

  // --- ElemTypes for sum types ---

  test("ElemTypes for enum subtypes") {
    val m = Made.derived[ETEnum]
    summon[m.ElemTypes =:= (ETEnum.X.type, ETEnum.Y)]
  }

  test("ElemTypes for sealed trait with mixed cases") {
    val m = Made.derived[ETMixedADT]
    summon[m.ElemTypes =:= (ETMixedADT.Leaf.type, ETMixedADT.Node)]
  }

  // --- ElemTypes with complex field types ---

  test("ElemTypes with collection fields") {
    val m = Made.derived[ETCollections]
    summon[m.ElemTypes =:= (List[Int], Map[String, Boolean], Option[Double])]
  }

// --- Fixtures ---

case class ETProduct(a: Int, b: String, c: Boolean)
case class ETGeneric[T](value: T, label: String)
case class ETEmpty()
case class ETValueClass(s: String) extends AnyVal
@transparent case class ETTransparent(value: Int)
case object ETObject
case class ETNamed(@name("renamed") x: Int, y: String)

enum ETEnum:
  case X
  case Y(v: Int)

sealed trait ETMixedADT
object ETMixedADT:
  case object Leaf extends ETMixedADT
  case class Node(value: Int) extends ETMixedADT

case class ETCollections(xs: List[Int], m: Map[String, Boolean], opt: Option[Double])

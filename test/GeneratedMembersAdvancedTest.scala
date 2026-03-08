package made

import made.annotation.*

class GeneratedMembersAdvancedTest extends munit.FunSuite:

  // --- Generated vals and defs returning complex types ---

  test("@generated returning List") {
    val m = Made.derived[GenList]
    val gen *: EmptyTuple = m.generatedElems
    assertEquals(gen(GenList(3)), List(1, 2, 3))
  }

  test("@generated returning Option") {
    val m = Made.derived[GenOption]
    val gen *: EmptyTuple = m.generatedElems
    assertEquals(gen(GenOption("")), None)
    assertEquals(gen(GenOption("hi")), Some("hi"))
  }

  test("@generated returning Map") {
    val m = Made.derived[GenMap]
    val gen *: EmptyTuple = m.generatedElems
    assertEquals(gen(GenMap("k", 1)), Map("k" -> 1))
  }

  // --- Multiple generated with different return types ---

  test("multiple @generated with mixed return types") {
    val m = Made.derived[MultiGen]
    val (g1, g2, g3) = m.generatedElems
    val instance = MultiGen("hello")
    assertEquals(g1(instance), 5)
    assertEquals(g2(instance), "HELLO")
    assertEquals(g3(instance), true)
  }

  // --- Generated member labels ---

  test("generated elem labels") {
    val m = Made.derived[MultiGen]
    val (g1, g2, g3) = m.generatedElems
    assertEquals(g1.label, "len")
    assertEquals(g2.label, "upper")
    assertEquals(g3.label, "nonEmpty")
  }

  // --- Generated on val vs def ---

  test("@generated val vs def both work") {
    val m = Made.derived[GenValAndDef]
    val (gVal, gDef) = m.generatedElems
    val instance = GenValAndDef(10)
    assertEquals(gVal(instance), 100)
    assertEquals(gDef(instance), 20)
  }

  // --- Generated default is always None ---

  test("all generated defaults are None") {
    val m = Made.derived[MultiGen]
    val (g1, g2, g3) = m.generatedElems
    assertEquals(g1.default, None)
    assertEquals(g2.default, None)
    assertEquals(g3.default, None)
  }

  // --- Generated members on sealed trait accessed via subtype ---

  test("@generated on sealed trait works for all subtypes") {
    val m = Made.derived[GenTrait]
    val gen *: EmptyTuple = m.generatedElems
    assertEquals(gen(GenTrait.A(5)), "trait-gen")
    assertEquals(gen(GenTrait.B), "trait-gen")
  }

  // --- Generated member with @name ---

  test("@generated with @name override") {
    val m = Made.derived[GenWithName]
    val gen *: EmptyTuple = m.generatedElems
    assertEquals(gen.label, "custom_gen")
    assertEquals(gen(GenWithName(42)), "42")
  }

// --- Fixtures ---

case class GenList(n: Int):
  @generated def items: List[Int] = (1 to n).toList

case class GenOption(s: String):
  @generated def maybe: Option[String] = Option.when(s.nonEmpty)(s)

case class GenMap(key: String, value: Int):
  @generated def asMap: Map[String, Int] = Map(key -> value)

case class MultiGen(s: String):
  @generated def len: Int = s.length
  @generated def upper: String = s.toUpperCase
  @generated def nonEmpty: Boolean = s.nonEmpty

case class GenValAndDef(x: Int):
  @generated val squared: Int = x * x
  @generated def doubled: Int = x * 2

sealed trait GenTrait:
  @generated val tag: String = "trait-gen"

object GenTrait:
  case class A(v: Int) extends GenTrait
  case object B extends GenTrait

case class GenWithName(x: Int):
  @generated @name("custom_gen") def gen: String = x.toString

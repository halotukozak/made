package made

import made.annotation.*
class GeneratedAnnotationTest extends munit.FunSuite:
  import GeneratedAnnotationTest.*

  // --- Type-level verification ---

  test("product: @generated members are exposed via GeneratedElems and compute values") {
    val mirror = Made.derived[Prod]

    type Expected = GeneratedMadeElem {
      type Type = String
      type Label = "ab"
      type Metadata = Meta @generated
      type OuterType = Prod
    } *: GeneratedMadeElem {
      type Type = Int
      type Label = "len"
      type Metadata = Meta @generated
      type OuterType = Prod
    } *: EmptyTuple

    summon[mirror.GeneratedElems =:= Expected]

    val value = Prod(2, "x")
    val gAb *: gLen *: EmptyTuple = mirror.generatedElems
    assert(gAb(value) == "2-x")
    assert(gLen(value) == 1)
  }

  test("value class: @generated members are exposed") {
    val mirror = Made.derived[VC]

    type Expected = GeneratedMadeElem {
      type Type = String
      type Label = "upper"
      type Metadata = Meta @generated
      type OuterType = VC
    } *: EmptyTuple

    summon[mirror.GeneratedElems =:= Expected]

    val gUpper *: EmptyTuple = mirror.generatedElems
    assert(gUpper(VC("ab")) == "AB")
  }

  test("sum (sealed trait): @generated members on the trait are exposed") {
    val mirror = Made.derived[SumADT]

    type Expected = GeneratedMadeElem {
      type Type = Int
      type Label = "const"
      type Metadata = Meta @generated
      type OuterType = SumADT
    } *: EmptyTuple

    summon[mirror.GeneratedElems =:= Expected]

    val gConst *: EmptyTuple = mirror.generatedElems
    assert(gConst(SumADT.Case1(10)) == 42)
    assert(gConst(SumADT.Case2) == 42)
  }

  test("enum: @generated members on the enum are exposed") {
    val mirror = Made.derived[GenEnum]

    type Expected = GeneratedMadeElem {
      type Type = String
      type Label = "info"
      type Metadata = Meta @generated
      type OuterType = GenEnum
    } *: EmptyTuple

    summon[mirror.GeneratedElems =:= Expected]

    val gInfo *: EmptyTuple = mirror.generatedElems
    assert(gInfo(GenEnum.A) == "ENUM")
    assert(gInfo(GenEnum.B(5)) == "ENUM")
  }

  test("singleton object: @generated members are exposed and compute values") {
    val mirror = Made.derived[GenObj.type]

    type Expected = GeneratedMadeElem {
      type Type = Int
      type Label = "id"
      type Metadata = Meta @generated
      type OuterType = GenObj.type
    } *: EmptyTuple

    summon[mirror.GeneratedElems =:= Expected]

    val gId *: EmptyTuple = mirror.generatedElems
    assert(gId(GenObj) == 7)
  }

  // --- Complex return types ---

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

  // --- Val vs def ---

  test("@generated val vs def both work") {
    val m = Made.derived[GenValAndDef]
    val (gVal, gDef) = m.generatedElems
    val instance = GenValAndDef(10)
    assertEquals(gVal(instance), 100)
    assertEquals(gDef(instance), 20)
  }

  // --- Multiple generated with labels ---

  test("multiple @generated with mixed return types and labels") {
    val m = Made.derived[MultiGen]
    val (g1, g2, g3) = m.generatedElems
    val instance = MultiGen("hello")

    assertEquals(g1.label, "len")
    assertEquals(g2.label, "upper")
    assertEquals(g3.label, "nonEmpty")

    assertEquals(g1(instance), 5)
    assertEquals(g2(instance), "HELLO")
    assertEquals(g3(instance), true)
  }

  // --- Generated elems are not MadeFieldElemWithDefault ---

  test("all generated elems have no default") {
    val m = Made.derived[MultiGen]
    val (g1, g2, g3) = m.generatedElems
    assert(!g1.isInstanceOf[MadeFieldElemWithDefault])
    assert(!g2.isInstanceOf[MadeFieldElemWithDefault])
    assert(!g3.isInstanceOf[MadeFieldElemWithDefault])
  }

  // --- @name on generated ---

  test("@generated with @name override") {
    val m = Made.derived[GenWithName]
    val gen *: EmptyTuple = m.generatedElems
    assertEquals(gen.label, "custom_gen")
    assertEquals(gen(GenWithName(42)), "42")
  }

object GeneratedAnnotationTest:
  sealed trait SumADT:
    @generated val const: Int = 42

  final case class Prod(a: Int, b: String):
    @generated def ab: String = s"$a-$b"
    @generated def len: Int = b.length

  final case class VC(x: String) extends AnyVal:
    @generated def upper: String = x.toUpperCase

  enum GenEnum:
    case A
    case B(i: Int)
    @generated val info: String = "ENUM"

  object SumADT:
    final case class Case1(i: Int) extends SumADT
    case object Case2 extends SumADT

  case object GenObj:
    @generated def id: Int = 7

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

  case class GenWithName(x: Int):
    @generated @name("custom_gen") def gen: String = x.toString

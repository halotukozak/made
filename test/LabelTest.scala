package made

import made.annotation.*

class LabelTest extends munit.FunSuite:

  // --- Type-level label ---

  test("label for case class") {
    val mirror = Made.derived[SimpleCaseClass]
    assertEquals(mirror.label, "SimpleCaseClass")
  }

  // todo: fix
//  test("label for case class with @name") {
//    val mirror = Made.derived[NamedProduct]
//    assertEquals(mirror.label, "custom")
//  }

  test("label for object") {
    val mirror = Made.derived[SimpleObject.type]
    assertEquals(mirror.label, "SimpleObject")
  }

  test("label for Unit") {
    val mirror = Made.derived[Unit]
    assertEquals(mirror.label, "Unit")
  }

  test("label for enum") {
    val mirror = Made.derived[SimpleEnum]
    assertEquals(mirror.label, "SimpleEnum")
  }

  test("label for value class") {
    val mirror = Made.derived[ValueClass]
    assertEquals(mirror.label, "ValueClass")
  }

  test("label for @transparent case class") {
    val mirror = Made.derived[TransparentClass]
    assertEquals(mirror.label, "TransparentClass")
  }

  test("label for generic case class") {
    val mirror = Made.derived[Box[Int]]
    assertEquals(mirror.label, "Box")
  }

  // --- Element labels ---

  test("elem label for product fields") {
    val mirror = Made.derived[SimpleCaseClass]
    val id *: name *: EmptyTuple = mirror.elems
    assertEquals(id.label, "id")
    assertEquals(name.label, "name")
  }

  test("elem label for enum subtypes") {
    val mirror = Made.derived[SimpleEnum]
    val case1 *: case2 *: EmptyTuple = mirror.elems
    assertEquals(case1.label, "Case1")
    assertEquals(case2.label, "Case2")
  }

  test("elem label with @name override") {
    val mirror = Made.derived[NamedEnum]
    val case1 *: case2 *: EmptyTuple = mirror.elems
    assertEquals(case1.label, "C1")
    assertEquals(case2.label, "Case2")
  }

  test("elem label for generated members") {
    val mirror = Made.derived[HasGenerated]
    val gen *: EmptyTuple = mirror.generatedElems
    assertEquals(gen.label, "gen")
  }

  test("elem label for @transparent field") {
    val mirror = Made.derived[TransparentClass]
    val field *: EmptyTuple = mirror.elems
    assertEquals(field.label, "int")
  }

  test("elem label with inherited @name") {
    val mirror = Made.derived[InheritedName]
    val field *: EmptyTuple = mirror.elems
    assertEquals(field.label, "customName")
  }

  // --- elemLabels extension ---

  test("elemLabels for product") {
    val mirror = Made.derived[LabelProduct]
    val labels: ("x", "y", "z") = mirror.elemLabels
    assertEquals(labels, ("x", "y", "z"))
  }

  test("elemLabels for product with @name overrides") {
    val mirror = Made.derived[LabelNamedFields]
    val labels: ("renamed_a", "b") = mirror.elemLabels
    assertEquals(labels, ("renamed_a", "b"))
  }

  test("elemLabels for enum") {
    val mirror = Made.derived[LabelEnum]
    val labels: ("A", "B", "C") = mirror.elemLabels
    assertEquals(labels, ("A", "B", "C"))
  }

  test("elemLabels for enum with @name overrides") {
    val mirror = Made.derived[LabelNamedEnum]
    val labels: ("alpha", "B", "gamma") = mirror.elemLabels
    assertEquals(labels, ("alpha", "B", "gamma"))
  }

  test("elemLabels for transparent class") {
    val mirror = Made.derived[TransparentClass]
    val labels: "int" *: EmptyTuple = mirror.elemLabels
    assertEquals(labels, "int" *: EmptyTuple)
  }

  test("elemLabels for singleton is empty") {
    val mirror = Made.derived[SimpleObject.type]
    val labels: EmptyTuple = mirror.elemLabels
    assertEquals(labels, EmptyTuple)
  }

  test("elemLabels for value class") {
    val mirror = Made.derived[ValueClass]
    val labels: "str" *: EmptyTuple = mirror.elemLabels
    assertEquals(labels, "str" *: EmptyTuple)
  }

  test("elemLabels for generic product") {
    val mirror = Made.derived[LabelGeneric[Int]]
    val labels: ("value", "label") = mirror.elemLabels
    assertEquals(labels, ("value", "label"))
  }

  test("elemLabels with inherited @name") {
    val mirror = Made.derived[InheritedName]
    val labels: "customName" *: EmptyTuple = mirror.elemLabels
    assertEquals(labels, "customName" *: EmptyTuple)
  }

  test("elemLabels for mixed ADT") {
    val mirror = Made.derived[LabelMixed]
    val labels: ("Leaf", "Branch") = mirror.elemLabels
    assertEquals(labels, ("Leaf", "Branch"))
  }

  // --- elem label matches elemLabels ---

  test("elem labels match elemLabels tuple") {
    val mirror = Made.derived[LabelProduct]
    val x *: y *: z *: EmptyTuple = mirror.elems
    val labels = mirror.elemLabels
    assertEquals(labels._1, x.label)
    assertEquals(labels._2, y.label)
    assertEquals(labels._3, z.label)
  }

// --- Fixtures ---

@name("custom")
case class NamedProduct(x: Int)

case class LabelProduct(x: Int, y: String, z: Boolean)
case class LabelNamedFields(@name("renamed_a") a: Int, b: String)
case class LabelGeneric[T](value: T, label: String)

enum LabelEnum:
  case A, B, C

enum LabelNamedEnum:
  @name("alpha") case A
  case B
  @name("gamma") case C

sealed trait LabelMixed
object LabelMixed:
  case object Leaf extends LabelMixed
  case class Branch(left: LabelMixed, right: LabelMixed) extends LabelMixed

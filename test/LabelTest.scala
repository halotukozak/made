package made

import made.annotation.*
import made.Made.label

class LabelTest extends munit.FunSuite:

  test("label for case class") {
    val mirror = Made.derived[SimpleCaseClass]
    assertEquals(mirror.label, "SimpleCaseClass")
  }

  //todo: fix
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

  test("elem label for product fields") {
    val mirror = Made.derived[SimpleCaseClass]
    val id *: name *: EmptyTuple = mirror.mirroredElems
    assertEquals(id.label, "id")
    assertEquals(name.label, "name")
  }

  test("elem label for enum subtypes") {
    val mirror = Made.derived[SimpleEnum]
    val case1 *: case2 *: EmptyTuple = mirror.mirroredElems
    assertEquals(case1.label, "Case1")
    assertEquals(case2.label, "Case2")
  }

  test("elem label with @name override") {
    val mirror = Made.derived[NamedEnum]
    val case1 *: case2 *: EmptyTuple = mirror.mirroredElems
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
    val field *: EmptyTuple = mirror.mirroredElems
    assertEquals(field.label, "int")
  }

  test("elem label with inherited @name") {
    val mirror = Made.derived[InheritedName]
    val field *: EmptyTuple = mirror.mirroredElems
    assertEquals(field.label, "customName")
  }

@name("custom")
case class NamedProduct(x: Int)

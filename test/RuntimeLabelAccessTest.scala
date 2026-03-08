package made

import made.annotation.*

class RuntimeLabelAccessTest extends munit.FunSuite:

  // --- elemLabels extension ---

  test("elemLabels for simple product") {
    val mirror = Made.derived[LabelProduct]
    val labels: ("x", "y", "z") = mirror.elemLabels
    assertEquals(labels, ("x", "y", "z"))
  }

  test("elemLabels for product with @name overrides") {
    val mirror = Made.derived[NamedFields]
    val labels: ("renamed_a", "b") = mirror.elemLabels
    assertEquals(labels, ("renamed_a", "b"))
  }

  test("elemLabels for enum") {
    val mirror = Made.derived[LabelEnum]
    val labels: ("A", "B", "C") = mirror.elemLabels
    assertEquals(labels, ("A", "B", "C"))
  }

  test("elemLabels for enum with @name overrides") {
    val mirror = Made.derived[NamedLabelEnum]
    val labels: ("alpha", "B", "gamma") = mirror.elemLabels
    assertEquals(labels, ("alpha", "B", "gamma"))
  }

  test("elemLabels for transparent class") {
    val mirror = Made.derived[LabelTransparent]
    val labels: ("inner" *: EmptyTuple) = mirror.elemLabels
    assertEquals(labels, "inner" *: EmptyTuple)
  }

  test("elemLabels for singleton is empty") {
    val mirror = Made.derived[LabelObject.type]
    val labels: EmptyTuple = mirror.elemLabels
    assertEquals(labels, EmptyTuple)
  }

  test("elemLabels for value class") {
    val mirror = Made.derived[LabelValueClass]
    val labels: ("s" *: EmptyTuple) = mirror.elemLabels
    assertEquals(labels, "s" *: EmptyTuple)
  }

  // --- label extension on individual elems ---

  test("label on each elem matches elemLabels") {
    val mirror = Made.derived[LabelProduct]
    val x *: y *: z *: EmptyTuple = mirror.elems
    assertEquals(x.label, "x")
    assertEquals(y.label, "y")
    assertEquals(z.label, "z")

    val labels = mirror.elemLabels
    assertEquals(labels._1, x.label)
    assertEquals(labels._2, y.label)
    assertEquals(labels._3, z.label)
  }

  test("label on enum subtypes matches elemLabels") {
    val mirror = Made.derived[LabelEnum]
    val a *: b *: c *: EmptyTuple = mirror.elems
    assertEquals(a.label, "A")
    assertEquals(b.label, "B")
    assertEquals(c.label, "C")
  }

  // --- label on generated members ---

  test("label on generated elems") {
    val mirror = Made.derived[LabelWithGenerated]
    val gen *: EmptyTuple = mirror.generatedElems
    assertEquals(gen.label, "derived")
  }

  // --- label for generic types ---

  test("label for generic product strips type args") {
    val mirror = Made.derived[LabelGeneric[Int]]
    assertEquals(mirror.label, "LabelGeneric")
  }

  test("elemLabels for generic product") {
    val mirror = Made.derived[LabelGeneric[Int]]
    val labels: ("value", "label") = mirror.elemLabels
    assertEquals(labels, ("value", "label"))
  }

  // --- inherited @name reflected in elemLabels ---

  test("elemLabels with inherited @name") {
    val mirror = Made.derived[LabelInherited]
    val labels: ("custom" *: EmptyTuple) = mirror.elemLabels
    assertEquals(labels, "custom" *: EmptyTuple)
  }

  // --- sealed trait with mixed singleton and non-singleton ---

  test("elemLabels for mixed ADT") {
    val mirror = Made.derived[LabelMixed]
    val labels: ("Leaf", "Branch") = mirror.elemLabels
    assertEquals(labels, ("Leaf", "Branch"))
  }

// --- Fixtures ---

case class LabelProduct(x: Int, y: String, z: Boolean)
case class NamedFields(@name("renamed_a") a: Int, b: String)
case class LabelValueClass(s: String) extends AnyVal

enum LabelEnum:
  case A, B, C

enum NamedLabelEnum:
  @name("alpha") case A
  case B
  @name("gamma") case C

@transparent
case class LabelTransparent(inner: String)

case object LabelObject

case class LabelWithGenerated(x: Int):
  @generated def derived: String = x.toString

case class LabelGeneric[T](value: T, label: String)

trait LabelTrait:
  @name("custom")
  def field: Int

case class LabelInherited(field: Int) extends LabelTrait

sealed trait LabelMixed
object LabelMixed:
  case object Leaf extends LabelMixed
  case class Branch(left: LabelMixed, right: LabelMixed) extends LabelMixed

package made

import made.annotation.*

class CompileTimeAccessTest extends munit.FunSuite:

  // --- Compile-time label collection via inline recursion ---

  inline def collectLabels[Tup <: Tuple]: Tuple.Map[Tup, Made.ExtractLabel] = inline compiletime.erasedValue[Tup] match
    case _: EmptyTuple => EmptyTuple
    case _: (m *: t) => compiletime.constValue[Made.ExtractLabel[m]] *: collectLabels[t]

  test("collectLabels for product fields") {
    val mirror = Made.derived[CTProduct]
    val labels = collectLabels[mirror.Elems]
    assertEquals(labels, "x" *: "y" *: "z" *: EmptyTuple)
  }

  test("collectLabels for enum subtypes") {
    val mirror = Made.derived[CTEnum]
    val labels = collectLabels[mirror.Elems]
    assertEquals(labels, "A" *: "B" *: "C" *: EmptyTuple)
  }

  test("collectLabels for enum with @name overrides") {
    val mirror = Made.derived[CTNamedEnum]
    val labels = collectLabels[mirror.Elems]
    assertEquals(labels, "alpha" *: "B" *: "gamma" *: EmptyTuple)
  }

  test("collectLabels for product with @name overrides") {
    val mirror = Made.derived[CTNamedFields]
    val labels = collectLabels[mirror.Elems]
    assertEquals(labels, "renamed_a" *: "b" *: EmptyTuple)
  }

  test("collectLabels for value class") {
    val mirror = Made.derived[CTValueClass]
    val labels = collectLabels[mirror.Elems]
    assertEquals(labels, "str" *: EmptyTuple)
  }

  test("collectLabels for transparent class") {
    val mirror = Made.derived[CTTransparent]
    val labels = collectLabels[mirror.Elems]
    assertEquals(labels, "value" *: EmptyTuple)
  }

  test("collectLabels for singleton is empty") {
    val mirror = Made.derived[CTObject.type]
    val labels = collectLabels[mirror.Elems]
    assertEquals(labels, EmptyTuple)
  }

  test("collectLabels for generic product") {
    val mirror = Made.derived[CTGeneric[Int]]
    val labels = collectLabels[mirror.Elems]
    assertEquals(labels, "value" *: "label" *: EmptyTuple)
  }

  test("collectLabels for mixed ADT") {
    val mirror = Made.derived[CTMixed]
    val labels = collectLabels[mirror.Elems]
    assertEquals(labels, "Leaf" *: "Branch" *: EmptyTuple)
  }

  // --- Compile-time singleton value access via destructuring ---

  test("singleton values via destructuring") {
    val mirror = Made.derived[CTSingletons]
    val a *: b *: c *: EmptyTuple = mirror.elems
    assertEquals(a.value, CTSingletons.A)
    assertEquals(b.value, CTSingletons.B)
    assertEquals(c.value, CTSingletons.C)
  }

  test("singleton values from mixed enum via destructuring") {
    val mirror = Made.derived[CTEnum]
    val a *: b *: _ *: EmptyTuple = mirror.elems
    assertEquals(a.value, CTEnum.A)
    assertEquals(b.value, CTEnum.B)
    // CTEnum.C is not a singleton (has parameters), so no .value
  }

  // --- Compile-time per-element label access via destructuring ---

  test("per-element label via destructuring - product") {
    val mirror = Made.derived[CTProduct]
    val x *: y *: z *: EmptyTuple = mirror.elems
    assertEquals(x.label, "x")
    assertEquals(y.label, "y")
    assertEquals(z.label, "z")
  }

  test("per-element label via destructuring - enum") {
    val mirror = Made.derived[CTEnum]
    val a *: b *: c *: EmptyTuple = mirror.elems
    assertEquals(a.label, "A")
    assertEquals(b.label, "B")
    assertEquals(c.label, "C")
  }

  test("per-element label via destructuring - generated elems") {
    val mirror = Made.derived[CTWithGenerated]
    val gen *: EmptyTuple = mirror.generatedElems
    assertEquals(gen.label, "computed")
  }

  // --- Compile-time elemLabels (typed tuple) ---

  test("elemLabels returns typed tuple for product") {
    val mirror = Made.derived[CTProduct]
    val labels: ("x", "y", "z") = mirror.elemLabels
    assertEquals(labels, ("x", "y", "z"))
  }

  test("elemLabels returns typed tuple for enum") {
    val mirror = Made.derived[CTEnum]
    val labels: ("A", "B", "C") = mirror.elemLabels
    assertEquals(labels, ("A", "B", "C"))
  }

  test("elemLabels returns typed tuple for enum with @name") {
    val mirror = Made.derived[CTNamedEnum]
    val labels: ("alpha", "B", "gamma") = mirror.elemLabels
    assertEquals(labels, ("alpha", "B", "gamma"))
  }

  test("elemLabels for empty product") {
    val mirror = Made.derived[CTEmpty]
    val labels: EmptyTuple = mirror.elemLabels
    assertEquals(labels, EmptyTuple)
  }

  // --- Compile-time type-level label verification ---

  test("ElemLabels type matches expected literal types") {
    val mirror = Made.derived[CTProduct]
    summon[mirror.ElemLabels =:= ("x", "y", "z")]
  }

  test("ElemLabels type with @name overrides") {
    val mirror = Made.derived[CTNamedFields]
    summon[mirror.ElemLabels =:= ("renamed_a", "b")]
  }

  // --- Compile-time annotation queries ---

  test("per-element hasAnnotation at compile time") {
    val mirror = Made.derived[CTAnnotated]
    val x *: y *: EmptyTuple = mirror.elems
    assert(x.hasAnnotation[CTMarker])
    assert(!y.hasAnnotation[CTMarker])
  }

  test("per-element getAnnotation at compile time") {
    val mirror = Made.derived[CTParamAnnotated]
    val x *: y *: EmptyTuple = mirror.elems
    assertEquals(x.getAnnotation[CTTag].get.value, "first")
    assertEquals(y.getAnnotation[CTTag].get.value, "second")
  }

  // --- Compile-time mirror label ---

  test("mirror label at compile time") {
    val mirror = Made.derived[CTProduct]
    val lbl: "CTProduct" = mirror.label
    assertEquals(lbl, "CTProduct")
  }

  test("mirror label for enum") {
    val mirror = Made.derived[CTEnum]
    val lbl: "CTEnum" = mirror.label
    assertEquals(lbl, "CTEnum")
  }

  test("mirror label for singleton") {
    val mirror = Made.derived[CTObject.type]
    val lbl: "CTObject" = mirror.label
    assertEquals(lbl, "CTObject")
  }

// --- Fixtures ---

case class CTProduct(x: Int, y: String, z: Boolean)
case class CTEmpty()
case object CTObject
case class CTNamedFields(@name("renamed_a") a: Int, b: String)
case class CTGeneric[T](value: T, label: String)
case class CTValueClass(str: String) extends AnyVal
@transparent case class CTTransparent(value: Int)

enum CTEnum:
  case A, B, C

enum CTNamedEnum:
  @name("alpha") case A
  case B
  @name("gamma") case C

enum CTSingletons:
  case A, B, C

sealed trait CTMixed
object CTMixed:
  case object Leaf extends CTMixed
  case class Branch(left: CTMixed, right: CTMixed) extends CTMixed

class CTMarker extends MetaAnnotation
case class CTTag(value: String) extends MetaAnnotation

case class CTAnnotated(@CTMarker x: Int, y: String)
case class CTParamAnnotated(@CTTag("first") x: Int, @CTTag("second") y: String)

case class CTWithGenerated(s: String):
  @generated def computed: Int = s.length

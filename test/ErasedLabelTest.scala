package made

import made.annotation.*
import scala.compiletime.testing.typeCheckErrors

class ErasedLabelTest extends munit.FunSuite:

  // --- Erased access: label, hasAnnotation, getAnnotation require compile-time types ---

  test("Seq[MadeElem].map(_.label) does not compile - label requires concrete type") {
    val errors = typeCheckErrors("""
      val m = Made.derived[ELProduct]
      val elems: List[MadeElem] = m.elems.toList.asInstanceOf[List[MadeElem]]
      elems.map(_.label)
    """)
    assert(errors.nonEmpty, "Expected compile error for erased label access")
  }

  test("Seq[MadeElem].map(_.hasAnnotation[...]) compiles but loses type info - always false") {
    val m = Made.derived[ELAnnotated]
    val elems: List[MadeElem] = m.elems.toList.asInstanceOf[List[MadeElem]]
    // When erased to MadeElem, Metadata becomes <: Meta (no annotations visible)
    val results = elems.map(_.hasAnnotation[ELMarker])
    assertEquals(results, List(false, false)) // annotation info is lost
  }

  test("Seq[MadeElem].map(_.getAnnotation[...]) compiles but loses type info - always None") {
    val m = Made.derived[ELAnnotated]
    val elems: List[MadeElem] = m.elems.toList.asInstanceOf[List[MadeElem]]
    val results = elems.map(_.getAnnotation[ELMarker])
    assertEquals(results, List(None, None)) // annotation info is lost
  }

  // --- elemLabels.toList is the correct way to get labels as a runtime collection ---

  test("elemLabels.toList provides labels as List[String] at runtime") {
    val m = Made.derived[ELProduct]
    val labels = m.elemLabels.toList.asInstanceOf[List[String]]
    assertEquals(labels, List("x", "y", "z"))
  }

  test("elemLabels.toList with @name overrides") {
    val m = Made.derived[ELNamed]
    val labels = m.elemLabels.toList.asInstanceOf[List[String]]
    assertEquals(labels, List("renamed", "b"))
  }

  test("elemLabels.toList for enum subtypes") {
    val m = Made.derived[ELEnum]
    val labels = m.elemLabels.toList.asInstanceOf[List[String]]
    assertEquals(labels, List("A", "B", "C"))
  }

  test("elemLabels.toList for enum with @name") {
    val m = Made.derived[ELNamedEnum]
    val labels = m.elemLabels.toList.asInstanceOf[List[String]]
    assertEquals(labels, List("first", "B"))
  }

  // --- Zip labels with defaults at runtime ---

  test("zip elemLabels with field defaults at runtime") {
    val m = Made.derived[ELWithDefaults]
    val labels = m.elemLabels.toList.asInstanceOf[List[String]]
    val elems = m.elems.toList.asInstanceOf[List[MadeFieldElem]]
    val defaults = elems.map(_.default)
    val labelsWithDefaults = labels.zip(defaults)
    assertEquals(
      labelsWithDefaults,
      List(("a", None), ("b", Some("hello")), ("c", Some(42))),
    )
  }

  // --- Zip labels with generated values at runtime ---

  test("zip generated elem labels with computed values at runtime") {
    val m = Made.derived[ELWithGenerated]
    val instance = ELWithGenerated("test")

    val genLabels = List("len", "upper") // known at compile time from type
    val genElems = m.generatedElems.toList.asInstanceOf[List[GeneratedMadeElem { type OuterType = ELWithGenerated }]]
    val results = genLabels.zip(genElems.map(_.apply(instance)))
    assertEquals(results, List(("len", 4), ("upper", "TEST")))
  }

  // --- Erased MadeFieldElem: .default still works ---

  test("Seq[MadeFieldElem].map(_.default) works at runtime") {
    val m = Made.derived[ELWithDefaults]
    val elems = m.elems.toList.asInstanceOf[List[MadeFieldElem]]
    val defaults = elems.map(_.default)
    assertEquals(defaults, List(None, Some("hello"), Some(42)))
  }

  // --- Erased GeneratedMadeElem: .default always None ---

  test("Seq[GeneratedMadeElem].map(_.default) all None at runtime") {
    val m = Made.derived[ELWithGenerated]
    val genElems = m.generatedElems.toList.asInstanceOf[List[GeneratedMadeElem]]
    val defaults = genElems.map(_.default)
    assertEquals(defaults, List(None, None))
  }

  // --- Erased MadeSubSingletonElem: .value works ---

  test("collect singleton values from erased Seq at runtime") {
    val m: Made.Sum {
      type Type = ELEnum
      type Label = "ELEnum"
      type Metadata = Meta
      type Elems = MadeSubSingletonElem {
        type Type = ELEnum.A.type
        type Label = "A"
        type Metadata = Meta
      } *: MadeSubSingletonElem {
        type Type = ELEnum.B.type
        type Label = "B"
        type Metadata = Meta
      } *: MadeSubElem {
        type Type = ELEnum.C
        type Label = "C"
        type Metadata = Meta
      } *: EmptyTuple
    } = Made.derived[ELEnum]

    val elems = m.elems.toList
    val singletonValues = elems.collect { case s: MadeSubSingletonElem => s.value }
    assertEquals(singletonValues.size, 2)
    assert(singletonValues.contains(ELEnum.A))
    assert(singletonValues.contains(ELEnum.B))
  }

// --- Fixtures ---

case class ELProduct(x: Int, y: String, z: Boolean)
case class ELNamed(@name("renamed") a: Int, b: String)
case class ELWithDefaults(a: Int, b: String = "hello", c: Int = 42)

class ELMarker extends MetaAnnotation
case class ELAnnotated(@ELMarker x: Int, y: String)

enum ELEnum:
  case A, B
  case C(v: Int)

enum ELNamedEnum:
  @name("first") case A
  case B

case class ELWithGenerated(s: String):
  @generated def len: Int = s.length
  @generated def upper: String = s.toUpperCase

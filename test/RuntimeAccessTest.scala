package made

import made.annotation.*

import scala.compiletime.testing.typeCheckErrors

class RuntimeAccessTest extends munit.FunSuite:

  // --- Type erasure: what works and what doesn't ---

  test("Seq[MadeElem].map(_.label) does not compile - label requires concrete type") {
    val errors = typeCheckErrors("""
      val m = Made.derived[RAProduct]
      val elems: List[MadeElem] = m.elems.toList.asInstanceOf[List[MadeElem]]
      elems.map(_.label)
    """)
    assert(errors.nonEmpty, "Expected compile error for erased label access")
  }

  test("Seq[MadeElem].map(_.hasAnnotation[...]) compiles but loses type info - always false") {
    val m = Made.derived[RAAnnotated]
    val elems: List[MadeElem] = m.elems.toList.asInstanceOf[List[MadeElem]]
    val results = elems.map(_.hasAnnotation[RAMarker])
    assertEquals(results, List(false, false))
  }

  test("Seq[MadeElem].map(_.getAnnotation[...]) compiles but loses type info - always None") {
    val m = Made.derived[RAAnnotated]
    val elems: List[MadeElem] = m.elems.toList.asInstanceOf[List[MadeElem]]
    val results = elems.map(_.getAnnotation[RAMarker])
    assertEquals(results, List(None, None))
  }

  // --- elems.toList / toArray ---

  test("elems.toList for product") {
    val m = Made.derived[RAProduct]
    val elems = m.elems.toList.asInstanceOf[List[MadeFieldElem]]
    assertEquals(elems.size, 3)
  }

  test("elems.toList for enum") {
    val m = Made.derived[RAEnum]
    val elems = m.elems.toList.asInstanceOf[List[MadeElem]]
    assertEquals(elems.size, 3)
  }

  test("elems.toList for singleton is empty") {
    val m = Made.derived[RAObject.type]
    assert(m.elems.toList.isEmpty)
  }

  test("elems.toArray") {
    val m = Made.derived[RAProduct]
    assertEquals(m.elems.toArray.length, 3)
  }

  test("generatedElems.toList") {
    val m = Made.derived[RAWithGenerated]
    val genElems = m.generatedElems.toList.asInstanceOf[List[GeneratedMadeElem]]
    assertEquals(genElems.size, 2)
  }

  // --- Runtime default access ---

  test("Seq[MadeFieldElem] runtime default access via MadeFieldElemWithDefault") {
    val m = Made.derived[RAWithDefaults]
    val elems = m.elems.toList.asInstanceOf[List[MadeFieldElem]]
    val defaults = elems.map {
      case e: MadeFieldElemWithDefault => Some(e.default)
      case _ => None
    }
    assertEquals(defaults, List(None, Some("hello"), Some(true)))
  }
//todo: fix
//  // --- Runtime .value works on erased MadeSubSingletonElem ---
//
//  test("collect singleton values from erased Seq") {
//    val m: Made.Sum {
//      type Type = RAEnum
//      type Label = "RAEnum"
//      type Metadata = Meta
//      type Elems = MadeSubSingletonElem {
//        type Type = RAEnum.A.type
//        type Label = "A"
//        type Metadata = Meta
//      } *: MadeSubSingletonElem {
//        type Type = RAEnum.B.type
//        type Label = "B"
//        type Metadata = Meta
//      } *: MadeSubElem {
//        type Type = RAEnum.C
//        type Label = "C"
//        type Metadata = Meta
//      } *: EmptyTuple
//    } = Made.derived[RAEnum]
//
//    val singletons = m.elems.toList.collect { case s: MadeSubSingletonElem => s.value }
//    assertEquals(singletons.size, 2)
//    assert(singletons.contains(RAEnum.A))
//    assert(singletons.contains(RAEnum.B))
//  }

  // --- elemLabels.toList is the correct alternative to erased _.label ---

  test("elemLabels.toList provides labels as List[String]") {
    val m = Made.derived[RAProduct]
    val labels = m.elemLabels.toList.asInstanceOf[List[String]]
    assertEquals(labels, List("x", "y", "z"))
  }

  test("elemLabels.toList with @name overrides") {
    val m = Made.derived[RANamed]
    val labels = m.elemLabels.toList.asInstanceOf[List[String]]
    assertEquals(labels, List("renamed", "b"))
  }

  // --- Zip labels with defaults ---

  test("zip elemLabels with defaults at runtime") {
    val m = Made.derived[RAWithDefaults]
    val labels = m.elemLabels.toList.asInstanceOf[List[String]]
    val elems = m.elems.toList.asInstanceOf[List[MadeFieldElem]]
    val zipped = labels.zip(elems.map {
      case e: MadeFieldElemWithDefault => Some(e.default)
      case _ => None
    })
    assertEquals(zipped, List(("x", None), ("y", Some("hello")), ("z", Some(true))))
  }

  // --- Seq[Made] ---

  test("store multiple Made mirrors in Seq[Made]") {
    val mirrors: Seq[Made] = Seq(
      Made.derived[RAProduct],
      Made.derived[RAEnum],
      Made.derived[RAObject.type],
    )
    assertEquals(mirrors.size, 3)
  }

  test("Seq[Made] - elems.toList sizes") {
    val mirrors: Seq[Made] = Seq(
      Made.derived[RAProduct],
      Made.derived[RAEmpty],
      Made.derived[RAObject.type],
    )
    assertEquals(mirrors.map(_.elems.toList.size), Seq(3, 0, 0))
  }

  test("Seq[Made] - generatedElems.toList sizes") {
    val mirrors: Seq[Made] = Seq(
      Made.derived[RAProduct],
      Made.derived[RAWithGenerated],
    )
    assertEquals(mirrors.map(_.generatedElems.toList.size), Seq(0, 2))
  }

  // --- Runtime generated elem apply ---

  test("iterate generatedElems.toList and apply to instance") {
    val m = Made.derived[RAWithGenerated]
    val instance = RAWithGenerated("test")
    val genElems = m.generatedElems.toList.asInstanceOf[List[GeneratedMadeElem { type OuterType = RAWithGenerated }]]
    val results = genElems.map(_.apply(instance))
    assertEquals(results, List(4, "TEST"))
  }

  // --- Round-trip: defaults -> fromUnsafeArray ---

  test("round-trip: collect defaults and build instance") {
    val m = Made.derived[RAAllDefaults]
    val elems = m.elems.toList.asInstanceOf[List[MadeFieldElemWithDefault]]
    val defaults = elems.map(_.default)
    val instance = m.fromUnsafeArray(defaults.toArray)
    assertEquals(instance, RAAllDefaults())
  }

  // --- Transparent in Seq[Made] ---

  test("transparent mirror in Seq[Made]") {
    val m = Made.derived[RATransparent]
    val mirrors: Seq[Made] = Seq(m)
    assertEquals(mirrors.head.elems.toList.size, 1)
  }

case class RAProduct(x: Int, y: String, z: Boolean)
case class RAEmpty()
case object RAObject
case class RANamed(@name("renamed") a: Int, b: String)

class RAMarker extends MetaAnnotation
case class RAAnnotated(@RAMarker x: Int, y: String)

case class RAWithDefaults(x: Int, y: String = "hello", z: Boolean = true)
case class RAAllDefaults(a: Int = 1, b: String = "default")

case class RAWithGenerated(s: String):
  @generated def len: Int = s.length
  @generated def upper: String = s.toUpperCase

enum RAEnum:
  case A, B
  case C(value: Int)

@transparent
case class RATransparent(value: Int)

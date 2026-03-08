package made

import made.annotation.*

class RuntimeCollectionAccessTest extends munit.FunSuite:

  // --- elems.toList / toArray ---

  test("elems.toList returns correct field elems at runtime") {
    val m = Made.derived[RCProduct]
    val elems = m.elems.toList.asInstanceOf[List[MadeFieldElem]]
    assertEquals(elems.size, 3)
  }

  test("elems.toList preserves order") {
    val m = Made.derived[RCProduct]
    val labels = m.elemLabels.toList.asInstanceOf[List[String]]
    assertEquals(labels, List("x", "y", "z"))
  }

  test("generatedElems.toList at runtime") {
    val m = Made.derived[RCWithGenerated]
    val genElems = m.generatedElems.toList.asInstanceOf[List[GeneratedMadeElem]]
    assertEquals(genElems.size, 2)
  }

  test("elems.toList for enum subtypes") {
    val m = Made.derived[RCEnum]
    val elems = m.elems.toList.asInstanceOf[List[MadeElem]]
    assertEquals(elems.size, 3)
  }

  test("elems.toList for singleton is empty") {
    val m = Made.derived[RCObject.type]
    val elems = m.elems.toList
    assert(elems.isEmpty)
  }

  // --- Runtime iteration over defaults ---

  test("iterate elems.toList to collect defaults") {
    val m = Made.derived[RCWithDefaults]
    val elems = m.elems.toList.asInstanceOf[List[MadeFieldElem]]
    val defaults = elems.map(_.default)
    assertEquals(defaults, List(None, Some("hello"), Some(true)))
  }

  test("iterate elems.toList and zip with labels") {
    val m = Made.derived[RCWithDefaults]
    val labels = m.elemLabels.toList.asInstanceOf[List[String]]
    val elems = m.elems.toList.asInstanceOf[List[MadeFieldElem]]
    val zipped = labels.zip(elems.map(_.default))
    assertEquals(zipped, List(("x", None), ("y", Some("hello")), ("z", Some(true))))
  }

  // --- Runtime generated elem apply ---

  test("iterate generatedElems.toList and apply to instance") {
    val m = Made.derived[RCWithGenerated]
    val instance = RCWithGenerated("test")
    val genElems = m.generatedElems.toList.asInstanceOf[List[GeneratedMadeElem { type OuterType = RCWithGenerated }]]
    val results = genElems.map(_.apply(instance))
    assertEquals(results, List(4, "TEST"))
  }

  // --- Seq[Made] / storing mirrors in collections ---

  test("store multiple Made mirrors in Seq[Made]") {
    val mirrors: Seq[Made] = Seq(
      Made.derived[RCProduct],
      Made.derived[RCEnum],
      Made.derived[RCObject.type],
      Made.derived[RCWithGenerated],
    )
    assertEquals(mirrors.size, 4)
  }

  test("Seq[Made] - access label on each mirror") {
    val m1 = Made.derived[RCProduct]
    val m2 = Made.derived[RCObject.type]
    val m3 = Made.derived[RCEnum]

    assertEquals(m1.label, "RCProduct")
    assertEquals(m2.label, "RCObject")
    assertEquals(m3.label, "RCEnum")
  }

  test("Seq[Made] - access elems.toList on each mirror") {
    val mirrors: Seq[Made] = Seq(
      Made.derived[RCProduct],
      Made.derived[RCEmpty],
      Made.derived[RCObject.type],
    )
    val elemCounts = mirrors.map(_.elems.toList.size)
    assertEquals(elemCounts, Seq(3, 0, 0))
  }

  test("Seq[Made] - generatedElems.toList on each mirror") {
    val mirrors: Seq[Made] = Seq(
      Made.derived[RCProduct],
      Made.derived[RCWithGenerated],
    )
    val genCounts = mirrors.map(_.generatedElems.toList.size)
    assertEquals(genCounts, Seq(0, 2))
  }

  // --- Seq[Made.ProductOf] for products ---

  test("Seq of product mirrors - fromUnsafeArray at runtime") {
    val m1 = Made.derived[RCProduct]
    val m2 = Made.derived[RCEmpty]

    val r1 = m1.fromUnsafeArray(Array(1, "hi", true))
    assertEquals(r1, RCProduct(1, "hi", true))

    val r2 = m2.fromUnsafeArray(Array.empty)
    assertEquals(r2, RCEmpty())
  }

  // --- Collecting labels via toList from multiple mirrors ---

  test("collect all elemLabels across mirrors") {
    val m1 = Made.derived[RCProduct]
    val m2 = Made.derived[RCWithDefaults]

    val labels1 = m1.elemLabels.toList.asInstanceOf[List[String]]
    val labels2 = m2.elemLabels.toList.asInstanceOf[List[String]]
    val allLabels = labels1 ++ labels2
    assertEquals(allLabels, List("x", "y", "z", "x", "y", "z"))
  }

  // --- Sum: singleton values via toList ---

  test("sum elems.toList - access singleton values") {
    val m: Made.Sum {
      type Type = RCEnum
      type Label = "RCEnum"
      type Metadata = Meta
      type Elems = MadeSubSingletonElem {
        type Type = RCEnum.A.type
        type Label = "A"
        type Metadata = Meta
      } *: MadeSubSingletonElem {
        type Type = RCEnum.B.type
        type Label = "B"
        type Metadata = Meta
      } *: MadeSubElem {
        type Type = RCEnum.C
        type Label = "C"
        type Metadata = Meta
      } *: EmptyTuple
    } = Made.derived[RCEnum]

    val elems = m.elems.toList
    val singletons = elems.collect { case s: MadeSubSingletonElem => s.value }
    assertEquals(singletons.size, 2)
    assert(singletons.contains(RCEnum.A))
    assert(singletons.contains(RCEnum.B))
  }

  // --- elems.toArray ---

  test("elems.toArray works for product fields") {
    val m = Made.derived[RCProduct]
    val arr = m.elems.toArray
    assertEquals(arr.length, 3)
  }

  // --- Round-trip: toList defaults -> fromUnsafeArray ---

  test("round-trip: collect defaults and build instance") {
    val m = Made.derived[RCAllDefaults]
    val elems = m.elems.toList.asInstanceOf[List[MadeFieldElem]]
    val defaults = elems.map(_.default.get)
    val instance = m.fromUnsafeArray(defaults.toArray)
    assertEquals(instance, RCAllDefaults())
  }

  // --- Transparent mirror in Seq[Made] ---

  test("transparent mirror in Seq[Made]") {
    val m = Made.derived[RCTransparent]
    val mirrors: Seq[Made] = Seq(m)
    assertEquals(mirrors.head.elems.toList.size, 1)
  }

  test("transparent wrap/unwrap at runtime") {
    val m = Made.derived[RCTransparent]
    val wrapped = m.wrap(42)
    assertEquals(m.unwrap(wrapped), 42)
    assertEquals(wrapped, RCTransparent(42))
  }

// --- Fixtures ---

case class RCProduct(x: Int, y: String, z: Boolean)
case class RCEmpty()
case object RCObject

case class RCWithDefaults(x: Int, y: String = "hello", z: Boolean = true)
case class RCAllDefaults(a: Int = 1, b: String = "default")

case class RCWithGenerated(s: String):
  @generated def len: Int = s.length
  @generated def upper: String = s.toUpperCase

enum RCEnum:
  case A, B
  case C(value: Int)

@transparent
case class RCTransparent(value: Int)

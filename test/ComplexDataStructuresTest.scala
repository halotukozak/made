package made

import made.annotation.*

class ComplexDataStructuresTest extends munit.FunSuite:

  // --- Products with collection types ---

  test("case class with List field") {
    val m: Made.Product {
      type Type = WithList
      type Label = "WithList"
      type Metadata = Meta
      type Elems = MadeFieldElem {
        type Type = Int
        type Label = "id"
        type Metadata = Meta
      } *: MadeFieldElem {
        type Type = List[String]
        type Label = "items"
        type Metadata = Meta
      } *: EmptyTuple
    } = Made.derived[WithList]

    val id *: items *: EmptyTuple = m.elems
    assertEquals(id.label, "id")
    assertEquals(items.label, "items")
  }

  test("case class with Map field") {
    val _: Made.Product {
      type Type = WithMap
      type Label = "WithMap"
      type Metadata = Meta
      type Elems = MadeFieldElem {
        type Type = Map[String, Int]
        type Label = "data"
        type Metadata = Meta
      } *: EmptyTuple
    } = Made.derived[WithMap]
  }

  test("case class with Set and Vector fields") {
    val _: Made.Product {
      type Type = WithSetAndVector
      type Elems = MadeFieldElem {
        type Type = Set[String]
        type Label = "tags"
        type Metadata = Meta
      } *: MadeFieldElem {
        type Type = Vector[Double]
        type Label = "scores"
        type Metadata = Meta
      } *: EmptyTuple
    } = Made.derived[WithSetAndVector]
  }

  test("fromUnsafeArray with collection fields") {
    val m = Made.derived[WithList]
    val result = m.fromUnsafeArray(Array(1, List("a", "b")))
    assertEquals(result, WithList(1, List("a", "b")))
  }

  test("fromUnsafeArray with Map field") {
    val m = Made.derived[WithMap]
    val result = m.fromUnsafeArray(Array(Map("x" -> 1)))
    assertEquals(result, WithMap(Map("x" -> 1)))
  }

  // --- Nested case classes ---

  test("nested case class") {
    val _: Made.Product {
      type Type = CDSOuter
      type Elems = MadeFieldElem {
        type Type = CDSInner
        type Label = "inner"
        type Metadata = Meta
      } *: EmptyTuple
    } = Made.derived[CDSOuter]
  }

  test("fromUnsafeArray for nested case class") {
    val m = Made.derived[CDSOuter]
    val result = m.fromUnsafeArray(Array(CDSInner(List(1, 2))))
    assertEquals(result, CDSOuter(CDSInner(List(1, 2))))
  }

  test("case class with Option of collection") {
    val _: Made.Product {
      type Elems = MadeFieldElem {
        type Type = Option[List[String]]
        type Label = "items"
        type Metadata = Meta
      } *: EmptyTuple
    } = Made.derived[WithOptionalList]
  }

  test("case class with nested Option") {
    val _: Made.Product {
      type Elems = MadeFieldElem {
        type Type = Option[Option[Int]]
        type Label = "value"
        type Metadata = Meta
      } *: EmptyTuple
    } = Made.derived[NestedOption]
  }

  // --- Tuple fields ---

  test("case class with tuple field") {
    val _: Made.Product {
      type Elems = MadeFieldElem {
        type Type = (String, Int)
        type Label = "pair"
        type Metadata = Meta
      } *: EmptyTuple
    } = Made.derived[WithTuple]
  }

  test("fromUnsafeArray with tuple field") {
    val m = Made.derived[WithTuple]
    val result = m.fromUnsafeArray(Array(("hello", 42)))
    assertEquals(result, WithTuple(("hello", 42)))
  }

  // --- Either fields ---

  test("case class with Either field") {
    val _: Made.Product {
      type Elems = MadeFieldElem {
        type Type = Either[String, Int]
        type Label = "result"
        type Metadata = Meta
      } *: EmptyTuple
    } = Made.derived[WithEither]
  }

  // --- Complex enum / sealed trait hierarchies ---

  test("sealed trait with case classes having complex fields") {
    val m = Made.derived[CDSEvent]
    val created *: updated *: deleted *: EmptyTuple = m.elems
    assertEquals(created.label, "Created")
    assertEquals(updated.label, "Updated")
    assertEquals(deleted.label, "Deleted")
  }

  test("singleton subtype value in sealed trait") {
    val m: Made.Sum {
      type Type = CDSEvent
      type Label = "CDSEvent"
      type Metadata = Meta
      type Elems = MadeSubElem {
        type Type = CDSEvent.Created
        type Label = "Created"
        type Metadata = Meta
      } *: MadeSubElem {
        type Type = CDSEvent.Updated
        type Label = "Updated"
        type Metadata = Meta
      } *: MadeSubSingletonElem {
        type Type = CDSEvent.Deleted.type
        type Label = "Deleted"
        type Metadata = Meta
      } *: EmptyTuple
    } = Made.derived[CDSEvent]

    val _ *: _ *: deleted *: EmptyTuple = m.elems
    assert(deleted.value == CDSEvent.Deleted)
  }

  test("enum with parametric cases") {
    val m = Made.derived[CDSResponse]
    val ok *: err *: EmptyTuple = m.elems
    assertEquals(ok.label, "Ok")
    assertEquals(err.label, "Err")
  }

  // --- Many fields ---

  test("case class with many fields") {
    val m = Made.derived[ManyFields]
    assertEquals(m.label, "ManyFields")
    val a *: b *: c *: d *: e *: f *: EmptyTuple = m.elems
    assertEquals(a.label, "a")
    assertEquals(b.label, "b")
    assertEquals(c.label, "c")
    assertEquals(d.label, "d")
    assertEquals(e.label, "e")
    assertEquals(f.label, "f")
  }

  test("fromUnsafeArray for case class with many fields") {
    val m = Made.derived[ManyFields]
    val result = m.fromUnsafeArray(Array(1, "two", 3.0, true, 5L, List("six")))
    assertEquals(result, ManyFields(1, "two", 3.0, true, 5L, List("six")))
  }

  // --- Generic types with multiple type parameters ---

  test("case class with two type parameters") {
    val _: Made.Product {
      type Type = CDSPair[String, Int]
      type Label = "CDSPair"
      type Metadata = Meta
      type Elems = MadeFieldElem {
        type Type = String
        type Label = "first"
        type Metadata = Meta
      } *: MadeFieldElem {
        type Type = Int
        type Label = "second"
        type Metadata = Meta
      } *: EmptyTuple
    } = Made.derived[CDSPair[String, Int]]
  }

  test("case class with bounded type parameter") {
    val _: Made.Product {
      type Type = Bounded[List[Int]]
      type Label = "Bounded"
      type Metadata = Meta
      type Elems = MadeFieldElem {
        type Type = List[Int]
        type Label = "value"
        type Metadata = Meta
      } *: EmptyTuple
    } = Made.derived[Bounded[List[Int]]]
  }

  // --- Defaults with collection types ---

  test("defaults with collection types") {
    val m = Made.derived[WithCollectionDefaults]
    val xs *: ys *: EmptyTuple = m.elems
    assertEquals(xs.default, Some(List.empty[Int]))
    assertEquals(ys.default, Some(Map("key" -> "value")))
  }

  // --- @whenAbsent with complex types ---

  test("@whenAbsent with List default") {
    val m = Made.derived[WhenAbsentCollection]
    val items *: EmptyTuple = m.elems
    assertEquals(items.default, Some(List(1, 2, 3)))
  }

// --- Fixtures ---

case class WithList(id: Int, items: List[String])
case class WithMap(data: Map[String, Int])
case class WithSetAndVector(tags: Set[String], scores: Vector[Double])
case class CDSInner(values: List[Int])
case class CDSOuter(inner: CDSInner)
case class WithOptionalList(items: Option[List[String]])
case class NestedOption(value: Option[Option[Int]])
case class WithTuple(pair: (String, Int))
case class WithEither(result: Either[String, Int])

sealed trait CDSEvent
object CDSEvent:
  case class Created(name: String, tags: List[String]) extends CDSEvent
  case class Updated(fields: Map[String, String]) extends CDSEvent
  case object Deleted extends CDSEvent

enum CDSResponse:
  case Ok(data: Map[String, Any])
  case Err(code: Int, message: String)

case class ManyFields(a: Int, b: String, c: Double, d: Boolean, e: Long, f: List[String])

case class CDSPair[A, B](first: A, second: B)
case class Bounded[T <: Iterable[?]](value: T)

case class WithCollectionDefaults(xs: List[Int] = List.empty, ys: Map[String, String] = Map("key" -> "value"))
case class WhenAbsentCollection(@whenAbsent(List(1, 2, 3)) items: List[Int])

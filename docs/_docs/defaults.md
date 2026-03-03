---
title: Default Values
---

# Default Values

This guide explains how Made resolves default values for product fields during derivation. When you derive a type class
that constructs product instances from partial data - a JSON decoder, a config loader, a builder - you need to know
which fields have fallback values and what those values are. Made makes this available through `MadeFieldElem.default`,
a method on each field element that returns `Option[MirroredType]` resolved at compile time.

The default value for each field is determined by a three-level priority chain. The Made macro inspects annotations and
constructor signatures at compile time, selects the highest-priority source, and bakes the result into the field
element. At runtime, calling `.default` simply returns the pre-computed `Option` - there is no reflection or annotation
processing at runtime.

This guide assumes you have read the type class derivation guide and understand Made mirrors, `MadeFieldElem`, and the
`constValueTuple` pattern for label extraction.

## The Priority Chain

Made resolves default values using the following priority chain (first match wins):

1. `@whenAbsent(value)` - explicit annotation default (highest priority)
2. `@optionalParam` - uses the `Default[T]` type class to produce an empty value
3. Constructor default - Scala's standard default parameter value
4. `None` - no default available

The following type demonstrates all four levels in a single definition. The `host` field has no default. The `port`
field has both `@whenAbsent(8080)` and a constructor default of `0` - the annotation wins. The `timeout` field uses
`@optionalParam`, which summons `Default[Option[Int]]` to produce `None`. The `retries` field uses a plain constructor
default.

```scala
import made.*
import made.annotation.*
import scala.compiletime.*

case class Config(host: String, @whenAbsent(8080) port: Int = 0, @optionalParam timeout: Option[Int], retries: Int = 3)

val mirror = Made.derived[Config]
val (host, port, timeout, retries) = mirror.mirroredElems

assert(host.default.isEmpty)
assert(port.default.contains(8080))
assert(timeout.default.contains(None))
assert(retries.default.contains(3))
```

## @whenAbsent: Annotation Defaults

`@whenAbsent[T](v: => T)` provides an explicit default value with the highest priority in the resolution chain. The
value is by-name, meaning it is evaluated lazily each time `.default` is called on the corresponding `MadeFieldElem`.

When a field carries both `@whenAbsent` and a constructor default, the annotation value always wins. This is the
intended design: `@whenAbsent` exists precisely so that derivation authors can assign a different default than the one
used by direct construction.

```scala
import made.*
import made.annotation.*

case class WithWhenAbsent(@whenAbsent(42) a: Int = 0)

val mirror = Made.derived[WithWhenAbsent]
val elem *: EmptyTuple = mirror.mirroredElems

assert(elem.default.contains(42))
assert(WithWhenAbsent().a == 0)
```

The assertion shows that `elem.default` returns `Some(42)` (from the annotation), while `WithWhenAbsent()` constructs
with `a = 0` (the constructor default). The two values are intentionally different - the annotation controls what
derivation code sees, while the constructor default controls what direct callers get.

## whenAbsent.value: Keeping Values in Sync

When you want the annotation value and the constructor default to agree, the two values can easily diverge. If you write
`@whenAbsent(8080) port: Int = 8080` and later change one but forget the other, derivation code and direct construction
will silently produce different results.

The `whenAbsent.value` macro solves this. It reads the `@whenAbsent` annotation value at compile time and uses it as the
constructor default. Both paths - direct construction via `ServerConfig()` and derivation via
`MadeFieldElem.default` - produce the same value from a single source of truth.

```scala
import made.*
import made.annotation.*

case class ServerConfig(@whenAbsent(8080) port: Int = whenAbsent.value)

assert(ServerConfig().port == 8080)

val mirror = Made.derived[ServerConfig]
val elem *: EmptyTuple = mirror.mirroredElems

assert(elem.default.contains(8080))
```

## @optionalParam and Default

`@optionalParam` marks a field as optional. When Made encounters this annotation, it summons `Default[T]` at compile
time to produce the empty value for that field's type. This sits at priority level 2 - below `@whenAbsent` but above
constructor defaults.

The `Default[O]` type class is the extension point. It extends `() => O`, so calling a `Default` instance produces the
empty value. Made ships two built-in instances: `Default[Option[A]]` returns `None`, and `Default[A | Null]` returns
`null`.

```scala
import made.*
import made.annotation.*

case class Request(@optionalParam body: Option[String], @optionalParam header: String | Null, query: Option[String])

val mirror = Made.derived[Request]
val (body, header, query) = mirror.mirroredElems

assert(body.default.contains(None))
assert(header.default.contains(null: String | Null))
assert(query.default.isEmpty)
```

The `query` field has type `Option[String]` but no `@optionalParam` annotation and no constructor default, so its
`default` is `None` (the `Option` meaning "no default available", not the `Option[String]` value `None`).

To support your own optional types, provide a `Default` instance. The following example defines a `Fallback[A]` wrapper
and a `Default` instance for `Fallback[String]`, then uses `@optionalParam` on a field of that type.

```scala
import made.*
import made.annotation.*

case class Fallback[A](value: A)

given Default[Fallback[String]] = () => Fallback("N/A")

case class Settings(@optionalParam label: Fallback[String])

val mirror = Made.derived[Settings]
val elem *: EmptyTuple = mirror.mirroredElems

assert(elem.default.contains(Fallback("N/A")))
```

## Using Defaults in Derivation

The practical payoff of default values is enabling partial construction. The following example defines a `FromMap[T]`
type class that builds a `T` from a `Map[String, Any]`, falling back to `MadeFieldElem.default` when a key is missing.

The derivation function summons a `Made.Of[T]` mirror (which the `transparent inline given` resolves to `Made.Product`
at the expansion site), extracts field labels via `constValueTuple`, and iterates the map to build an array of field
values. When a key is absent, it uses `elem.default.getOrElse(throw ...)` to provide the fallback value.

```scala
import made.*
import scala.compiletime.*

trait FromMap[T]:
  def fromMap(map: Map[String, Any]): T

object FromMap:
  inline given derived[T](using m: Made.Of[T]): FromMap[T] = inline m match
    case given Made.ProductOf[T] => derivedProduct[T]
    case _ => compiletime.error("Cannot derive FromMap")

  inline private def derivedProduct[T](using m: Made.ProductOf[T]): FromMap[T] = source =>
    val labels = compiletime.constValueTuple[m.MirroredElemLabels].toList.asInstanceOf[List[String]]
    val elems = m.mirroredElems.toList.asInstanceOf[List[MadeFieldElem]]

    val values = labels
      .zip(elems)
      .map: (label, elem) =>
        source
          .get(label)
          .orElse(elem.default)
          .getOrElse(throw IllegalArgumentException(s"Missing key '$label' with no default"))

    m.fromUnsafeArray(values.toArray)


case class User(name: String, age: Int = 25, @optionalParam address: Option[String]) derives FromMap

val alice = summon[FromMap[User]].fromMap(Map("name" -> "Alice", "age" -> 30))
assert(alice == User("Alice", 30, None))

val bob = summon[FromMap[User]].fromMap(Map("name" -> "Bob", "address" -> Some("Cracow")))
assert(bob == User("Alice", 25, Some("Cracow")))
```

The `User` type has a constructor default of `25` for `age` and optional address param. When the map contains only
`"name"`, the derivation falls back to `elem.default` (which is `Some(25)` and `Some(None)`) and constructs
`User("Alice", 25)`. Without the default, calling`fromMap(Map("name" -> "Alice"))` would throw an
`IllegalArgumentException`.

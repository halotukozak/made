---
title: Deriving Type Classes with Made
---

# Deriving Type Classes with Made

This guide walks through deriving a `Show[T]` type class from scratch using Made mirrors. By the end, you will know how
to summon a Made mirror, iterate its runtime element objects, and wire up automatic derivation for product types, sum
types, singletons, and transparent wrappers.

The guide assumes familiarity with Scala 3's `scala.deriving.Mirror`. Where standard Mirror derivation relies on
compile-time `summonInline` over `MirroredElemTypes`, Made provides a different approach: runtime element objects
(`MadeFieldElem`, `MadeSubElem`, `MadeSubSingletonElem`) that carry labels, types, and metadata. This means your
derivation logic iterates a concrete tuple of element objects rather than recursing over type-level tuples.

We build incrementally: primitive instances first, then a derivation helper for each mirror kind (product, transparent,
sum, singleton), and finally the `inline def derived` dispatcher that ties them together.

## Domain Types

The examples throughout this guide use a small set of domain types. Product derivation uses `User`.

```scala
case class User(name: String, age: Int)
```

The transparent wrapper example uses `Email`, a single-field case class annotated with `@transparent` so that Made
derives a `Made.Transparent` mirror instead of `Made.Product`.

```scala
import made.annotation.transparent

@transparent
case class Email(value: String)
```

Sum type examples use `Shape` with mixed singleton and parameterised subtypes.

```scala
sealed trait Shape

case class Circle(radius: Double) extends Shape

case class Rectangle(width: Double, height: Double) extends Shape

case object Point extends Shape
```

## The Show Trait and Primitive Instances

`Show[T]` converts a value of type `T` to its string representation. The output format is constructor-style:
`User(name = Alice, age = 30)`.

Primitive instances handle the leaf types that product and sum derivation will recurse into.

```scala
trait Show[T]:
  def show(value: T): String

object Show:
  given Show[String] = (value: String) => value

  given Show[Int] = (value: Int) => value.toString

  given Show[Long] = (value: Long) => value.toString

  given Show[Double] = (value: Double) => value.toString

  given Show[Boolean] = (value: Boolean) => value.toString
```

## Product Derivation

Product derivation is the core Made pattern. Given a mirror for a product type `T`, you extract field labels and types
at compile time, then zip them with the product's field values at runtime to build the string representation.

The derivation function must be `inline` because extracting labels from Made's type-level `MirroredLabel` and
`MirroredElemLabels` requires `constValue` and `constValueTuple`, which only work in inline context. The mirror
parameter is typed as `Made.ProductOf[T]` - a type alias for `Made.Product { type MirroredType = T }`. By passing the
mirror explicitly, the compiler sees the fully refined type (including `MirroredLabel` and `MirroredElemLabels`) at the
inline expansion site.

The steps are:

1. Use `constValue[m.MirroredLabel]` to get the type name as a runtime string.
2. Use `constValueTuple[m.MirroredElemLabels].toList.asInstanceOf[List[String]]` to materialise field labels.
3. Use `compiletime.summonAll[Tuple.Map[m.MirroredElemTypes, Show]]` to resolve `Show` instances for each field at
   compile time - no manual instance list needed.
4. Use `value.productIterator.toList` to get field values.
5. Zip labels with values and field `Show` instances, format each triple as `label = value`, and join them inside the
   type name.

Given the imports `import made.*`, along with the `Show` trait and domain types defined above, the product derivation
function is:

```scala
inline def deriveProduct[T <: Product](m: Made.ProductOf[T]): Show[T] = value =>
  val typeName = compiletime.constValue[m.MirroredLabel]
  val labels = compiletime.constValueTuple[m.MirroredElemLabels].toList.asInstanceOf[List[String]]
  val values = value.productIterator.toList
  val fieldShows = compiletime.summonAll[Tuple.Map[m.MirroredElemTypes, Show]].toList.asInstanceOf[List[Show[Any]]]
  val fields = labels.lazyZip(values).lazyZip(fieldShows).map((label, value, s) => s"$label = ${s.show(value)}")

  s"$typeName(${fields.mkString(", ")})"
```

The call to `compiletime.summonAll` maps the type-level element tuple `m.MirroredElemTypes` to `Show` instances at
compile time. For `User`, this resolves `Show[String]` and `Show[Int]` from the primitive givens above. This eliminates
any need to manually list field instances - the compiler handles it.

The key Made-specific insight here is that `mirroredElems` gives you runtime `MadeFieldElem` objects. While this example
uses `constValueTuple` for labels (same as standard Mirror), the runtime element objects become essential when you need
metadata, defaults, or annotations - capabilities that standard Mirror lacks entirely.

## Transparent Mirrors

A `Made.Transparent` mirror wraps a single value. It is produced for case classes annotated with `@transparent` that
have exactly one constructor field. The mirror provides `unwrap` to extract the inner value and `wrap` to reconstruct
the wrapper.

For `Show`, a transparent type displays as `TypeName(innerValue)` - the wrapper name followed by the shown inner value.

Transparent derivation unwraps the value and delegates to the underlying type's `Show`. The function takes
`Made.TransparentOf[T]` as the mirror parameter. `compiletime.summonInline[Show[m.MirroredElemType]]` resolves the
`Show` instance for the single underlying type at compile time. Because the type is transparent, the wrapper name is
omitted - `Email("alice@example.com")` shows as `alice@example.com`, not `Email(alice@example.com)`.

```scala
inline def deriveTransparent[T](m: Made.TransparentOf[T]): Show[T] = value =>
  val underlyingShow = compiletime.summonInline[Show[m.MirroredElemType]]
  val inner = m.unwrap(value)
  underlyingShow.show(inner)
```

Transparent mirrors also carry a single-element `mirroredElems` tuple containing one `MadeFieldElem`, so you could
iterate it the same way as a product. However, `unwrap`/`wrap` is the idiomatic approach - it makes the single-field
semantics explicit and avoids the overhead of tuple iteration for what is always exactly one element.

## Sum Type Derivation

Sum type derivation handles sealed traits and enums. Where product derivation iterates field elements (`MadeFieldElem`),
sum derivation dispatches on the runtime type of the value to find the correct subtype `Show` instance.

The function below takes a `Made.SumOf[T]` mirror and uses `compiletime.summonAll` to resolve both `ClassTag` and
`Show` instances for every subtype at compile time. At runtime, it zips the two lists and finds the first `ClassTag`
whose `unapply` matches the value - this handles both singleton subtypes (case objects) and parameterised subtypes
(case classes) uniformly.

```scala
import scala.reflect.ClassTag

inline def deriveSum[T](m: Made.SumOf[T]): Show[T] = value =>
  val subtypeClasses = compiletime.summonAll[Tuple.Map[m.MirroredElemTypes, ClassTag]].toList.asInstanceOf[List[ClassTag[?]]]
  val subtypeShows = compiletime.summonAll[Tuple.Map[m.MirroredElemTypes, Show]].toList.asInstanceOf[List[Show[Any]]]

  subtypeClasses
    .lazyZip(subtypeShows)
    .collectFirst:
      case (clazz, s) if clazz.unapply(value).isDefined => s.show(value)
    .getOrElse(throw IllegalStateException("Unable to find subtype"))
```

The `ClassTag.unapply` check performs a runtime type test: for `Point`, the `ClassTag[Point.type]` matches; for
`Circle(3.14)`, the `ClassTag[Circle]` matches. Once the matching subtype is found, its `Show` instance formats the
value. The `compiletime.summonAll` calls resolve instances for all subtypes at compile time - for `Shape`, this means
`ClassTag` and `Show` for `Circle`, `Rectangle`, and `Point.type` must all be in scope.

## Singleton Mirrors

`Made.Singleton` is produced for standalone objects and `Unit`. Its `mirroredElems` is `EmptyTuple` - there are no
fields or subtypes to iterate. The singleton instance is available via `value`.

For `Show`, a singleton simply outputs its type label. Sum derivation already handles singletons via `ClassTag`
matching, but standalone singleton mirrors let you extract the label directly.

```scala
inline def deriveSingleton[T](m: Made.SingletonOf[T]): Show[T] = _ => compiletime.constValue[m.MirroredLabel]
```

In practice, standalone singleton derivation is rare. Most singletons appear as sum type subtypes, where the `ClassTag`
dispatch in `deriveSum` handles them automatically.

## Auto-Derivation: `inline def derived`

The derivation helpers above each handle one mirror kind. The `inline def derived` method ties them together by
pattern-matching on the Made mirror to determine which path to take: `Made.ProductOf[T]` for case classes,
`Made.SumOf[T]` for sealed traits and enums, `Made.SingletonOf[T]` for objects and `Unit`, and `Made.TransparentOf[T]`
for `@transparent` wrappers. Because `Made.derived` is a `transparent inline given`, the compiler resolves the exact
mirror subtype at the inline expansion site, so these pattern matches are exhaustive at compile time.

The `derives` clause on a type definition triggers this: writing `case class User(...) derives Show` causes the compiler
to look for `Show.derived`, passing the `Made.Of[User]` instance as the using parameter.

```scala
inline def derived[T](using m: Made.Of[T]): Show[T] = inline m match
  case m: Made.ProductOf[T & Product] => deriveProduct(m).asInstanceOf[Show[T]]
  case m: Made.SumOf[T] => deriveSum(m)
  case m: Made.SingletonOf[T] => deriveSingleton(m)
  case m: Made.TransparentOf[T] => deriveTransparent(m)
```

The product branch uses `T & Product` as the type bound because `Made.ProductOf[T]` requires its `T` to extend
`Product`. The `.asInstanceOf[Show[T]]` cast bridges the `Show[T & Product]` return type back to `Show[T]`.

With this dispatcher in place, calling `Show.derived[User]` passes `Made.Of[User]` (which is a `Made.ProductOf[T]` at
runtime) and the `inline match` routes to `deriveProduct`.

```scala
given Show[Circle] = Show.derived[Circle]
given Show[Rectangle] = Show.derived[Rectangle]
given Show[Point.type] = Show.derived[Point.type]

val userShow = Show.derived[User]
assert(userShow.show(User("Alice", 30)) == "User(name = Alice, age = 30)")

val emailShow = Show.derived[Email]
assert(emailShow.show(Email("alice@example.com")) == "alice@example.com")

val shapeShow: Show[Shape] = Show.derived[Shape]
assert(shapeShow.show(Point) == "Point")
assert(shapeShow.show(Circle(3.14)) == "Circle(radius = 3.14)")
assert(shapeShow.show(Rectangle(2.0, 5.0)) == "Rectangle(width = 2.0, height = 5.0)")
```

Note that sum derivation requires `Show` instances for each subtype to be in scope before deriving the sum itself.
The `given` declarations for `Circle`, `Rectangle`, and `Point.type` provide these.

```scala 3
val userShow = Show.derived[User]
val emailShow = Show.derived[Email]
val shapeShow: Show[Shape] = Show.derived[Shape]
val originShow = Show.derived[Origin.type]

println(userShow.show(User("Alice", 30)))
println(emailShow.show(Email("alice@example.com")))
println(shapeShow.show(Point))
println(shapeShow.show(Circle(3.14)))
println(shapeShow.show(Rectangle(2.0, 5.0)))
println(originShow.show(Origin))
```

Expected output:

```
User(name = Alice, age = 30)
alice@example.com
Point
Circle(radius = 3.14)
Rectangle(width = 2.0, height = 5.0)
Origin
```

To adapt this for a different type class, replace the `Show` trait and primitive instances with your type class, keep
the same `inline def derived` structure with its four-way mirror match, and adjust the per-branch logic. The product
branch iterates field labels and values. The sum branch dispatches on subtype classes. The singleton branch returns a
fixed value. The transparent branch delegates to the inner type.

This guide covers the fundamentals. A production-ready derivation would also handle e.g. recursive types (e.g.,
`Tree` with `Branch(left: Tree, right: Tree)`), collection fields like `List[T]` or `Option[T]`, and caching of
derived instances to avoid redundant inline expansions.
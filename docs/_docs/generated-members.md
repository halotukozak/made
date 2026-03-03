---
title: Generated Members
---

# Generated Members

This guide explains how Made exposes non-constructor members - vals and defs annotated with `@generated` - through the
`GeneratedMadeElem` type and the `generatedElems` tuple. Standard `scala.deriving.Mirror` has no concept of
non-constructor members. Made's `@generated` annotation marks vals and defs for inclusion in the mirror, making computed
and derived properties visible to derivation code alongside constructor fields.

This matters for schema generators, API spec generators, documentation generators, and serializers that need to include
computed fields. A JSON schema generator, for example, may want to expose a `fullName` computed from `firstName` and
`lastName` as a read-only property in the schema output. With Made, the derivation can discover and access that computed
member through the same mirror it uses for constructor fields.

This guide assumes you have read the type class derivation guide and understand Made mirrors, `MadeFieldElem`, and the
`mirroredElems` tuple.

## The Two-Tuple Model

Made keeps constructor elements and generated members in separate tuples. The `mirroredElems` method returns a tuple of
`MadeFieldElem` (for products) or `MadeSubElem`/`MadeSubSingletonElem` (for sums), representing constructor parameters
or subtypes. The `generatedElems` method returns a tuple of `GeneratedMadeElem`, representing vals and defs annotated
with `@generated`.

This separation is intentional. Generated members cannot participate in product construction (`fromUnsafeArray`) because
they are computed from an instance, not stored as constructor parameters. Keeping them in a separate tuple means
existing derivation code - like `Show[T]` from the type class derivation guide - continues to work without
modification. It simply never sees generated members.

The following example defines a `Measurement` type with two constructor fields and one generated def, then demonstrates
accessing both tuples.

```scala
import made.*
import made.annotation.*

case class Measurement(value: Double, unit: String):
  @generated def display: String = s"$value $unit"

val mirror = Made.derived[Measurement]
val (valueFld, unitFld) = mirror.mirroredElems
val displayGen *: EmptyTuple = mirror.generatedElems

val m = Measurement(9.81, "m/s")
assert(displayGen(m) == "9.81 m/s")
```

The `mirroredElems` tuple contains elements for `value` and `unit` - the constructor fields. The `generatedElems` tuple
contains a single element for `display` - the `@generated` def. Derivation code that iterates `mirroredElems` will
never encounter `display`.

## GeneratedMadeElem API

`GeneratedMadeElem` extends `MadeFieldElem` (which extends `MadeElem`). It inherits the standard type members:
`MirroredType` is the return type of the generated member, `MirroredLabel` is its name, and `Metadata` is the annotation
chain (which always includes `@generated` since the annotation itself extends `MetaAnnotation`).

`GeneratedMadeElem` adds one unique type member and one unique method.

`type OuterMirroredType` is the type that declares the `@generated` member. For a generated def on
`case class Prod(...)`, `OuterMirroredType` is `Prod`.

`def apply(outer: OuterMirroredType): MirroredType` computes the generated value from an instance. This is the only way
to obtain the value of a generated member through the mirror. You call `apply` with an instance of the declaring type,
and it returns the computed result.

The `default` method inherited from `MadeFieldElem` always returns `None` for generated members. Generated members are
not constructor parameters and have no defaults. The value is always computed via `apply`, never via `default`.

The companion object provides two type aliases: `GeneratedMadeElem.Of[T]` refines `MirroredType` to `T`, and
`GeneratedMadeElem.OuterOf[Outer]` refines `OuterMirroredType` to `Outer`.

The following example examines the API on a type with two generated members.

```scala
import made.*
import made.annotation.*

case class Prod(a: Int, b: String):
  @generated def ab: String = s"$a-$b"

  @generated def len: Int = b.length

val mirror = Made.derived[Prod]
val gAb *: gLen *: EmptyTuple = mirror.generatedElems

val p = Prod(2, "x")
assert(gAb(p) == "2-x")
assert(gLen(p) == 1)
assert(gAb.default.isEmpty)
assert(gLen.default.isEmpty)
```

Calling `gAb(p)` invokes the generated def `ab` on `p`, producing `"2-x"`. Calling `gAb.default` returns `None` because
generated members have no constructor defaults.

## Supported Mirror Types

The `@generated` annotation is supported on all mirror types except `Made.Transparent`.

**Made.Product** supports `@generated`. This is the primary use case: case classes with computed vals and defs.

**Made.Sum** supports `@generated`. The annotation goes on the sealed trait or enum itself. The `apply` method takes an
instance of the sum type, so the generated value is computed polymorphically across all subtypes. Subtype-level
generated members belong to the subtype's own Made mirror, not the sum mirror.

**Made.Singleton** supports `@generated`. Objects with computed vals and defs expose them through `generatedElems`.

**Made.Transparent** does **not** support `@generated`. Attempting to use `@generated` on a transparent mirror type
produces a compile error.

The following example demonstrates generated members on a sealed trait sum type.

```scala
import made.*
import made.annotation.*

sealed trait Shape:
  @generated val description: String = "a shape"

case class Circle(radius: Double) extends Shape

case class Square(side: Double) extends Shape

val mirror = Made.derived[Shape]
val desc *: EmptyTuple = mirror.generatedElems
assert(desc(Circle(1.0)) == "a shape")
assert(desc(Square(2.0)) == "a shape")
```

The `description` val is declared on `Shape` and annotated with `@generated`. The sum mirror's `generatedElems` includes
it. Calling `desc(Circle(1.0))` computes the value through an instance of the sum type.

## Practical Example: Describe

This section builds a `Describe[T]` type class that produces a string representation of an instance, listing both
constructor fields and generated members with their labels and computed values. This demonstrates the unique value of
`generatedElems`: exposing non-constructor members that standard `Mirror` cannot see.

The type class has a single method that takes an instance and returns a formatted description.

```scala
import made.*
import made.annotation.*

case class SensorReading(id: String, value: Double):
  @generated def summary: String = s"$id=$value"
  @generated def isValid: Boolean = value >= 0

trait Describe[T]:
  def apply(instance: T): String

object Describe:
  inline given derived[T](using mirror: Made.Of[T]): Describe[T] = inline mirror match
    case given Made.ProductOf[T] => derivedProduct[T]
    case _ => compiletime.error("some error")

  inline private def derivedProduct[T](using mirror: Made.ProductOf[T]): Describe[T] = instance =>
    val fieldLabels = compiletime
      .constValueTuple[mirror.MirroredElemLabels]
      .toList
      .asInstanceOf[List[String]]

    val genElems = mirror.generatedElems.toList
    val fieldValues = instance.asInstanceOf[Product].productIterator.toList
    val fieldEntries = fieldLabels.zip(fieldValues).map((label, value) => s"$label = $value")
    val genEntries = genElems.map: elem =>
      val gen = elem.asInstanceOf[GeneratedMadeElem.OuterOf[T]]
      gen(instance)

    val typeName = compiletime.constValue[mirror.MirroredLabel]

    (fieldEntries ++ genEntries).mkString(s"$typeName(\n", ",\n", "\n)")

val describe = Describe.derived[SensorReading]
val output = describe(SensorReading("temp", 23.5))
assert(output.contains("id = temp"))
assert(output.contains("value = 23.5"))
assert(output.contains("temp=23.5"))
assert(output.contains("true"))
```

The derivation iterates `mirroredElems` labels via `constValueTuple[mirror.MirroredElemLabels]` for constructor field
names, then iterates `generatedElems.toList` for generated members. Each generated element is cast to
`GeneratedMadeElem { type OuterMirroredType = T }` so that `apply(instance)` compiles with the correct outer type. The
result includes both constructor fields and computed values.

A fully generic version that also extracts generated member labels at compile time would use inline recursion over the
`GeneratedElems` tuple type, applying the same `Tuple.Map` and `constValueTuple` techniques used for
`MirroredElemLabels`. For the purposes of this guide, the example above demonstrates the key pattern: accessing both
tuples and calling `apply` on generated elements.

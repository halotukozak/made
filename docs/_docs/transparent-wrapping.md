---
title: Transparent Wrapping
order: 2
---

# Transparent Wrapping

This guide explains how Made handles transparent wrapper types - single-field case classes annotated with `@transparent`
that act as newtypes around an underlying value. Made derives a `Made.Transparent` mirror for these types instead of the
usual `Made.Product`, providing `unwrap` and `wrap` methods for bidirectional conversion between the wrapper and its
inner type.

Transparent wrappers are common in domain modelling. Types like `Email(value: String)`, `UserId(value: Long)`, or
`Temperature(celsius: Double)` add type safety without changing the underlying representation. When deriving type classes
for these types, you typically want to delegate to the inner type rather than treating them as single-field products.
Made's transparent mirror makes this delegation explicit.

This guide assumes familiarity with Made mirrors and the type class derivation pattern from the [Show derivation guide](deriving-show.md).

## Defining a Transparent Type

A transparent type is a case class with exactly one constructor field, annotated with `@transparent`. The annotation
tells Made to produce a `Made.Transparent` mirror instead of `Made.Product`.

```scala
import made.annotation.transparent

@transparent
case class Email(value: String)

@transparent
case class UserId(value: Long)
```

Without `@transparent`, these would derive as `Made.Product` with a single `MadeFieldElem`. With the annotation, they
derive as `Made.Transparent` with `unwrap`/`wrap` methods that directly access the inner value.

## The Made.Transparent Mirror

Calling `Made.derived` on a `@transparent` type returns a `Made.Transparent` mirror. The mirror provides:

- `type ElemType` - the single wrapped field's type
- `def unwrap(value: Type): ElemType` - extracts the inner value
- `def wrap(value: ElemType): Type` - constructs the wrapper from an inner value
- `type Label` - the type name as a string literal type
- `type Metadata` - the annotation chain, which includes `@transparent`
- `def elems` - a single-element tuple containing one `MadeFieldElem`

The `GeneratedElems` type is fixed to `EmptyTuple`. `@generated` members are not supported on transparent types and
will cause a compile error.

```scala
import made.*
import made.annotation.transparent

@transparent
case class Email(value: String)

val mirror = Made.derived[Email]
val email = Email("alice@example.com")

assert(mirror.unwrap(email) == "alice@example.com")
assert(mirror.wrap("bob@example.com") == Email("bob@example.com"))
```

The `unwrap`/`wrap` pair is the idiomatic way to work with transparent mirrors. While `elems` is available and
contains a single `MadeFieldElem`, the direct methods make the single-field semantics explicit and avoid tuple
iteration overhead.

## TransparentWrapping

The `TransparentWrapping[R, T]` trait is the underlying mechanism that powers `Made.Transparent`. It provides two
methods: `wrap(r: R): T` and `unwrap(t: T): R`, where `R` is the field type and `T` is the wrapper type.

`TransparentWrapping.derived[R, T]` generates the implementation at compile time using a Scala 3 macro. The macro
inspects the case class structure, finds the single field, and emits code that calls the constructor (for `wrap`) and
the field accessor (for `unwrap`). This is what the `Made.Transparent` mirror delegates to internally.

You do not need to use `TransparentWrapping` directly in most cases. The `Made.Transparent` mirror wraps it and
provides the same `unwrap`/`wrap` methods with the correct type refinements. `TransparentWrapping` exists as a
separate trait so that the bidirectional conversion logic is isolated from the mirror infrastructure.

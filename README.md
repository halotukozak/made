# M&DE

**Mirror Annotations & Default Extraction** — a Scala 3 macro library that extends `scala.deriving.Mirror` with
annotation metadata, default values, generated members, and transparent wrapper support.

## Overview

Scala 3's built-in `Mirror` provides basic type-level information about case classes and enums, but it stops short of
exposing annotations, default values, or computed members. M&DE fills that gap — it derives enriched mirrors at
compile time that carry:

- **Type-level annotation metadata** — custom annotations on types and fields, queryable at both type level and runtime
- **Default value extraction** — from constructor defaults, `@whenAbsent` annotations, and `@optionalParam` markers
- **Generated members** — `@generated` vals and defs exposed as first-class elements of the mirror
- **Transparent wrappers** — `@transparent` single-field case classes with compile-time wrap/unwrap
- **Custom labels** — `@name` to override the label of a type or field

Made supports case classes, enums, sealed traits, objects, value classes, and higher-kinded types.

## Acknowledgements

M&DE is inspired by:

- [**AVSystem commons**](https://github.com/AVSystem/scala-commons) by [**ghik**](https://github.com/ghik)
- [**ops-mirror**](https://github.com/bishabosha/ops-mirror) by [**bishabosha**](https://github.com/bishabosha)

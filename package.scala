package made

/**
 * Extended mirrors for Scala types, adding annotation metadata, element-level detail,
 * and generated member support on top of standard `scala.deriving.Mirror`.
 *
 * === Mirror Hierarchy ===
 *
 * [[Made]] is the root sealed trait. Compile-time derivation via [[Made.derived]]
 * produces one of four subtypes:
 *
 *  - [[Made.Product]] -- for case classes and value classes. Provides
 *    `fromUnsafeArray` for construction. `MirroredElems` is a tuple of
 *    [[MadeFieldElem]], and `GeneratedElems` carries any `@generated` members.
 *  - [[Made.Sum]] -- for sealed traits and enums. `MirroredElems` is a tuple of
 *    [[MadeSubElem]] (non-singleton subtypes) and [[MadeSubSingletonElem]]
 *    (singleton subtypes such as case objects and parameterless enum cases).
 *  - [[Made.Singleton]] -- for objects, `Unit`, and singleton types. Provides
 *    `value` returning the singleton instance. `MirroredElems` is always
 *    `EmptyTuple`.
 *  - [[Made.Transparent]] -- for single-field case classes annotated with
 *    `@transparent`. Provides `wrap`/`unwrap` for the single field.
 *    `GeneratedElems` is always `EmptyTuple` (`@generated` members are not
 *    supported on transparent types).
 *
 * === Mirror Type Decision Tree ===
 *
 * `Made.derived[T]` selects the mirror subtype using this priority
 * (first match wins):
 *
 *  1. Singleton type, object, or `Unit` -> [[Made.Singleton]]
 *  2. Annotated with `@transparent` -> [[Made.Transparent]]
 *     (must have exactly one constructor field, no `@generated`)
 *  3. Value class (extends `AnyVal`) -> [[Made.Product]]
 *  4. Has `Mirror.ProductOf[T]` -> [[Made.Product]]
 *  5. Has `Mirror.SumOf[T]` -> [[Made.Sum]]
 *
 * === Element Hierarchy ===
 *
 * [[MadeElem]] is the root sealed trait for elements within a mirror's
 * `MirroredElems` tuple. Each element carries a `MirroredType`,
 * `MirroredLabel`, and `Metadata`. The concrete subtypes are:
 *
 *  - [[MadeFieldElem]] -- a constructor parameter in a product type.
 *    Provides `default` for default value resolution with the priority
 *    chain: `@whenAbsent` > `@optionalParam` > constructor default.
 *  - [[MadeSubElem]] -- a non-singleton subtype in a sum type.
 *  - [[MadeSubSingletonElem]] -- extends [[MadeSubElem]], a singleton
 *    subtype (case object or parameterless enum case). Provides `value`
 *    returning the singleton instance.
 *  - [[GeneratedMadeElem]] -- extends [[MadeFieldElem]], represents a
 *    `@generated` val or def. Provides `apply(outer)` to compute its
 *    value from an instance of the declaring type. Lives in
 *    `GeneratedElems`, not `MirroredElems`.
 *
 * === Metadata ===
 *
 * `Metadata` is a type member (`type Metadata <: Meta`) present on both
 * [[Made]] and [[MadeElem]]. It carries annotations as an `AnnotatedType`
 * chain: when annotations extending `MetaAnnotation` are present on the
 * annotated element, `Metadata` becomes `Meta @Annotation1 @Annotation2 ...`.
 * When no annotations are present, `Metadata = Meta`.
 *
 * At the mirror level, query annotations using the `hasAnnotation[A]` and
 * `getAnnotation[A]` extension methods defined in `MadeAnnotation`.
 * Element-level `Metadata` is accessible as a type member on each
 * [[MadeElem]] but has no convenience query methods.
 *
 * @see [[Made]]
 * @see [[MadeElem]]
 * @see [[Made.Product]]
 * @see [[Made.Sum]]
 * @see [[Made.Singleton]]
 * @see [[Made.Transparent]]
 * @see [[MadeFieldElem]]
 * @see [[MadeSubElem]]
 * @see [[MadeSubSingletonElem]]
 * @see [[GeneratedMadeElem]]
 */
package object `package`

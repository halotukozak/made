package made.annotation

/**
 * Annotation system for Made mirrors.
 *
 * All Made annotations extend [[MetaAnnotation]], which itself extends
 * `scala.annotation.RefiningAnnotation`. Because they are refining annotations,
 * they refine the type of the annotated element and are preserved in the
 * `Metadata` type member during `Made.derived`.
 *
 * === MetaAnnotation ===
 *
 * [[MetaAnnotation]] is the base class for Made annotations. Annotations
 * extending `MetaAnnotation` are automatically captured in the `Metadata`
 * type member as an `AnnotatedType` chain and can be queried at runtime
 * via `hasAnnotation[A]` and `getAnnotation[A]` on a `Made` instance.
 *
 * === Annotations ===
 *
 *  - [[generated]] -- marks a `val` or `def` to include in
 *    `Made.GeneratedElems`
 *  - [[name]] -- overrides the `MirroredLabel` for a type or field
 *  - [[optionalParam]] -- marks a field to use `OptionLike.none` as its
 *    default value
 *  - [[transparent]] -- marks a single-field case class for
 *    `Made.Transparent` derivation
 *  - [[whenAbsent]] -- provides an explicit default value with highest
 *    priority in the default resolution chain
 *
 * Note that [[whenAbsent]] extends `RefiningAnnotation` directly, NOT
 * `MetaAnnotation`. This means it provides default values but is NOT
 * captured in `Metadata` and cannot be queried via `hasAnnotation` or
 * `getAnnotation`.
 *
 * @see [[MetaAnnotation]]
 * @see [[generated]]
 * @see [[name]]
 * @see [[optionalParam]]
 * @see [[transparent]]
 * @see [[whenAbsent]]
 * @see [[made.Made]]
 */
package object `package`

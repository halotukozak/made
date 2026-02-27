package made.annotation

/**
 * Annotation system for Made mirrors.
 *
 * Annotations extending [[MetaAnnotation]] are refining annotations: they refine the
 * type of the annotated element and are captured in the `Metadata` type member during
 * `Made.derived`. Query them at runtime via `hasAnnotation[A]` and `getAnnotation[A]`
 * on a `Made` instance.
 *
 * The exception is [[whenAbsent]], which extends `RefiningAnnotation` directly and
 * is NOT captured in `Metadata` — it only provides default values.
 *
 * @see [[MetaAnnotation]]
 * @see [[made.Made]]
 */
package object `package`

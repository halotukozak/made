package made

/**
 * Annotation system for Made mirrors.
 *
 * Annotations extending [[made.annotation.MetaAnnotation]] are refining annotations: they refine the
 * type of the annotated element and are captured in the `Metadata` type member during
 * `Made.derived`. Query them at runtime via `hasAnnotation[A]` and `getAnnotation[A]`
 * on a `Made` instance.
 *
 * @see [[made.annotation.MetaAnnotation]]
 * @see [[made.Made]]
 */
package object annotation

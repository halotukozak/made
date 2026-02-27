package made.annotation

import scala.annotation.RefiningAnnotation

/**
 * Base class for all Made meta-annotations.
 *
 * Extends `RefiningAnnotation` so that annotations refine the type of the
 * annotated element. Annotations extending this class are automatically
 * captured in the `Metadata` type member during `Made.derived` and can be
 * queried at runtime via `hasAnnotation[A]` and `getAnnotation[A]`
 * extension methods on a `Made` instance.
 *
 * Custom annotations should extend this class to participate in the
 * Made metadata system.
 *
 * @see [[made.Made]]
 * @see [[generated]]
 * @see [[name]]
 * @see [[optionalParam]]
 * @see [[transparent]]
 */
open class MetaAnnotation extends RefiningAnnotation

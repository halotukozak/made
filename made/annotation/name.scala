package made.annotation
/**
 * Overrides the `MirroredLabel` type member for the annotated type or field.
 *
 * The `name` parameter becomes the compile-time string literal used as
 * `MirroredLabel`. Can be applied to both types (overrides
 * `Made.MirroredLabel`) and fields (overrides `MadeElem.MirroredLabel`).
 *
 * @param name the label override
 * @see [[made.Made]]
 * @see [[made.MadeElem]]
 * @see [[MetaAnnotation]]
 */
class name(val name: String) extends MetaAnnotation

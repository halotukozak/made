package made.annotation

/**
 * Marks a `val` or `def` to be included in `Made.GeneratedElems`.
 *
 * The annotated member becomes a [[made.GeneratedMadeElem]] with an
 * `apply(outer)` method that computes its value from an instance of the
 * declaring type. Only valid on vals and defs, not on constructor
 * parameters or other members.
 *
 * @see [[made.GeneratedMadeElem]]
 * @see [[MetaAnnotation]]
 */
class generated extends MetaAnnotation

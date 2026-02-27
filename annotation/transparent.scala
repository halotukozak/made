package made.annotation

/**
 * Marks a case class for `Made.Transparent` derivation instead of `Made.Product`.
 *
 * The case class must have exactly one constructor field. `@generated`
 * members are not supported on `@transparent` types and will cause a
 * compile error. The resulting mirror provides `wrap`/`unwrap` methods
 * for the single field.
 *
 * @see [[made.Made.Transparent]]
 * @see [[made.TransparentWrapping]]
 * @see [[MetaAnnotation]]
 */
class transparent extends MetaAnnotation

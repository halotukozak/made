package made.annotation

/**
 * Marks a field to use `Default[T].apply()` as its default value.
 *
 * Requires a `Default` instance for the field's type to be in implicit
 * scope. In the default resolution priority chain:
 * `@whenAbsent` > `@optionalParam` > constructor default.
 *
 * @see [[made.Default]]
 * @see [[whenAbsent]]
 * @see [[MetaAnnotation]]
 */
class optionalParam extends MetaAnnotation

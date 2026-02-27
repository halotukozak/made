package made.annotation

/**
 * Marks a field to use `OptionLike.none` as its default value.
 *
 * Requires an `OptionLike` instance for the field's type to be in implicit
 * scope. In the default resolution priority chain:
 * `@whenAbsent` > `@optionalParam` > constructor default.
 *
 * @see [[made.OptionLike]]
 * @see [[whenAbsent]]
 * @see [[MetaAnnotation]]
 */
class optionalParam extends MetaAnnotation

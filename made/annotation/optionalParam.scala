package made.annotation

/**
 * Marks a field to use `OptionLike.none` as its default value.
 *
 * Requires an `OptionLike` instance for the field's type to be in implicit
 * scope. In the default resolution priority chain:
 * `@whenAbsent` > `@optionalParam` > constructor default.
 *
 * @see [[made.Default]]
 * @see [[whenAbsent]]
 * @see [[MetaAnnotation]]
 */
//todo fix docs
class optionalParam extends MetaAnnotation

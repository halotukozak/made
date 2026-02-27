/**
 * Extended mirrors for Scala types, adding annotation metadata, element-level detail,
 * and generated member support on top of standard `scala.deriving.Mirror`.
 *
 * Derive a mirror with `Made.derived[T]`. The resulting mirror subtype depends on `T`:
 * singletons, transparent wrappers, products (case classes), or sums (sealed traits/enums).
 *
 * Each mirror carries a `MirroredElems` tuple of [[made.MadeElem]] subtypes describing
 * constructor fields or sum subtypes, and a `Metadata` type member encoding
 * annotations as an `AnnotatedType` chain around [[made.Meta]].
 *
 * @see [[made.Made]]
 * @see [[made.MadeElem]]
 */
package object made

package made

import made.annotation.*
import scala.compiletime.testing.typeCheckErrors

class DuplicateNameTest extends munit.FunSuite:

  test("product with duplicate @name on fields should not compile") {
    val errors = typeCheckErrors("Made.derived[DuplicateNameProduct]")
    assert(errors.exists(_.message.contains("have the same @name")))
  }

  test("sum with duplicate @name on subtypes should not compile") {
    val errors = typeCheckErrors("Made.derived[DuplicateNameSum]")
    assert(errors.exists(_.message.contains("have the same @name")))
  }

  test("product where @name collides with another field's natural name should not compile") {
    val errors = typeCheckErrors("Made.derived[NameCollidesWithFieldProduct]")
    assert(errors.exists(_.message.contains("have the same @name")))
  }

  test("sum where @name collides with another subtype's natural name should not compile") {
    val errors = typeCheckErrors("Made.derived[NameCollidesWithSubtypeSum]")
    assert(errors.exists(_.message.contains("have the same @name")))
  }

  test("product with distinct @name annotations should compile") {
    val errors = typeCheckErrors("Made.derived[DistinctNameProduct]")
    assert(errors.isEmpty, s"Expected no errors but got: $errors")
  }

  test("sum with distinct @name annotations should compile") {
    val errors = typeCheckErrors("Made.derived[DistinctNameSum]")
    assert(errors.isEmpty, s"Expected no errors but got: $errors")
  }

// --- test fixtures ---

case class DuplicateNameProduct(@name("x") a: String, @name("x") b: Int)

sealed trait DuplicateNameSum
object DuplicateNameSum:
  @name("X") case class A(v: Int) extends DuplicateNameSum
  @name("X") case class B(v: String) extends DuplicateNameSum

case class NameCollidesWithFieldProduct(@name("b") a: String, b: Int)

sealed trait NameCollidesWithSubtypeSum
object NameCollidesWithSubtypeSum:
  @name("B") case class A(v: Int) extends NameCollidesWithSubtypeSum
  case class B(v: String) extends NameCollidesWithSubtypeSum

case class DistinctNameProduct(@name("x") a: String, @name("y") b: Int)

enum DistinctNameSum:
  @name("X") case A
  @name("Y") case B

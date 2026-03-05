package made
package example.transparent

import made.annotation.*

import scala.annotation.nowarn

class TransparentWrappingTest extends munit.FunSuite:
  test("unwrap and wrap") {
    @transparent
    case class Email(value: String)

    val mirror = Made.derived[Email]
    val email = Email("alice@example.com")

    assert(mirror.unwrap(email) == "alice@example.com")
    assert(mirror.wrap("bob@example.com") == Email("bob@example.com"))
  }

  test("metadata includes @transparent") {
    @transparent
    case class Tag(value: String)

    val mirror = Made.derived[Tag]
    assert(mirror.hasAnnotation[transparent])
  }

  test("codec derivation") {
    trait Codec[T]:
      def encode(value: T): String
      def decode(raw: String): T

    object Codec:
      given Codec[String] with
        def encode(value: String): String = value
        def decode(raw: String): String = raw

      given Codec[Long] with
        def encode(value: Long): String = value.toString
        def decode(raw: String): Long = raw.toLong

      inline def derived[T](using m: Made.Of[T]): Codec[T] = inline m match
        case m: Made.TransparentOf[T] => deriveTransparent(m)
        case _ => compiletime.error("Codec derivation only supports transparent types in this example")

      @nowarn("msg=New anonymous class")
      inline def deriveTransparent[T](m: Made.TransparentOf[T]): Codec[T] =
        val innerCodec = compiletime.summonInline[Codec[m.ElemType]]
        new Codec[T]:
          def encode(value: T): String = innerCodec.encode(m.unwrap(value))
          def decode(raw: String): T = m.wrap(innerCodec.decode(raw))

    @transparent
    case class Email(value: String)

    @transparent
    case class UserId(value: Long)

    val emailCodec = Codec.derived[Email]
    assert(emailCodec.encode(Email("alice@example.com")) == "alice@example.com")
    assert(emailCodec.decode("bob@example.com") == Email("bob@example.com"))

    val userIdCodec = Codec.derived[UserId]
    assert(userIdCodec.encode(UserId(42L)) == "42")
    assert(userIdCodec.decode("99") == UserId(99L))
  }

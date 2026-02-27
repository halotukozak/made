package made

class OptionLikeTest extends munit.FunSuite:
  test("OptionLike for Option[A]") {
    val optionLike = summon[OptionLike.Aux[Option[String], String]]

    assertEquals(optionLike.none, None)
    assertEquals(optionLike.some("test"), Some("test"))

    assertEquals(optionLike.isDefined(Some("test")), true)
    assertEquals(optionLike.isDefined(None), false)

    assertEquals(optionLike.get(Some("test")), "test")

    assertEquals(optionLike.getOrElse(Some("test"), "default"), "test")
    assertEquals(optionLike.getOrElse(None, "default"), "default")

    var count = 0
    optionLike.foreach(Some("test"), _ => count += 1)
    assertEquals(count, 1)
    optionLike.foreach(None, _ => count += 1)
    assertEquals(count, 1)

    assertEquals(optionLike.fold(Some("test"), "empty")(_ + "!"), "test!")
    assertEquals(optionLike.fold(None, "empty")(_ + "!"), "empty")

    assertEquals(optionLike.apply("test"), Some("test"))
    assertEquals(optionLike.apply(null.asInstanceOf[String]), None: Option[String]) // ignoreNulls: Boolean = true
  }

  test("OptionLike for A | Null") {
    val optionLike = summon[OptionLike.Aux[String | Null, String]]

    assertEquals(optionLike.none, null: String | Null)
    assertEquals(optionLike.some("test"), "test": String | Null)

    assertEquals(optionLike.isDefined("test"), true)
    assertEquals(optionLike.isDefined(null: String | Null), false)

    assertEquals(optionLike.get("test"), "test")

    assertEquals(optionLike.getOrElse("test", "default"), "test")
    assertEquals(optionLike.getOrElse(null: String | Null, "default"), "default")

    var count = 0
    optionLike.foreach("test", _ => count += 1)
    assertEquals(count, 1)
    optionLike.foreach(null: String | Null, _ => count += 1)
    assertEquals(count, 1)

    assertEquals(optionLike.fold("test", "empty")(_ + "!"), "test!")
    assertEquals(optionLike.fold(null: String | Null, "empty")(_ + "!"), "empty")

    assertEquals(optionLike.apply("test"), "test": String | Null)
    // ignoreNulls: Boolean = false
    // Note: if we pass null here, it will return some(null) which is null
    assertEquals(optionLike.apply(null.asInstanceOf[String]), null: String | Null)
  }

  test("OptionLike convert") {
    val optionLikeOpt = summon[OptionLike.Aux[Option[String], String]]
    val optionLikeNull = summon[OptionLike.Aux[String | Null, String]]

    assertEquals(optionLikeOpt.convert(Some("test"), optionLikeNull)(_.toUpperCase), "TEST": String | Null)
    assertEquals(optionLikeOpt.convert(None, optionLikeNull)(_.toUpperCase), null: String | Null)

    assertEquals(optionLikeNull.convert("test", optionLikeOpt)(_.toUpperCase), Some("TEST"))
    assertEquals(optionLikeNull.convert(null: String | Null, optionLikeOpt)(_.toUpperCase), None: Option[String])
  }

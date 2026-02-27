package made

import made.annotation.*
class GeneratedAnnotationTest extends munit.FunSuite:
  import GeneratedAnnotationTest.*

  test("product: @generated members are exposed via GeneratedElems and compute values") {
    val mirror = Made.derived[Prod]

    type Expected = GeneratedMadeElem {
      type MirroredType = String
      type MirroredLabel = "ab"
      type Metadata = Meta @generated
      type OuterMirroredType = Prod
    } *: GeneratedMadeElem {
      type MirroredType = Int
      type MirroredLabel = "len"
      type Metadata = Meta @generated
      type OuterMirroredType = Prod
    } *: EmptyTuple

    summon[mirror.GeneratedElems =:= Expected]

    val value = Prod(2, "x")
    val gAb *: gLen *: EmptyTuple = mirror.generatedElems
    assert(gAb(value) == "2-x")
    assert(gLen(value) == 1)
  }

  test("value class: @generated members are exposed") {
    val mirror = Made.derived[VC]

    type Expected = GeneratedMadeElem {
      type MirroredType = String
      type MirroredLabel = "upper"
      type Metadata = Meta @generated
      type OuterMirroredType = VC
    } *: EmptyTuple

    summon[mirror.GeneratedElems =:= Expected]

    val gUpper *: EmptyTuple = mirror.generatedElems
    assert(gUpper(VC("ab")) == "AB")
  }

  test("sum (sealed trait): @generated members on the trait are exposed") {
    val mirror = Made.derived[SumADT]

    type Expected = GeneratedMadeElem {
      type MirroredType = Int
      type MirroredLabel = "const"
      type Metadata = Meta @generated
      type OuterMirroredType = SumADT
    } *: EmptyTuple

    summon[mirror.GeneratedElems =:= Expected]

    val gConst *: EmptyTuple = mirror.generatedElems
    assert(gConst(SumADT.Case1(10)) == 42)
    assert(gConst(SumADT.Case2) == 42)
  }

  test("enum: @generated members on the enum are exposed") {
    val mirror = Made.derived[GenEnum]

    type Expected = GeneratedMadeElem {
      type MirroredType = String
      type MirroredLabel = "info"
      type Metadata = Meta @generated
      type OuterMirroredType = GenEnum
    } *: EmptyTuple

    summon[mirror.GeneratedElems =:= Expected]

    val gInfo *: EmptyTuple = mirror.generatedElems
    assert(gInfo(GenEnum.A) == "ENUM")
    assert(gInfo(GenEnum.B(5)) == "ENUM")
  }

  test("singleton object: @generated members are exposed and compute values") {
    val mirror = Made.derived[GenObj.type]

    type Expected = GeneratedMadeElem {
      type MirroredType = Int
      type MirroredLabel = "id"
      type Metadata = Meta @generated
      type OuterMirroredType = GenObj.type
    } *: EmptyTuple

    summon[mirror.GeneratedElems =:= Expected]

    val gId *: EmptyTuple = mirror.generatedElems
    assert(gId(GenObj) == 7)
  }

object GeneratedAnnotationTest:
  sealed trait SumADT:
    @generated val const: Int = 42

  final case class Prod(a: Int, b: String):
    @generated def ab: String = s"$a-$b"
    @generated def len: Int = b.length

  final case class VC(x: String) extends AnyVal:
    @generated def upper: String = x.toUpperCase

  enum GenEnum:
    case A
    case B(i: Int)
    @generated val info: String = "ENUM"

  object SumADT:
    final case class Case1(i: Int) extends SumADT
    case object Case2 extends SumADT

  case object GenObj:
    @generated def id: Int = 7

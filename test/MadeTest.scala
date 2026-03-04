package made

import made.annotation.*
import scala.deriving.Mirror.Sum

class MadeTest extends munit.FunSuite:
  test("DerMirror for case class") {
    val _: Made {
      type MirroredType = SimpleCaseClass
      type Label = "SimpleCaseClass"
      type MirroredElems = MadeFieldElem {
        type MirroredType = Long
        type Label = "id"
        type Metadata = Meta
      } *: MadeFieldElem {
        type MirroredType = String
        type Label = "name"
        type Metadata = Meta
      } *: EmptyTuple
      type Metadata = Meta
    } = Made.derived[SimpleCaseClass]
  }

  test("DerMirror for case class with no fields") {
    val _: Made.Product {
      type MirroredType = NoFields
      type Label = "NoFields"
      type Metadata = Meta
      type MirroredElems = EmptyTuple
    } = Made.derived[NoFields]
  }

  test("DerMirror for generic case class") {
    val _: Made.Product {
      type MirroredType = Box[Int]
      type Label = "Box"
      type Metadata = Meta
      type MirroredElems = MadeFieldElem {
        type MirroredType = Int
        type Label = "a"
        type Metadata = Meta
      } *: EmptyTuple
    } = Made.derived[Box[Int]]
  }

  test("DerMirror for enum") {
    val _: Made.Sum {
      type MirroredType = SimpleEnum
      type Label = "SimpleEnum"
      type Metadata = Meta
      type MirroredElems = MadeSubSingletonElem {
        type MirroredType = SimpleEnum.Case1.type
        type Label = "Case1"
        type Metadata = Meta
      } *: MadeSubElem {
        type MirroredType = SimpleEnum.Case2
        type Label = "Case2"
        type Metadata = Meta
      } *: EmptyTuple
    } = Made.derived[SimpleEnum]
  }

  test("DerMirror for object") {
    val mirror: Made.Singleton {
      type MirroredType = SimpleObject.type
      type Label = "SimpleObject"
      type Metadata = Meta
      type MirroredElems = EmptyTuple
    } = Made.derived[SimpleObject.type]

    assert(mirror.value == SimpleObject)
  }

  test("DerMirror for Unit") {
    val mirror: Made.Singleton {
      type MirroredType = Unit
      type Label = "Unit"
      type Metadata = Meta
    } = Made.derived[Unit]
    assert(mirror.value == ())
  }

  test("DerMirror for value class") {
    val mirror: Made.Product {
      type MirroredType = ValueClass
      type Label = "ValueClass"
      type Metadata = Meta
    } = Made.derived[ValueClass]
    assert(mirror.fromUnsafeArray(Array("test")) == ValueClass("test"))
  }

  test("DerMirror for @transparent case class") {
    val mirror: Made.Transparent {
      type MirroredType = TransparentClass
      type Label = "TransparentClass"
      type Metadata = Meta @transparent
      type MirroredElemType = Int
      type MirroredElems = MadeFieldElem {
        type MirroredType = Int
        type Label = "int"
        type Metadata = Meta
      } *: EmptyTuple
    } = Made.derived[TransparentClass]

    val tc = TransparentClass(42)
    assert(mirror.unwrap(tc) == 42)
    assert(mirror.wrap(42) == tc)
  }

  test("getAnnotation and hasAnnotation") {
    val mirror = Made.derived[AnnotatedCaseClass]
    summon[mirror.Metadata =:= (Meta @Annotation2 @Annotation1)]

    assert(mirror.hasAnnotation[Annotation1])
    assert(mirror.hasAnnotation[Annotation2])
    assert(!mirror.hasAnnotation[Annotation3])

    assert(mirror.getAnnotation[Annotation1].isDefined)
    assert(mirror.getAnnotation[Annotation2].isDefined)
    assert(mirror.getAnnotation[Annotation3].isEmpty)
  }

  test("parametrized annotation") {
    val mirror = Made.derived[ParamAnnotated]
    val annot = mirror.getAnnotation[ParamAnnotation].get
    assert(annot.value == "foo")
  }

  test("DerMirror with annotations") {
    val _: Made {
      type Metadata = Meta @Annotation2 @Annotation1
    } = Made.derived[AnnotatedCaseClass]
  }

  test("DerMirror with many annotations") {
    val _: Made {
      type Metadata = Meta @Annotation3 @Annotation2 @Annotation1
    } = Made.derived[ManyAnnotated]
  }

  test("DerMirror for enum with @name") {
    val _: Made.Sum {
      type MirroredType = NamedEnum
      type Label = "NamedEnum"
      type Metadata = Meta
      type MirroredElems <: MadeElem {
        type MirroredType = NamedEnum.Case1.type
        type Label = "C1"
//        type Metadata = Meta @name("C1")
      } *: MadeElem {
        type MirroredType = NamedEnum.Case2.type
        type Label = "Case2"
        type Metadata = Meta
      } *: EmptyTuple
    } = Made.derived[NamedEnum]
  }

  test("DerMirror for recursive ADT") {
    val _: Made.Sum {
      type MirroredType = Recursive
      type Label = "Recursive"
      type Metadata = Meta
      type MirroredElems = MadeSubSingletonElem {
        type MirroredType = Recursive.End.type
        type Label = "End"
        type Metadata = Meta
      } *: MadeSubElem {
        type MirroredType = Recursive.Next
        type Label = "Next"
        type Metadata = Meta
      } *: EmptyTuple
    } = Made.derived[Recursive]
  }

  test("DerMirror for ADT with mixed cases") {
    val _: Made.Sum {
      type MirroredType = MixedADT
      type Label = "MixedADT"
      type Metadata = Meta
      type MirroredElems = MadeSubElem {
        type MirroredType = MixedADT.CaseClass
        type Label = "CaseClass"
        type Metadata = Meta
      } *: MadeSubSingletonElem {
        type MirroredType = MixedADT.CaseObj.type
        type Label = "CaseObj"
        type Metadata = Meta
      } *: EmptyTuple
    } = Made.derived[MixedADT]
  }

  test("DerMirror should include @generated members") {
    val m: Made {
      type MirroredType = HasGenerated
      type Label = "HasGenerated"
      type Metadata = Meta
      type MirroredElems = MadeFieldElem {
        type MirroredType = String
        type Label = "str"
        type Metadata = Meta
      } *: EmptyTuple
      type GeneratedElems = GeneratedMadeElem {
        type OuterMirroredType = HasGenerated
        type MirroredType = Int
        type Label = "gen"
        type Metadata = Meta @generated
      } *: EmptyTuple
    } = Made.derived[HasGenerated]

    val instance = HasGenerated("test")
    assert(m.generatedElems(0).apply(instance) == 4)
  }

  test("DerMirror for HK case class") {
    val _: Made.Product {
      type MirroredType = HKBox[List]
      type Label = "HKBox"
      type Metadata = Meta
      type MirroredElems = MadeFieldElem {
        type MirroredType = List[Int]
        type Label = "fa"
        type Metadata = Meta
      } *: EmptyTuple
    } = Made.derived[HKBox[List]]
  }

  test("DerMirror for HK sum") {
    val _: Made.Sum {
      type MirroredType = HKADT[List, Int]
      type Label = "HKADT"
      type Metadata = Meta
      type MirroredElems = MadeSubElem {
        type MirroredType = HKADT.Case1[List, Int]
        type Label = "Case1"
        type Metadata = Meta
      } *: MadeSubElem {
        type MirroredType = HKADT.Case2[List, Int]
        type Label = "Case2"
        type Metadata = Meta
      } *: EmptyTuple
    } = Made.derived[HKADT[List, Int]]
  }

  test("DerMirror for recursive case class") {
    val _: Made.Product {
      type MirroredType = Recursive.Next
      type Label = "Next"
      type Metadata = Meta
      type MirroredElems = MadeFieldElem {
        type MirroredType = Recursive
        type Label = "r"
        type Metadata = Meta
      } *: EmptyTuple
    } = Made.derived[Recursive.Next]
  }

  test("fromUnsafeArray for recursive case class") {
    val mirror = Made.derived[Recursive.Next]
    val result = mirror.fromUnsafeArray(Array(Recursive.End))
    assert(result == Recursive.Next(Recursive.End))
  }

  test("DerMirror for recursive case class with Option") {
    val _: Made.Product {
      type MirroredType = RecTree
      type Label = "RecTree"
      type Metadata = Meta
      type MirroredElems = MadeFieldElem {
        type MirroredType = Int
        type Label = "value"
        type Metadata = Meta
      } *: MadeFieldElem {
        type MirroredType = Option[RecTree]
        type Label = "left"
        type Metadata = Meta
      } *: MadeFieldElem {
        type MirroredType = Option[RecTree]
        type Label = "right"
        type Metadata = Meta
      } *: EmptyTuple
    } = Made.derived[RecTree]
  }

  test("fromUnsafeArray for recursive case class with Option") {
    val mirror = Made.derived[RecTree]
    val leaf = RecTree(1, None, None)
    val tree = RecTree(0, Some(leaf), None)
    val result = mirror.fromUnsafeArray(Array(0, Some(leaf), None))
    assert(result == tree)
  }

  test("DerMirror for case class with wildcard") {
    val _: Made.Product {
      type MirroredType = Box[?]
      type Label = "Box"
      type Metadata = Meta;
      type MirroredElems <: MadeElem {
        type Label = "a"
        type Metadata = Meta
      } *: EmptyTuple
    } = Made.derived[Box[?]]
  }

  test("fromUnsafeArray for simple case class") {
    val mirror = Made.derived[SimpleCaseClass]
    val result = mirror.fromUnsafeArray(Array(42L, "test"))
    assert(result == SimpleCaseClass(42L, "test"))
  }

  test("fromUnsafeArray for case class with no fields") {
    val mirror = Made.derived[NoFields]
    val result = mirror.fromUnsafeArray(Array.empty)
    assert(result == NoFields())
  }

  test("fromUnsafeArray for generic case class") {
    val mirror = Made.derived[Box[String]]
    val result = mirror.fromUnsafeArray(Array("content"))
    assert(result == Box("content"))
  }

  test("MadeElem.hasAnnotation on generated elem finds @generated") {
    val mirror = Made.derived[HasGenerated]
    val gen *: EmptyTuple = mirror.generatedElems

    assert(gen.hasAnnotation[generated])
    assert(!gen.hasAnnotation[Annotation1])
  }

  test("MadeElem.getAnnotation on generated elem returns Some for @generated") {
    val mirror = Made.derived[HasGenerated]
    val gen *: EmptyTuple = mirror.generatedElems

    assert(gen.getAnnotation[generated].isDefined)
    assert(gen.getAnnotation[Annotation1].isEmpty)
  }

  test("MadeElem.hasAnnotation returns false for unannotated field") {
    val mirror = Made.derived[SimpleCaseClass]
    val id *: name *: EmptyTuple = mirror.mirroredElems

    assert(!id.hasAnnotation[Annotation1])
    assert(!id.hasAnnotation[generated])
    assert(!name.hasAnnotation[Annotation1])
  }

  test("MadeElem.getAnnotation returns None for unannotated field") {
    val mirror = Made.derived[SimpleCaseClass]
    val id *: name *: EmptyTuple = mirror.mirroredElems

    assert(id.getAnnotation[Annotation1].isEmpty)
    assert(name.getAnnotation[Annotation1].isEmpty)
  }

  test("MadeElem.hasAnnotation with custom annotation on generated member") {
    val mirror = Made.derived[HasCustomAnnotatedGenerated]
    val gen *: EmptyTuple = mirror.generatedElems

    assert(gen.hasAnnotation[generated])
    assert(gen.hasAnnotation[Annotation1])
    assert(!gen.hasAnnotation[Annotation2])
  }

  test("MadeElem.getAnnotation with custom annotation on generated member") {
    val mirror = Made.derived[HasCustomAnnotatedGenerated]
    val gen *: EmptyTuple = mirror.generatedElems

    assert(gen.getAnnotation[generated].isDefined)
    assert(gen.getAnnotation[Annotation1].isDefined)
    assert(gen.getAnnotation[Annotation2].isEmpty)
  }

  test("MadeElem.getAnnotation retrieves parametrized annotation from generated member") {
    val mirror = Made.derived[HasParamAnnotatedGenerated]
    val gen *: EmptyTuple = mirror.generatedElems

    val annot = gen.getAnnotation[ParamAnnotation].get
    assert(annot.value == "gen-param")
  }

  test("MadeElem.hasAnnotation on multiple generated elems") {
    val mirror = Made.derived[HasMultipleGenerated]
    val g1 *: g2 *: EmptyTuple = mirror.generatedElems

    assert(g1.hasAnnotation[generated])
    assert(g1.hasAnnotation[Annotation1])
    assert(!g1.hasAnnotation[Annotation2])

    assert(g2.hasAnnotation[generated])
    assert(!g2.hasAnnotation[Annotation1])
    assert(g2.hasAnnotation[Annotation2])
  }

  test("MadeElem.getAnnotation on multiple generated elems") {
    val mirror = Made.derived[HasMultipleGenerated]
    val g1 *: g2 *: EmptyTuple = mirror.generatedElems

    assert(g1.getAnnotation[Annotation1].isDefined)
    assert(g1.getAnnotation[Annotation2].isEmpty)

    assert(g2.getAnnotation[Annotation1].isEmpty)
    assert(g2.getAnnotation[Annotation2].isDefined)
  }

  test("inherit name") {
    val mirror = Made.derived[InheritedName]
    val fieldElem *: EmptyTuple = mirror.mirroredElems
    summon[fieldElem.Label =:= "customName"]
  }

sealed trait MixedADT
sealed trait HKADT[F[_], T]
case class SimpleCaseClass(id: Long, name: String)
case class NoFields()
enum SimpleEnum:
  case Case1
  case Case2(data: String)
case class ValueClass(str: String) extends AnyVal
@transparent
case class TransparentClass(int: Int)
case class ParamAnnotation(value: String) extends MetaAnnotation
@ParamAnnotation("foo")
case class ParamAnnotated(id: Int)
case class Box[T](a: T)
enum NamedEnum:
  @name("C1") case Case1
  case Case2
class Annotation1 extends MetaAnnotation
class Annotation2 extends MetaAnnotation
class Annotation3 extends MetaAnnotation
@Annotation1 @Annotation2
case class AnnotatedCaseClass()
@Annotation1 @Annotation2 @Annotation3
case class ManyAnnotated()
enum Recursive:
  case End
  case Next(r: Recursive)
case class HasGenerated(str: String):
  @generated def gen: Int = str.length
case class HKBox[F[_]](fa: F[Int])
object HKADT:
  case class Case1[F[_], T](a: T) extends HKADT[F, T]
  case class Case2[F[_], T](fa: F[T]) extends HKADT[F, T]
case class RecTree(value: Int, left: Option[RecTree], right: Option[RecTree])
case object SimpleObject
object MixedADT:
  case class CaseClass(v: Int) extends MixedADT
  case object CaseObj extends MixedADT

trait TraitWithName:
  @name("customName")
  def field: String

case class InheritedName(field: String) extends TraitWithName

case class HasCustomAnnotatedGenerated(x: Int):
  @generated @Annotation1 def tag: String = x.toString

case class HasParamAnnotatedGenerated(x: Int):
  @generated @ParamAnnotation("gen-param") def info: String = s"x=$x"

case class HasMultipleGenerated(x: Int):
  @generated @Annotation1 def first: String = x.toString
  @generated @Annotation2 def second: Int = x * 2

package made

import scala.quoted.*

extension (comp: Expr.type)
  private[made] def ofOption[A: Type](opt: Option[Expr[A]])(using Quotes): Expr[Option[A]] = opt match
    case Some(expr) => '{ Some($expr) }
    case None => '{ None }

// like ValueOf but without the implicit search and boxing
inline private[made] def singleValueOf[T <: scala.Singleton]: T = ${ singleValueOfImpl[T] }

private def singleValueOfImpl[T <: scala.Singleton: Type](using quotes: Quotes): Expr[T] =
  import quotes.reflect.*
  val term = TypeRepr.of[T] match
    case ConstantType(c: Constant) => Literal(c)
    case tp: TypeRef if tp <:< TypeRepr.of[Unit] => Literal(UnitConstant())
    case n: TermRef => Ref(n.termSymbol)
    case ts: ThisType => This(ts.classSymbol.get)
    case tp => report.errorAndAbort(s"Unsupported singleton type: ${tp.show}")
  term.asExprOf[T]

private[made] def stringToType(str: String)(using quotes: Quotes): Type[? <: String] =
  import quotes.reflect.*
  ConstantType(StringConstant(str)).asType.asInstanceOf[Type[? <: String]]

private[made] def traverseTypes(tpes: List[Type[? <: AnyKind]])(using Quotes): Type[? <: Tuple] =
  val empty: Type[? <: Tuple] = Type.of[EmptyTuple]
  tpes.foldRight(empty):
    case ('[tpe], '[type acc <: Tuple; acc]) => Type.of[tpe *: acc]
    case (_, _) => wontHappen

private[made] def traverseTuple(tpe: Type[? <: Tuple])(using quotes: Quotes): List[Type[? <: AnyKind]] = tpe match
  case '[EmptyTuple] => Nil
  case '[t *: ts] => Type.of[t] :: traverseTuple(Type.of[ts])

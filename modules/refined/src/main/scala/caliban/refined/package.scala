package caliban

import caliban.Value._
import caliban.schema.{ ArgBuilder, Schema }
import eu.timepit.refined.api.{ RefType, Validate }

package object refined {

  implicit def refinedSchema[T, P, F[_, _]](
    implicit underlying: Schema[Any, T],
    refType: RefType[F]
  ): Schema[Any, F[T, P]] =
    underlying.contramap(refType.unwrap)

  // FIXME
  // diverging implicit expansion for type caliban.schema.Schema[Any,F[B,P]]
  // starting with method streamSchema in trait GenericSchema
  /*
  implicit def coercibleRefinedSchema[A, B, P, F[_, _]](
    implicit schema: Schema[Any, F[B, P]],
    coercible: Coercible[A, F[B, P]]
  ): Schema[Any, A] =
    schema.contramap(coercible.apply)
   */

  implicit def refinedArgBuilder[T, P, F[_, _]](
    implicit validate: Validate[T, P],
    refType: RefType[F]
  ): ArgBuilder[F[T, P]] =
    new ArgBuilder[F[T, P]] {
      // see leftMap: cats.syntax.either.catsSyntaxEither
      lazy val refineArgBuilder: Any => Either[CalibanError.ExecutionError, F[T, P]] =
        value =>
          refType.refine(value.asInstanceOf[T]) match {
            case r @ Right(_) => r.asInstanceOf[Either[CalibanError.ExecutionError, F[T, P]]]
            case Left(str)    => Left(CalibanError.ExecutionError(str))
          }

      override def build(input: InputValue): Either[CalibanError.ExecutionError, F[T, P]] =
        input match {
          case v: IntValue =>
            v match {
              case IntValue.IntNumber(value)    => refineArgBuilder(value)
              case IntValue.LongNumber(value)   => refineArgBuilder(value)
              case IntValue.BigIntNumber(value) => refineArgBuilder(value)
            }
          case v: FloatValue =>
            v match {
              case FloatValue.FloatNumber(value)      => refineArgBuilder(value)
              case FloatValue.DoubleNumber(value)     => refineArgBuilder(value)
              case FloatValue.BigDecimalNumber(value) => refineArgBuilder(value)
            }
          case StringValue(value)  => refineArgBuilder(value)
          case BooleanValue(value) => refineArgBuilder(value)
          case EnumValue(value)    => refineArgBuilder(value)
          //case NullValue =>
          case other =>
            Left(CalibanError.ExecutionError(s"Can't build a Refined from input $other"))
        }
    }

  // FIXME
//  implicit def coercibleRefinedArgBuilder[A, B, P, F[_, _]](
//    implicit argBuilder: ArgBuilder[F[B, P]],
//    coercible: Coercible[F[B, P], A]
//  ): ArgBuilder[A] =
//    argBuilder.map(coercible.apply)

}

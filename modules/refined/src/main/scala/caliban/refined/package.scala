package caliban

import caliban.Value._
import caliban.schema.{ ArgBuilder, Schema }
import eu.timepit.refined.api.{ RefType, Validate }
import io.estatico.newtype.Coercible

// See https://github.com/estatico/scala-newtype/issues/64
package object refined {

  implicit def refinedSchema[T, P, F[_, _]](
    implicit underlying: Schema[Any, T],
    refType: RefType[F]
  ): Schema[Any, F[T, P]] =
    underlying.contramap(refType.unwrap)

  /*
   * Given a Schema instance for a phantom type F[R, P]
   * that contains a value of type R and refined type P,
   * derive a Schema instance for newtype N
   */
  implicit def coercibleRefinedSchema[R, N, P, F[_, _]](
    implicit ev: Coercible[Schema[Any, F[R, P]], Schema[Any, N]],
    schema: Schema[Any, F[R, P]]
  ): Schema[Any, N] =
    ev(schema)

  // see io.circe.refined and caliban.ValueCirce
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

  /*
   * Given an ArgBuilder instance for a phantom type F[R, P]
   * that contains a value of type R and refined type P,
   * derive an ArgBuilder instance for newtype N
   */
  implicit def coercibleRefinedArgBuilder[R, N, P, F[_, _]](
    implicit ev: Coercible[ArgBuilder[F[R, P]], ArgBuilder[N]],
    argBuilder: ArgBuilder[F[R, P]]
  ): ArgBuilder[N] =
    ev(argBuilder)

}

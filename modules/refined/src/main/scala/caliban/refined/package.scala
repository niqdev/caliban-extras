package caliban

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

  implicit def refinedArgBuilder[T, P, F[_, _]](
    implicit validate: Validate[T, P],
    refType: RefType[F],
    sourceArgBuilder: ArgBuilder[T]
  ): ArgBuilder[F[T, P]] = sourceArgBuilder.flatMap { t =>
    refType.refine(t) match {
      case Left(str)    => Left(CalibanError.ExecutionError(str))
      case r @ Right(_) => r.asInstanceOf[Either[CalibanError.ExecutionError, F[T, P]]]
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

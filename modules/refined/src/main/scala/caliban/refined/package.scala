package caliban

import caliban.schema.Schema
import eu.timepit.refined.api.RefType

package object refined {

  implicit def refinedSchema[T, P, F[_, _]](
    implicit underlying: Schema[Any, T],
    refType: RefType[F]
  ): Schema[Any, F[T, P]] =
    underlying.contramap(refType.unwrap)
}

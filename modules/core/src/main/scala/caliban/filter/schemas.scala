package caliban.filter

import caliban.schema.Schema
import cats.data.NonEmptyList

object schemas {

  implicit def nonEmptyListSchema[A](
    implicit ev: Schema[Any, A]
  ): Schema[Any, NonEmptyList[A]] =
    Schema.listSchema[A].contramap(_.toList)
}

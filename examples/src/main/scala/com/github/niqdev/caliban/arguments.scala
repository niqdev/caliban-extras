package com.github.niqdev.caliban

import caliban.CalibanError
import caliban.Value.StringValue
import caliban.pagination.arguments.ForwardPaginationArg
import caliban.pagination.schemas.{ Cursor, First }
import caliban.schema.ArgBuilder
import cats.syntax.either._
import eu.timepit.refined.types.string.NonEmptyString

object arguments extends ArgumentInstances {

  final case class UserArg(name: NonEmptyString)
  final case class RepositoryArg(name: NonEmptyString)
  // TODO RepositoriesArg(first*, after, orderBy: {direction*, field*}) * is mandatory
  final case class RepositoriesArg(first: First, after: Option[Cursor]) extends ForwardPaginationArg
}

sealed trait ArgumentInstances {

  implicit val nonEmptyStringArgBuilder: ArgBuilder[NonEmptyString] = {
    case StringValue(value) =>
      NonEmptyString.from(value).leftMap(CalibanError.ExecutionError(_))
    case other =>
      Left(CalibanError.ExecutionError(s"Can't build a NonEmptyString from input $other"))
  }
}

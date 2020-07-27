package com.github.niqdev.caliban

import caliban.pagination.arguments.ForwardPaginationArg
import caliban.pagination.schemas.{ Cursor, First }
import eu.timepit.refined.types.numeric.PosInt
import eu.timepit.refined.types.string.NonEmptyString

object arguments {

  final case class UserArg(name: NonEmptyString)
  final case class RepositoryArg(name: NonEmptyString)
  // TODO RepositoriesArg(first*, after, orderBy: {direction*, field*}) * is mandatory
  final case class RepositoriesArg(first: First, after: Option[Cursor]) extends ForwardPaginationArg
  final case class IssueArg(number: PosInt)
  final case class IssuesArg(first: First, after: Option[Cursor]) extends ForwardPaginationArg
}

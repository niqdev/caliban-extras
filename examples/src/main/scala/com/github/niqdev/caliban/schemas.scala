package com.github.niqdev.caliban

import java.time.Instant

import caliban.pagination.schemas._
import caliban.schema.Schema
import com.github.niqdev.caliban.arguments.RepositoriesArg
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Url
import eu.timepit.refined.types.numeric.NonNegLong
import eu.timepit.refined.types.string.NonEmptyString

object schema extends SchemaInstances {

  // TODO rename UserNodeF > User
  final case class UserNode[F[_]](
    id: NodeId,
    name: NonEmptyString,
    createdAt: Instant,
    updatedAt: Instant,
    //repository: Repository,
    repositories: RepositoriesArg => F[RepositoryConnection[F]]
  ) extends Node[F]
  object UserNode {
    final val idPrefix = "user:v1:"
  }

  // TODO add issue|issues
  final case class RepositoryNode[F[_]](
    id: NodeId,
    name: NonEmptyString,
    url: String Refined Url,
    isFork: Boolean,
    createdAt: Instant,
    updatedAt: Instant
  ) extends Node[F]
  object RepositoryNode {
    val idPrefix = "repository:v1:"
  }

  final case class RepositoryConnection[F[_]](
    edges: List[RepositoryEdge[F]],
    nodes: List[RepositoryNode[F]],
    pageInfo: PageInfo,
    totalCount: NonNegLong
  )

  final case class RepositoryEdge[F[_]](
    cursor: Cursor,
    node: RepositoryNode[F]
  )
}

sealed trait SchemaInstances {

  implicit val instantSchema: Schema[Any, Instant] =
    Schema.longSchema.contramap(_.getEpochSecond)

  implicit val nonEmptyStringSchema: Schema[Any, NonEmptyString] =
    Schema.stringSchema.contramap(_.value)

  implicit val urlSchema: Schema[Any, Url] =
    Schema.stringSchema.contramap(_.toString)
}

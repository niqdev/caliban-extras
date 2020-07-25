package com.github.niqdev.caliban

import java.time.Instant

import caliban.pagination.schemas._
import caliban.schema.Annotations.{ GQLInterface, GQLName }
import caliban.schema.Schema
import com.github.niqdev.caliban.arguments.RepositoriesArg
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Url
import eu.timepit.refined.types.numeric.NonNegLong
import eu.timepit.refined.types.string.NonEmptyString

object schema extends SchemaInstances {

  // TODO see caliban.pagination.schemas.Node
  @GQLInterface
  sealed trait Node[F[_]] {
    def id: NodeId
  }

  final case class Edge[F[_], T <: Node[F]](
    cursor: Cursor,
    node: T
  )

  /*
   * If the client is paginating with first/after (Forward Pagination):
   * - hasNextPage is true if further edges exist, otherwise false
   * - hasPreviousPage may be true if edges prior to after exist, if it can do so efficiently, otherwise may return false
   *
   * If the client is paginating with last/before (Backward Pagination):
   * - hasNextPage may be true if edges further from before exist, if it can do so efficiently, otherwise may return false
   * - hasPreviousPage is true if prior edges exist, otherwise false
   *
   * startCursor and endCursor must be the cursors corresponding to the first and last nodes in edges, respectively
   */
  final case class PageInfo(
    hasNextPage: Boolean,
    hasPreviousPage: Boolean,
    startCursor: Cursor,
    endCursor: Cursor
  )

  final case class Connection[F[_], T <: Node[F]](
    edges: List[Edge[F, T]],
    nodes: List[T],
    pageInfo: PageInfo,
    totalCount: NonNegLong
  )

  @GQLName("User")
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
  @GQLName("Repository")
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

  // TODO delete
  final case class RepositoryConnection[F[_]](
    edges: List[RepositoryEdge[F]],
    nodes: List[RepositoryNode[F]],
    pageInfo: PageInfo,
    totalCount: NonNegLong
  )

  // TODO delete
  final case class RepositoryEdge[F[_]](
    cursor: Cursor,
    node: RepositoryNode[F]
  )
}

sealed trait SchemaInstances {

  implicit val instantSchema: Schema[Any, Instant] =
    Schema.longSchema.contramap(_.getEpochSecond)

}

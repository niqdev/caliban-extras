package com.github.niqdev.caliban

import java.time.Instant

import caliban.pagination.arguments._
import caliban.pagination.schemas._
import caliban.schema.Annotations.{ GQLInterface, GQLName }
import com.github.niqdev.caliban.arguments._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Url
import eu.timepit.refined.types.numeric.{ NonNegLong, PosInt }
import eu.timepit.refined.types.string.NonEmptyString

object schemas {

  /**
    * Node roots
    *
    * TODO move in caliban.pagination.resolvers
    */
  final case class NodeRoot[F[_]](
    node: NodeArg => F[Option[Node[F]]],
    nodes: NodesArg => F[List[Option[Node[F]]]]
  )

  /**
    * GitHub roots
    */
  final case class GitHubRoot[F[_]](
    user: UserArg => F[Option[UserNode[F]]],
    repository: RepositoryArg => F[Option[RepositoryNode[F]]],
    repositories: RepositoriesArg => F[Connection[F, RepositoryNode[F]]]
  )

  // TODO see caliban.pagination.schemas.Node
  @GQLInterface
  @GQLName("Node")
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
    //repository: RepositoryArg => F[RepositoryNode[F]]
    repositories: RepositoriesArg => F[Connection[F, RepositoryNode[F]]]
  ) extends Node[F]
  object UserNode {
    final val idPrefix = "user:v1:"
  }

  @GQLName("Repository")
  final case class RepositoryNode[F[_]](
    id: NodeId,
    name: NonEmptyString,
    url: String Refined Url,
    isFork: Boolean,
    createdAt: Instant,
    updatedAt: Instant
    //issue: IssueArg => F[IssueNode[F]]
    //issues: IssuesArg => F[Connection[F, IssueNode[F]]]
  ) extends Node[F]
  object RepositoryNode {
    val idPrefix = "repository:v1:"
  }

  // TODO add example of enumeratum
  sealed trait IssueStatus
  object IssueStatus {
    final case object OPEN  extends IssueStatus
    final case object CLOSE extends IssueStatus
  }

  @GQLName("Issue")
  final case class IssueNode[F[_]](
    id: NodeId,
    number: PosInt,
    state: IssueStatus,
    title: NonEmptyString,
    body: NonEmptyString,
    createdAt: Instant,
    updatedAt: Instant
  ) extends Node[F]
  object IssueNode {
    val idPrefix = "issue:v1:"
  }
}

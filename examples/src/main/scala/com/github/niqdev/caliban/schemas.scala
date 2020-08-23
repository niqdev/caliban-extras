package com.github.niqdev.caliban

import java.time.Instant

import caliban.filter.schemas._
import caliban.pagination.Base64String
import caliban.schema.Annotations.{ GQLInterface, GQLName }
import caliban.schema.Schema
import com.github.niqdev.caliban.schemas._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Url
import eu.timepit.refined.types.numeric.{ NonNegInt, NonNegLong, PosInt }
import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype

object schemas extends SchemaInstances {

  @newtype case class NodeId(value: Base64String)
  @newtype case class Cursor(value: Base64String)
  @newtype case class First(value: NonNegInt)
  @newtype case class Last(value: NonNegInt)

  // TODO every Node must have a prefix
  // TODO create Cursor sealed trait with prefix
  object Cursor {
    final val prefix = "cursor:v1:"
  }

  // TODO move in caliban.pagination.arguments
  trait ForwardPaginationArg {
    def first: First
    // "after" is the cursor of the last edge in the previous page
    def after: Option[Cursor]
  }

  // TODO move in caliban.pagination.arguments
  trait BackwardPaginationArg {
    def last: Last
    // "before" is the cursor of the first edge in the next page
    def before: Option[Cursor]
  }

  // TODO move in caliban.pagination.arguments
  final case class NodeArg(id: NodeId)
  // TODO move in caliban.pagination.arguments
  final case class NodesArg(ids: List[NodeId])
  final case class UserArg(name: NonEmptyString, filters: Option[Filter]) extends FilterArg
  final case class UsersArg(first: First, after: Option[Cursor])          extends ForwardPaginationArg
  final case class RepositoryArg(name: NonEmptyString)
  // TODO RepositoriesArg(first*, after, orderBy: {direction*, field*}, filters) * is mandatory
  final case class RepositoriesArg(first: First, after: Option[Cursor]) extends ForwardPaginationArg
  final case class IssueArg(number: PosInt)
  final case class IssuesArg(first: First, after: Option[Cursor]) extends ForwardPaginationArg

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
    users: UsersArg => F[Connection[F, UserNode[F]]],
    repository: RepositoryArg => F[Option[RepositoryNode[F]]],
    repositories: RepositoriesArg => F[Connection[F, RepositoryNode[F]]],
    issue: IssueArg => F[Option[IssueNode[F]]],
    issues: IssuesArg => F[Connection[F, IssueNode[F]]]
  )

  // TODO how to move in caliban.pagination? not sealed and not higher-kinded
  // TODO Node schema https://gist.github.com/paulpdaniels/d8e932b9faee19812d2de8f56dd77a51
  @GQLInterface
  @GQLName("Node")
  sealed trait Node[F[_]] {
    def id: NodeId
  }

  final case class Edge[F[_], N <: Node[F]](
    cursor: Cursor,
    node: N
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

  final case class Connection[F[_], N <: Node[F]](
    edges: List[Edge[F, N]],
    nodes: List[N],
    pageInfo: PageInfo,
    totalCount: NonNegLong
  )

  @GQLName("User")
  final case class UserNode[F[_]](
    id: NodeId,
    name: NonEmptyString,
    createdAt: Instant,
    updatedAt: Instant,
    repository: RepositoryArg => F[Option[RepositoryNode[F]]],
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
    updatedAt: Instant,
    issue: IssueArg => F[Option[IssueNode[F]]],
    issues: IssuesArg => F[Connection[F, IssueNode[F]]]
  ) extends Node[F]
  object RepositoryNode {
    val idPrefix = "repository:v1:"
  }

  // TODO add example of enumeratum (?)
  sealed trait IssueStatus
  object IssueStatus {
    final case object OPEN  extends IssueStatus
    final case object CLOSE extends IssueStatus
  }

  @GQLName("Issue")
  final case class IssueNode[F[_]](
    id: NodeId,
    number: PosInt,
    status: IssueStatus,
    title: NonEmptyString,
    body: NonEmptyString,
    createdAt: Instant,
    updatedAt: Instant
  ) extends Node[F]
  object IssueNode {
    val idPrefix = "issue:v1:"
  }
}

// TODO: UserNode, RepositoryNode, Node, Edge, Connection
// declare instances to avoid "Method too large" caused by magnolia
sealed trait SchemaInstances {

  implicit val nodeIdSchema: Schema[Any, NodeId] =
    Schema.stringSchema.contramap(_.value.value)
  implicit val cursorSchema: Schema[Any, Cursor] =
    Schema.stringSchema.contramap(_.value.value)
  implicit val firstSchema: Schema[Any, First] =
    Schema.intSchema.contramap(_.value.value)
  implicit val lastSchema: Schema[Any, Last] =
    Schema.intSchema.contramap(_.value.value)

  implicit val nodeArgSchema: Schema[Any, NodeArg] =
    Schema.gen[NodeArg]
  implicit val nodesArgSchema: Schema[Any, NodesArg] =
    Schema.gen[NodesArg]
  implicit val userArgSchema: Schema[Any, UserArg] =
    Schema.gen[UserArg]
  implicit val usersArgSchema: Schema[Any, UsersArg] =
    Schema.gen[UsersArg]
  implicit val repositoryArgSchema: Schema[Any, RepositoryArg] =
    Schema.gen[RepositoryArg]
  implicit val repositoriesArgSchema: Schema[Any, RepositoriesArg] =
    Schema.gen[RepositoriesArg]
  implicit val issueArgSchema: Schema[Any, IssueArg] =
    Schema.gen[IssueArg]
  implicit val issuesArgSchema: Schema[Any, IssuesArg] =
    Schema.gen[IssuesArg]

  implicit val issueStatusSchema: Schema[Any, IssueStatus] =
    Schema.gen[IssueStatus]
  implicit def issueNodeSchema[F[_]]: Schema[Any, IssueNode[F]] =
    Schema.gen[IssueNode[F]]
}

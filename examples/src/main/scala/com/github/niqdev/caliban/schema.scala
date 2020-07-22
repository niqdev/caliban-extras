package com.github.niqdev.caliban

import java.time.Instant

import caliban.CalibanError
import caliban.Value.{ IntValue, StringValue }
import caliban.interop.cats.CatsInterop
import caliban.pagination.Base64String
import caliban.schema.Annotations.GQLInterface
import caliban.schema.{ ArgBuilder, Schema }
import cats.effect.Effect
import cats.syntax.either._
import com.github.niqdev.caliban.schema._
import com.github.niqdev.caliban.schema.arguments._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Url
import eu.timepit.refined.types.numeric.{ NonNegInt, NonNegLong }
import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype

object schema extends CommonSchemaInstances with CommonArgInstances {

  // TODO RepositoriesArg(first*, after, orderBy: {direction*, field*}) * is mandatory
  // TODO ForwardPaginationArg change to trait
  object arguments {
    final case class NodeArg(id: NodeId)
    final case class UserArg(name: NonEmptyString)
    final case class RepositoryArg(name: NonEmptyString)
    // "after" is the cursor of the last edge in the previous page
    final case class ForwardPaginationArg(
      first: Offset,
      after: Option[Cursor]
    )
    // "before" is the cursor of the first edge in the next page
    final case class BackwardPaginationArg(
      last: Offset,
      before: Option[Cursor]
    )
  }

  // TODO replace Offset with first/last
  @newtype case class Offset(value: NonNegInt)
  @newtype case class NodeId(value: Base64String)
  @newtype case class Cursor(value: Base64String)
  object Cursor {
    final val prefix = "cursor:v1:"
  }

  // Node is also a root node and it must be of higher-kinded i.e. Node[F[_]]
  // for more details see docs/graphql-error.txt
  @GQLInterface
  sealed trait Node[F[_]] {
    def id: NodeId
  }

  // TODO rename UserNodeF > User
  final case class UserNode[F[_]](
    id: NodeId,
    name: NonEmptyString,
    createdAt: Instant,
    updatedAt: Instant,
    //repository: Repository,
    repositories: ForwardPaginationArg => F[RepositoryConnection[F]]
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
}

protected[caliban] sealed trait CommonSchemaInstances {

  // see caliban.interop.cats.implicits.effectSchema
  implicit def effectSchema[F[_]: Effect, R, A](implicit ev: Schema[R, A]): Schema[R, F[A]] =
    CatsInterop.schema

  implicit val instantSchema: Schema[Any, Instant] =
    Schema.longSchema.contramap(_.getEpochSecond)

  implicit val nonEmptyStringSchema: Schema[Any, NonEmptyString] =
    Schema.stringSchema.contramap(_.value)

  implicit val base64StringSchema: Schema[Any, Base64String] =
    Schema.stringSchema.contramap(_.value)

  implicit val nodeIdSchema: Schema[Any, NodeId] =
    base64StringSchema.contramap(_.value)

  implicit val cursorSchema: Schema[Any, Cursor] =
    base64StringSchema.contramap(_.value)

  implicit val urlSchema: Schema[Any, Url] =
    Schema.stringSchema.contramap(_.toString)

  implicit val offsetSchema: Schema[Any, Offset] =
    Schema.intSchema.contramap(_.value.value)
}

protected[caliban] sealed trait CommonArgInstances {

  implicit val nonEmptyStringArgBuilder: ArgBuilder[NonEmptyString] = {
    case StringValue(value) =>
      NonEmptyString.from(value).leftMap(CalibanError.ExecutionError(_))
    case other =>
      Left(CalibanError.ExecutionError(s"Can't build a NonEmptyString from input $other"))
  }

  implicit val base64StringArgBuilder: ArgBuilder[Base64String] = {
    case StringValue(value) =>
      Base64String.from(value).leftMap(CalibanError.ExecutionError(_))
    case other =>
      Left(CalibanError.ExecutionError(s"Can't build a Base64String from input $other"))
  }

  implicit val nodeIdArgBuilder: ArgBuilder[NodeId] =
    base64StringArgBuilder.map(NodeId.apply)

  implicit val cursorArgBuilder: ArgBuilder[Cursor] =
    base64StringArgBuilder.map(Cursor.apply)

  implicit val offsetArgBuilder: ArgBuilder[Offset] = {
    case value: IntValue =>
      NonNegInt.from(value.toInt).map(Offset.apply).leftMap(CalibanError.ExecutionError(_))
    case other =>
      Left(CalibanError.ExecutionError(s"Can't build a NonNegInt from input $other"))
  }

}

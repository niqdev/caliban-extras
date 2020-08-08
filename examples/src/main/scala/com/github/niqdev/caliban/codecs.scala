package com.github.niqdev.caliban

import java.util.UUID

import caliban.pagination.Base64String
import cats.syntax.either._
import com.github.niqdev.caliban.models._
import com.github.niqdev.caliban.repositories._
import com.github.niqdev.caliban.schemas._
import eu.timepit.refined.types.numeric.PosLong

// TODO use magnolia for typeclass derivation
object codecs {

  /**
    * Encodes a model into a GraphQL schema
    */
  trait SchemaEncoder[M, S] {
    def from(model: M): S
  }

  object SchemaEncoder {
    def apply[M, S](implicit ev: SchemaEncoder[M, S]): SchemaEncoder[M, S] = ev

    implicit lazy val cursorSchemaEncoder: SchemaEncoder[RowNumber, Cursor] =
      model =>
        Base64String
          .encodeWithPrefix(s"${model.value.value}", Cursor.prefix)
          .map(Cursor.apply)
          .toOption
          .get

    private[this] def uuidSchemaEncoder(prefix: String): SchemaEncoder[UUID, NodeId] =
      model =>
        Base64String
          .encodeWithPrefix(model.toString, prefix)
          .map(NodeId.apply)
          .toOption
          .get

    implicit lazy val userIdSchemaEncoder: SchemaEncoder[UserId, NodeId] =
      model => uuidSchemaEncoder(UserNode.idPrefix).from(model.value)

    implicit lazy val repositoryIdSchemaEncoder: SchemaEncoder[RepositoryId, NodeId] =
      model => uuidSchemaEncoder(RepositoryNode.idPrefix).from(model.value)

    implicit lazy val issueIdSchemaEncoder: SchemaEncoder[IssueId, NodeId] =
      model => uuidSchemaEncoder(IssueNode.idPrefix).from(model.value)

    implicit def userSchemaEncoder[F[_]](
      implicit idSchemaEncoder: SchemaEncoder[UserId, NodeId]
    ): SchemaEncoder[
      (
        User,
        RepositoryArg => F[Option[RepositoryNode[F]]],
        RepositoriesArg => F[Connection[F, RepositoryNode[F]]]
      ),
      UserNode[F]
    ] = {
      case (model, getRepositoryF, getRepositoriesF) =>
        UserNode(
          id = idSchemaEncoder.from(model.id),
          name = model.name.value,
          createdAt = model.createdAt,
          updatedAt = model.updatedAt,
          repository = getRepositoryF,
          repositories = getRepositoriesF
        )
    }

    implicit def repositorySchemaEncoder[F[_]](
      implicit idSchemaEncoder: SchemaEncoder[RepositoryId, NodeId]
    ): SchemaEncoder[
      (
        Repository,
        IssueArg => F[Option[IssueNode[F]]],
        IssuesArg => F[Connection[F, IssueNode[F]]]
      ),
      RepositoryNode[F]
    ] = {
      case (model, getIssueF, getIssuesF) =>
        RepositoryNode(
          id = idSchemaEncoder.from(model.id),
          name = model.name.value,
          url = model.url.value,
          isFork = model.isFork,
          createdAt = model.createdAt,
          updatedAt = model.updatedAt,
          issue = getIssueF,
          issues = getIssuesF
        )
    }

    implicit def issueSchemaEncoder[F[_]](
      implicit idSchemaEncoder: SchemaEncoder[IssueId, NodeId]
    ): SchemaEncoder[Issue, IssueNode[F]] =
      model =>
        IssueNode(
          id = idSchemaEncoder.from(model.id),
          number = model.number.value,
          status = model.status match {
            case models.IssueStatus.Open =>
              schemas.IssueStatus.OPEN
            case models.IssueStatus.Close =>
              schemas.IssueStatus.CLOSE
          },
          title = model.title.value,
          body = model.body.value,
          createdAt = model.createdAt,
          updatedAt = model.updatedAt
        )

    implicit def edgeSchemaEncoder[F[_], M, S <: Node[F]](
      implicit cSchemaEncoder: SchemaEncoder[RowNumber, Cursor],
      nSchemaEncoder: SchemaEncoder[M, S]
    ): SchemaEncoder[(M, RowNumber), Edge[F, S]] = {
      case (model, rowNumber) =>
        Edge(
          cursor = cSchemaEncoder.from(rowNumber),
          node = nSchemaEncoder.from(model)
        )
    }
  }

  final class SchemaEncoderOps[M](private val model: M) extends AnyVal {
    def encodeFrom[S](implicit schemaEncoder: SchemaEncoder[M, S]): S =
      schemaEncoder.from(model)
  }

  /**
    * Decodes a GraphQL schema into a model
    */
  trait SchemaDecoder[S, M] {
    def to(schema: S): Either[Throwable, M]
  }

  object SchemaDecoder {
    def apply[S, M](implicit ev: SchemaDecoder[S, M]): SchemaDecoder[S, M] = ev

    private[this] def uuidSchemaDecoder(prefix: String): SchemaDecoder[NodeId, UUID] =
      schema =>
        Base64String
          .decodeWithoutPrefix(schema.value, prefix)
          .flatMap(uuidString => Either.catchNonFatal(UUID.fromString(uuidString)))

    implicit lazy val userIdSchemaDecoder: SchemaDecoder[NodeId, UserId] =
      schema => uuidSchemaDecoder(UserNode.idPrefix).to(schema).map(UserId.apply)

    implicit lazy val repositoryIdSchemaDecoder: SchemaDecoder[NodeId, RepositoryId] =
      schema => uuidSchemaDecoder(RepositoryNode.idPrefix).to(schema).map(RepositoryId.apply)

    implicit lazy val issueIdSchemaDecoder: SchemaDecoder[NodeId, IssueId] =
      schema => uuidSchemaDecoder(IssueNode.idPrefix).to(schema).map(IssueId.apply)

    implicit lazy val cursorSchemaDecoder: SchemaDecoder[Cursor, RowNumber] =
      schema =>
        Base64String
          .decodeWithoutPrefix(schema.value, Cursor.prefix)
          .flatMap(cursorString => PosLong.from(cursorString.toLong).leftMap(new IllegalArgumentException(_)))
          .map(RowNumber.apply)

    implicit lazy val firstSchemaDecoder: SchemaDecoder[First, Limit] =
      schema => Limit(schema.value).asRight[Throwable]

    implicit def optionSchemaDecoder[S, M](
      implicit schemaDecoder: SchemaDecoder[S, M]
    ): SchemaDecoder[Option[S], Option[M]] =
      maybeSchema => maybeSchema.fold(Option.empty[M])(i => schemaDecoder.to(i).toOption).asRight[Throwable]
  }
}

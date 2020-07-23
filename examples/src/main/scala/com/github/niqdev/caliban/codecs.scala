package com.github.niqdev.caliban

import java.util.UUID

import caliban.pagination.Base64String
import caliban.pagination.schemas.{ Cursor, First, NodeId }
import cats.syntax.either._
import com.github.niqdev.caliban.arguments.RepositoriesArg
import com.github.niqdev.caliban.models._
import com.github.niqdev.caliban.repositories._
import com.github.niqdev.caliban.schema._
import eu.timepit.refined.types.numeric.PosLong

// TODO SchemaDecoderOps + move instances in sealed traits
object codecs {

  /**
    * Schema encoder
    */
  trait SchemaEncoder[A, B] {
    def from(model: A): B
  }

  object SchemaEncoder {
    def apply[A, B](implicit ev: SchemaEncoder[A, B]): SchemaEncoder[A, B] = ev

    // TODO
    implicit lazy val cursorSchemaEncoder: SchemaEncoder[RowNumber, Cursor] =
      rowNumber =>
        Base64String
          .encodeWithPrefix(s"${rowNumber.value.value}", Cursor.prefix)
          .map(Cursor.apply)
          .toOption
          .get

    implicit lazy val userNodeIdSchemaEncoder: SchemaEncoder[UserId, NodeId] =
      model =>
        Base64String
          .encodeWithPrefix(s"${model.value.toString}", UserNode.idPrefix)
          .map(NodeId.apply)
          .toOption
          .get

    implicit def userNodeSchemaEncoder[F[_]](
      implicit uniSchemaEncoder: SchemaEncoder[UserId, NodeId]
    ): SchemaEncoder[(User, RepositoriesArg => F[RepositoryConnection[F]]), UserNode[F]] = {
      case (user, getRepositoryConnectionF) =>
        UserNode(
          id = uniSchemaEncoder.from(user.id),
          name = user.name.value,
          createdAt = user.createdAt,
          updatedAt = user.updatedAt,
          repositories = getRepositoryConnectionF
        )
    }

    implicit lazy val repositoryNodeIdSchemaEncoder: SchemaEncoder[RepositoryId, NodeId] =
      model =>
        Base64String
          .encode(s"${RepositoryNode.idPrefix}${model.value.toString}")
          .map(NodeId.apply)
          .toOption
          .get

    implicit def repositoryNodeSchemaEncoder[F[_]](
      implicit rniSchemaEncoder: SchemaEncoder[RepositoryId, NodeId]
    ): SchemaEncoder[Repository, RepositoryNode[F]] =
      model =>
        RepositoryNode(
          id = rniSchemaEncoder.from(model.id),
          name = model.name.value,
          url = model.url.value,
          isFork = model.isFork,
          createdAt = model.createdAt,
          updatedAt = model.updatedAt
        )

    implicit def repositoryEdgeSchemaEncoder[F[_]](
      implicit cSchemaEncoder: SchemaEncoder[RowNumber, Cursor],
      //rniSchemaEncoder: SchemaEncoder[RepositoryId, NodeId],
      rnSchemaEncoder: SchemaEncoder[Repository, RepositoryNode[F]]
    ): SchemaEncoder[(Repository, RowNumber), RepositoryEdge[F]] = {
      case (model, rowNumber) =>
        RepositoryEdge(
          cursor = cSchemaEncoder.from(rowNumber),
          node = rnSchemaEncoder.from(model)
        )
    }
  }

  final class SchemaEncoderOps[A](private val model: A) extends AnyVal {
    def encodeFrom[B](implicit schemaEncoder: SchemaEncoder[A, B]): B =
      schemaEncoder.from(model)
  }

  /**
    * Schema decoder
    */
  trait SchemaDecoder[A, B] {
    def to(schema: A): Either[Throwable, B]
  }

  object SchemaDecoder {
    def apply[A, B](implicit ev: SchemaDecoder[A, B]): SchemaDecoder[A, B] = ev

    private[this] def uuidSchemaDecoder(prefix: String): SchemaDecoder[NodeId, UUID] =
      schema =>
        Base64String
          .decodeWithoutPrefix(schema.value, prefix)
          .flatMap(uuidString => Either.catchNonFatal(UUID.fromString(uuidString)))

    implicit lazy val userIdSchemaDecoder: SchemaDecoder[NodeId, UserId] =
      schema => uuidSchemaDecoder(UserNode.idPrefix).to(schema).map(UserId.apply)

    implicit lazy val repositoryIdSchemaDecoder: SchemaDecoder[NodeId, RepositoryId] =
      schema => uuidSchemaDecoder(RepositoryNode.idPrefix).to(schema).map(RepositoryId.apply)

    implicit lazy val cursorSchemaDecoder: SchemaDecoder[Cursor, RowNumber] =
      schema =>
        Base64String
          .decodeWithoutPrefix(schema.value, Cursor.prefix)
          .flatMap(cursorString => PosLong.from(cursorString.toLong).leftMap(new IllegalArgumentException(_)))
          .map(RowNumber.apply)

    implicit lazy val firstSchemaDecoder: SchemaDecoder[First, Limit] =
      schema => Limit(schema.value).asRight[Throwable]

    implicit def optionSchemaDecoder[I, O](
      implicit schemaDecoder: SchemaDecoder[I, O]
    ): SchemaDecoder[Option[I], Option[O]] =
      maybeSchema => maybeSchema.fold(Option.empty[O])(i => schemaDecoder.to(i).toOption).asRight[Throwable]
  }
}

package caliban.pagination

import caliban.interop.cats.CatsInterop
import caliban.pagination.schemas.{ Cursor, First, Last, NodeId }
import caliban.schema.Annotations.GQLInterface
import caliban.schema.Schema
import cats.effect.Effect
import eu.timepit.refined.types.numeric.{ NonNegInt, NonNegLong }
import io.estatico.newtype.macros.newtype

// TODO refined schema
object schemas extends SchemaInstances {

  @newtype case class NodeId(value: Base64String)
  @newtype case class Cursor(value: Base64String)
  @newtype case class First(value: NonNegInt)
  @newtype case class Last(value: NonNegInt)

  // TODO trait WithPrefix or Node.prefix?
  // TODO every Node must have a prefix
  object Cursor {
    final val prefix = "cursor:v1:"
  }

  @GQLInterface
  trait Node[F[_]] {
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
}

protected[pagination] sealed trait SchemaInstances {

  // see caliban.interop.cats.implicits.effectSchema
  implicit def effectSchema[F[_]: Effect, R, A](implicit ev: Schema[R, A]): Schema[R, F[A]] =
    CatsInterop.schema

  implicit val base64StringSchema: Schema[Any, Base64String] =
    Schema.stringSchema.contramap(_.value)

  implicit val nodeIdSchema: Schema[Any, NodeId] =
    base64StringSchema.contramap(_.value)

  implicit val cursorSchema: Schema[Any, Cursor] =
    base64StringSchema.contramap(_.value)

  implicit val firstSchema: Schema[Any, First] =
    Schema.intSchema.contramap(_.value.value)

  implicit val lastSchema: Schema[Any, Last] =
    Schema.intSchema.contramap(_.value.value)

  implicit val nonNegLongSchema: Schema[Any, NonNegLong] =
    Schema.longSchema.contramap(_.value)
}

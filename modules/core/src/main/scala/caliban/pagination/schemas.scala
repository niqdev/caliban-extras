package caliban.pagination

import caliban.interop.cats.CatsInterop
import caliban.pagination.schemas._
import caliban.schema.Schema
import cats.effect.Effect
import eu.timepit.refined.types.numeric.{ NonNegInt, NonNegLong }
import io.estatico.newtype.macros.newtype

object schemas extends PaginationSchemaInstances {

  @newtype case class NodeId(value: Base64String)
  @newtype case class Cursor(value: Base64String)
  @newtype case class First(value: NonNegInt)
  @newtype case class Last(value: NonNegInt)

  // TODO every Node must have a prefix
  object Cursor {
    final val prefix = "cursor:v1:"
  }

  // TODO not supported by caliban: to be able to expose it as Root node, it must be a sealed trait and higher-kinded
  /*
  @GQLInterface
  trait Node {
    def id: NodeId
  }
   */
}

// TODO add generic refined schema/argument derivation
protected[pagination] sealed trait PaginationSchemaInstances {

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

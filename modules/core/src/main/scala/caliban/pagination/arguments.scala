package caliban.pagination

import caliban.pagination.schemas.{ Cursor, First, Last, NodeId }
import caliban.schema.ArgBuilder
import eu.timepit.refined.types.numeric.NonNegInt
import io.estatico.newtype.Coercible

object arguments {

  object implicits extends PaginationArgumentInstances

  final case class NodeArg(id: NodeId)
  final case class NodesArg(ids: List[NodeId])

  trait ForwardPaginationArg {
    def first: First
    // "after" is the cursor of the last edge in the previous page
    def after: Option[Cursor]
  }

  trait BackwardPaginationArg {
    def last: Last
    // "before" is the cursor of the first edge in the next page
    def before: Option[Cursor]
  }
}

protected[pagination] sealed trait PaginationArgumentInstances {

  implicit def base64StringArgBuilder[T](
    implicit ab: ArgBuilder[Base64String],
    coercible: Coercible[Base64String, T]
  ): ArgBuilder[T] =
    ab.map(coercible.apply)

  implicit def nonNegIntArgBuilder[T](
    implicit ab: ArgBuilder[NonNegInt],
    coercible: Coercible[NonNegInt, T]
  ): ArgBuilder[T] =
    ab.map(coercible.apply)
}

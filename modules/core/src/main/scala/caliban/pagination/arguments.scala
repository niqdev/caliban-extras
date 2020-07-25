package caliban.pagination

import caliban.pagination.schemas.{ Cursor, First, Last, NodeId }
import caliban.schema.ArgBuilder
import eu.timepit.refined.types.numeric.NonNegInt

object arguments extends PaginationArgumentInstances {

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

  implicit def nodeIdArgBuilder(
    implicit ab: ArgBuilder[Base64String]
  ): ArgBuilder[NodeId] =
    ab.map(NodeId.apply)

  implicit def cursorArgBuilder(
    implicit ab: ArgBuilder[Base64String]
  ): ArgBuilder[Cursor] =
    ab.map(Cursor.apply)

  implicit def firstArgBuilder(
    implicit ab: ArgBuilder[NonNegInt]
  ): ArgBuilder[First] =
    ab.map(First.apply)

  implicit def lastArgBuilder(
    implicit ab: ArgBuilder[NonNegInt]
  ): ArgBuilder[Last] =
    ab.map(Last.apply)
}

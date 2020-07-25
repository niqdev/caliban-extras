package caliban.pagination

import caliban.pagination.schemas.{ Cursor, First, Last, NodeId }

object arguments {

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

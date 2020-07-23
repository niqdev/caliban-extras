package caliban.pagination

import caliban.CalibanError
import caliban.Value.{ IntValue, StringValue }
import caliban.pagination.schemas.{ Cursor, First, Last, NodeId }
import caliban.schema.ArgBuilder
import cats.syntax.either._
import eu.timepit.refined.types.numeric.NonNegInt

object arguments extends ArgumentInstances {

  final case class NodeArg(id: NodeId)

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

protected[pagination] sealed trait ArgumentInstances {

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

  implicit val nonNegIntArgBuilder: ArgBuilder[NonNegInt] = {
    case value: IntValue =>
      NonNegInt.from(value.toInt).leftMap(CalibanError.ExecutionError(_))
    case other =>
      Left(CalibanError.ExecutionError(s"Can't build a NonNegInt from input $other"))
  }

  implicit val firstArgBuilder: ArgBuilder[First] =
    nonNegIntArgBuilder.map(First.apply)

  implicit val lastArgBuilder: ArgBuilder[Last] =
    nonNegIntArgBuilder.map(Last.apply)
}

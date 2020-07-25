package caliban.pagination

import eu.timepit.refined.types.numeric.NonNegInt
import io.estatico.newtype.macros.newtype

object schemas {

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

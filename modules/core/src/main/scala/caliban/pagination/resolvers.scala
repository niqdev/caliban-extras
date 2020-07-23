package caliban.pagination

import caliban.pagination.arguments._
import caliban.pagination.schemas._
import caliban.{ GraphQL, RootResolver }
import cats.effect.Effect

object resolvers {

  /**
    * Node roots
    */
  final case class NodeQueries[F[_]](
    node: NodeArg => F[Option[Node[F]]],
    nodes: NodesArg => F[List[Node[F]]]
  )
  object NodeQueries {
    private[this] def resolver[F[_]: Effect]: NodeQueries[F] =
      NodeQueries(
        node = _ => Effect[F].pure(None),
        nodes = _ => Effect[F].pure(List.empty)
      )

    // TODO log errors: mapError or Wrapper
    def api[F[_]: Effect]: GraphQL[Any] =
      GraphQL.graphQL(RootResolver(resolver[F]))
  }
}

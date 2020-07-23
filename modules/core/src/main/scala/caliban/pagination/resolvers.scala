package caliban.pagination

import caliban.pagination.arguments._
import caliban.pagination.schemas._
import caliban.{ GraphQL, RootResolver }
import cats.effect.Effect

object resolvers {

  /**
    * Query resolver
    */
  final case class Queries[F[_]](
    node: NodeArg => F[Option[Node[F]]]
  )
  object Queries {
    private[this] def resolver[F[_]: Effect]: Queries[F] =
      Queries(
        node = _ => Effect[F].pure(None)
      )

    // TODO log errors: mapError or Wrapper
    def api[F[_]: Effect]: GraphQL[Any] =
      GraphQL.graphQL(RootResolver(resolver[F]))
  }
}

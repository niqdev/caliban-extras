package com.github.niqdev.caliban

import caliban.{ GraphQL, RootResolver }
import cats.effect.Effect
import com.github.niqdev.caliban.schema._
import com.github.niqdev.caliban.schema.arguments._
import com.github.niqdev.caliban.services._

object queries {

  /**
    * Root Nodes
    */
  final case class Queries[F[_]](
    node: NodeArg => F[Option[Node[F]]],
    user: UserArg => F[Option[UserNode[F]]],
    repository: RepositoryArg => F[Option[RepositoryNode[F]]],
    repositories: ForwardPaginationArg => F[RepositoryConnection[F]]
  )
  object Queries {
    private[this] def resolver[F[_]: Effect](services: Services[F]): Queries[F] =
      Queries(
        node = nodeArg => services.nodeService.findNode(nodeArg.id),
        user = userArg => services.userService.findByName(userArg.name),
        repository = repositoryArg => services.repositoryService.findByName(repositoryArg.name),
        repositories = services.repositoryService.connection(None)
      )

    // TODO log errors: mapError or Wrapper
    def api[F[_]: Effect](services: Services[F]): GraphQL[Any] =
      GraphQL.graphQL(RootResolver(resolver[F](services)))
  }
}

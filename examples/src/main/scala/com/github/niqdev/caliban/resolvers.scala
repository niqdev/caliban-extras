package com.github.niqdev.caliban

import caliban.{ GraphQL, RootResolver }
import cats.effect.{ Effect, Resource, Sync }
import com.github.niqdev.caliban.schemas._
import com.github.niqdev.caliban.services._

object resolvers {
  import caliban.interop.cats.implicits._
  import caliban.filter.schemas._
  import caliban.refined._

  object NodeRootResolver {
    object NodeQueries {
      private[resolvers] def resolver[F[_]: Effect](services: Services[F]): NodeRoot[F] =
        NodeRoot(
          node = nodeArg => services.nodeService.findNode(nodeArg.id),
          nodes = nodesArg => services.nodeService.findNodes(nodesArg.ids)
        )
    }
    def api[F[_]: Effect](services: Services[F]): GraphQL[Any] =
      GraphQL.graphQL(RootResolver(NodeQueries.resolver[F](services)))
  }

  object GitHubRootResolver {
    object GitHubQueries {
      private[resolvers] def resolver[F[_]: Effect](services: Services[F]): GitHubRoot[F] =
        GitHubRoot(
          user = userArg => services.userService.findByName(userArg.name),
          repository = repositoryArg => services.repositoryService.findByName(repositoryArg.name),
          repositories = services.repositoryService.connection(None)
        )
    }
    def api[F[_]: Effect](services: Services[F]): GraphQL[Any] =
      GraphQL.graphQL(RootResolver(GitHubQueries.resolver[F](services)))
  }

  /**
    * Resolvers
    */
  object Resolvers {
    // TODO log errors: mapError or Wrapper
    private[this] def api[F[_]: Effect](services: Services[F]): GraphQL[Any] =
      NodeRootResolver.api[F](services) |+|
        GitHubRootResolver.api[F](services)

    def make[F[_]: Effect](services: Services[F]) =
      Resource.liftF(Sync[F].pure(api[F](services)))
  }
}

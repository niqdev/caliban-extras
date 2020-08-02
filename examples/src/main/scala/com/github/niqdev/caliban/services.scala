package com.github.niqdev.caliban

import caliban.pagination.schemas._
import cats.effect.{ Resource, Sync }
import cats.instances.list._
import cats.instances.option._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.foldable._
import cats.syntax.functor._
import cats.syntax.nested._
import cats.syntax.traverse._
import com.github.niqdev.caliban.arguments.RepositoriesArg
import com.github.niqdev.caliban.codecs._
import com.github.niqdev.caliban.models._
import com.github.niqdev.caliban.repositories._
import com.github.niqdev.caliban.schemas._
import eu.timepit.refined.types.string.NonEmptyString

object services {

  /**
    * User service
    */
  sealed abstract class UserService[F[_]](
    userRepo: UserRepo[F],
    repositoryService: RepositoryService[F]
  )(
    implicit F: Sync[F]
  ) {

    protected[caliban] val toNode: User => UserNode[F] =
      user => (user, repositoryService.connection(Some(user.id))).encodeFrom[UserNode[F]]

    def findNode(id: NodeId): F[Option[UserNode[F]]] =
      F.fromEither(SchemaDecoder[NodeId, UserId].to(id))
        .flatMap(userRepo.findById)
        .nested
        .map(toNode)
        .value

    def findByName(name: NonEmptyString): F[Option[UserNode[F]]] =
      userRepo.findByName(name).nested.map(toNode).value

  }
  object UserService {
    def apply[F[_]: Sync](
      userRepo: UserRepo[F],
      repositoryService: RepositoryService[F]
    ): UserService[F] =
      new UserService[F](userRepo, repositoryService) {}
  }

  /**
    * Repository service
    */
  sealed abstract class RepositoryService[F[_]](
    repositoryRepo: RepositoryRepo[F]
  )(
    implicit F: Sync[F]
  ) {

    protected[caliban] val toNode: Repository => RepositoryNode[F] =
      _.encodeFrom[RepositoryNode[F]]

    protected[caliban] val toEdge: Repository => RowNumber => Edge[F, RepositoryNode[F]] =
      repository => rowNumber => (repository -> rowNumber).encodeFrom[Edge[F, RepositoryNode[F]]]

    def findNode(id: NodeId): F[Option[RepositoryNode[F]]] =
      F.fromEither(SchemaDecoder[NodeId, RepositoryId].to(id))
        .flatMap(repositoryRepo.findById)
        .nested
        .map(toNode)
        .value

    def findByName(name: NonEmptyString): F[Option[RepositoryNode[F]]] =
      repositoryRepo.findByName(name).nested.map(toNode).value

    // supports Forward Pagination only
    def connection(maybeUserId: Option[UserId]): RepositoriesArg => F[Connection[F, RepositoryNode[F]]] =
      paginationArg => {

        def isFullSize(repositories: List[(Repository, RowNumber)], limit: Limit) =
          repositories.length == limit.value.value

        def dropLastRepository(repositories: List[(Repository, RowNumber)], limit: Limit) =
          if (isFullSize(repositories, limit)) repositories.dropRight(1)
          else repositories

        for {
          limit <- F.fromEither(SchemaDecoder[First, Limit].to(paginationArg.first))
          limitPlusOne = Limit.inc(limit)
          nextRowNumber <- F.fromEither(
            SchemaDecoder[Option[Cursor], Option[RowNumber]].to(paginationArg.after)
          )
          // try to fetch N+1 repositories to efficiently verify hasNextPage
          repositoriesPlusOne <- maybeUserId.fold(repositoryRepo.find(limitPlusOne, nextRowNumber))(
            repositoryRepo.findByUserId(limitPlusOne, nextRowNumber)
          )
          repositories = dropLastRepository(repositoriesPlusOne, limitPlusOne)
          edges <- F.pure(repositories).nested.map(repository => toEdge(repository._1)(repository._2)).value
          nodes <- F.pure(repositories).nested.map(repository => toNode(repository._1)).value
          pageInfo <- F.pure {
            PageInfo(
              isFullSize(repositoriesPlusOne, limitPlusOne),
              false, // default false
              repositories.head._2.encodeFrom[Cursor],
              repositories.last._2.encodeFrom[Cursor]
            )
          }
          totalCount <- maybeUserId.fold(repositoryRepo.count)(repositoryRepo.countByUserId)
        } yield Connection(edges, nodes, pageInfo, totalCount)
      }

  }
  object RepositoryService {
    def apply[F[_]: Sync](repositoryRepo: RepositoryRepo[F]): RepositoryService[F] =
      new RepositoryService[F](repositoryRepo) {}
  }

  /**
    * Node service
    */
  sealed abstract class NodeService[F[_]](
    userService: UserService[F],
    repositoryService: RepositoryService[F]
  )(
    implicit F: Sync[F]
  ) {

    // TODO create specific error + log WARN
    private[this] def recoverInvalidNode[T <: Node[F]]: PartialFunction[Throwable, Option[T]] = {
      case _: IllegalArgumentException => None
    }

    def findNode(id: NodeId): F[Option[Node[F]]] =
      for {
        userNode       <- userService.findNode(id).recover(recoverInvalidNode[UserNode[F]])
        repositoryNode <- repositoryService.findNode(id).recover(recoverInvalidNode[RepositoryNode[F]])
      } yield List(userNode, repositoryNode).collectFirstSomeM(List(_)).head

    def findNodes(ids: List[NodeId]): F[List[Option[Node[F]]]] =
      F.pure(ids).flatMap(_.traverse(id => findNode(id)))
  }
  object NodeService {
    def apply[F[_]: Sync](repositories: Repositories[F]): NodeService[F] = {
      val repositoryService = RepositoryService[F](repositories.repositoryRepo)
      new NodeService[F](
        UserService[F](repositories.userRepo, repositoryService),
        repositoryService
      ) {}
    }
  }

  /**
    * Services
    */
  sealed trait Services[F[_]] {
    def nodeService: NodeService[F]
    def userService: UserService[F]
    def repositoryService: RepositoryService[F]
  }
  object Services {
    private[this] def apply[F[_]: Sync](repos: Repositories[F]) =
      new Services[F] {
        val nodeService: NodeService[F]             = NodeService[F](repos)
        val repositoryService: RepositoryService[F] = RepositoryService[F](repos.repositoryRepo)
        val userService: UserService[F]             = UserService[F](repos.userRepo, repositoryService)
      }

    def make[F[_]: Sync](repos: Repositories[F]) =
      Resource.liftF(Sync[F].pure(apply[F](repos)))
  }

}

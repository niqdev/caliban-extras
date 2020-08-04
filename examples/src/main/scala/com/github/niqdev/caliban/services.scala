package com.github.niqdev.caliban

import cats.effect.{ Resource, Sync }
import cats.instances.list._
import cats.instances.option._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.foldable._
import cats.syntax.functor._
import cats.syntax.nested._
import cats.syntax.traverse._
import com.github.niqdev.caliban.codecs._
import com.github.niqdev.caliban.models._
import com.github.niqdev.caliban.repositories._
import com.github.niqdev.caliban.schemas._
import eu.timepit.refined.types.numeric.NonNegLong
import eu.timepit.refined.types.string.NonEmptyString

// TODO remove annotation
@scala.annotation.nowarn
object services {

  // TODO improve types Effect | Id | Node (schema) | Model
  sealed abstract class BaseService[F[_], I, N <: Node[F], M](
    repository: BaseRepository[F, I, M]
  )(
    implicit F: Sync[F],
    idSchemaDecoder: SchemaDecoder[NodeId, I]
  ) {
    // TODO implement for all
    // from model to schema (node)
    protected def toNode: M => N

    def findNode(id: NodeId): F[Option[N]] =
      F.fromEither(idSchemaDecoder.to(id))
        .flatMap(repository.findById)
        .nested
        .map(toNode)
        .value
  }

  sealed abstract class PaginationService[F[_], I, N <: Node[F], M](
    repository: BaseRepository[F, I, M]
  )(
    implicit F: Sync[F],
    idSchemaDecoder: SchemaDecoder[NodeId, I]
  ) extends BaseService[F, I, N, M](repository) {

    // TODO implement for all
    protected def toEdge: M => RowNumber => Edge[F, N]

    protected def findConnection(
      findItems: (Limit, Option[RowNumber]) => F[List[(M, RowNumber)]],
      countItems: => F[NonNegLong]
    ): ForwardPaginationArg => F[Connection[F, N]] =
      paginationArg => {

        def isFullSize(items: List[(M, RowNumber)], limit: Limit) =
          items.length == limit.value.value

        def dropLastItem(items: List[(M, RowNumber)], limit: Limit) =
          if (isFullSize(items, limit)) items.dropRight(1)
          else items

        for {
          limit <- F.fromEither(SchemaDecoder[First, Limit].to(paginationArg.first))
          limitPlusOne = Limit.inc(limit)
          nextRowNumber <- F.fromEither(
            SchemaDecoder[Option[Cursor], Option[RowNumber]].to(paginationArg.after)
          )
          // try to fetch N+1 items to efficiently verify hasNextPage
          itemsPlusOne <- findItems(limitPlusOne, nextRowNumber)
          items = dropLastItem(itemsPlusOne, limitPlusOne)
          edges <- F.pure(items).nested.map(repository => toEdge(repository._1)(repository._2)).value
          nodes <- F.pure(items).nested.map(repository => toNode(repository._1)).value
          pageInfo <- F.pure {
            PageInfo(
              isFullSize(itemsPlusOne, limitPlusOne),
              false, // default false
              items.head._2.encodeFrom[Cursor],
              items.last._2.encodeFrom[Cursor]
            )
          }
          totalCount <- countItems
        } yield Connection(edges, nodes, pageInfo, totalCount)
      }
  }

  /**
    * User service
    */
  sealed abstract class UserService[F[_]: Sync](
    userRepo: UserRepo[F],
    repositoryService: RepositoryService[F]
  ) extends BaseService[F, UserId, UserNode[F], User](userRepo) {

    // TODO move repositoryService.findRepositoryConnection in SchemaF
    override protected val toNode: User => UserNode[F] =
      user => (user, repositoryService.findRepositoryConnection(Some(user.id))).encodeFrom[UserNode[F]]

    def findByName(name: NonEmptyString): F[Option[UserNode[F]]] =
      userRepo.findByName(name).nested.map(toNode).value
  }

  /**
    * Repository service
    */
  sealed abstract class RepositoryService[F[_]: Sync](
    repositoryRepo: RepositoryRepo[F],
    issueService: IssueService[F]
  ) extends PaginationService[F, RepositoryId, RepositoryNode[F], Repository](repositoryRepo) {

    override protected val toNode: Repository => RepositoryNode[F] =
      _.encodeFrom[RepositoryNode[F]]

    override protected val toEdge: Repository => RowNumber => Edge[F, RepositoryNode[F]] =
      repository => rowNumber => (repository -> rowNumber).encodeFrom[Edge[F, RepositoryNode[F]]]

    def findByName(name: NonEmptyString): F[Option[RepositoryNode[F]]] =
      repositoryRepo.findByName(name).nested.map(toNode).value

    def findRepositoryConnection(
      maybeUserId: Option[UserId]
    ): RepositoriesArg => F[Connection[F, RepositoryNode[F]]] = {
      val findItems: (Limit, Option[RowNumber]) => F[List[(Repository, RowNumber)]] =
        (limit, rowNumber) =>
          maybeUserId.fold(repositoryRepo.find(limit, rowNumber))(
            repositoryRepo.findByUserId(limit, rowNumber)
          )
      val countItems: F[NonNegLong] =
        maybeUserId.fold(repositoryRepo.count)(repositoryRepo.countByUserId)

      findConnection(findItems, countItems)
    }
  }

  /**
    * Issue service
    */
  sealed abstract class IssueService[F[_]: Sync](
    issueRepo: IssueRepo[F]
  ) extends PaginationService[F, IssueId, IssueNode[F], Issue](issueRepo) {
    override protected def toEdge: Issue => RowNumber => Edge[F, IssueNode[F]] = ???
    override protected def toNode: Issue => IssueNode[F]                       = ???
  }

  /**
    * Node service
    */
  sealed abstract class NodeService[F[_]](
    userService: UserService[F],
    repositoryService: RepositoryService[F],
    issueService: IssueService[F]
  )(implicit F: Sync[F]) {

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

  /**
    * Services
    */
  sealed trait Services[F[_]] {
    def nodeService: NodeService[F]
    def userService: UserService[F]
    def repositoryService: RepositoryService[F]
    def issueService: IssueService[F]
  }
  object Services {
    private[this] def apply[F[_]: Sync](repos: Repositories[F]) =
      new Services[F] {
        val issueService: IssueService[F] =
          new IssueService[F](repos.issueRepo) {}
        val repositoryService: RepositoryService[F] =
          new RepositoryService[F](repos.repositoryRepo, issueService) {}
        val userService: UserService[F] =
          new UserService[F](repos.userRepo, repositoryService) {}
        val nodeService: NodeService[F] =
          new NodeService[F](userService, repositoryService, issueService) {}
      }

    def make[F[_]: Sync](repos: Repositories[F]) =
      Resource.liftF(Sync[F].pure(apply[F](repos)))
  }
}

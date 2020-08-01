package com.github.niqdev.caliban

import java.time.Instant
import java.util.UUID

import enumeratum.values.{ StringCirceEnum, StringDoobieEnum, StringEnum, StringEnumEntry }
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Url
import eu.timepit.refined.types.numeric.PosInt
import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype

object models {

  @newtype case class UserId(value: UUID)
  @newtype case class UserName(value: NonEmptyString)

  final case class User(
    id: UserId,
    name: UserName,
    createdAt: Instant,
    updatedAt: Instant
  )

  @newtype case class RepositoryId(value: UUID)
  @newtype case class RepositoryName(value: NonEmptyString)
  @newtype case class RepositoryUrl(value: String Refined Url)

  final case class Repository(
    id: RepositoryId,
    userId: UserId,
    name: RepositoryName,
    url: RepositoryUrl,
    isFork: Boolean,
    createdAt: Instant,
    updatedAt: Instant
  )

  @newtype case class IssueId(value: UUID)
  @newtype case class IssueNumber(value: PosInt)
  @newtype case class IssueTitle(value: NonEmptyString)
  @newtype case class IssueBody(value: NonEmptyString)

  sealed abstract class IssueStatus(val value: String) extends StringEnumEntry

  case object IssueStatus
      extends StringEnum[IssueStatus]
      with StringCirceEnum[IssueStatus]
      with StringDoobieEnum[IssueStatus] {

    val values = findValues

    case object Open  extends IssueStatus("OPEN")
    case object Close extends IssueStatus("CLOSE")
  }

  final case class Issue(
    id: IssueId,
    repositoryId: RepositoryId,
    number: IssueNumber,
    status: IssueStatus,
    title: IssueTitle,
    body: IssueBody,
    createdAt: Instant,
    updatedAt: Instant
  )
}

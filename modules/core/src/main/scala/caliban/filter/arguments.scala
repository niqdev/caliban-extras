package caliban.filter

import caliban.schema.ArgBuilder
import caliban.{ CalibanError, InputValue }
import cats.data.NonEmptyList
import cats.instances.either._
import cats.instances.list._
import cats.syntax.traverse._
import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype

object arguments {

  @newtype case class FieldName(value: NonEmptyString)
  @newtype case class FieldValue(value: NonEmptyString)

  sealed trait Operator
  object Operator {
    final case object EQUAL                 extends Operator
    final case object NOT_EQUAL             extends Operator
    final case object SMALLER_THAN          extends Operator
    final case object SMALLER_THAN_OR_EQUAL extends Operator
    final case object GREATER_THAN          extends Operator
    final case object GREATER_THAN_OR_EQUAL extends Operator
    final case object IN                    extends Operator
    final case object NOT_IN                extends Operator
    final case object LIKE                  extends Operator
    final case object NOT_LIKE              extends Operator
    final case object BETWEEN               extends Operator
    final case object NOT_BETWEEN           extends Operator
    final case object IS_NULL               extends Operator
    final case object IS_NOT_NULL           extends Operator
  }

  sealed trait Conjunction
  object Conjunction {
    final case object AND extends Conjunction
    final case object OR  extends Conjunction
  }

  final case class Condition(
    operator: Operator,
    field: FieldName,
    value: NonEmptyList[FieldValue]
  )

  // TODO conjunction is Option[Conjunction] ?
  // TODO add groups
  final case class Filter(
    conjunction: Conjunction = Conjunction.AND,
    conditions: NonEmptyList[Condition]
  )

  /**
    * Filter Argument
    */
  trait FilterArg {
    def filters: Option[Filter]
  }

  implicit def nonEmptyListArgBuilder[A](
    implicit ev: ArgBuilder[A]
  ): ArgBuilder[NonEmptyList[A]] = {
    case InputValue.ListValue(items) if items.nonEmpty =>
      items.map(ev.build).sequence.map(NonEmptyList.fromListUnsafe)
    case other =>
      Left(CalibanError.ExecutionError(s"Can't build a NonEmptyList from input $other"))
  }
}

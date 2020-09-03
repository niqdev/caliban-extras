package caliban.filter

import caliban.filter.schemas._
import caliban.schema.{ ArgBuilder, Schema }
import caliban.{ CalibanError, InputValue }
import cats.data.NonEmptyList
import cats.syntax.traverse._
import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype

object schemas extends FilterSchemaInstances with FilterArgInstances {

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
}

sealed trait FilterSchemaInstances {

  implicit val fieldNameSchema: Schema[Any, FieldName] =
    Schema.stringSchema.contramap(_.value.value)
  implicit val fieldValueSchema: Schema[Any, FieldValue] =
    Schema.stringSchema.contramap(_.value.value)

  implicit val operatorSchema: Schema[Any, Operator] =
    Schema.gen[Operator]
  implicit val conjunctionSchema: Schema[Any, Conjunction] =
    Schema.gen[Conjunction]
  implicit val conditionSchema: Schema[Any, Condition] =
    Schema.gen[Condition]
  implicit val filterSchema: Schema[Any, Filter] =
    Schema.gen[Filter]

  implicit def nonEmptyListSchema[A](
    implicit ev: Schema[Any, A]
  ): Schema[Any, NonEmptyList[A]] =
    Schema.listSchema[A].contramap(_.toList)
}

sealed trait FilterArgInstances {

  implicit def nonEmptyListArgBuilder[A](
    implicit ev: ArgBuilder[A]
  ): ArgBuilder[NonEmptyList[A]] = {
    case InputValue.ListValue(items) if items.nonEmpty =>
      items.map(ev.build).sequence.map(NonEmptyList.fromListUnsafe)
    case other =>
      Left(CalibanError.ExecutionError(s"Can't build a NonEmptyList from input $other"))
  }
}

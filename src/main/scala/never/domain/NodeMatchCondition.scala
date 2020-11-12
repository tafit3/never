package never.domain

sealed trait NodeMatchCondition
object NodeMatchCondition {
  case object Empty extends NodeMatchCondition
  case class And(conditions: List[NodeMatchCondition]) extends NodeMatchCondition
  case class Or(conditions: List[NodeMatchCondition]) extends NodeMatchCondition
  case class StatusEquals(status: String) extends NodeMatchCondition
  case class TagsContainAnyOf(tags: Set[String]) extends NodeMatchCondition
  case class TagsContainNoneOf(tags: Set[String]) extends NodeMatchCondition
}

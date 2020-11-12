package never.domain

import never.FilteredViewCondition
import never.domain.NodeMatchCondition._

import scala.collection.mutable.ListBuffer

object NodeMatchConditionConverter {
  def toNodeMatchCondition(conditions: List[FilteredViewCondition]): NodeMatchCondition = {
    Or(conditions.map(toNodeMatchCondition))
  }

  private def toNodeMatchCondition(condition: FilteredViewCondition): NodeMatchCondition = {
    val components = ListBuffer.empty[NodeMatchCondition]
    condition.statusEqualTo.foreach(status => components.addOne(StatusEquals(status)))
    condition.containsAnyOfTags.foreach(tags => components.addOne(TagsContainAnyOf(tags)))
    condition.containsNoneOfTags.foreach(tags => components.addOne(TagsContainNoneOf(tags)))
    And(components.toList)
  }
}

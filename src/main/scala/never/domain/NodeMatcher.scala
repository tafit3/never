package never.domain

import never.domain.NodeMatchCondition._
import never.repository.Node

class NodeMatcher {

  def matches(node: Node, condition: NodeMatchCondition): Boolean = {
    new LocalMatcher(node).matches(condition)
  }

  private class LocalMatcher(node: Node) {
    def matches(condition: NodeMatchCondition): Boolean = {
      condition match {
        case Empty => true
        case And(conditions) => conditions.forall(matches)
        case Or(conditions) => conditions.exists(matches)
        case StatusEquals(status) => node.status == status
        case TagsContainAnyOf(tags) => tags.exists(node.tags)
        case TagsContainNoneOf(tags) => !tags.exists(node.tags)
        case _ => throw new IllegalStateException(s"Unsupport condition type: '$condition'")
      }
    }
  }
}

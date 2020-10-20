package never.domain

import java.time.Instant

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}

// views

case class NodeView(id: Long, created: Instant, status: String, content: String, depth: Int, parent: Option[Long], expandable: Boolean) {
  require(id > 0, "id must be greater than 0")
}

// event helper classes

case class ParentInfo(id: Long, childIndex: Int) {
  require(id > 0, "id must be greater than 0")
  require(childIndex >= 0, "childIndex must be greater than or equal to 0")
}

// events

case class NodeEvent(created: Instant, details: NodeEventDetails)

@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  include = JsonTypeInfo.As.PROPERTY,
)
@JsonSubTypes(Array(
  new Type(value = classOf[AddNode]),
  new Type(value = classOf[ChangeNodeStatus]),
  new Type(value = classOf[ChangeNodeContent]),
  new Type(value = classOf[DeleteNode]),
  new Type(value = classOf[MoveNode])
))
sealed trait NodeEventDetails
case class AddNode(id: Long, status: String, content: String) extends NodeEventDetails
case class ChangeNodeStatus(id: Long, status: String) extends NodeEventDetails
case class ChangeNodeContent(id: Long, content: String) extends NodeEventDetails
case class DeleteNode(id: Long) extends NodeEventDetails
case class MoveNode(id: Long, parentInfo: Option[ParentInfo]) extends NodeEventDetails

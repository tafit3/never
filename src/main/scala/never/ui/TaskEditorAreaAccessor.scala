package never.ui

trait TaskEditorAreaAccessor extends ComponentStateAccessor {
  def requestFocus(): Unit
  def setTags(tags: Set[String]): Unit
  def content: String
  def tags: Set[String]
}

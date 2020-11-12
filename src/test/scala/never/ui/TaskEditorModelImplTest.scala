package never.ui

import never.repository.{RepositoryReadApi, RepositoryWriteApi}
import never.ui.TaskEditorModel.EditingExistingNode
import org.scalatest.Inspectors

class TaskEditorModelImplTest extends BaseTest {

  val readApi = mock[RepositoryReadApi]
  val writeApi = mock[RepositoryWriteApi]

  val impl = new TaskEditorModelImpl(readApi, writeApi)

  val accessor = mock[TaskEditorAreaAccessor]
  when(accessor.content).thenReturn("")
  when(accessor.tags).thenReturn(Set.empty)
  val listener = mock[TaskEditorListener]
  impl.setStateAccessor(accessor)
  impl.addListener(listener)
  val SomeTags = Set("tag1", "tag2")

  "model" should {
    "do nothing on save when in empty editing state" in {
      // given
      impl.setEmptyEditingState()
      reset(listener)

      // when
      impl.save()

      // then
      verifyNoMoreInteractions(writeApi, listener)
    }

    "do nothing if the new node is empty or blank" in Inspectors.forAll(Seq("", "  ")) { text =>
      // given
      impl.editNewNode(None, Set.empty)
      when(accessor.content).thenReturn(text)
      reset(listener)

      // when
      impl.save()

      // then
      verifyNoMoreInteractions(writeApi, listener)
    }

    "add node if the new node is not empty" in {
      // given
      impl.editNewNode(None, Set.empty)
      when(accessor.content).thenReturn("abc")
      val node = someNodeView()
      val updatedNode = node.copy(content = "abc")
      when(readApi.nodeById(*)).thenReturn(Some(updatedNode))
      reset(listener)

      // when
      impl.save()

      // then
      verify(writeApi).addNode("TODO", "abc")
      verify(listener).nodeSaved(*)
      verify(listener).editingNodeChanged(EditingExistingNode, Some(updatedNode))
      verifyNoMoreInteractions(writeApi, listener)
    }

    "add data node if the new node is not empty" in {
      // given
      impl.editNewNode(Some("DATA"), Set.empty)
      when(accessor.content).thenReturn("abc")
      val node = someNodeView()
      val updatedNode = node.copy(content = "abc")
      when(readApi.nodeById(*)).thenReturn(Some(updatedNode))
      reset(listener)

      // when
      impl.save()

      // then
      verify(writeApi).addNode("DATA", "abc")
      verify(listener).nodeSaved(*)
      verify(listener).editingNodeChanged(EditingExistingNode, Some(updatedNode))
      verifyNoMoreInteractions(writeApi, listener)
    }

    "add node if the new node is not empty and set tags" in {
      // given
      impl.editNewNode(None, Set.empty)
      when(accessor.content).thenReturn("abc")
      when(accessor.tags).thenReturn(SomeTags)
      val node = someNodeView()
      val updatedNode = node.copy(content = "abc", tags = SomeTags)
      when(readApi.nodeById(*)).thenReturn(Some(updatedNode))
      reset(listener)

      // when
      impl.save()

      // then
      verify(writeApi).addNode("TODO", "abc")
      verify(writeApi).setTags(*, eqTo(SomeTags))
      verify(listener).nodeSaved(*)
      verify(listener).editingNodeChanged(EditingExistingNode, Some(updatedNode))
      verifyNoMoreInteractions(writeApi, listener)
    }

    "save changes for existing node" in {
      // given
      val node = someNodeView()
      when(readApi.nodeById(node.id)).thenReturn(Some(node))
      impl.editNode(node.id, focusEditor = false)
      reset(readApi, listener)
      val updatedNode = node.copy(content = "abc")
      when(readApi.nodeById(node.id)).thenReturn(Some(updatedNode))
      when(accessor.content).thenReturn("abc")

      // when
      impl.save()

      // then
      verify(writeApi).changeNodeContent(node.id, "abc")
      verify(listener).nodeSaved(*)
      verify(listener).editingNodeChanged(EditingExistingNode, Some(updatedNode))
      verifyNoMoreInteractions(writeApi, listener)
    }

    "save changes in tags for existing node" in {
      // given
      val node = someNodeView()
      when(readApi.nodeById(node.id)).thenReturn(Some(node))
      impl.editNode(node.id, focusEditor = false)
      reset(readApi, listener)
      val updatedNode = node.copy(tags = SomeTags)
      when(readApi.nodeById(node.id)).thenReturn(Some(updatedNode))
      when(accessor.content).thenReturn(node.content)
      when(accessor.tags).thenReturn(SomeTags)

      // when
      impl.save()

      // then
      verify(writeApi, never).changeNodeContent(*, *)
      verify(writeApi).setTags(node.id, SomeTags)
      verify(listener).nodeSaved(*)
      verify(listener).editingNodeChanged(EditingExistingNode, Some(updatedNode))
      verifyNoMoreInteractions(writeApi, listener)
    }

    "do nothing on save if content didn't change for existing node" in {
      // given
      val node = someNodeView()
      when(readApi.nodeById(node.id)).thenReturn(Some(node))
      impl.editNode(node.id, focusEditor = false)
      when(accessor.content).thenReturn(node.content)
      reset(listener)

      // when
      impl.save()

      // then
      verifyNoMoreInteractions(writeApi, listener)
    }

    "do nothing on save if tags didn't change for existing node" in {
      // given
      val node = someNodeView(tags = SomeTags)
      when(readApi.nodeById(node.id)).thenReturn(Some(node))
      impl.editNode(node.id, focusEditor = false)
      when(accessor.content).thenReturn(node.content)
      when(accessor.tags).thenReturn(SomeTags)
      reset(listener)

      // when
      impl.save()

      // then
      verifyNoMoreInteractions(writeApi, listener)
    }

    "return editing node id" in {
      // given
      val node = someNodeView()
      when(readApi.nodeById(node.id)).thenReturn(Some(node))
      impl.editNode(node.id, focusEditor = false)

      // when
      val id = impl.editingNodeId

      // then
      id.value shouldBe node.id
    }

    "return empty editing node id" in {
      // expect
      impl.editingNodeId shouldBe empty
    }

    "do nothing on edit node if already editing node with the same id" in {
      // given
      val node = someNodeView()
      when(readApi.nodeById(node.id)).thenReturn(Some(node))
      impl.editNode(node.id, focusEditor = false)
      reset(listener)

      // when
      impl.editNode(node.id, focusEditor = false)

      // then
      verifyNoMoreInteractions(writeApi, listener)
    }

    "save previous content on edit node if already editing node with the another id" in {
      // given
      val node = someNodeView()
      val node2 = someNodeView()
      when(readApi.nodeById(node.id)).thenReturn(Some(node))
      impl.editNode(node.id, focusEditor = false)
      reset(listener, readApi)
      val updatedNode = node.copy(content = "abc", tags = SomeTags)
      when(readApi.nodeById(node.id)).thenReturn(Some(updatedNode))
      when(readApi.nodeById(node2.id)).thenReturn(Some(node2))
      when(accessor.content).thenReturn("abc")
      when(accessor.tags).thenReturn(SomeTags)

      // when
      impl.editNode(node2.id, focusEditor = false)

      // then
      verify(writeApi).changeNodeContent(node.id, "abc")
      verify(writeApi).setTags(node.id, SomeTags)
      verify(listener).nodeSaved(node.id)
      verify(listener).editingNodeChanged(EditingExistingNode, Some(updatedNode))
      verify(listener).editingNodeChanged(EditingExistingNode, Some(node2))
      verifyNoMoreInteractions(writeApi, listener)
    }

    "lose focus" in {
      // when
      impl.loseFocus()

      // then
      verify(listener).loseFocus()
    }

    "request focus when creating new node" in {
      // when
      impl.editNewNode(None, Set.empty)

      // then
      verify(accessor).requestFocus()
    }
  }
}

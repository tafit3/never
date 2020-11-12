package never.ui

import never.test.StringTestUtils.StringOps

class TaskListModelImplTest extends BaseTest {

  val impl = new TaskListModelImpl

  val accessor = mock[TaskListAreaAccessor]
  impl.setStateAccessor(accessor)

  "model" should {
    "set text with no nodes" in {
      // when
      impl.setNodes(Nil)

      // then
      verify(accessor).setText("--", 0)
    }

    "set text with one node" in {
      // given
      val nodes = List(someNodeView(content = "abc"))

      // when
      impl.setNodes(nodes)

      // then
      verify(accessor).setText(
        """--
          |.  TODO abc
        """.stripMarginTr, 0)
    }

    "select the only node if caret is on the line of this node" in {
      // given
      when(accessor.getLineOfCaretPosition).thenReturn(0)
      val nodes = List(someNodeView(content = "abc"))
      impl.setNodes(nodes)
      reset(accessor)
      when(accessor.getLineOfCaretPosition).thenReturn(1)

      // when
      impl.setNodes(nodes)

      // then
      verify(accessor).setText(
        """--
          |.  TODO abc
        """.stripMarginTr, 1)
    }

    "render node description shorter than the full task content" in {
      // given
      val nodes = List(someNodeView(content = "123456789 a123456789 b123456789 c123456789 d123456789 e123456789 f123456789 g123456789 h123456789"))

      // when
      impl.setNodes(nodes)

      // then
      verify(accessor).setText(
        """--
          |.  TODO 123456789 a123456789 b123456789 c123456789 d123456789 e123456789 f123456789 g123456789...
        """.stripMarginTr, 0)
    }

    "render each node on separate line" in {
      // given
      val nodes = List(someNodeView(content = "task1"), someNodeView(content = "task2"), someNodeView(content = "task3"))

      // when
      impl.setNodes(nodes)

      // then
      verify(accessor).setText(
        """--
          |.  TODO task1
          |.  TODO task2
          |.  TODO task3
        """.stripMarginTr, 0)
    }

    "render DONE status" in {
      // given
      val nodes = List(someNodeView(content = "some-task", status = "DONE"))

      // when
      impl.setNodes(nodes)

      // then
      verify(accessor).setText(
        """--
          |.  DONE some-task
        """.stripMarginTr, 0)
    }

    "render node with various depths" in {
      // given
      val nodes = List(
        someNodeView(content = "abc", depth = 1),
        someNodeView(content = "def", depth = 3),
        someNodeView(content = "ghi", depth = 2))

      // when
      impl.setNodes(nodes)

      // then
      verify(accessor).setText(
        """--
          |..  TODO abc
          |....  TODO def
          |...  TODO ghi
        """.stripMarginTr, 0)
    }

    "render expandable node" in {
      // given
      val nodes = List(someNodeView(content = "some-task", expandable = true))

      // when
      impl.setNodes(nodes)

      // then
      verify(accessor).setText(
        """--
          |.* TODO some-task
        """.stripMarginTr, 0)
    }

    "return no selected node" in {
      // expect
      impl.selectedNode shouldBe empty
    }

    "return a selected node" in {
      // given
      when(accessor.getLineOfCaretPosition).thenReturn(0)
      val node = someNodeView(content = "abc")
      impl.setNodes(List(node))
      reset(accessor)
      when(accessor.getLineOfCaretPosition).thenReturn(1)

      // when
      val selected = impl.selectedNode

      // then
      selected shouldBe Some(node)
    }

    "render node with timestamp" in {
      // given
      impl.setNodes(List(someNodeView(content = "some-task", created = someInstant)))
      reset(accessor)

      // when
      impl.flipTimestampVisibility()

      // then
      verify(accessor).setText(
        """--
          |.  2001-09-09 TODO some-task
        """.stripMarginTr, 0)
    }

    "render node without timestamp after 2 flips" in {
      // given
      impl.setNodes(List(someNodeView(content = "some-task", created = someInstant)))
      impl.flipTimestampVisibility()
      reset(accessor)

      // when
      impl.flipTimestampVisibility()

      // then
      verify(accessor).setText(
        """--
          |.  TODO some-task
        """.stripMarginTr, 0)
    }

    "request focus" in {
      // when
      impl.requestFocus()

      // then
      verify(accessor).requestFocus()
    }

    "render description only up to first 2 newline characters" in {
      // given
      val nodes = List(someNodeView(content = "task1\n\nxyz"))

      // when
      impl.setNodes(nodes)

      // then
      verify(accessor).setText(
        """--
          |.  TODO task1
        """.stripMarginTr, 0)
    }
  }
}

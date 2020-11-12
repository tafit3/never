package never.ui

import java.awt.event.{ActionEvent, InputEvent, KeyAdapter, KeyEvent}
import java.awt.{Color, Component, GridBagConstraints, GridBagLayout, Toolkit}
import java.time.Instant
import java.time.Instant.now

import javax.swing.event.UndoableEditEvent
import javax.swing.text.JTextComponent
import javax.swing.undo.{CannotRedoException, CannotUndoException, UndoManager}
import javax.swing.{AbstractAction, KeyStroke, _}
import never.domain.NodeView
import never.ui.TaskEditor.EditViewData
import never.ui.TaskEditorModel.{AddingNewNode, EditingExistingNode, EditingState, Empty}
import never.util.Constants.{DefaultEmptyBorder, EmptyValuePlaceholder}
import never.util.DateUtils
import org.apache.commons.lang3.StringUtils

import scala.collection.immutable.SortedSet

object TaskEditor {
  case class EditViewData(instant: Instant, text: String, status: String, tags: Set[String])
}

class TaskEditor(parentComponent: Component, model: TaskEditorModel) {
  private val MASK = Toolkit.getDefaultToolkit.getMenuShortcutKeyMask
  private val helpButton = new JButton("?")
  private val upperLabel = new JLabel
  private val tagsField = new JTextField()
  private val editArea = ComponentFactory.createTextArea()

  model.addListener(new TaskEditorListener {
    override def nodeSaved(id: Long): Unit = ()

    override def editingNodeChanged(editingState: EditingState, editingNode: Option[NodeView]): Unit = {
      changeEditView(editingState match {
        case Empty => None
        case AddingNewNode(newNodeStatus) =>
          Some(EditViewData(now(), "", "adding " + newNodeStatus, Set.empty))
        case EditingExistingNode =>
          editingNode.map(node => EditViewData(node.created, node.content, node.status, node.tags))
      })
    }

    override def loseFocus(): Unit = ()
  })

  val defaultKeyAdapter = new KeyAdapter {
    override def keyReleased(e: KeyEvent): Unit = {
      if(e.getKeyCode == KeyEvent.VK_ENTER && ((e.getModifiersEx & InputEvent.CTRL_DOWN_MASK) > 0)) {
        model.save()
      } else if(e.getKeyCode == KeyEvent.VK_ESCAPE) {
        model.save()
        model.loseFocus()
      }
    }
  }

  tagsField.addKeyListener(defaultKeyAdapter)
  editArea.addKeyListener(defaultKeyAdapter)

  editArea.setLineWrap(true)
  editArea.setWrapStyleWord(true)

  val undoManager = new UndoManager

  editArea.getDocument.addUndoableEditListener((e: UndoableEditEvent) => {
    undoManager.addEdit(e.getEdit)
  })

  editArea.getActionMap.put("Undo", new AbstractAction("Undo") {
    override def actionPerformed(evt: ActionEvent): Unit = {
      try if (undoManager.canUndo) undoManager.undo
      catch {
        case e: CannotUndoException =>
          e.printStackTrace()
      }
    }
  })
  editArea.getInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, MASK), "Undo")
  editArea.getActionMap.put("Redo", new AbstractAction("Redo") {
    override def actionPerformed(evt: ActionEvent): Unit = {
      try if (undoManager.canRedo) undoManager.redo
      catch {
        case e: CannotRedoException =>
          e.printStackTrace()
      }
    }
  })
  editArea.getInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, MASK), "Redo")

  model.setStateAccessor(new TaskEditorAreaAccessor {
    def requestFocus(): Unit = editArea.requestFocus()

    def content: String = editArea.getText

    def tags: Set[String] = deserializeTags(tagsField.getText)

    def setTags(tags: Set[String]): Unit = tagsField.setText(serializeTags(tags))
  })

  def createTagsPanel(): JPanel = {
    val panel = new JPanel(new GridBagLayout)
    panel.setBorder(DefaultEmptyBorder)
    val c = new GridBagConstraints

    c.gridx = 0
    c.gridy = 0
    c.gridwidth = 1
    c.gridheight = 1
    c.weightx = 0
    c.weighty = 0
    panel.add(new JLabel("Tags: "), c)

    c.gridx = 1
    c.gridy = 0
    c.gridwidth = 1
    c.gridheight = 1
    c.weightx = 1
    c.weighty = 0
    c.fill = GridBagConstraints.BOTH
    panel.add(tagsField, c)

    panel
  }

  def initializePanel(): JPanel = {
    val panel = new JPanel(new GridBagLayout)
    val c = new GridBagConstraints
    c.gridx = 0
    c.gridy = 0
    c.gridwidth = 1
    c.gridheight = 1
    c.weightx = 1
    c.weighty = 0
    c.fill = GridBagConstraints.BOTH
    panel.add(upperLabel, c)

    c.gridx = 1
    c.gridy = 0
    c.gridwidth = 1
    c.gridheight = 1
    c.weightx = 0
    c.weighty = 0
    panel.add(helpButton, c)
    helpButton.addActionListener((_: ActionEvent) => {
      JOptionPane.showMessageDialog(parentComponent,
        "F1 - new task node\n" +
        "F2 - new data node\n" +
        "F3 - new KB node\n" +
        "F4 - new snippet\n" +
        "F5 - select for move\n" +
        "F6 - insert below\n" +
        "F7 - insert root\n" +
        "F8 - flip TODO/DONE\n" +
        "F9 - delete\n" +
        "Tab - edit node, Ctrl+Enter - confirm, Esc - cancel")
    })

    c.gridx = 0
    c.gridy = 1
    c.gridwidth = 2
    c.gridheight = 1
    c.weightx = 1
    c.weighty = 0
    panel.add(createTagsPanel(), c)

    c.gridx = 0
    c.gridy = 2
    c.gridwidth = 2
    c.gridheight = 1
    c.weightx = 1
    c.weighty = 1
    panel.add(new JScrollPane(editArea), c)

    changeEditView(None)

    panel
  }

  private def changeEditView(data: Option[EditViewData]): Unit = {
    data match {
      case Some(EditViewData(instant, text, status, tags)) =>
        upperLabel.setText(DateUtils.format(instant) + "  "+status)
        tagsField.setText(serializeTags(tags))
        editArea.setText(text)
        undoManager.discardAllEdits()
        List(editArea, tagsField).foreach(setEnabled(_, true))
      case None =>
        upperLabel.setText(EmptyValuePlaceholder)
        List(editArea, tagsField).foreach { textComponent =>
          textComponent.setText("")
          setEnabled(textComponent, false)
        }
    }
  }

  private def setEnabled(textComponent: JTextComponent, enabled: Boolean): Unit = {
    textComponent.setEnabled(enabled)
    textComponent.setBackground(if(enabled) Color.WHITE else Color.GRAY)
  }

  private def serializeTags(tags: Set[String]): String = {
    (SortedSet.empty[String] ++ tags).mkString(", ")
  }

  private def deserializeTags(tags: String): Set[String] = {
    tags.split(",").toList.map(_.trim()).filter(StringUtils.isNotBlank).toSet
  }
}

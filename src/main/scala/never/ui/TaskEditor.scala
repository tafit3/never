package never.ui

import java.awt.{Toolkit, _}
import java.awt.event.{ActionEvent, InputEvent, KeyAdapter, KeyEvent}
import java.time.Instant
import java.time.Instant.now

import javax.swing.{AbstractAction, KeyStroke, _}
import javax.swing.event.{UndoableEditEvent, UndoableEditListener}
import javax.swing.undo.{CannotRedoException, CannotUndoException, UndoManager}
import never.domain.NodeView
import never.ui.MainFrame.LabelMargin
import never.ui.TaskEditor.EditViewData
import never.ui.TaskEditorModel.{EditingState, Empty}
import never.util.Constants.EMPTY_VALUE_PLACEHOLDER
import never.util.DateUtils

object TaskEditor {
  case class EditViewData(instant: Instant, text: String, status: String)
}

class TaskEditor(parentComponent: Component, model: TaskEditorModel) {
  private val MASK = Toolkit.getDefaultToolkit.getMenuShortcutKeyMask
  private val helpButton = new JButton("?")
  private val upperLabel = new JLabel
  private val editArea = ComponentFactory.createTextArea()

  model.addListener(new TaskEditorListener {
    override def nodeSaved(id: Long): Unit = ()

    override def editingNodeChanged(editingState: EditingState, editingNode: Option[NodeView]): Unit = {
      changeEditView(editingState match {
        case Empty => None
        case _ => Some(editingNode match {
          case Some(node) =>
            EditViewData(node.created, node.content, node.status)
          case None =>
            EditViewData(now(), "", "")
        })
      })
    }

    override def loseFocus(): Unit = ()
  })

  editArea.addKeyListener(new KeyAdapter {
    override def keyReleased(e: KeyEvent): Unit = {
      if(e.getKeyCode == KeyEvent.VK_ENTER && ((e.getModifiersEx & InputEvent.CTRL_DOWN_MASK) > 0)) {
        model.save()
      } else if(e.getKeyCode == KeyEvent.VK_ESCAPE) {
        model.save()
        model.loseFocus()
      }
    }
  })

  editArea.setLineWrap(true)
  editArea.setWrapStyleWord(true)

  val undoManager = new UndoManager

  editArea.getDocument.addUndoableEditListener(new UndoableEditListener {
    override def undoableEditHappened(e: UndoableEditEvent): Unit = {
      undoManager.addEdit(e.getEdit)
    }
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

    def getText: String = editArea.getText
  })

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
    c.insets = new Insets(LabelMargin,LabelMargin,LabelMargin,LabelMargin)
    panel.add(upperLabel, c)

    c.gridx = 1
    c.gridy = 0
    c.gridwidth = 1
    c.gridheight = 1
    c.weightx = 0
    c.weighty = 0
    panel.add(helpButton, c)
    helpButton.addActionListener((actionEvent: ActionEvent) => {
      JOptionPane.showMessageDialog(parentComponent,
        "F2 - new node\nTab - edit node, Ctrl+Enter - confirm, Esc - cancel, F3 - flip timestamp visibility\n" +
          "F4 - cycle views, F5 - select for move, F6 - insert below, F7 - insert root, F8 - flip TODO/DONE\n" +
          "F9 - delete")
    })

    c.gridx = 0
    c.gridy = 1
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
      case Some(EditViewData(instant, text, status)) =>
        upperLabel.setText(DateUtils.format(instant) + "  "+status)
        editArea.setText(text)
        undoManager.discardAllEdits()
        editArea.setEnabled(true)
        editArea.setBackground(Color.WHITE)
      case None =>
        upperLabel.setText(EMPTY_VALUE_PLACEHOLDER)
        editArea.setText("")
        editArea.setEnabled(false)
        editArea.setBackground(Color.GRAY)
    }
  }
}

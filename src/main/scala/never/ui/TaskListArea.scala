package never.ui

import java.awt.event.{KeyAdapter, KeyEvent}

import javax.swing.JTextArea
import javax.swing.event.ChangeEvent
import javax.swing.text.{AbstractDocument, AttributeSet, DocumentFilter}

class TaskListArea(model: TaskListModel) extends ListenerSupport[TaskListAreaListener] {
  private val area: JTextArea = ComponentFactory.createTextArea()
  private val protector = new TextAreaProtector

  def getArea: JTextArea = area

  area.getDocument.asInstanceOf[AbstractDocument].setDocumentFilter(new DocumentFilter {
    override def insertString(fb: DocumentFilter.FilterBypass, offset: Int, string: String, attr: AttributeSet): Unit = {
      if(protector.editing) {
        super.insertString(fb, offset, string, attr)
      }
    }

    override def remove(fb: DocumentFilter.FilterBypass, offset: Int, length: Int): Unit = {
      if(protector.editing) {
        super.remove(fb, offset, length)
      }
    }

    override def replace(fb: DocumentFilter.FilterBypass, offset: Int, length: Int, text: String, attrs: AttributeSet): Unit = {
      if(protector.editing) {
        super.replace(fb, offset, length, text, attrs)
      }
    }
  })

  private def editSelectedNode(): Unit = {
    model.selectedNode.foreach(selected => fire(_.editNode(selected.id)))
  }

  area.addKeyListener(new KeyAdapter {
    override def keyReleased(e: KeyEvent): Unit = {
      if(e.getKeyCode == KeyEvent.VK_TAB) {
        editSelectedNode()
      }
    }
  })

  area.getCaret.addChangeListener((_: ChangeEvent) => {
    if(!protector.editing) {
      editSelectedNode()
    }
  })

  model.setStateAccessor(new TaskListAreaAccessor {
    override def getLineOfOffset(position: Int): Int = area.getLineOfOffset(position)

    override def getCaretPosition: Int = area.getCaretPosition

    override def getText: String = area.getText

    override def setText(text: String, selectedLine: Int): Unit = {
      protector.guard {
        area.setText(text)
        moveCaretToLine(selectedLine)
      }
    }

    override def requestFocus(): Unit = {
      area.requestFocus()
    }
  })

  private def moveCaretToLine(selectedLine: Int): Unit = {
    val s = area.getText
    var p = 0
    var line = 0
    while(p < s.length && line < selectedLine) {
      if(s.charAt(p) == '\n') {
        line += 1
      }
      p += 1
    }
    if(line >= selectedLine && p < s.length) {
      protector.guard {
        area.setCaretPosition(p)
      }
    }
  }

}

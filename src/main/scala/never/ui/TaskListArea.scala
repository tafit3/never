package never.ui

import java.awt.BorderLayout
import java.awt.event.{KeyAdapter, KeyEvent}

import javax.swing.{BorderFactory, JPanel, JScrollPane, JTextArea, JTextField}
import javax.swing.event.ChangeEvent
import javax.swing.text.{AbstractDocument, AttributeSet, DocumentFilter}
import never.ui.MainFrame.MarginSize

class TaskListArea(model: TaskListModel) extends ListenerSupport[TaskListAreaListener] {
  private val filter: JTextField = ComponentFactory.createTextField()
  private val area: JTextArea = ComponentFactory.createTextArea()
  private val protector = new TextAreaProtector

  def getArea: JTextArea = area

  def createPanel(): JPanel = {
    val panel = new JPanel()
    panel.setLayout(new BorderLayout)
    panel.setBorder(BorderFactory.createEmptyBorder(MarginSize, MarginSize, MarginSize, MarginSize))
    panel.add(filter, BorderLayout.NORTH)
    panel.add(new JScrollPane(area))
    panel
  }

  filter.addKeyListener(new KeyAdapter {
    override def keyReleased(e: KeyEvent): Unit = {
      if(e.getKeyCode == KeyEvent.VK_ENTER) {
        fire(_.applyFilter(filter.getText()))
      }
    }
  })

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

  private def editSelectedNode(focusEditor: Boolean): Unit = {
    model.selectedNode.foreach(selected => fire(_.editNode(selected.id, focusEditor)))
  }

  area.addKeyListener(new KeyAdapter {
    override def keyReleased(e: KeyEvent): Unit = {
      if(e.getKeyCode == KeyEvent.VK_TAB) {
        editSelectedNode(true)
      }
    }
  })

  area.getCaret.addChangeListener((_: ChangeEvent) => {
    if(!protector.editing) {
      editSelectedNode(false)
    }
  })

  model.setStateAccessor(new TaskListAreaAccessor {
    override def getLineOfCaretPosition: Int = area.getLineOfOffset(area.getCaretPosition)

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

    override def isFocused(): Boolean = {
      area.isFocusOwner
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

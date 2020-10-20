package never.ui

import javax.swing.JOptionPane.{CANCEL_OPTION, NO_OPTION, YES_NO_CANCEL_OPTION, YES_NO_OPTION, YES_OPTION}
import java.time.Instant.now
import java.awt.event.{ActionEvent, ActionListener, FocusAdapter, FocusEvent, InputEvent, KeyAdapter, KeyEvent, MouseAdapter, MouseEvent}
import java.awt.{BorderLayout, Color, Font, Frame, GridBagConstraints, GridBagLayout, GridLayout, Insets}
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date

import javax.swing._
import javax.swing.event.{ChangeEvent, ChangeListener, DocumentEvent, DocumentListener, MouseInputAdapter}
import javax.swing.text.{AbstractDocument, AttributeSet, DocumentFilter}
import never.domain.{NodeView, ParentInfo}
import never.repository.{Node, RepositoryReadApi, RepositoryWriteApi}

import scala.annotation.tailrec
import scala.collection.mutable

object MainFrame {
  val MonoFont = new Font(Font.MONOSPACED, Font.PLAIN, 12)
  val MarginSize = 3
  val LabelMargin = 5

  sealed trait EditingState
  case object Empty extends EditingState
  case object AddingNewNode extends EditingState
  case object EditingExistingNode extends EditingState

  sealed trait ViewType {
    def name: String
  }
  case object AllFlatByTimeDesc extends ViewType {
    val name = "all tasks flat sorted by time descending"
  }
  case object AllTreeByTimeDesc extends ViewType {
    val name = "all tasks in tree sorted by time descending"
  }
  val AllViewTypes = List(AllFlatByTimeDesc, AllTreeByTimeDesc)
}

class MainFrame(readApi: RepositoryReadApi, writeApi: RepositoryWriteApi) extends JFrame("never") {
  import MainFrame._
  setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)

  private val dateFormat = new SimpleDateFormat("yyyy-MM-dd")
  private val datetimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

  private val helpButton = new JButton("?")
  private val upperLabel = new JLabel
  private val area = new JTextArea
  private var areaEnabled = false
  private val editArea = new JTextArea
  private var nodes: List[NodeView] = Nil
  private var editingNode: Option[NodeView] = None
  private var timestampsVisible = false
  private var editingState: EditingState = Empty

  private var actionId = 1

  private var viewType: ViewType = AllTreeByTimeDesc
  private var selectedForMove: Option[Long] = None
  private val expandedNodes = mutable.Set.empty[Long]

  def rightPanel(): JPanel = {
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
    helpButton.addActionListener(new ActionListener {
      override def actionPerformed(actionEvent: ActionEvent): Unit = {
        JOptionPane.showMessageDialog(MainFrame.this,
          "F2 - new node\nTab - edit node, Ctrl+Enter - confirm, Esc - cancel, F3 - flip timestamp visibility\n" +
            "F4 - cycle views, F5 - select for move, F6 - insert below, F7 - insert root, F8 - flip TODO/DONE\n" +
            "F9 - delete")
      }
    })

    c.gridx = 0
    c.gridy = 1
    c.gridwidth = 2
    c.gridheight = 1
    c.weightx = 1
    c.weighty = 1
    editArea.setFont(MonoFont)
    panel.add(new JScrollPane(editArea), c)

    panel
  }

  def mainPanel(): JPanel = {
    val panel = new JPanel(new GridLayout(1, 2))

    area.setFont(MonoFont)
//    area.getCaret.setVisible(true)
//    area.addFocusListener(new FocusAdapter {
//      override def focusGained(e: FocusEvent): Unit = {
//        area.getCaret.setVisible(true)
//      }
//    })

    area.getDocument.asInstanceOf[AbstractDocument].setDocumentFilter(
      new DocumentFilter {
        override def insertString(fb: DocumentFilter.FilterBypass, offset: Int, string: String, attr: AttributeSet): Unit = {
          if(areaEnabled) {
            super.insertString(fb, offset, string, attr)
          }
        }

        override def remove(fb: DocumentFilter.FilterBypass, offset: Int, length: Int): Unit = {
          if(areaEnabled) {
            super.remove(fb, offset, length)
          }
        }

        override def replace(fb: DocumentFilter.FilterBypass, offset: Int, length: Int, text: String, attrs: AttributeSet): Unit = {
          if(areaEnabled) {
            super.replace(fb, offset, length, text, attrs)
          }
        }
      })
    area.addKeyListener(new KeyAdapter {
      override def keyReleased(e: KeyEvent): Unit = {
        if(e.getKeyCode == KeyEvent.VK_TAB) {
          getSelectedNode().foreach { selected =>
            editExistingNode(selected)
            editArea.requestFocus()
          }
        }
      }
    })
    area.getCaret.addChangeListener((_: ChangeEvent) => {
      if(!areaEnabled) {
        getSelectedNode().foreach { selected =>
          editingState match {
            case Empty =>
              editExistingNode(selected)
            case AddingNewNode =>
              editExistingNode(selected)
            case EditingExistingNode =>
              editExistingNode(selected)
          }
        }
      }
    })
    panel.add(new JScrollPane(area))

    editArea.setFont(MonoFont)
    editArea.addKeyListener(new KeyAdapter {
      override def keyReleased(e: KeyEvent): Unit = {
        if(e.getKeyCode == KeyEvent.VK_ENTER && ((e.getModifiersEx & InputEvent.CTRL_DOWN_MASK) > 0)) {
          finishEdit()
          //clearEditPanel()
        } else if(e.getKeyCode == KeyEvent.VK_ESCAPE) {
          escapeCurrentEdit()
          area.requestFocus()
          //clearEditPanel()
        }
      }
    })
    panel.add(rightPanel())

    panel
  }

  private def getSelectedNode(): Option[NodeView] = {
    val line = area.getLineOfOffset(area.getCaretPosition)
    Option.when(line >= 0 && line < nodes.size)(nodes(line))
  }

  private def startUi(): Unit = {
    val panel = new JPanel
    panel.setLayout(new BorderLayout)
    panel.setBorder(BorderFactory.createEmptyBorder(MarginSize, MarginSize, MarginSize, MarginSize))
    panel.add(mainPanel())
    add(panel)

    bind(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), () => editNewNode())
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), () => flipTimestampVisibility())
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0), () => cycleViewType())
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), () => selectForMove())
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0), () => insertBelow())
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0), () => insertRoot())
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0), () => flipTodoDone())
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0), () => deleteNode())
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_QUOTE, 0), () => flipExpandCurrentNode())

    changeEditView(None)
    viewAll()

    setVisible(true)
    //setExtendedState(Frame.MAXIMIZED_BOTH)
    setSize(800,600)
    area.requestFocus()
  }

  startUi()

  private def bind(keyStroke: KeyStroke, runnable: Runnable): Unit = {
    val inputMap = area.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
    val actionMap = area.getActionMap
    actionId += 1
    val actionName = "action" + actionId
    inputMap.put(keyStroke, actionName)
    actionMap.put(actionName, new AbstractAction() {
      override def actionPerformed(e: ActionEvent): Unit = {
        runnable.run()
      }
    })
  }

  private def escapeCurrentEdit(): Unit = {
    editingState match {
      case Empty =>
      case AddingNewNode =>
        if(!editArea.getText.isBlank) {
          finishEdit()
        }
      case EditingExistingNode =>
        require(editingNode.isDefined)
        if(editingNode.exists(_.content != editArea.getText)) {
          finishEdit()
        }
    }
  }

  private def deleteNode(): Unit = {
    if(false) {
      //FIXME deleting node doesn't update child nodes
      getSelectedNode().foreach { node =>
        if (JOptionPane.showConfirmDialog(MainFrame.this, "Delete node", "Are you sure?", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
          writeApi.deleteNode(node.id)
          viewAll()
        }
      }
    }
  }

  private def flipExpandCurrentNode(): Unit = {
    getSelectedNode().foreach { selectedNode =>
      if(expandedNodes.contains(selectedNode.id)) {
        expandedNodes.remove(selectedNode.id)
      } else {
        expandedNodes += selectedNode.id
      }
      viewAll()
    }
  }

  private def flipTimestampVisibility(): Unit = {
    timestampsVisible = !timestampsVisible
    viewAll()
  }

  private def flipTodoDone(): Unit = {
    getSelectedNode().foreach { node =>
      val newStatus = if(node.status == "TODO") {
        "DONE"
      } else {
        "TODO"
      }
      writeApi.changeNodeStatus(node.id, newStatus)
      viewAll()
    }
  }

  private def cycleViewType(): Unit = {
    viewType = nextViewType(viewType)
    viewAll()
  }

  private def selectForMove(): Unit = {
    selectedForMove = getSelectedNode().map(_.id)
  }

  private def insertBelow(): Unit = {
    selectedForMove.foreach { idToMove =>
      getSelectedNode().foreach { newParentNode =>
        writeApi.moveNode(idToMove, Some(ParentInfo(newParentNode.id, 0)))
        viewAll()
      }
    }
  }

  private def insertRoot(): Unit = {
    selectedForMove.foreach { idToMove =>
      writeApi.moveNode(idToMove, None)
      viewAll()
    }
  }

  private def nextViewType(current: ViewType): ViewType = {
    val index = AllViewTypes.indexOf(current)
    AllViewTypes(if(index < AllViewTypes.length - 1) index + 1 else 0)
  }

  private case class EditViewData(instant: Instant, text: String, status: String)

  private def changeEditView(data: Option[EditViewData]): Unit = {
    data match {
      case Some(EditViewData(instant, text, status)) =>
        upperLabel.setText(dateFormat.format(Date.from(instant)) + "  "+status)
        editArea.setText(text)
        editArea.setEnabled(true)
        editArea.setBackground(Color.WHITE)
      case None =>
        upperLabel.setText("--")
        editArea.setText("")
        editArea.setEnabled(false)
        editArea.setBackground(Color.GRAY)
    }
  }

  private def finishEdit(): Unit = {
    val id = editingNode match {
      case Some(node) =>
        writeApi.changeNodeContent(node.id, editArea.getText())
        node.id
      case None =>
        writeApi.addNode("TODO", editArea.getText())
    }
    viewAll()
    editingNode = nodes.find(_.id == id)
    editingState = EditingExistingNode
  }

  private def clearEditPanel(): Unit = {
    editingState = Empty
    editingNode = None
    changeEditView(None)
    area.requestFocus()
  }

  private def editNewNode(): Unit = {
    escapeCurrentEdit()
    editingNode = None
    changeEditView(Some(EditViewData(now(), "", "")))
    editArea.requestFocus()
    editingState = AddingNewNode
  }

  private def editExistingNode(node: NodeView): Unit = {
    if(editingNode.forall(_.id != node.id)) {
      escapeCurrentEdit()
      editingNode = Some(node)
      changeEditView(Some(EditViewData(node.created, node.content, node.status)))
      editingState = EditingExistingNode
    }
  }

  private def view(s: String): Unit = {
    areaEnabled = true
    area.setText(s)
    areaEnabled = false
  }

  private def viewAll(): Unit = {
    val id = getSelectedNode().map(_.id)
    viewType match {
      case AllFlatByTimeDesc =>
        nodes = readApi.allNodesByCreatedDesc
      case AllTreeByTimeDesc =>
        nodes = readApi.allNodesAsTreeByCreatedDesc(expandedNodes.toSet)
    }
    viewThem()
    id.foreach(selectedId => {
      @tailrec
      def process(index: Int): Unit = {
        if(index < nodes.length) {
          val node = nodes(index)
          if(node.id == selectedId) {
            moveCaretToLine(index)
          } else {
            process(index + 1)
          }
        }
      }
      process(0)
    })
  }

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
      areaEnabled = true
      area.setCaretPosition(p)
      areaEnabled = false
    }
  }

  private def viewThem(): Unit = {
    view(nodes.map(nodeToLine).mkString("\n"))
  }

  private def nodeToLine(node: NodeView): String = {
    val date = if(timestampsVisible) dateFormat.format(Date.from(node.created)) + " " else ""
    ("." * node.depth) + date + node.status + " " + flatPrefix(node.content, 50) + (if(node.expandable) " ..." else "")
  }

  private def flatPrefix(s: String, maxLen: Int): String = {
    val flat = s.replaceAll("\\s+", " ").trim()
    if(flat.length > maxLen) {
      flat.substring(0, maxLen)
    } else {
      flat
    }
  }
}

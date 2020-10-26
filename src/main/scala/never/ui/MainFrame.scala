package never.ui

import java.awt.event.{ActionEvent, KeyEvent}
import java.awt.{BorderLayout, GridLayout}

import javax.swing._
import never.repository.{RepositoryReadApi, RepositoryWriteApi}

object MainFrame {
  val MarginSize = 3
  val LabelMargin = 5
}

class MainFrame(readApi: RepositoryReadApi, writeApi: RepositoryWriteApi) extends JFrame("never") {
  import MainFrame._
  setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)

  private var actionId = 1
  private val taskListModel = new TaskListModelImpl
  private val taskEditorModel = new TaskEditorModelImpl(readApi, writeApi)
  private val model = new MainFrameModel(taskListModel, taskEditorModel, readApi, writeApi)

  private val taskListArea = new TaskListArea(taskListModel)
  private val taskEditor = new TaskEditor(MainFrame.this, taskEditorModel)

  def mainPanel(): JPanel = {
    val panel = new JPanel(new GridLayout(1, 2))
    panel.add(new JScrollPane(taskListArea.getArea))
    panel.add(taskEditor.initializePanel())
    panel
  }

  private def startUi(): Unit = {
    taskListArea.addListener(new TaskListAreaListener() {
      override def editNode(id: Long, focusEditor: Boolean): Unit = {
        taskEditorModel.editNode(id, focusEditor)
      }
    })
    val panel = new JPanel
    panel.setLayout(new BorderLayout)
    panel.setBorder(BorderFactory.createEmptyBorder(MarginSize, MarginSize, MarginSize, MarginSize))
    panel.add(mainPanel())
    add(panel)

    bind(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), () => model.editNewNode())
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), () => model.flipTimestampVisibility())
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0), () => model.cycleViewType())
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), () => model.selectForMove())
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0), () => model.insertBelow())
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0), () => model.insertRoot())
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0), () => model.flipTodoDone())
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_QUOTE, 0), () => model.flipExpandCurrentNode())

    model.refreshTaskList()

    setVisible(true)
    //setExtendedState(Frame.MAXIMIZED_BOTH)
    setSize(800,600)
    taskListModel.requestFocus()
  }

  startUi()

  private def bind(keyStroke: KeyStroke, runnable: Runnable): Unit = {
    val inputMap = taskListArea.getArea.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
    val actionMap = taskListArea.getArea.getActionMap
    actionId += 1
    val actionName = "action" + actionId
    inputMap.put(keyStroke, actionName)
    actionMap.put(actionName, new AbstractAction() {
      override def actionPerformed(e: ActionEvent): Unit = {
        runnable.run()
      }
    })
  }

}

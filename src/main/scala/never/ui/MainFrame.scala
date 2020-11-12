package never.ui

import java.awt.event.{ActionEvent, KeyEvent}
import java.awt.{BorderLayout, Frame, GridLayout}

import javax.swing._
import never.AppConfig
import never.domain.NodeMatchCondition
import never.repository.{RepositoryReadApi, RepositoryWriteApi}
import never.ui.MainFrameModel.NodeNotSelected
import never.util.Constants.DefaultEmptyBorder

class MainFrame(readApi: RepositoryReadApi, writeApi: RepositoryWriteApi, appConfig: AppConfig) extends JFrame("never") {
  setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)

  private var actionId = 1
  private val taskListModel = new TaskListModelImpl
  private val taskEditorModel = new TaskEditorModelImpl(readApi, writeApi)
  private val model = new MainFrameModel(taskListModel, taskEditorModel, readApi, writeApi)

  private val taskListArea = new TaskListArea(taskListModel)
  private val taskEditor = new TaskEditor(MainFrame.this, taskEditorModel)

  def mainPanel(): JPanel = {
    val panel = new JPanel(new GridLayout(1, 2))
    panel.add(taskListArea.createPanel())
    panel.add(taskEditor.initializePanel())
    panel
  }

  private def startUi(): Unit = {
    taskListArea.addListener(new TaskListAreaListener() {
      override def editNode(id: Long, focusEditor: Boolean): Unit = {
        taskEditorModel.editNode(id, focusEditor)
      }

      override def applyFilter(filter: String): Unit = {
        model.applyTextSearchRegex(filter)
      }
    })
    val panel = new JPanel
    panel.setLayout(new BorderLayout)
    panel.setBorder(DefaultEmptyBorder)
    panel.add(mainPanel())
    add(panel)

    def createMenuBar(): JMenuBar = {
      val menuBar = new JMenuBar
      val menu = new JMenu("Edit")
      val menuItem = new JMenuItem("Change status")
      menuItem.addActionListener((_: ActionEvent) => changeStatus())
      menu.add(menuItem)
      menuBar.add(menu)

      menuBar.add(createFilteredViewsMenu())

      menuBar
    }
    setJMenuBar(createMenuBar())

    bind(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), () => model.editNewDataNode())
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), () => model.editNewTaskNode())
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), () => model.flipTimestampVisibility())
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0), () => model.cycleViewType())
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), () => model.selectForMove())
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0), () => model.insertBelow())
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0), () => model.insertRoot())
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0), () => model.flipTodoDone())
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_QUOTE, 0), () => model.flipExpandCurrentNode())

    model.refreshTaskList()

    setVisible(true)
    if(appConfig.maximizeOnStartup) {
      setExtendedState(Frame.MAXIMIZED_BOTH)
    }
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

  private def createFilteredViewsMenu(): JMenu = {
    def switchTo(menuItem: JRadioButtonMenuItem, nodeMatchCondition: NodeMatchCondition): Unit = {
      model.switchFilteredView(nodeMatchCondition)
      menuItem.setSelected(true)
    }

    val buttonGroup = new ButtonGroup
    val menu = new JMenu("Filtered views")
    val menuItem = new JRadioButtonMenuItem("all", true)
    menuItem.addActionListener((_: ActionEvent) => switchTo(menuItem, NodeMatchCondition.Empty))
    menu.add(menuItem)
    buttonGroup.add(menuItem)
    appConfig.filteredViews.foreach { filteredView =>
      val menuItem = new JRadioButtonMenuItem(filteredView.name)
      menuItem.addActionListener((_: ActionEvent) => switchTo(menuItem, filteredView.condition))
      menu.add(menuItem)
      buttonGroup.add(menuItem)
    }
    menu
  }

  private def changeStatus(): Unit = {
    if(taskListModel.selectedNode.isDefined) {
      new ChangeStatusFrame(this, (status: String) => {
        model.setStatusOfSelectedNode(status) match {
          case NodeNotSelected => JOptionPane.showMessageDialog(this, "ERROR: No node selected.")
          case _ =>
        }
      })
    } else {
      JOptionPane.showMessageDialog(this, "No node selected.")
    }
  }
}

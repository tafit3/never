package never.ui

import java.awt.event.ActionEvent
import java.awt.{BorderLayout, GridBagConstraints, GridBagLayout}

import javax.swing.{JButton, JDialog, JFrame, JPanel}
import never.util.Constants
import never.util.Constants.DefaultEmptyBorder

trait ChangeStatusCallback {
  def setStatus(status: String): Unit
}

class ChangeStatusFrame(owner: JFrame, callback: ChangeStatusCallback) extends JDialog(owner, "Change status", true) {

  def closeFrame(): Unit = {
    ChangeStatusFrame.this.dispose()
  }

  def createPanel(): JPanel = {
    val panel = new JPanel(new GridBagLayout)
    panel.setBorder(DefaultEmptyBorder)
    val c = new GridBagConstraints

    c.gridx = 0
    c.gridy = 0
    c.gridwidth = 1
    c.gridheight = 1
    c.weightx = 1
    c.weighty = 0
    c.insets = Constants.LabelInsets
    val defaultStatuses = List("TODO", "DONE", "DATA")
    for { i <- defaultStatuses.indices} {
      val button = new JButton("Set to " + defaultStatuses(i))
      button.addActionListener((_: ActionEvent) => {
        callback.setStatus(defaultStatuses(i))
        closeFrame()
      })
      c.gridx = i
      panel.add(button, c)
    }

    c.gridx = 0
    c.gridy = 1
    c.gridwidth = 3
    c.gridheight = 1
    c.weightx = 1
    c.weighty = 0
    c.fill = GridBagConstraints.NONE
    c.anchor = GridBagConstraints.EAST
    val button = new JButton("Cancel")
    button.addActionListener((_: ActionEvent) => closeFrame())
    panel.add(button, c)
    panel
  }

  setLayout(new BorderLayout)
  add(createPanel())
  setSize(500, 150)
  setLocationRelativeTo(owner)
  setVisible(true)
}

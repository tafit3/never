package never.ui

import java.awt.Font

import javax.swing.{JTextArea, JTextField}

object ComponentFactory {
  private val MonoFont = new Font(Font.MONOSPACED, Font.PLAIN, 12)

  def createTextArea(): JTextArea = {
    val area = new JTextArea()
    area.setFont(MonoFont)
    area
  }

  def createTextField(): JTextField = {
    val textField = new JTextField()
    textField.setFont(MonoFont)
    textField
  }
}

package never.ui

import java.awt.Font

import javax.swing.JTextArea

object ComponentFactory {
  private val MonoFont = new Font(Font.MONOSPACED, Font.PLAIN, 12)

  def createTextArea(): JTextArea = {
    val area = new JTextArea()
    area.setFont(MonoFont)
    area
  }
}

package never.util

import java.awt.Insets

import javax.swing.BorderFactory

object Constants {
  val EmptyValuePlaceholder = "--"
  val MarginSize = 3
  val LabelMargin = 5
  val LabelInsets = new Insets(LabelMargin,LabelMargin,LabelMargin,LabelMargin)
  val DefaultEmptyBorder = BorderFactory.createEmptyBorder(MarginSize, MarginSize, MarginSize, MarginSize)
}

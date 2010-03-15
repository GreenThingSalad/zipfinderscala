package zipfinder.gui

import java.awt.Component
import java.awt.Container

import javax.swing._
import javax.swing.JPanel

class LimitedJFileChooser extends JFileChooser {
  def customize {
    setControlButtonsAreShown(false)
    setFileHidingEnabled(false)
    removeExcessButtons(this, getComponents)
    removeExcessFields(this, getComponents)
  }

  private def removeExcessButtons(parent: Container, components: Array[Component]) {
    var first = true
    for (component <- components) {
      if (component.isInstanceOf[AbstractButton]) {
        if (first) {
          first = false
        } else {
          val button = component.asInstanceOf[AbstractButton]
          parent remove button
        }
      }
      if (component.isInstanceOf[Container]) {
        val container = component.asInstanceOf[Container]
        removeExcessButtons(container, container.getComponents)
      }
    }
  }

  private def removeExcessFields(parent: Container, components: Array[Component]) {
    val component = components(components.length - 1)
    if (component.isInstanceOf[JPanel]) {
      parent remove component
    } else {
      throw new IllegalStateException("Layout is not as expected")
    }
  }
}

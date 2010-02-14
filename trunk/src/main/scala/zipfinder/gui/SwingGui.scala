package zipfinder.gui

import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.io.File

import javax.swing._
import javax.swing.text.BadLocationException
import swing._
import swing.event._
import GridBagPanel._

class SwingGui(recentDirectories:Array[String], recentStringsToFind:Array[String], searchButtonListener:SearchButtonListener) {
  // Specalare för att det inte finns någon standard för PropertyChangeListener i Scala
  def PropertyChangeListener(f: PropertyChangeEvent => Unit) = new PropertyChangeListener {
    def propertyChange(e: PropertyChangeEvent) { f(e) }
  }

	private val TEXT_ROWS = 10
	private val INSET = 5
	private val MAX_LINES = 500
	private val SEARCH_LABEL = "Search"
	// I stället för att ha frame som en var
	private var showFrameFunction:Unit = _
	private val directoryComboBox = new ComboBox(recentDirectories) {
	  // Specialare för att sätta editable
	  peer.setEditable(true)
	  // Specialare för att komma åt värde
      def getOldValue:String = peer.getEditor.getItem.toString
      // Specialare för att få scala events
      peer.addActionListener(Swing.ActionListener { e => publish(ValueChanged(this))} )
	}
	private val textToFindField = new RecentStringsTextField(recentStringsToFind) { text = "" }
    private val console = new TextArea("") {
      rows = TEXT_ROWS
    }
    private val searchButton = new Button(SEARCH_LABEL)

	{
		// Set Look & Feel
		try {
//			javax.swing.UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel")
		} catch {
		  case e:Exception => e.printStackTrace
		}
	}

	def addToConsole(text:String) {
		SwingUtilities.invokeLater(new Runnable {
			def run {
				while (console.lineCount > MAX_LINES) {
					try {
						val offset = console.peer.getLineEndOffset(0)
						console.peer.getDocument.remove(0, offset)
					} catch {
					  case e:BadLocationException => e.printStackTrace
					}
				}
				console.append(text + "\n")
			}
		})
	}


	def createComponents {
		val insets = new Insets(INSET, INSET, INSET, INSET)
		val directoryTree = new LimitedFileChooser{
			fileSelectionMode = FileChooser.SelectionMode.DirectoriesOnly
			customize
			// Specialare för att få scala events
			peer.addPropertyChangeListener(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY,
					PropertyChangeListener { e => publish(SelectionChanged(this))})
		}

	    val upperPanel = new GridBagPanel {
	      peer.getLayout.asInstanceOf[GridBagLayout].columnWidths =  Array(INSET, INSET)
	      peer.getLayout.asInstanceOf[GridBagLayout].rowHeights =  Array(INSET, INSET)
	      peer.getLayout.asInstanceOf[GridBagLayout].columnWeights =  Array( 1, 0 )
	      peer.getLayout.asInstanceOf[GridBagLayout].rowWeights =  Array( 0, 1 )
	      layout(directoryComboBox) = new Constraints(
	        0, 0, 1, 1, 0.0, 0.0, Anchor.CENTER.id,	Fill.Horizontal.id, insets, 0, 0)
	      if (directoryComboBox.selection != null)
			directoryTree.selectedFile = new File(directoryComboBox.selection.toString)
          layout(directoryTree) = new Constraints(
	        0, 1, 2, 1, 0.0, 0.0, Anchor.CENTER.id,	Fill.Both.id, insets, 0, 0)
	    }
        val lowerPanel = new GridBagPanel {
	      peer.getLayout.asInstanceOf[GridBagLayout].columnWidths =  Array(INSET, INSET)
	      peer.getLayout.asInstanceOf[GridBagLayout].rowHeights =  Array(INSET, INSET)
	      peer.getLayout.asInstanceOf[GridBagLayout].columnWeights =  Array( 1, 0 )
	      peer.getLayout.asInstanceOf[GridBagLayout].rowWeights =  Array( 0, 1 )
	      val consoleScroll = new ScrollPane(console)
          layout(textToFindField) = new Constraints(
           	 0, 0, 1, 1, 0.0, 0.0, Anchor.CENTER.id, Fill.Horizontal.id, insets, 0, 0);
          layout(searchButton) = new Constraints(
             1, 0, 1, 1, 0.0, 0.0, Anchor.CENTER.id, Fill.None.id, insets, 0, 0)
          layout(consoleScroll) = new Constraints(
           	 0, 1, 2, 1, 0.0, 0.0, Anchor.CENTER.id, Fill.Both.id, insets, 0, 0)
        }
		val frame = new Frame {
		  title = "ZipFinderScala2"
		  val splitPanel = new SplitPane(Orientation.Horizontal, upperPanel, lowerPanel) { resizeWeight = 1.0 }
		  contents = splitPanel
		  peer.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		  listenTo(searchButton, directoryComboBox, directoryTree)
		  reactions += {
		    case ButtonClicked(b) =>
		      if (searchButton.text == SEARCH_LABEL) {
					searchButtonListener.performSearch
				} else {
					searchButtonListener.performStopSearch
				}
		    case ValueChanged(e) =>
				val file = new File(directoryComboBox.item)
				if (file.canRead) {
					directoryTree.selectedFile = file
				}
		    case SelectionChanged(e) =>
				val selectedItem = directoryComboBox.getOldValue
				val oldValue = if (selectedItem == null) "" else selectedItem.toString
				if (oldValue != directoryTree.selectedFile.toString) {
					directoryComboBox.item = directoryTree.selectedFile.toString
				}
		  }		  
      peer.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
		}
		showFrameFunction = { frame.visible = true }
		frame.pack
	}

	def getDirectory = {
		directoryComboBox.getOldValue
	}

	def getStringToFind = {
		textToFindField.text
	}

	def showDoneWorking {
		SwingUtilities.invokeLater(new Runnable {
			def run {
				searchButton.text = SEARCH_LABEL
				searchButton.enabled = true
			}
		})
	}

	def showFrame {
		showFrameFunction
	}

	def showWorking {
		searchButton.enabled = false
		SwingUtilities.invokeLater(new Runnable {
			def run {
			  searchButton.text =
			    searchButton.text match {
			      case "working.   " =>  "working..  "
			      case "working..  " =>  "working... "
			      case "working... " =>  "working...."
			      case "working...." =>  "working.   "
			      case _ => "working.   "
			    }
			}
		})
	}

}

trait SearchButtonListener {
	def performSearch
	def performStopSearch
}
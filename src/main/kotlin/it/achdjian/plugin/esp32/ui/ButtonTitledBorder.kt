package it.achdjian.plugin.esp32.ui

import com.intellij.ui.IdeBorderFactory
import com.intellij.util.ui.DialogUtil
import com.intellij.util.ui.JBInsets
import com.intellij.util.ui.UIUtil
import java.awt.*
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.border.TitledBorder

/**
 * @author evgeny zakrevsky
 */
class ButtonTitledBorder(title: String, val parent: Component, val statusChange: (close: Boolean) -> Any) : TitledBorder(title), MouseListener {

    companion object {
        private const val INDENT=20
        private val Insets = Insets(IdeBorderFactory.TITLED_BORDER_TOP_INSET, IdeBorderFactory.TITLED_BORDER_LEFT_INSET, IdeBorderFactory.TITLED_BORDER_BOTTOM_INSET, IdeBorderFactory.TITLED_BORDER_RIGHT_INSET)
    }

    private val titledSeparator =  ButtonTitledSeparator(title)
    private val insideInsets: Insets
    private val outsideInsets: Insets
    private var myShowLine = true
    private var deltaButtonY=0
    private var close = true

    init {
        DialogUtil.registerMnemonic(titledSeparator.label, null)

        outsideInsets = JBInsets.create(Insets)
        insideInsets = JBInsets(ButtonTitledSeparator.BOTTOM_INSET, INDENT, 0, 0)
        parent.addMouseListener(this)
    }

    override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
        val labelY = y + outsideInsets.top

        val titledSeparator = getTitledSeparator(c)
        val label = titledSeparator.label
        val button = titledSeparator.button
        val labelSize = label.preferredSize
        label.size = labelSize

        deltaButtonY = labelSize.height/2-button.height/2
        val buttonX = x +outsideInsets.left
        val buttonY = y + outsideInsets.top+deltaButtonY

        g.translate(buttonX, buttonY)
        button.paint(g)

        val labelX = buttonX + button.width
        g.translate(labelX, -(labelSize.height/2-button.height/2))
        label.paint(g)

        val separatorX = labelX + labelSize.width + ButtonTitledSeparator.SEPARATOR_LEFT_INSET
        val separatorY = labelY + if (UIUtil.isUnderAquaLookAndFeel()) 2 else labelSize.height / 2 - 1
        val separatorW = Math.max(0, width - separatorX - ButtonTitledSeparator.SEPARATOR_RIGHT_INSET)
        val separatorH = 2

        val separator = titledSeparator.separator
        separator.setSize(separatorW, separatorH)
        g.translate(separatorX - labelX, separatorY - labelY)
        if (myShowLine) {
            separator.paint(g)
        }
        g.translate(-separatorX, -separatorY)
    }

    private fun getTitledSeparator(c: Component): ButtonTitledSeparator {
        titledSeparator.isEnabled = c.isEnabled
        return titledSeparator
    }

    override fun getMinimumSize(c: Component): Dimension {
        val insets = getBorderInsets(c)
        val minSize = Dimension(insets.right + insets.left, insets.top + insets.bottom)
        val separatorSize = getTitledSeparator(c).preferredSize
        minSize.width = Math.max(minSize.width, separatorSize.width + outsideInsets.left + outsideInsets.right)
        return minSize
    }

    override fun getBorderInsets(c: Component?, insets: Insets): Insets {
        insets.top += (getTitledSeparator(c!!).preferredSize.getHeight() - ButtonTitledSeparator.TOP_INSET.toDouble() - ButtonTitledSeparator.BOTTOM_INSET.toDouble()).toInt()
        insets.top += UIUtil.DEFAULT_VGAP
        insets.top += insideInsets.top
        insets.left += insideInsets.left
        insets.bottom += insideInsets.bottom
        insets.right += insideInsets.right
        insets.top += outsideInsets.top
        insets.left += outsideInsets.left
        insets.bottom += outsideInsets.bottom
        insets.right += outsideInsets.right
        return insets
    }

    private fun dispatchMouseEvent(event: MouseEvent) {

        val button = titledSeparator.button

        if (button.visibleRect.contains(Point(event.point.x-outsideInsets.left, event.point.y-(deltaButtonY+outsideInsets.top)))){
            close = if (close) {
                titledSeparator.buttonOpen()
                false
            } else {
                titledSeparator.buttonClose()
                true
            }
            parent.repaint()
            statusChange(close)
        }
    }

    override fun mouseReleased(event: MouseEvent) {}
    override fun mouseEntered(event: MouseEvent) {}
    override fun mouseClicked(event: MouseEvent) = dispatchMouseEvent(event)
    override fun mouseExited(event: MouseEvent) {}
    override fun mousePressed(event: MouseEvent){}
}

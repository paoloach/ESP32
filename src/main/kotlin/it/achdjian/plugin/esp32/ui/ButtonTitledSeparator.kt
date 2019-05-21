/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.achdjian.plugin.esp32.ui

import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.*
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.border.Border

class ButtonTitledSeparator constructor( text: String ) : JPanel() {


    val label: JBLabel = object : JBLabel() {
        override fun getFont(): Font {
            return UIUtil.getTitledBorderFont()
        }
    }
    val separator = JSeparator(SwingConstants.HORIZONTAL)
    private val closeImg: ImageIcon
    private val openImg: ImageIcon
    val button: JButton

    init {
        layout = GridBagLayout()
        add(label, GridBagConstraints(0, 0, 1, 1, 0.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.NONE, Insets(0, 0, 0, 0), 0, 0))
        add(separator,
                GridBagConstraints(1, 0, GridBagConstraints.REMAINDER, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                        Insets(2, SEPARATOR_LEFT_INSET, 0, SEPARATOR_RIGHT_INSET), 0, 0))
        border = EMPTY_BORDER
        label.text = text
        val imgSize = label.preferredSize.height / 2
        closeImg = ImageIcon(ImageIO.read(ButtonTitledSeparator::class.java.classLoader.getResourceAsStream(CLOSE_IMG)).getScaledInstance(imgSize, imgSize, Image.SCALE_SMOOTH))
        openImg = ImageIcon(ImageIO.read(ButtonTitledSeparator::class.java.classLoader.getResourceAsStream(OPEN_IMG)).getScaledInstance(imgSize, imgSize, Image.SCALE_SMOOTH))
        button = createButton(imgSize)
    }


    private fun createButton(imgSize: Int): JButton {

        val button = JButton(closeImg)
        button.border = BorderFactory.createEmptyBorder()
        button.isContentAreaFilled = false
        button.size = Dimension(imgSize, imgSize)
        return button
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        label.isEnabled = enabled
        separator.isEnabled = enabled
        button.isEnabled=enabled
    }

    fun buttonOpen() { button.icon = openImg}
    fun buttonClose() { button.icon = closeImg}

    companion object {
        val TOP_INSET = 7
        val BOTTOM_INSET = 5
        val SEPARATOR_LEFT_INSET = 6
        val SEPARATOR_RIGHT_INSET = 3

        val EMPTY_BORDER: Border = JBUI.Borders.empty(TOP_INSET, 0, BOTTOM_INSET, 0)
        const val CLOSE_IMG = "images/drop-right-arrow.png"
        const val OPEN_IMG = "images/drop-down-arrow.png"
    }
}

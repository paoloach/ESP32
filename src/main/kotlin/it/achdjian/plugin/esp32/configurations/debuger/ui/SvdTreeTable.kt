package it.achdjian.plugin.esp32.configurations.debuger.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.text.TextWithMnemonic
import com.intellij.ui.treeStructure.treetable.TreeTable
import com.intellij.ui.treeStructure.treetable.TreeTableCellRenderer
import com.intellij.ui.treeStructure.treetable.TreeTableTree
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.tree.TreeUtil
import com.intellij.xdebugger.impl.ui.XDebuggerUIConstants
import java.awt.Component
import java.awt.datatransfer.StringSelection
import java.awt.event.ActionEvent
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.io.InputStream
import java.nio.file.Files
import java.util.*
import javax.swing.AbstractAction
import javax.swing.JTable
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeExpansionListener
import javax.swing.table.DefaultTableCellRenderer


fun createSvdTreeTable(windowProfileState: ProjectState): SvdTreeTable {
    val root = SvdRoot()
    return SvdTreeTable(root.treeTableModel, windowProfileState, root)
}


private class NameTreeTableCellRenderer(table: SvdTreeTable, private val myTree: TreeTableTree) :
    TreeTableCellRenderer(table, myTree) {
    override fun getTableCellRendererComponent(
        table: JTable,
        value: Any,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        val component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
        if (!isSelected) {
            val pathForRow = myTree.getPathForRow(row)
            val rowObject = pathForRow?.lastPathComponent
            if (rowObject is SvdValue<*> && rowObject.isChanged) {
                myTree.foreground = XDebuggerUIConstants.CHANGED_VALUE_ATTRIBUTES.fgColor
            }
        }
        return component
    }

}

private class HideNodeAction(val table: SvdTreeTable) : DumbAwareAction("Hide node") {
    override fun actionPerformed(e: AnActionEvent) {
        val selectedNode = table.tree.lastSelectedPathComponent
        if (selectedNode != null && selectedNode !is SvdField) {
            val treeWalker = ArrayDeque<BaseSvdNode>()
            treeWalker.addLast(selectedNode as SvdNode<*>)
            while (!treeWalker.isEmpty()) {
                val node = treeWalker.removeLast() as SvdNode<*>
                if (node !is SvdField) {
                    if (node !is SvdRegister) {
                        treeWalker.addAll(node.children)
                    }
                    if (node is SvdFile) {
                        table.root.children.remove(node)
                    }
                    if (node !is SvdRoot) {
                        table.root.activeNodes.remove(node)
                    }
                }
            }
            val expandedPaths = TreeUtil.collectExpandedPaths(table.tree)
            table.root.treeTableModel.notifyTreeUpdated()
            table.myWindowProfileState.notifyTreeChange(table)
            TreeUtil.restoreExpandedPaths(table.tree, expandedPaths)
        }
    }

    override fun update(e: AnActionEvent) {
        val selectedNode = table.tree.lastSelectedPathComponent as SvdNode<*>?
        selectedNode?.let {
            e.presentation.isVisible = true
            e.presentation.setTextWithMnemonic {
                TextWithMnemonic.fromPlainText("Hide Node ${selectedNode.name}")
            }
            e.presentation.isEnabled = selectedNode !is SvdField
        } ?: run { e.presentation.isVisible = false }

    }
}


private class SetFormatAction(val format: Format, val table: SvdTreeTable) : ToggleAction(format.readableName) {
    private var selected = false
    override fun isDumbAware(): Boolean = true
    override fun isSelected(e: AnActionEvent): Boolean = selected

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        if (state) {
            val selectedNode = table.tree.lastSelectedPathComponent
            if (selectedNode is SvdValue<*>) {
                selectedNode.format = format
                selected = true
            }
        }
    }

    override fun update(e: AnActionEvent) {
        val selectedNode = table.tree.lastSelectedPathComponent
        if (selectedNode is SvdValue<*>) {
            e.presentation.isEnabled = true
            selected = selectedNode.format == format
        } else {
            e.presentation.isEnabled = false
            selected = false
        }
        super.update(e)
    }
}

private class CopyPeripheralAddressAction(val table: SvdTreeTable) : DumbAwareAction("Copy peripherical address") {
    override fun update(e: AnActionEvent) {
        val selectedNode = table.tree.lastSelectedPathComponent
        e.presentation.isEnabled = selectedNode is SvdRegister
        super.update(e)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val selectedNode = table.tree.lastSelectedPathComponent
        if (selectedNode is SvdRegister) {
            val text = addressToString(selectedNode.address)
            CopyPasteManager.getInstance().setContents(StringSelection(text))
        }
    }
}


private class CopyValueAction(val table: SvdTreeTable) :
    DumbAwareAction("Action title", "Action description", AllIcons.Actions.Copy) {

    init {
        copyShortcutFrom(ActionManager.getInstance().getAction("\$Copy"))
    }

    override fun actionPerformed(e: AnActionEvent) {
        doCopy()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = table.isValueSelected
    }

    fun doCopy() {
        val selectedNode = table.tree.lastSelectedPathComponent
        if (selectedNode is SvdValue<*>) {
            val text = selectedNode.displayValue
            CopyPasteManager.getInstance().setContents(StringSelection(text))
        }
    }


}

class SvdTreeTable(model: SvdTreeTableModel, val myWindowProfileState: ProjectState, val root: SvdRoot) :
    TreeTable(model) {

    init {
        setAutoResizeMode(3)
        val xWidth = getFontMetrics(font).charWidth('X')
        val columnModel = getColumnModel()
        val nameColumn = columnModel.getColumn(0)
        val tree = this.tree
        nameColumn.cellRenderer = NameTreeTableCellRenderer(this, tree)
        nameColumn.width = xWidth * NAME_LENGTH + COLUMN_EXTRA_PX
        val valueColumn = columnModel.getColumn(1)
        valueColumn.width = xWidth * HEX_VALUE_LENGTH + COLUMN_EXTRA_PX
        val descriptionColumn = columnModel.getColumn(2)
        descriptionColumn.minWidth = 0
        descriptionColumn.preferredWidth = 0
        descriptionColumn.width = 0
        getTableHeader().resizingColumn = descriptionColumn
        ExpandingTreeTableSpeedSearch(this) { treePath ->
            treePath?.let {
                val component = it.lastPathComponent
                if (component is SvdNode<*>) {
                    "${component.name} ${component.description}"
                } else
                    null
            }
        }
        setupPopupMenu()
        setRootVisible(false)
        val descriptionRenderer = DefaultTableCellRenderer()
        descriptionRenderer.foreground = UIUtil.getContextHelpForeground()
        descriptionColumn.cellRenderer = descriptionRenderer
        tree.addTreeExpansionListener(object : TreeExpansionListener {
            override fun treeExpanded(event: TreeExpansionEvent) {
                myWindowProfileState.notifyTreeNodeChanged(this@SvdTreeTable, event.path)
            }

            override fun treeCollapsed(event: TreeExpansionEvent) {
                myWindowProfileState.notifyTreeNodeChanged(this@SvdTreeTable, event.path)
            }
        })
    }


    val isValueSelected: Boolean
        get() = tree.lastSelectedPathComponent is SvdValue<*>

    private fun setupPopupMenu() {
        val actionManager = ActionManager.getInstance()
        val group = DefaultActionGroup()
        val copyValueAction = CopyValueAction(this)
        actionMap.put("copy", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                copyValueAction.doCopy()
            }
        })
        group.add(copyValueAction)
        group.add(CopyPeripheralAddressAction(this))
        group.addSeparator()

        Format.values().forEach { group.add(SetFormatAction(it, this)) }
        group.addSeparator()
        group.add(HideNodeAction(this))
        val menu = actionManager.createActionPopupMenu("DebuggerToolbar", group)
        this.componentPopupMenu = menu.component
    }

    fun setAutoUpdate(autoUpdate: Boolean) {
        foreground = if (autoUpdate) UIUtil.getTableForeground() else UIUtil.getLabelDisabledForeground()
        this.repaint()
    }

    fun restoreState() {
        reopenFiles()
        restoreColumns()
        restoreNodeStates()
    }

    private fun reopenFiles() {
        myWindowProfileState.loadedFiles
            .filter { Files.exists(it) }
            .firstOrNull { Files.isReadable(it) }
            ?.forEach {
                var inputStream: InputStream? = null
                try {
                    inputStream = BufferedInputStream(FileInputStream(it.toFile()))
                    root.addFile(inputStream, it)
                } catch (e: Exception) {
                    log.warn(e)
                } finally {
                    inputStream?.close()
                }

            }
    }

    private fun restoreColumns() {
        val columnOrder = myWindowProfileState.columnOrder
        val columnWidths = myWindowProfileState.columnWidths
        val columnCount = Math.min(Math.min(this.columnCount, columnOrder.size), columnWidths.size)
        for (i in 0 until columnCount) {
            val columnName = myWindowProfileState.columnOrder[i]
            try {
                val index = columnModel.getColumnIndex(columnName)
                if (index != i) {
                    columnModel.moveColumn(index, i)
                }
                columnModel.getColumn(i).width = myWindowProfileState.columnWidths[i]
            } catch (var8: IllegalArgumentException) {
            }
        }
    }

    private fun restoreNodeStates() {
        val nodes = myWindowProfileState.nodes
        val nodeWalker = ArrayDeque(root.children)
        while (!nodeWalker.isEmpty()) {
            val svdNode = nodeWalker.removeLast()
            nodeWalker.addAll(svdNode.children)
            nodes[svdNode.id]?.takeIf { it.length == 2 }?.let { nodeState ->
                root.activeNodes.add(svdNode)
                svdNode.setFormatFromSign(nodeState[0])
            }
        }
        root.treeTableModel.resetActiveCache()
        TreeUtil.treePathTraverser(tree).forEach { treePath ->
            val node = treePath.lastPathComponent as SvdNode<*>
            nodes[node.id]?.takeIf { it[1] == 'E' }?.let { tree.expandPath(treePath) }
        }
    }


    companion object {
        private val log = Logger.getInstance(SvdTreeTable::class.java)
        private const val HEX_VALUE_LENGTH = 10
        private const val COLUMN_EXTRA_PX = 12
        private const val NAME_LENGTH = 25
    }


}

package it.achdjian.plugin.esp32.configurations.debuger.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.util.PopupUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.*
import com.intellij.util.containers.Convertor
import com.intellij.util.ui.tree.TreeUtil
import java.awt.Dimension
import java.io.IOException
import java.nio.file.Paths
import javax.swing.JComponent
import javax.swing.JTree
import javax.swing.SwingUtilities
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath


private fun findActive(nodes: MutableSet<SvdNode<*>>, parent: CheckedTreeNode): Boolean {
    var active = false
    if (parent.userObject is SvdRegister) {
        active = parent.isChecked
    } else {
        for (i in 0 until parent.childCount) {
            active =  active or findActive(nodes, parent.getChildAt(i) as CheckedTreeNode)
        }
    }
    if (active) {
        nodes.add(parent.userObject as SvdNode<*>)
    }
    return active
}

private class UnloadAction(val parent: SvdShowHideDialog)  :  AnAction("Unload", "Unload", AllIcons.General.Remove), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        parent.myTree.selectionPath?.let { path->
            val checkedTreeNode = path.lastPathComponent as CheckedTreeNode
            val selected = checkedTreeNode.userObject
            if (selected is SvdFile) {
                val index = parent.myCheckedTreeRoot.getIndex(checkedTreeNode)
                parent.myCheckedTreeRoot.remove(checkedTreeNode)
                parent.myTree.removeSelectionPath(path)
                (parent.myTree.model as DefaultTreeModel).nodesWereRemoved(
                    parent.myCheckedTreeRoot,
                    intArrayOf(index),
                    arrayOf<Any>(checkedTreeNode)
                )
                parent.mySvdRoot.unloadFile(selected)
            }
        }
    }

    override fun update(e: AnActionEvent) {
        val path = parent.myTree.selectionPath
        val fileSelected =  path != null && (path.lastPathComponent as CheckedTreeNode).userObject is SvdFile
        e.presentation.isEnabled = fileSelected
    }
}

class MyCheckboxTreeCellRenderer: CheckboxTree.CheckboxTreeCellRenderer(){
    override fun customizeRenderer(tree: JTree, value: Any, selected: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean) {
        super.customizeRenderer(tree, value, selected, expanded, leaf, row, hasFocus)
        textRenderer.append((value as CheckedTreeNode).userObject.toString())
    }
}

class MyCheckBoxTree(checkedTreeRoot: CheckedTreeNode) : CheckboxTree(MyCheckboxTreeCellRenderer(), checkedTreeRoot) {
    override fun installSpeedSearch() {
        TreeSpeedSearch(this, Convertor { treePath -> treePath.lastPathComponent.toString() }, true)
    }
}

class SvdShowHideDialog(val mySvdRoot: SvdRoot, private val myProject: Project) {
    internal val myCheckedTreeRoot = CheckedTreeNode(mySvdRoot)
    internal val myTree = MyCheckBoxTree(myCheckedTreeRoot)

    private lateinit var myToolbarComponent: JComponent
    private fun addCheckedFolder(checkedParent: CheckedTreeNode, parent: SvdNode<*>) {
        if (parent !is SvdRegister) {
            parent.children.forEach {svdNode->
                val checkedTreeNode = CheckedTreeNode(svdNode)
                checkedTreeNode.isChecked = mySvdRoot.isActive(svdNode)
                checkedParent.add(checkedTreeNode)
                addCheckedFolder(checkedTreeNode, svdNode as SvdNode<*>)
            }
        }
    }

    fun showHideNodes() {
        addCheckedFolder(myCheckedTreeRoot, mySvdRoot)
        myTree.setSelectionRow(0)
        TreeUtil.treePathTraverser(myTree)
            .filter { (it.lastPathComponent as CheckedTreeNode).userObject !is SvdPeripheral }
            .forEach { myTree.expandPath(it) }

        val scrollPane = ScrollPaneFactory.createScrollPane(myTree)
        val bounds = ScreenUtil.getMainScreenBounds()
        scrollPane.preferredSize = Dimension(bounds.width / 2, bounds.height * 3 / 4)
        val actionGroup = DefaultActionGroup()
        val loadAction: AnAction = object : DumbAwareAction("Load File", "Hardware Definition Description", AllIcons.General.Add) {
            override fun actionPerformed(e: AnActionEvent) = loadFile()
        }
        val actionManager = ActionManager.getInstance()
        actionGroup.add(loadAction)
        actionGroup.add(UnloadAction(this))
        val emptyText = myTree.emptyText
        emptyText.text = "No registers loader"
        emptyText.appendSecondaryText("Load file", SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES) {  loadFile() }
        val builder = DialogBuilder()
        builder.addCloseButton()
        builder.setCenterPanel(scrollPane)
        builder.setTitle("Select Peripherical to show")
        builder.setPreferredFocusComponent(myTree)
        val toolbar = actionManager.createActionToolbar("toolbar", actionGroup, true)
        myToolbarComponent = toolbar.component
        builder.setNorthPanel(myToolbarComponent)
        builder.setHelpId("toolwindows.peripheralview")
        if (mySvdRoot.children.isEmpty()) {
            SwingUtilities.invokeLater { loadFile() }
        }
        builder.show()
        val nodes= mutableSetOf<SvdNode<*>>()
        findActive(nodes, myCheckedTreeRoot)
        mySvdRoot.setActive(nodes)
    }

    private fun loadFile() {
        val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor("svd")
        FileChooser.chooseFile(descriptor, myProject, null as VirtualFile?) ?.let { file->
            try {
                val path = Paths.get(file.path)
                var foundFile: CheckedTreeNode? = null
                var checkedFile: CheckedTreeNode
                var i = 0
                while (i < myCheckedTreeRoot.childCount && foundFile == null) {
                    checkedFile = myCheckedTreeRoot.getChildAt(i) as CheckedTreeNode
                    val svdFile = checkedFile.userObject as SvdFile
                    if (path.compareTo(svdFile.path) == 0){
                        foundFile = checkedFile
                    }
                    ++i
                }

                foundFile ?. let {
                    myTree.selectionPath = TreePath(arrayOf<Any?>(myCheckedTreeRoot, it))
                    PopupUtil.showBalloonForComponent(myToolbarComponent, "The file $it is already loaded", MessageType.INFO, false, null as Disposable?)
                    return
                }
                mySvdRoot.addFile(file.inputStream, path)?.let { svdFile ->
                    checkedFile = CheckedTreeNode(svdFile)
                    addCheckedFolder(checkedFile, svdFile)
                    myCheckedTreeRoot.add(checkedFile)
                    (myTree.model as DefaultTreeModel).nodesWereInserted(myCheckedTreeRoot, intArrayOf(myCheckedTreeRoot.childCount - 1))
                    val filePath = TreePath(arrayOf<Any?>(myCheckedTreeRoot, checkedFile))
                    TreeUtil.treePathTraverser(myTree).filter { filePath.isDescendant(it) }.forEach { myTree.expandPath(it) }
                }
            } catch (var8: IOException) {
                Logger.getInstance(SvdShowHideDialog::class.java)
                    .error("Unexpected exception while reading " + file.canonicalPath, var8)
            }
        }
    }
}

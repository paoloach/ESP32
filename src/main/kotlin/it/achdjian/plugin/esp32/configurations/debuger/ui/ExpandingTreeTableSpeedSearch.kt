package it.achdjian.plugin.esp32.configurations.debuger.ui

import com.intellij.ui.TreeTableSpeedSearch
import com.intellij.ui.treeStructure.treetable.TreeTable
import com.intellij.util.ui.tree.TreeUtil
import javax.swing.tree.TreePath

internal class ExpandingTreeTableSpeedSearch(treeTable: TreeTable, stringConverter: (TreePath?) -> String?) :
    TreeTableSpeedSearch(treeTable, stringConverter) {

    override fun getAllElements(): Array<Any> =
        TreeUtil.treePathTraverser((this.component as TreeTable).tree).map { it as Any }.toList().toTypedArray()

    override fun selectElement(element: Any, selectedText: String) {
        val treePath = element as TreePath
        (this.component as TreeTable).tree.expandPath(treePath.parentPath)
        super.selectElement(element, selectedText)
    }
}
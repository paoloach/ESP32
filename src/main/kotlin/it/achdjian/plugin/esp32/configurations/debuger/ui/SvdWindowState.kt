package it.achdjian.plugin.esp32.configurations.debuger.ui

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.ui.treeStructure.treetable.TreeTableTree
import com.intellij.util.containers.hash.LinkedHashMap
import com.intellij.util.ui.tree.TreeUtil
import com.intellij.xdebugger.XDebugProcess
import java.nio.file.Path
import java.util.*
import javax.swing.table.TableColumn
import javax.swing.tree.TreePath


private fun calcState(tree: TreeTableTree, treePath: TreePath, svdNode: SvdNode<*>): String? {
    val nodeState = charArrayOf('-', 'O')
    if (svdNode is SvdValue) {
        if (svdNode is SvdField && svdNode.defaultFormat === svdNode.format) {
            return null
        }
        nodeState[0] = svdNode.format.sign
    }
    nodeState[1] = if (tree.isExpanded(treePath)) 'E' else 'C'
    return String(nodeState)
}


class ProjectState {
    val columnWidths = mutableListOf<Int>()
    val columnOrder = mutableListOf<String>()
    val loadedFiles = mutableListOf<Path>()
    val nodes = LinkedHashMap<String, String>()

    fun notifyColumnsChange(treeTable: SvdTreeTable) {
        columnWidths.clear()
        columnOrder.clear()
        val columns: Enumeration<*> = treeTable.columnModel.getColumns()
        while (columns.hasMoreElements()) {
            val column = columns.nextElement() as TableColumn
            columnWidths.add(column.width)
            columnOrder.add(column.identifier.toString())
        }
    }

    fun notifyTreeNodeChanged(treeTable: SvdTreeTable, treePath: TreePath) {
        val svdNode = treePath.lastPathComponent as SvdNode<*>
        calcState(treeTable.tree, treePath, svdNode)
            ?. let { nodes[svdNode.id] = it }
            ?: nodes.remove(svdNode.id)
    }

    fun notifyTreeChange(treeTable: SvdTreeTable) {
        val svdRoot = treeTable.root
        loadedFiles.clear()
        loadedFiles.addAll(svdRoot.children.filterIsInstance<SvdFile>().map { it.path })
        nodes.clear()

        TreeUtil.treePathTraverser(treeTable.tree).forEach { treePath->
            val svdNode = treePath.lastPathComponent as SvdNode<*>
            if (svdNode !is SvdRoot && svdRoot.isActive(svdNode)){
                calcState(treeTable.tree, treePath, svdNode) ?.let {
                    nodes[svdNode.id] = it
                }
            }
        }
    }
}

@State(
    name = "ESP32SvdWindowState",
    storages = [Storage("\$WORKSPACE_FILE$")]
)
class SvdWindowState :  PersistentStateComponent<ProjectState> {
    private var projectState = ProjectState()

    override fun getState(): ProjectState {
        return projectState
    }

    override fun loadState(projectState: ProjectState) {
        this.projectState = projectState
    }



    companion object {
        operator fun get(process: XDebugProcess): ProjectState {
            val svdWindowState =   process.session.project.getComponent(SvdWindowState::class.java) as SvdWindowState
            return svdWindowState.projectState
        }
    }

}
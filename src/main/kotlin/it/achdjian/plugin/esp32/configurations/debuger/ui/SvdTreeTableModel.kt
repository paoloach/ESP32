package it.achdjian.plugin.esp32.configurations.debuger.ui

import com.intellij.ui.treeStructure.treetable.TreeTableModel
import com.intellij.util.ArrayUtil
import com.intellij.util.SmartList
import com.jetbrains.cidr.execution.debugger.CidrDebugProcess
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JTree
import javax.swing.event.TreeModelEvent
import javax.swing.event.TreeModelListener
import javax.swing.tree.TreePath


class SvdTreeTableModel(val myRoot: SvdRoot) : TreeTableModel {
    private val myActiveCache = mutableMapOf<SvdNode<*>, List<BaseSvdNode>>()
    private val myListeners = SmartList<TreeModelListener>()
    private var myCoalescer = createAdressCoalescer(myRoot.activeNodes)
    private val myCancelled = AtomicBoolean()

    override fun getColumnCount() = 3

    override fun getColumnName(column: Int) = when (column) {
        0 -> "name"
        1 -> "value"
        else -> "description"
    }

    override fun getColumnClass(column: Int): Class<*> =
        if (column == 0) TreeTableModel::class.java else String::class.java

    override fun getValueAt(node: Any, column: Int) = when (column) {
        0 -> node.toString()
        1 -> (node as SvdNode<*>).displayValue
        else -> (node as SvdNode<*>).description
    }

    override fun isCellEditable(node: Any, column: Int) = false

    override fun setValueAt(aValue: Any?, node: Any, column: Int) {
    }

    override fun setTree(tree: JTree) {}

    override fun getChild(parent: Any, index: Int): Any {
        val activeChildren: List<*> = getActiveChildren(parent)
        return activeChildren[index]!!
    }

    private fun getActiveChildren(parentObject: Any): List<BaseSvdNode> {
        return if (parentObject !== myRoot && parentObject !is SvdRegister) {
            val parent = parentObject as SvdNode<*>
            myActiveCache[parent] ?: run {
                val nodes = parent.children.filter { myRoot.isActive(it) }
                myActiveCache[parent] = nodes
                nodes
            }
        } else {
            (parentObject as SvdNode<*>).children
        }
    }

    override fun getChildCount(parent: Any) = getActiveChildren(parent).size

    override fun isLeaf(node: Any) = getChildCount(node) == 0

    override fun valueForPathChanged(path: TreePath, newValue: Any?) {
    }

    override fun getIndexOfChild(parent: Any, child: Any): Int = getActiveChildren(parent).indexOf(child)

    override fun addTreeModelListener(l: TreeModelListener) {
        myListeners.add(l)
    }

    override fun getRoot(): Any =myRoot

    override fun removeTreeModelListener(l: TreeModelListener) {
        myListeners.remove(l)
    }

    fun notifyFileInserted(newSvdFile: SvdFile) {
        val event = TreeModelEvent(
            this,
            TreePath(myRoot),
            intArrayOf(getIndexOfChild(root, newSvdFile)),
            arrayOf<Any>(newSvdFile)
        )
        myListeners.forEach { it.treeStructureChanged(event) }
    }

    fun notifyTreeUpdated() {
        resetActiveCache()
        val event = TreeModelEvent(this, TreePath(myRoot), ArrayUtil.EMPTY_INT_ARRAY, ArrayUtil.EMPTY_OBJECT_ARRAY)
        myListeners.forEach { it.treeStructureChanged(event) }
    }

    fun notifyFileDeleted(deleted: SvdFile, index: Int) {
        val event = TreeModelEvent(this, TreePath(myRoot), intArrayOf(index), arrayOf<Any>(deleted))
        myListeners.forEach { it.treeNodesRemoved(event) }
    }

    private fun resetActiveCache() {
        myActiveCache.clear()
        myCoalescer = createAdressCoalescer(myRoot.activeNodes)
    }

    fun updateValues(process: CidrDebugProcess, updateListener: Runnable) {
        myCancelled.set(false)
        myCoalescer.updateValues(process, myCancelled, updateListener)
    }

    fun cancelUpdates() = myCancelled.set(true)

}

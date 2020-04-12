package it.achdjian.plugin.esp32.configurations.debuger.ui

import com.intellij.execution.ui.RunnerLayoutUi
import com.intellij.icons.AllIcons
import com.intellij.ide.actions.ContextHelpAction
import com.intellij.ide.scratch.ScratchRootType
import com.intellij.internal.statistic.eventLog.FeatureUsageData
import com.intellij.internal.statistic.service.fus.collectors.FUCounterUsageLogger
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.treeStructure.treetable.TreeTableTree
import com.intellij.util.ui.tree.TreeUtil
import com.intellij.xdebugger.XDebugSessionListener
import com.jetbrains.cidr.execution.debugger.CidrDebugProcess
import it.achdjian.plugin.esp32.CUSTOM_GDB_RUN_CONFIGURATION
import java.awt.BorderLayout
import java.awt.datatransfer.StringSelection
import java.io.PrintWriter
import java.io.StringWriter
import java.io.Writer
import java.util.*
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.ChangeEvent
import javax.swing.event.ListSelectionEvent
import javax.swing.event.TableColumnModelEvent
import javax.swing.event.TableColumnModelListener


fun triggerPeripheralViewOpen(project: Project, activeNodesCounter: Int) {
    val data = FeatureUsageData().addProject(project).addData("ACTIVE_PERIPHERAL_NODES", activeNodesCounter)
    FUCounterUsageLogger.getInstance().logEvent(project, "esp32", "PERIPHERAL_VIEW_SHOWN", data)
}

fun createPanel(process: CidrDebugProcess): SvdPanel {
    val svdWindowProfileState = SvdWindowState[process]
    val treeTable = createSvdTreeTable(svdWindowProfileState)
    val scrollPane = ScrollPaneFactory.createScrollPane(treeTable, 22, 30)
    return SvdPanel(scrollPane, DefaultActionGroup(), treeTable, process, svdWindowProfileState)
}

fun registerPeripheralTab(process: CidrDebugProcess, ui: RunnerLayoutUi) {
    val svdPanel = createPanel(process)
    val content = ui.createContent("svdView", svdPanel, "Peripherals", CUSTOM_GDB_RUN_CONFIGURATION, svdPanel.defaultFocusComponent)
    content.isCloseable = false
    content.helpId = "toolwindows.peripheralview"
    ui.addContent(content)
    process.session.addSessionListener(object : XDebugSessionListener {
        override fun sessionPaused() {
            svdPanel.updateValues(false)
        }

        override fun beforeSessionResume() {
            svdPanel.cancelUpdates()
        }

        override fun sessionStopped() {
            svdPanel.cancelUpdates()
        }
    })
}

class SvdPanel(innerPanel: JComponent, group: DefaultActionGroup, val myTreeTable: SvdTreeTable, val myProcess: CidrDebugProcess, val myWindowProfileState: ProjectState) :
    JPanel(BorderLayout()) {
    private var autoUpdate: Boolean
    private var myPendingUpdateValue = false
    val defaultFocusComponent = innerPanel

    private fun setupActions(group: DefaultActionGroup) {
        group.add(object : ToggleAction("Disable Refresh on Step", null, AllIcons.Actions.StopRefresh){
            override fun isSelected(e: AnActionEvent)= !autoUpdate

            override fun isDumbAware() = true

            override fun setSelected(e: AnActionEvent, state: Boolean) {
                autoUpdate = !state
                e.presentation.text = if (autoUpdate) "Disable Refresh on Step"
                                      else "Enable Refresh on Step"
                myTreeTable.setAutoUpdate(autoUpdate)
            }
        })

        group.add(object : DumbAwareAction("Panel Refresh", null, AllIcons.Actions.Refresh){
            override fun actionPerformed(e: AnActionEvent) = updateValues(true)

            override fun update(e: AnActionEvent) {
                e.presentation.isEnabled = myProcess.session.isSuspended
            }
        })
        group.addSeparator()
        group.add(object : DumbAwareAction("Export csv to  Clipboard", null, AllIcons.Actions.Copy){
            override fun actionPerformed(e: AnActionEvent) {
                var writer: StringWriter? = null
                try {
                    writer = StringWriter()
                    var printWriter: PrintWriter? = null
                    try {
                        printWriter = PrintWriter(writer)
                        myTreeTable.root.export(printWriter)
                        CopyPasteManager.getInstance().setContents(StringSelection(writer.toString()))
                    } catch (e: Exception) {

                    } finally {
                        printWriter?.close()
                    }
                } finally {
                    writer?.close()
                }
            }
        })
        group.add(object : DumbAwareAction("Open csv Editor", null, AllIcons.ToolbarDecorator.Export){
            override fun actionPerformed(e: AnActionEvent) {
                val profile = myProcess.session.runProfile
                var filename = String.format(
                    "Peripheral-%s%tF-%2\$tT",
                    if (profile != null) profile.name + "-" else "",
                    Date()
                )
                filename = FileUtil.sanitizeFileName(filename, false) + ".csv"
                val project = myProcess.project
                var writer: Writer? =null
                var printWriter: PrintWriter? = null
                try {
                    writer = StringWriter()
                    writer?.let{printWriter = PrintWriter(it)}
                    printWriter?.let { myTreeTable.root.export(it)}
                    val scratchFile = ScratchRootType.getInstance().createScratchFile(project, filename, PlainTextLanguage.INSTANCE, writer.toString())
                    scratchFile?.let { OpenFileDescriptor(project, scratchFile).navigate(true) }
                } catch (e: Exception) {
                    throw RuntimeException(e)
                } finally {
                    printWriter?.close()
                    writer?.close()
                }
            }
        })
        group.addSeparator()
        group.add(object : DumbAwareAction("Configure", null, AllIcons.General.Filter){
            override fun actionPerformed(e: AnActionEvent) = configureSvd()
        })
        group.add(ContextHelpAction(HELP_ID))
    }

    private fun configureSvd() {
        val tree: TreeTableTree = myTreeTable.tree
        SvdShowHideDialog(myTreeTable.root, myProcess.project).showHideNodes()
        myTreeTable.root.treeTableModel.notifyTreeUpdated()
        TreeUtil.treePathTraverser(tree).filter{it.lastPathComponent !is SvdRegister}.forEach { tree.expandPath(it) }
        myWindowProfileState.notifyTreeChange(myTreeTable)
        updateValues(true)
    }

    fun updateValues(forced: Boolean) {
        if ((forced || autoUpdate) && myProcess.session.isSuspended) {
            if (this.isDisplayable) {
                doUpdateValues()
            } else {
                myPendingUpdateValue = true
            }
        }
    }

    private fun doUpdateValues() {
        try {
            myTreeTable.root.treeTableModel.updateValues(myProcess, Runnable  { myTreeTable.repaint() })
        } finally {
            myPendingUpdateValue = false
        }
    }

    fun cancelUpdates() = myTreeTable.root.treeTableModel.cancelUpdates()

    companion object {
        const val HELP_ID = "toolwindows.peripheralview"

    }

    init {
        autoUpdate = true
        val actionToolbar = ActionManager.getInstance().createActionToolbar("DebuggerToolbar", group, false)
        val component = actionToolbar.component
        this.add(component, "West")
        this.add(innerPanel, "Center")
        setupActions(group)
        val emptyText = myTreeTable.emptyText
        emptyText.text = "No registers loaded"
        emptyText.appendSecondaryText("Load file", SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES) { configureSvd() }
        myTreeTable.setAutoUpdate(autoUpdate)
        myTreeTable.getColumnModel().addColumnModelListener(object : TableColumnModelListener {
            override fun columnAdded(e: TableColumnModelEvent) {}
            override fun columnRemoved(e: TableColumnModelEvent) {}
            override fun columnMoved(e: TableColumnModelEvent) {
                myWindowProfileState.notifyColumnsChange(myTreeTable)
            }

            override fun columnMarginChanged(e: ChangeEvent) {
                myWindowProfileState.notifyColumnsChange(myTreeTable)
            }

            override fun columnSelectionChanged(e: ListSelectionEvent) {}
        })
        myTreeTable.restoreState()
        addHierarchyListener {
            if (this.isDisplayable && myPendingUpdateValue) {
                doUpdateValues()
                triggerPeripheralViewOpen(myProcess.project, myTreeTable.root.activeNodes.size)
            }
        }
    }
}
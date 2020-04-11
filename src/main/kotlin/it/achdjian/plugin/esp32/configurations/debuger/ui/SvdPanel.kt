package it.achdjian.plugin.esp32.configurations.debuger.ui

import com.intellij.execution.ui.RunnerLayoutUi
import com.intellij.icons.AllIcons
import com.intellij.ide.actions.ContextHelpAction
import com.intellij.ide.scratch.ScratchRootType
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.io.FileUtil
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.treeStructure.treetable.TreeTableTree
import com.intellij.util.containers.JBTreeTraverser
import com.intellij.util.ui.StatusText
import com.intellij.util.ui.tree.TreeUtil
import com.intellij.xdebugger.XDebugSessionListener
import com.jetbrains.cidr.execution.debugger.CidrDebugProcess
import java.awt.BorderLayout
import java.awt.datatransfer.StringSelection
import java.awt.event.ActionEvent
import java.awt.event.HierarchyEvent
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.io.Writer
import java.util.*
import java.util.function.Consumer
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.ChangeEvent
import javax.swing.event.ListSelectionEvent
import javax.swing.event.TableColumnModelEvent
import javax.swing.event.TableColumnModelListener
import javax.swing.tree.TreePath

class SvdPanel private constructor(
    innerPanel: JComponent,
    group: DefaultActionGroup,
    treeTable: SvdTreeTable,
    process: CidrDebugProcess,
    profileState: com.jetbrains.cidr.cpp.execution.debugger.embedded.svd.SvdWindowState.ProjectState
) :
    JPanel() {
    private val myInnerPanel: JComponent
    private val myTreeTable: SvdTreeTable
    private val myProcess: CidrDebugProcess
    private val myWindowProfileState: com.jetbrains.cidr.cpp.execution.debugger.embedded.svd.SvdWindowState.ProjectState
    private var autoUpdate: Boolean
    private var myPendingUpdateValue = false
    val defaultFocusComponent: JComponent
        get() {
            val var10000 = myInnerPanel
            if (var10000 == null) {
                `$$$reportNull$$$0`(4)
            }
            return var10000
        }

    private fun setupActions(group: DefaultActionGroup) {
        group.add(object : ToggleAction(
            EmbeddedBundle.message(
                "disable.refresh.on.step",
                arrayOfNulls<Any>(0)
            ), null as String?, AllIcons.Actions.StopRefresh
        ) {
            override fun isSelected(e: AnActionEvent): Boolean {
                if (e == null) {
                    `$$$reportNull$$$0`(0)
                }
                return !autoUpdate
            }

            override fun isDumbAware(): Boolean {
                return true
            }

            override fun setSelected(
                e: AnActionEvent,
                state: Boolean
            ) {
                if (e == null) {
                    `$$$reportNull$$$0`(1)
                }
                autoUpdate = !state
                e.presentation.setText(
                    if (autoUpdate) EmbeddedBundle.message(
                        "disable.refresh.on.step",
                        arrayOfNulls<Any>(0)
                    ) else EmbeddedBundle.message("enable.refresh.on.step", arrayOfNulls<Any>(0))
                )
                myTreeTable.setAutoUpdate(autoUpdate)
            }
        })
        group.add(object : DumbAwareAction(
            EmbeddedBundle.message(
                "svd.panel.refresh",
                arrayOfNulls<Any>(0)
            ), null as String?, AllIcons.Actions.Refresh
        ) {
            override fun actionPerformed(e: AnActionEvent) {
                if (e == null) {
                    `$$$reportNull$$$0`(0)
                }
                updateValues(true)
            }

            override fun update(e: AnActionEvent) {
                if (e == null) {
                    `$$$reportNull$$$0`(1)
                }
                e.presentation.isEnabled = myProcess.session.isSuspended
            }
        })
        group.addSeparator()
        group.add(object : DumbAwareAction(
            EmbeddedBundle.message(
                "svd.panel.export.csv.to.clipboard",
                arrayOfNulls<Any>(0)
            ), null as String?, AllIcons.Actions.Copy
        ) {
            override fun actionPerformed(e: AnActionEvent) {
                if (e == null) {
                    `$$$reportNull$$$0`(0)
                }
                try {
                    val writer = StringWriter()
                    var var3: Throwable? = null
                    try {
                        val printWriter = PrintWriter(writer)
                        var var5: Throwable? = null
                        try {
                            myTreeTable.getRoot().export(printWriter)
                            CopyPasteManager.getInstance()
                                .setContents(StringSelection(writer.toString()))
                        } catch (var30: Throwable) {
                            var5 = var30
                            throw var30
                        } finally {
                            if (printWriter != null) {
                                if (var5 != null) {
                                    try {
                                        printWriter.close()
                                    } catch (var29: Throwable) {
                                        var5.addSuppressed(var29)
                                    }
                                } else {
                                    printWriter.close()
                                }
                            }
                        }
                    } catch (var32: Throwable) {
                        var3 = var32
                        throw var32
                    } finally {
                        if (writer != null) {
                            if (var3 != null) {
                                try {
                                    writer.close()
                                } catch (var28: Throwable) {
                                    var3.addSuppressed(var28)
                                }
                            } else {
                                writer.close()
                            }
                        }
                    }
                } catch (var34: IOException) {
                    throw RuntimeException(var34)
                }
            }
        })
        group.add(object : DumbAwareAction(
            EmbeddedBundle.message(
                "svd.panel.open.csv.editor",
                arrayOfNulls<Any>(0)
            ), null as String?, AllIcons.ToolbarDecorator.Export
        ) {
            override fun actionPerformed(e: AnActionEvent) {
                if (e == null) {
                    `$$$reportNull$$$0`(0)
                }
                val profile =
                    myProcess.session.runProfile
                var filename = String.format(
                    "Peripheral-%s%tF-%2\$tT",
                    if (profile != null) profile.name + "-" else "",
                    Date()
                )
                filename = FileUtil.sanitizeFileName(filename, false) + ".csv"
                val project = myProcess.project
                try {
                    val writer: Writer = StringWriter()
                    var var6: Throwable? = null
                    try {
                        val printWriter = PrintWriter(writer)
                        var var8: Throwable? = null
                        try {
                            myTreeTable.getRoot().export(printWriter)
                            val scratchFile =
                                ScratchRootType.getInstance().createScratchFile(
                                    project,
                                    filename,
                                    PlainTextLanguage.INSTANCE,
                                    writer.toString()
                                )
                            if (scratchFile != null) {
                                val descriptor =
                                    OpenFileDescriptor(project, scratchFile)
                                descriptor.navigate(true)
                            }
                        } catch (var34: Throwable) {
                            var8 = var34
                            throw var34
                        } finally {
                            if (printWriter != null) {
                                if (var8 != null) {
                                    try {
                                        printWriter.close()
                                    } catch (var33: Throwable) {
                                        var8.addSuppressed(var33)
                                    }
                                } else {
                                    printWriter.close()
                                }
                            }
                        }
                    } catch (var36: Throwable) {
                        var6 = var36
                        throw var36
                    } finally {
                        if (writer != null) {
                            if (var6 != null) {
                                try {
                                    writer.close()
                                } catch (var32: Throwable) {
                                    var6.addSuppressed(var32)
                                }
                            } else {
                                writer.close()
                            }
                        }
                    }
                } catch (var38: IOException) {
                    throw RuntimeException(var38)
                }
            }
        })
        group.addSeparator()
        group.add(object : DumbAwareAction(
            EmbeddedBundle.message(
                "svd.panel.configure.action",
                arrayOfNulls<Any>(0)
            ), null as String?, AllIcons.General.Filter
        ) {
            override fun actionPerformed(e: AnActionEvent) {
                if (e == null) {
                    `$$$reportNull$$$0`(0)
                }
                configureSvd()
            }
        })
        group.add(ContextHelpAction("toolwindows.peripheralview"))
    }

    private fun configureSvd() {
        val root: SvdRoot = myTreeTable.getRoot()
        val tree: TreeTableTree = myTreeTable.getTree()
        SvdShowHideDialog(root, myProcess.project).showHideNodes()
        root.getTreeTableModel().notifyTreeUpdated()
        (TreeUtil.treePathTraverser(tree)
            .filter { path: TreePath -> path.lastPathComponent !is SvdRegister } as JBTreeTraverser<*>).forEach(
            Consumer<*> { treePath: * -> tree.expandPath(treePath) }
        )
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
            val var10000: SvdTreeTableModel = myTreeTable.getRoot().getTreeTableModel()
            val var10002: SvdTreeTable = myTreeTable
            var10000.updateValues(myProcess, var10002::repaint)
        } finally {
            myPendingUpdateValue = false
        }
    }

    fun cancelUpdates() {
        myTreeTable.getRoot().getTreeTableModel().cancelUpdates()
    }

    companion object {
        const val HELP_ID = "toolwindows.peripheralview"
        fun create(process: CidrDebugProcess): SvdPanel {
            if (process == null) {
                `$$$reportNull$$$0`(5)
            }
            val svdWindowProfileState: com.jetbrains.cidr.cpp.execution.debugger.embedded.svd.SvdWindowState.ProjectState =
                SvdWindowState.get(process)
            val treeTable: SvdTreeTable = SvdTreeTable.create(svdWindowProfileState)
            val scrollPane: JComponent = ScrollPaneFactory.createScrollPane(treeTable, 22, 30)
            return SvdPanel(
                scrollPane,
                DefaultActionGroup(),
                treeTable,
                process,
                svdWindowProfileState
            )
        }

        fun registerPeripheralTab(
            process: CidrDebugProcess,
            ui: RunnerLayoutUi
        ) {
            if (process == null) {
                `$$$reportNull$$$0`(6)
            }
            if (ui == null) {
                `$$$reportNull$$$0`(7)
            }
            val svdPanel = create(process)
            val content = ui.createContent(
                "svdView",
                svdPanel,
                EmbeddedBundle.message("svd.panel.peripherals", arrayOfNulls<Any>(0)),
                CLionEmbeddedIcons.CustomGdbRunConfiguration,
                svdPanel.defaultFocusComponent
            )
            content.isCloseable = false
            content.setHelpId("toolwindows.peripheralview")
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
    }

    init {
        if (innerPanel == null) {
            `$$$reportNull$$$0`(0)
        }
        if (group == null) {
            `$$$reportNull$$$0`(1)
        }
        if (treeTable == null) {
            `$$$reportNull$$$0`(2)
        }
        if (process == null) {
            `$$$reportNull$$$0`(3)
        }
        super(BorderLayout())
        autoUpdate = true
        val actionToolbar =
            ActionManager.getInstance()
                .createActionToolbar("DebuggerToolbar", group, false)
        val component = actionToolbar.component
        this.add(component, "West")
        this.add(innerPanel, "Center")
        myInnerPanel = innerPanel
        myTreeTable = treeTable
        myProcess = process
        setupActions(group)
        val emptyText: StatusText = treeTable.getEmptyText()
        emptyText.setText(EmbeddedBundle.message("svd.no.registers.loaded", arrayOfNulls<Any>(0)))
        emptyText.appendSecondaryText(
            EmbeddedBundle.message("svd.load.file", arrayOfNulls<Any>(0)),
            SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES
        ) { e: ActionEvent? -> configureSvd() }
        myWindowProfileState = profileState
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
        addHierarchyListener { e: HierarchyEvent? ->
            if (this.isDisplayable && myPendingUpdateValue) {
                doUpdateValues()
                EmbeddedUsagesCollector.triggerPeripheralViewOpen(
                    process.project,
                    myTreeTable.getRoot().getActiveNodes().size()
                )
            }
        }
    }
}
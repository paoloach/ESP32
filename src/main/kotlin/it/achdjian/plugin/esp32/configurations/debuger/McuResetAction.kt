package it.achdjian.plugin.esp32.configurations.debuger

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.jetbrains.cidr.execution.debugger.CidrDebugProcess
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriver
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriver.TargetState
import com.jetbrains.cidr.execution.debugger.backend.gdb.GDBDriver
import it.achdjian.plugin.esp32.ICON_RESET

class McuResetAction(private val debugProcess: ()->CidrDebugProcess, private val resetCommand: String) :
    AnAction("Reset", "Reset", ICON_RESET), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        val process = debugProcess()
        process.postCommand { drv: DebuggerDriver ->
            (drv as GDBDriver).interruptAndExecuteConsole(resetCommand)
            process.postCommand { driver: DebuggerDriver ->
                if (driver.state == TargetState.SUSPENDED) {
                    driver.stepInto(true, true)
                }
            }
        }
    }

}
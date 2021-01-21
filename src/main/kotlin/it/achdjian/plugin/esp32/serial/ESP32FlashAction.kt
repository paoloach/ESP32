package it.achdjian.plugin.esp32.serial

import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManagerEx
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import it.achdjian.plugin.esp32.ICON_SERIAL_FLASH
import it.achdjian.plugin.esp32.configurations.flash.ESP32FlashConfigurationType

class ESP32FlashAction(val project: Project) : ToggleAction("Flash", "Flash the ESP32", ICON_SERIAL_FLASH), DumbAware {
    override fun isSelected(p0: AnActionEvent): Boolean {
        return false
    }

    override fun setSelected(p0: AnActionEvent, p1: Boolean) {
        val serialService = ServiceManager.getService(project, ESP32SerialService::class.java)
        val runManager = RunManagerEx.getInstanceEx(project) as RunManagerImpl
        runManager
            .allSettings
            .firstOrNull { it.type is ESP32FlashConfigurationType }
            ?.let {
                serialService.close()

                val executor = DefaultRunExecutor.getRunExecutorInstance()
                val runner = ProgramRunner.getRunner(executor.id, it.configuration) as ProgramRunner

                val env = ExecutionEnvironment(executor, runner, it, project)
                runner.execute(env)
            }
    }
}
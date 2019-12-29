package it.achdjian.plugin.esp32.configurations.flash

import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.jetbrains.cidr.cpp.cmake.CMakeSettings
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace

class ESP32FlashLauncher(
    executeEnvironment: ExecutionEnvironment,
    private val ESP32FlashConfigurationState: ESP32FlashConfigurationState
) : CommandLineState(executeEnvironment){

    override fun startProcess(): ProcessHandler {
        val cmdLine = GeneralCommandLine("make")
        cmdLine.addParameter("flash")

        val cMakeWorkspace = CMakeWorkspace.getInstance(environment.project)
        cmdLine.withWorkDirectory(cMakeWorkspace.projectDir)

        CMakeWorkspace.getInstance(environment.project).modelTargets.first{it.name=="flash"}?.buildConfigurations?.first()?.let {
            val profileInfo = cMakeWorkspace.getProfileInfoFor(it)
            cmdLine.withWorkDirectory( profileInfo.generationDir )
        }

        CMakeSettings.getInstance(environment.project).profiles.firstOrNull()?.let {
            cmdLine.withEnvironment(it.additionalEnvironment)
        }

        ESP32FlashConfigurationState.port.let { cmdLine.withEnvironment("ESPPORT",  "/dev/$it")}
        ESP32FlashConfigurationState.baud.let { cmdLine.withEnvironment("ESPBAUD", it.toString())}
        return KillableColoredProcessHandler(cmdLine)
    }

}
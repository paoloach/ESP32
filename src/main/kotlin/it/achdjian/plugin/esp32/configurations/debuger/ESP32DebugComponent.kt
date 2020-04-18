package it.achdjian.plugin.esp32.configurations.debuger

import com.intellij.execution.ExecutionException
import com.intellij.execution.RunContentExecutor
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PtyCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.concurrency.FutureResult
import it.achdjian.plugin.esp32.configurations.debuger.openocd.OpenOCDDownloadFollower
import it.achdjian.plugin.esp32.configurations.debuger.openocd.OpenOCDErrorFilter
import it.achdjian.plugin.esp32.setting.ESP32SettingState
import java.awt.EventQueue
import java.io.File
import java.util.concurrent.Future

private fun Project.requireAndReport(fileToCheck: File?): File {
    return try {
        require(fileToCheck)
    } catch (e: ConfigurationException) {
        showErrorMessage(this, e)
        throw ProcessCanceledException()
    }
}

private fun require(fileToCheck: File?): File {
    return if (fileToCheck != null && fileToCheck.exists()) {
        fileToCheck
    } else {
        throw ConfigurationException("Set openocd location", "OpenOCD")
    }
}




fun createOpenOcdDownloadingCommandLine(config: ESP32DebugConfiguration, fileToLoad: File, shutdown:Boolean=true): GeneralCommandLine {
    return if (StringUtil.isEmpty(config.boardConfigFile)) {
        showErrorMessage(config.project, "OpenOCD", "OpenOCD board config not defined")
        throw ProcessCanceledException()
    } else {
        val openOcdBinary = config.project.requireAndReport( File(ESP32SettingState.esp32OpenOcdLocation))
        val ocdScripts = config.project.requireAndReport( findOcdScripts(openOcdBinary))
        val commandLine = PtyCommandLine().withWorkDirectory(openOcdBinary.parentFile)
                .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
                .withExePath(openOcdBinary.absolutePath)
                .withParameters("-s", ocdScripts.absolutePath,
                    "-f", config.boardConfigFile,
                    "-c", "program_esp ${fileToLoad.canonicalPath} 0x10000 verify reset exit")
        if (shutdown){
            commandLine.addParameters("-c", "shutdown")
        }
        return commandLine
    }
}

fun createOpenOcdCommandLine(config: ESP32DebugConfiguration): GeneralCommandLine {
    return if (StringUtil.isEmpty(config.boardConfigFile)) {
        showErrorMessage(config.project, "OpenOCD", "OpenOCD board config not defined")
        throw ProcessCanceledException()
    } else {
        val openOcdBinary = config.project.requireAndReport( File(ESP32SettingState.esp32OpenOcdLocation))
        val ocdScripts = config.project.requireAndReport( findOcdScripts(openOcdBinary))
        PtyCommandLine().withWorkDirectory(openOcdBinary.parentFile)
                .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
                .withParameters("-c", "tcl_port disabled")
                .withExePath(openOcdBinary.absolutePath)
                .withParameters("-s", ocdScripts.absolutePath,
                    "-f", config.boardConfigFile,
                    "-c", "gdb_port " + config.gdbPort,
                    "-c", "telnet_port " + config.telnetPort,
                    "-c", "init;reset init;")
    }
}


object ESP32OpenOcdComponent {
    private val LOG: Logger = Logger.getInstance(ESP32OpenOcdComponent::class.java)
     private var openOcdProcess: OSProcessHandler? = null

    fun stopOpenOcd() {
        openOcdProcess?.let {
            if (!it.isProcessTerminated && !it.isProcessTerminating) {
                if (EventQueue.isDispatchThread()) {
                    ProgressManager.getInstance().runProcessWithProgressSynchronously(
                        { doStopOpenOcd() },
                        "openocd stopping",
                        false,
                        null as Project?
                    )
                } else {
                    doStopOpenOcd()
                }
            }
        }
    }

    private fun doStopOpenOcd() {
        openOcdProcess?.let {
            it.destroyProcess()
            it.waitFor(2000L)
            openOcdProcess=null
        }
    }

    fun runOpenOcdServer(config: ESP32DebugConfiguration) {
        openOcdProcess?.let {
            if (!it.isProcessTerminated) {
                return
            }
        }
        val commandLine = createOpenOcdCommandLine(config)
        LOG.info("Start openOCD")
        val process = object : OSProcessHandler(commandLine) {
            override fun isSilentlyDestroyOnClose() = true
        }
        this.openOcdProcess = process
    }

    fun loadFirmware(esP32DebugConfiguration: ESP32DebugConfiguration, fileToLoad: File): Future<DownloadingStatus> {
        openOcdProcess?.let {
            if (!it.isProcessTerminated) {
                return FutureResult(DownloadingStatus.FLASH_ERROR)
            }
        }
        val commandLine = createOpenOcdDownloadingCommandLine(esP32DebugConfiguration, fileToLoad)

        try {
            val process = object : OSProcessHandler(commandLine) {
                override fun isSilentlyDestroyOnClose() = true
            }
            val downloadFollower = OpenOCDDownloadFollower(fileToLoad)
            process.addProcessListener(downloadFollower)
            val openOCDConsole = RunContentExecutor(esP32DebugConfiguration.project, process)
                .withTitle("OpenOCD console")
                .withActivateToolWindow(true)
                .withFilter(OpenOCDErrorFilter(esP32DebugConfiguration.project))
                .withStop({ process.destroyProcess() })
                { !process.isProcessTerminated && !process.isProcessTerminating }
            openOCDConsole.run()
            this.openOcdProcess = process
            return downloadFollower
        } catch (e: ExecutionException) {
            showErrorMessage(esP32DebugConfiguration.project, "OpenOCD", e.localizedMessage)
            return FutureResult(DownloadingStatus.FLASH_ERROR)
        }
    }

    fun waitDownloadingPrcessFinish() {
        TODO("Not yet implemented")
    }

}





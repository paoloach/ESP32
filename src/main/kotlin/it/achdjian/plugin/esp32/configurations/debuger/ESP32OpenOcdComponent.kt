package it.achdjian.plugin.esp32.configurations.debuger

import com.intellij.execution.ExecutionException
import com.intellij.execution.RunContentExecutor
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PtyCommandLine
import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.HyperlinkInfo
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.concurrency.FutureResult
import com.jetbrains.cidr.cpp.execution.gdbserver.Utils
import java.awt.EventQueue
import java.io.File
import java.util.concurrent.Future


//val SCRIPTS_PATH_SHORT = "scripts"
//val SCRIPTS_PATH_LONG = "share/openocd/scripts"
//private val ERROR_PREFIX = "Error: "
val IGNORED_STRINGS = arrayOf("clearing lockup after double fault", "LIB_USB_NOT_SUPPORTED")
val FAIL_STRINGS = arrayOf("** Programming Failed **", "communication failure", "** OpenOCD init failed **")
//private val FLASH_SUCCESS_TEXT = "** Programming Finished **"
//private val ADAPTER_SPEED = "adapter speed"
private val MY_COLOR_SCHEME = EditorColorsManager.getInstance().globalScheme

private fun containsOneOf(text: String, sampleStrings: Array<String>) = sampleStrings.any { text.contains(it) }

private fun findOcdScripts(openOcdLocation: File): File {
    val binFolder = openOcdLocation.parentFile
    val homeFolder = binFolder?.parentFile ?: openOcdLocation
    val ocdScripts = File(homeFolder, "share/openocd/scripts")
    if (!ocdScripts.exists()) {
        return File(homeFolder, "scripts")
    }
    return ocdScripts
}

private fun require(fileToCheck: File?): File {
    return if (fileToCheck != null && fileToCheck.exists()) {
        fileToCheck
    } else {
        throw ConfigurationException("Set openocd location","OpenOCD")
    }
}


fun createOpenOcdCommandLine(config: ESP32DebugConfiguration, fileToLoad: File?, additionalCommand: String, shutdown: Boolean): GeneralCommandLine {
    val ocdSettings = ApplicationManager.getApplication().getComponent(ESP32DebugConfigurationState::class.java)
    val project: Project = config.project
    return if (StringUtil.isEmpty(config.boardConfigFile)) {
        showErrorMessage(project, "OpenOCD", "OpenOCD board config not defined")
        throw ProcessCanceledException()
    } else {
        val openOcdBinary = requireAndReport(project, File(ocdSettings.openOcdLocation))
        val ocdScripts = requireAndReport(project, findOcdScripts(openOcdBinary))
        val commandLine =
            PtyCommandLine().withWorkDirectory(openOcdBinary.parentFile)
                .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
                .withParameters("-c", "tcl_port disabled")
                .withExePath(openOcdBinary.absolutePath)
        commandLine.addParameters("-s", ocdScripts.absolutePath)
        commandLine.addParameters("-c", "gdb_port " + config.gdbPort)
        commandLine.addParameters("-c", "telnet_port " + config.telnetPort)
        commandLine.addParameters("-f", config.boardConfigFile)
        fileToLoad?.let {
            val command = "program \"" + it.absolutePath.replace(File.separatorChar, '/') + "\""
            commandLine.addParameters("-c", command)
        }
        if (additionalCommand != null && additionalCommand.isNotEmpty()) {
            commandLine.addParameters("-c", additionalCommand)
        }
        if (shutdown) {
            commandLine.addParameters("-c", "shutdown")
        }
        commandLine
    }
}


private fun requireAndReport(project: Project, fileToCheck: File?): File {
    return try {
        require(fileToCheck)
    } catch (e: ConfigurationException) {
        showErrorMessage(project, e)
        throw ProcessCanceledException()
    }
}

class ESP32OpenOcdComponent {
    companion object {
        private val LOG: Logger =  Logger.getInstance(ESP32OpenOcdComponent::class.java)

    }
    private var process: OSProcessHandler? = null

    fun stopOpenOcd() {
        process?.let {
            if (!it.isProcessTerminated && !it.isProcessTerminating){
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
        process?.let {
            it.destroyProcess()
            it.waitFor(2000L)
        }
    }

    fun startOpenOcd(esP32DebugConfiguration: ESP32DebugConfiguration?, fileToLoad: File?, additionalCommand: String): Future<STATUS> {
        return esP32DebugConfiguration?.let {config->
            val commandLine = createOpenOcdCommandLine(config, fileToLoad, additionalCommand, false)

            if (process != null && !process!!.isProcessTerminated) {
                LOG.info("OpenOCD is already run")
                FutureResult(STATUS.FLASH_ERROR)
            } else {
                val virtualFile = fileToLoad?.let { VfsUtil.findFileByIoFile(it,true) }
                val project = esP32DebugConfiguration.project
                try {
                    val process = object : OSProcessHandler(commandLine) {
                        override fun isSilentlyDestroyOnClose()=true
                    }
                    val downloadFollower = DownloadFollower(virtualFile)
                    process.addProcessListener(downloadFollower)
                    val openOCDConsole = RunContentExecutor(project, process)
                        .withTitle("OpenOCD console")
                        .withActivateToolWindow(true)
                        .withFilter(ErrorFilter( project))
                        .withStop({ process.destroyProcess() })
                        { !process.isProcessTerminated && !process.isProcessTerminating }
                    openOCDConsole.run()
                    this.process=process
                    downloadFollower
                } catch (e: ExecutionException) {
                    showErrorMessage(project, "OpenOCD", e.localizedMessage)
                    FutureResult(STATUS.FLASH_ERROR)
                }
            }
        } ?:  FutureResult(STATUS.FLASH_ERROR)
    }

    enum class STATUS {
        FLASH_SUCCESS, FLASH_WARNING, FLASH_ERROR
    }
}

private class ErrorFilter ( val project: Project) : Filter {
    override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
        return if (containsOneOf(line, FAIL_STRINGS)) {
            showFailedDownloadNotification(project)
            object : Filter.Result(0, line.length, null as HyperlinkInfo?, MY_COLOR_SCHEME.getAttributes(ConsoleViewContentType.ERROR_OUTPUT_KEY)) {
                override fun getHighlighterLayer(): Int {
                    return 5000
                }
            }
        } else {
            if (line.contains("** Programming Finished **")) {
                showSuccessfulDownloadNotification(project)
            }
            null
        }
    }

}




private class DownloadFollower(private val vRunFile: VirtualFile?) : FutureResult<ESP32OpenOcdComponent.STATUS>(), ProcessListener {

    override fun startNotified(event: ProcessEvent) {
    }

    override fun processTerminated(event: ProcessEvent) {
        try {
            if (!this.isDone) {
                this.set(ESP32OpenOcdComponent.STATUS.FLASH_ERROR)
            }
        } catch (var3: Exception) {
            this.set(ESP32OpenOcdComponent.STATUS.FLASH_ERROR)
        }
    }

    override fun processWillTerminate(event: ProcessEvent, willBeDestroyed: Boolean) {
    }

    override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        val text = event.text.trim { it <= ' ' }
        if (containsOneOf(text,  FAIL_STRINGS)) {
            reset()
            set(ESP32OpenOcdComponent.STATUS.FLASH_ERROR)
        } else if (vRunFile == null && text.startsWith("adapter speed")) {
            reset()
            set(ESP32OpenOcdComponent.STATUS.FLASH_SUCCESS)
        } else if (text == "** Programming Finished **") {
            reset()
            vRunFile?.let {
                Utils.markDownloaded(vRunFile)
            }
            set(ESP32OpenOcdComponent.STATUS.FLASH_SUCCESS)
        } else if (containsOneOf(text, IGNORED_STRINGS)) {
            reset()
            set(ESP32OpenOcdComponent.STATUS.FLASH_WARNING)
        }
    }
}
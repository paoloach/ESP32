package it.achdjian.plugin.esp32.configurations.debuger

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.filters.ConsoleFilterProvider
import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.TextConsoleBuilder
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.ui.ExecutionConsole
import com.intellij.execution.ui.RunnerLayoutUi
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.ui.XDebugTabLayouter
import com.jetbrains.cidr.cpp.execution.debugger.backend.CLionGDBDriverConfiguration
import com.jetbrains.cidr.cpp.execution.gdbserver.DownloadType
import com.jetbrains.cidr.cpp.execution.gdbserver.Utils
import com.jetbrains.cidr.cpp.execution.remote.RemoteGDBLauncher
import com.jetbrains.cidr.cpp.execution.remote.getRemoteRunToolchainProblem
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains
import com.jetbrains.cidr.execution.debugger.CidrDebugProcess
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriver
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriverConfiguration
import com.jetbrains.cidr.execution.debugger.backend.gdb.GDBDriver
import com.jetbrains.cidr.execution.debugger.remote.CidrRemoteDebugParameters
import com.jetbrains.cidr.execution.debugger.remote.CidrRemoteGDBDebugProcess
import it.achdjian.plugin.esp32.configurations.debuger.ui.registerPeripheralTab
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException


fun Project.findOpenOcdAction(): ESP32OpenOcdComponent = getComponent(ESP32OpenOcdComponent::class.java)


class ESP32DebugProcessAdapter(val terminated:(event:ProcessEvent)->Unit) : ProcessAdapter() {
    override fun processTerminated(event:ProcessEvent){
        super.processTerminated(event)
        terminated(event)
    }
}

class ESP32XDebugTabLayouter(val process:ESP32DebugCidrRemoteGDBDebugProcess, private val innerLayouter: XDebugTabLayouter) : XDebugTabLayouter(){
    override fun  registerConsoleContent(ui: RunnerLayoutUi, console: ExecutionConsole) = innerLayouter.registerConsoleContent(ui, console)

    override fun registerAdditionalContent(  ui:RunnerLayoutUi) {
        innerLayouter.registerAdditionalContent(ui)
        registerPeripheralTab(process, ui)
    }
}

class ESP32DebugCidrRemoteGDBDebugProcess(driverConfiguration: DebuggerDriverConfiguration, parameters: CidrRemoteDebugParameters, session: XDebugSession, consoleBuilder: TextConsoleBuilder):
    CidrRemoteGDBDebugProcess(driverConfiguration, parameters, session, consoleBuilder, ConsoleFilterProvider{Filter.EMPTY_ARRAY;}) {

    override fun createTabLayouter(): XDebugTabLayouter {
        val innerLayouter = super.createTabLayouter()
        return ESP32XDebugTabLayouter(this, innerLayouter)
    }

    override fun doDisconnectTarget(inferior: DebuggerDriver.Inferior, shouldDestroy:Boolean) {
        try {
            (inferior.driver as GDBDriver).interruptAndExecuteConsole("monitor shutdown")
        } finally {
            super.doDisconnectTarget(inferior, shouldDestroy)
        }

    }
}



class ESP32DebugLauncher(project: Project, myDebuggerToolchain: CPPToolchains.Toolchain, private val myOpenOcdConfiguration: ESP32DebugConfiguration) :
    RemoteGDBLauncher(project, myDebuggerToolchain, CidrRemoteDebugParameters()){

    companion object {
        val RESTART_KEY = Key.create<AnAction>(ESP32DebugLauncher::class.java.name + "#restartAction")

    }

    override fun createProcess(state: CommandLineState) : ProcessHandler {
        val runFile = findRunFile(state)
        state.environment.project.findOpenOcdAction().stopOpenOcd()
        val commandLine = createOpenOcdCommandLine(myOpenOcdConfiguration, runFile, "reset", true)
        val osProcessHandler = OSProcessHandler(commandLine)
        osProcessHandler.addProcessListener(ESP32DebugProcessAdapter {
                if (it.exitCode == 0) {
                    showSuccessfulDownloadNotification(project)
                } else {
                    showFailedDownloadNotification(project)
                }
            }
        )

        return osProcessHandler
    }

    override fun createDebugProcess(state:CommandLineState, session: XDebugSession) : CidrDebugProcess {

        parameters.remoteCommand = "tcp:localhost:" + myOpenOcdConfiguration.gdbPort
        parameters.symbolFile = findRunFile(state).absolutePath
        val errorMessage = debuggerToolchain.getRemoteRunToolchainProblem(false)
        if (errorMessage != null) {
            throw ExecutionException(errorMessage)
        } else {
            val configuration = CLionGDBDriverConfiguration(project, debuggerToolchain)
            val debugProcess = ESP32DebugCidrRemoteGDBDebugProcess(configuration, parameters, session, state.consoleBuilder)
            configProcessHandler(debugProcess.processHandler, debugProcess.isDetachDefault, false, project)
            debugProcess.processHandler.addProcessListener(object: ProcessAdapter() {
                override fun processWillTerminate( event:ProcessEvent,  willBeDestroyed:Boolean) {
                    super.processWillTerminate(event, willBeDestroyed)
                    project.findOpenOcdAction().stopOpenOcd()
                }
            })
            debugProcess.processHandler.putUserData(RESTART_KEY,  McuResetAction( {debugProcess}, "monitor reset halt"))
            return debugProcess
        }
    }

    private fun findRunFile(commandLineState: CommandLineState ): File {

        val targetProfileName = commandLineState.executionTarget.displayName
        myOpenOcdConfiguration.getBuildAndRunConfigurations(targetProfileName)?.let {runConfigurations->
            runConfigurations.runFile?.let {
                if (it.exists() && it.isHidden)
                    return it
                else
                    throw ExecutionException("Invalid run file")
            }
        } ?: throw ExecutionException("Openocd target not defined")
    }

    override fun  startDebugProcess(commandLineState: CommandLineState, xDebugSession:XDebugSession): CidrDebugProcess {
        var runFile: File? = null
        if (myOpenOcdConfiguration.downloadType != DownloadType.NONE) {
            runFile = findRunFile(commandLineState)
            if (myOpenOcdConfiguration.downloadType == DownloadType.UPDATED_ONLY && Utils.isLatestUploaded(runFile)) {
                runFile = null
            }
        }

        xDebugSession.stop()
        val openOcdComponent = project.findOpenOcdAction()
        openOcdComponent.stopOpenOcd()
        val downloadResult = openOcdComponent.startOpenOcd(myOpenOcdConfiguration, runFile, myOpenOcdConfiguration.resetType.command)
        val progressManager = ProgressManager.getInstance()
        val progressTitle = runFile?.let { "Download firmware" } ?: "Start openOCD"

        val process = object : ThrowableComputable<ESP32OpenOcdComponent.STATUS, ExecutionException>{
            override fun compute(): ESP32OpenOcdComponent.STATUS {
                progressManager.progressIndicator.isIndeterminate = true
                try {
                    while (true) {
                        try {
                            return downloadResult.get(500L, TimeUnit.MILLISECONDS)
                        } catch (e: TimeoutException) {
                            ProgressManager.checkCanceled()
                        }
                    }
                } catch (e: java.util.concurrent.ExecutionException){
                    throw ExecutionException(e)
                }
            }

        }
        val downloadStatus=progressManager.runProcessWithProgressSynchronously(process, progressTitle, true, project)

        if (downloadStatus == ESP32OpenOcdComponent.STATUS.FLASH_ERROR) {
            downloadResult.cancel(true)
            showErrorMessage(project, "OpenOCD", "MCU Communicatoin Failure")
            throw ProcessCanceledException()
        } else {
            return super.startDebugProcess(commandLineState, xDebugSession)
        }
    }

    override fun collectAdditionalActions(state:CommandLineState, processHandler:ProcessHandler, executionConsole:ExecutionConsole, list:MutableList<in AnAction>) {
        super.collectAdditionalActions(state, processHandler, executionConsole, list)
        processHandler.getUserData(RESTART_KEY)?.let { list.add(it) }
    }


}
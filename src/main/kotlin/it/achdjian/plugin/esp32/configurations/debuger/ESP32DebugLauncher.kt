package it.achdjian.plugin.esp32.configurations.debuger

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.filters.ConsoleFilterProvider
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.ui.ExecutionConsole
import com.intellij.execution.ui.RunnerLayoutUi
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.ui.XDebugTabLayouter
import com.jetbrains.cidr.cpp.cmake.console.CMakeConsoleBuilder
import com.jetbrains.cidr.cpp.execution.debugger.backend.CLionGDBDriverConfiguration
import com.jetbrains.cidr.cpp.execution.remote.RemoteGDBLauncher
import com.jetbrains.cidr.cpp.execution.remote.getRemoteRunToolchainProblem
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains
import com.jetbrains.cidr.execution.debugger.CidrDebugProcess
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriverConfiguration
import com.jetbrains.cidr.execution.debugger.remote.CidrRemoteDebugParameters
import com.jetbrains.cidr.execution.debugger.remote.CidrRemoteGDBDebugProcess
import it.achdjian.plugin.esp32.configurations.debuger.ui.SvdPanel
import java.io.File


fun Project.findOpenOcdAction()  = getComponent(ESP32OpenOcdComponent::class.java)


class ESP32DebugProcessAdapter(val terminated:(event:ProcessEvent)->Unit) : ProcessAdapter() {
    override fun processTerminated(event:ProcessEvent){
        super.processTerminated(event)
        terminated(event)
    }
}

class ESP32DebugCidrRemoteGDBDebugProcess(driverConfiguration: DebuggerDriverConfiguration, parameters: CidrRemoteDebugParameters, session: XDebugSession, consoleBuilder: CMakeConsoleBuilder, consoleFilterProvider: ConsoleFilterProvider):
    CidrRemoteGDBDebugProcess(driverConfiguration, parameters, session, consoleBuilder, consoleFilterProvider) {
    override fun createTabLayouter(): XDebugTabLayouter {
        val innerLayouter = super.createTabLayouter();
        return object :XDebugTabLayouter() {
            override fun  registerConsoleContent(ui: RunnerLayoutUi, console: ExecutionConsole) = innerLayouter.registerConsoleContent(ui, console)

            override fun registerAdditionalContent(  ui:RunnerLayoutUi) {
                innerLayouter.registerAdditionalContent(ui);
                SvdPanel.registerPeripheralTab(<VAR_NAMELESS_ENCLOSURE>, ui);
            }
        };
    }

    protected void doDisconnectTarget(@NotNull Inferior inferior, boolean shouldDestroy) throws ExecutionException, DebuggerCommandException {
        if (inferior == null) {
            $$$reportNull$$$0(0);
        }

        try {
            ((GDBDriver)inferior.getDriver()).interruptAndExecuteConsole("monitor shutdown");
        } finally {
            super.doDisconnectTarget(inferior, shouldDestroy);
        }

    }
}

class ESP32DebugLauncher(project: Project, myDebuggerToolchain: CPPToolchains.Toolchain, val myOpenOcdConfiguration: ESP32DebugConfiguration) :
    RemoteGDBLauncher(project, myDebuggerToolchain, CidrRemoteDebugParameters()){

    companion object {
        val RESTART_KEY = Key.create<AnAction>(ESP32DebugLauncher::class.java.getName() + "#restartAction")

    }

    override fun createProcess(commandLineState: CommandLineState) : ProcessHandler {
        val runFile = findRunFile(commandLineState);
        commandLineState.getEnvironment().getProject().findOpenOcdAction().stopOpenOcd();
        val commandLine = createOpenOcdCommandLine(myOpenOcdConfiguration, runFile, "reset", true)
        val osProcessHandler = OSProcessHandler(commandLine);
        osProcessHandler.addProcessListener(ESP32DebugProcessAdapter {
                if (it.exitCode == 0) {
                    showSuccessfulDownloadNotification(project);
                } else {
                    showFailedDownloadNotification(project);
                }
            }
        )

        return osProcessHandler;
    }

    protected fun createDebugProcess( commandLineState:CommandLineState,   xDebugSession: XDebugSession) : CidrDebugProcess {

        parameters.remoteCommand = "tcp:localhost:" + myOpenOcdConfiguration.gdbPort
        parameters.symbolFile = findRunFile(commandLineState).absolutePath
        val errorMessage = debuggerToolchain.getRemoteRunToolchainProblem(false)
        if (errorMessage != null) {
            throw ExecutionException(errorMessage);
        } else {
            val configuration = CLionGDBDriverConfiguration(this.getProject(), debuggerToolchain);
            val debugProcess = CidrRemoteGDBDebugProcess(configuration, parameters, xDebugSession, commandLineState.getConsoleBuilder(),  {
                Filter.EMPTY_ARRAY;
            }) {
                public XDebugTabLayouter createTabLayouter() {
                    final XDebugTabLayouter innerLayouter = super.createTabLayouter();
                    return new XDebugTabLayouter() {
                        @NotNull
                        public Content registerConsoleContent(@NotNull RunnerLayoutUi ui, @NotNull ExecutionConsole console) {
                            if (ui == null) {
                                $$$reportNull$$$0(0);
                            }

                            if (console == null) {
                                $$$reportNull$$$0(1);
                            }

                            Content var10000 = innerLayouter.registerConsoleContent(ui, console);
                            if (var10000 == null) {
                                $$$reportNull$$$0(2);
                            }

                            return var10000;
                        }

                        public void registerAdditionalContent(@NotNull RunnerLayoutUi ui) {
                            if (ui == null) {
                                $$$reportNull$$$0(3);
                            }

                            innerLayouter.registerAdditionalContent(ui);
                            SvdPanel.registerPeripheralTab(<VAR_NAMELESS_ENCLOSURE>, ui);
                        }
                    };
                }

                protected void doDisconnectTarget(@NotNull Inferior inferior, boolean shouldDestroy) throws ExecutionException, DebuggerCommandException {
                    if (inferior == null) {
                        $$$reportNull$$$0(0);
                    }

                    try {
                        ((GDBDriver)inferior.getDriver()).interruptAndExecuteConsole("monitor shutdown");
                    } finally {
                        super.doDisconnectTarget(inferior, shouldDestroy);
                    }

                }
            };
            configProcessHandler(debugProcess.getProcessHandler(), debugProcess.isDetachDefault(), false, this.getProject());
            debugProcess.getProcessHandler().addProcessListener(new ProcessAdapter() {
                public void processWillTerminate(@NotNull ProcessEvent event, boolean willBeDestroyed) {
                    if (event == null) {
                        $$$reportNull$$$0(0);
                    }

                    super.processWillTerminate(event, willBeDestroyed);
                    OpenOcdLauncher.findOpenOcdAction(OpenOcdLauncher.this.getProject()).stopOpenOcd();
                }
            });
            debugProcess.getProcessHandler().putUserData(RESTART_KEY, new McuResetAction(() -> {
                return debugProcess;
            }, "monitor reset halt"));
            if (debugProcess == null) {
                $$$reportNull$$$0(7);
            }

            return debugProcess;
        }
    }

    private fun findRunFile(commandLineState: CommandLineState ): File {

        val targetProfileName = commandLineState.getExecutionTarget().getDisplayName()
        myOpenOcdConfiguration.getBuildAndRunConfigurations(targetProfileName)?.let {runConfigurations->
            val runFile = runConfigurations.getRunFile()
            runFile?.let {
                if (it.exists() && it.isHidden)
                    return it
                else
                    throw ExecutionException("Invalid run file")
            }
        } ?: throw ExecutionException("Openocd target not defined")
    }

    @NotNull
    public CidrDebugProcess startDebugProcess(@NotNull CommandLineState commandLineState, @NotNull XDebugSession xDebugSession) throws ExecutionException {
        if (commandLineState == null) {
            $$$reportNull$$$0(10);
        }

        if (xDebugSession == null) {
            $$$reportNull$$$0(11);
        }

        File runFile = null;
        if (this.myOpenOcdConfiguration.getDownloadType() != DownloadType.NONE) {
            runFile = this.findRunFile(commandLineState);
            if (this.myOpenOcdConfiguration.getDownloadType() == DownloadType.UPDATED_ONLY && Utils.isLatestUploaded(runFile)) {
                runFile = null;
            }
        }

        xDebugSession.stop();
        ESP32OpenOcdComponent openOcdComponent = findOpenOcdAction(this.getProject());
        openOcdComponent.stopOpenOcd();
        Future<STATUS> downloadResult = openOcdComponent.startOpenOcd(this.myOpenOcdConfiguration, runFile, this.myOpenOcdConfiguration.getResetType().getCommand());
        ProgressManager progressManager = ProgressManager.getInstance();
        ThrowableComputable<STATUS, ExecutionException> process = () -> {
            try {
                progressManager.getProgressIndicator().setIndeterminate(true);

                while(true) {
                    try {
                        return (STATUS)downloadResult.get(500L, TimeUnit.MILLISECONDS);
                    } catch (TimeoutException var3) {
                        ProgressManager.checkCanceled();
                    }
                }
            } catch (java.util.concurrent.ExecutionException | InterruptedException var4) {
                throw new ExecutionException(var4);
            }
        };
        String progressTitle = runFile == null ? EmbeddedBundle.message("start.openocd.title", new Object[0]) : EmbeddedBundle.message("download.firmware", new Object[0]);
        STATUS downloadStatus = (STATUS)progressManager.runProcessWithProgressSynchronously(process, progressTitle, true, this.getProject());
        if (downloadStatus == STATUS.FLASH_ERROR) {
            downloadResult.cancel(true);
            EmbeddedMessages.showErrorMessage(this.getProject(), "OpenOCD", EmbeddedBundle.message("mcu.communication.failure.title", new Object[0]));
            throw new ProcessCanceledException();
        } else {
            CidrDebugProcess var10000 = super.startDebugProcess(commandLineState, xDebugSession);
            if (var10000 == null) {
                $$$reportNull$$$0(12);
            }

            return var10000;
        }
    }

    protected void collectAdditionalActions(@NotNull CommandLineState commandLineState, @NotNull ProcessHandler processHandler, @NotNull ExecutionConsole executionConsole, @NotNull List<? super AnAction> list) throws ExecutionException {
        if (commandLineState == null) {
            $$$reportNull$$$0(13);
        }

        if (processHandler == null) {
            $$$reportNull$$$0(14);
        }

        if (executionConsole == null) {
            $$$reportNull$$$0(15);
        }

        if (list == null) {
            $$$reportNull$$$0(16);
        }

        super.collectAdditionalActions(commandLineState, processHandler, executionConsole, list);
        AnAction restart = (AnAction)processHandler.getUserData(RESTART_KEY);
        if (restart != null) {
            list.add(restart);
        }

    }


}
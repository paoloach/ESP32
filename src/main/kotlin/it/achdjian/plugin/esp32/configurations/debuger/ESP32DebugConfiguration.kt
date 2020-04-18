package it.achdjian.plugin.esp32.configurations.debuger

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RuntimeConfigurationException
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.InvalidDataException
import com.intellij.openapi.util.WriteExternalException
import com.intellij.openapi.util.text.StringUtil
import com.jetbrains.cidr.cpp.execution.CMakeAppRunConfiguration
import com.jetbrains.cidr.cpp.execution.gdbserver.DownloadType
import com.jetbrains.cidr.cpp.execution.gdbserver.Utils
import com.jetbrains.cidr.cpp.toolchains.CPPDebugger
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains
import com.jetbrains.cidr.execution.CidrCommandLineState
import com.jetbrains.cidr.execution.CidrExecutableDataHolder
import it.achdjian.plugin.esp32.ESP32_TOOLCHAIN
import it.achdjian.plugin.esp32.setting.ESP32SettingState
import org.jdom.Element
import java.io.File

class ESP32DebugConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    CMakeAppRunConfiguration(project, factory, name), CidrExecutableDataHolder {

    companion object {
        private val LOG = com.intellij.openapi.diagnostic.Logger.getInstance(ESP32DebugConfiguration::class.java)
        const val DEFAULT_GDB_PORT = 3333
        const val DEFAULT_TELNET_PORT = 4444

        private const val ATTR_GDB_PORT = "gdb-port"
        private const val ATTR_TELNET_PORT = "telnet-port"
        private const val ATTR_BOARD_CONFIG = "board-config"
        private const val ATTR_DOWNLOAD_TYPE= "download-type"
        const val ATTR_RESET_TYPE = "reset-type"
        val DEFAULT_RESET: ResetType = ResetType.INIT
        const val TAG_OPENOCD = "openocd"
    }

    var gdbPort = DEFAULT_GDB_PORT
    var telnetPort = DEFAULT_TELNET_PORT
    var boardConfigFile = ""
    var downloadType: DownloadType = DownloadType.ALWAYS
    var resetType: ResetType = DEFAULT_RESET


    override fun getState(executor: Executor, environment: ExecutionEnvironment): CidrCommandLineState {
        val toolchain = CPPToolchains.getInstance().toolchains.firstOrNull{it.name==ESP32_TOOLCHAIN} ?: createToolchain()
        val launcher = ESP32DebugLauncher(project, toolchain, this)
        return CidrCommandLineState(environment, launcher)
    }

    private fun createToolchain(): CPPToolchains.Toolchain {
        val defaultToolchain = CPPToolchains.getInstance().defaultToolchain

        val path = File(ESP32SettingState.crosscompilerPath)
        val compiler = File(path, "xtensa-esp32-elf-gcc")
        val cxxCompiler = File(path, "xtensa-esp32-elf-g++")
        val debugger = File(path, "xtensa-esp32-elf-gdb")

        return defaultToolchain?.let { toolchain ->
            val debugger = CPPDebugger.customGdb(debugger)
            val esp32Toolchain = CPPToolchains.Toolchain(toolchain.osType, toolchain.toolSetKind,debugger)
            esp32Toolchain.name=ESP32_TOOLCHAIN
            esp32Toolchain.customCCompilerPath=compiler.absolutePath
            esp32Toolchain.customCXXCompilerPath=cxxCompiler.absolutePath
            esp32Toolchain
        } ?: throw RuntimeConfigurationException("Debugger not configurable")
    }

    @Throws(WriteExternalException::class)
    override fun writeExternal(parentElement: Element) {
        try {
            super.writeExternal(parentElement)
            val element = Element(TAG_OPENOCD)
            parentElement.addContent(element)
            element.setAttribute(ATTR_GDB_PORT, gdbPort.toString())
            element.setAttribute(ATTR_TELNET_PORT, telnetPort.toString())
            if (boardConfigFile.isNotBlank()) {
                element.setAttribute(ATTR_BOARD_CONFIG, boardConfigFile)
            }
            element.setAttribute(ATTR_RESET_TYPE, resetType.name)
            element.setAttribute(ATTR_DOWNLOAD_TYPE, downloadType.name)
        } catch (e: Exception){
            LOG.error("Writing conf: ", e)
        }

    }


    @Throws(InvalidDataException::class)
    override fun readExternal(parentElement: Element) {
        try {
            super.readExternal(parentElement)
            val element = parentElement.getChild(TAG_OPENOCD)
            boardConfigFile = element?.let { element.getAttributeValue(ATTR_BOARD_CONFIG) } ?: ""
            gdbPort = Utils.readIntAttr(element, ATTR_GDB_PORT, DEFAULT_GDB_PORT)
            telnetPort = Utils.readIntAttr(element, ATTR_TELNET_PORT, DEFAULT_TELNET_PORT)
            resetType = Utils.readEnumAttr(element, ATTR_RESET_TYPE, DEFAULT_RESET)
            downloadType = Utils.readEnumAttr(element, ATTR_DOWNLOAD_TYPE, DownloadType.ALWAYS)
        } catch (e: Exception){
            LOG.error("Error reading: ", e)
        }
    }

    @Throws(RuntimeConfigurationException::class)
    override fun checkConfiguration() {
        super.checkConfiguration()
        Utils.checkPort(gdbPort)
        Utils.checkPort(telnetPort)
        if (gdbPort == telnetPort) {
            throw RuntimeConfigurationException("gdb and telnet ports should be different")
        } else if (StringUtil.isEmpty(boardConfigFile)) {
            throw RuntimeConfigurationException("Board config not defined")
        }
    }
}

enum class ResetType(val command: String) {
    RUN("init;reset run;"), INIT("init;reset init;"), HALT("init;reset halt"), NONE("");

    override fun toString() = Utils.toBeautyString(super.toString())

}
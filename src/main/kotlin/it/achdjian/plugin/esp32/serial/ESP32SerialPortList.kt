package it.achdjian.plugin.esp32.serial

import com.intellij.execution.RunManagerEx
import com.intellij.openapi.project.Project
import it.achdjian.plugin.esp32.actions.configParsing
import it.achdjian.plugin.esp32.configurationName
import it.achdjian.plugin.esp32.configurations.flash.ESP32FlashRunConfiguration
import it.achdjian.plugin.esp32.setting.ESP32SettingState
import jssc.SerialNativeInterface
import java.io.File
import java.util.*
import java.util.regex.Pattern
import kotlin.math.min


fun getSerialPort(project: Project): ESP32SerialPortData? {
    val flashConfPort = RunManagerEx.getInstanceEx(project)
        .allSettings
        .firstOrNull { it.name == configurationName }
        ?.configuration
        ?.let {
            (it as ESP32FlashRunConfiguration).flashConfigurationState.port
        }

    val config = configParsing(project)
    val port = flashConfPort ?: ESP32SettingState.serialPortName
    val baud = config["CONFIG_ESPTOOLPY_MONITOR_BAUD"]?.toIntOrNull() ?: ESP32SettingState.serialPortBaud


    val portNames = ESP32SerialPortList.getPortNames()
    if (portNames.isEmpty())
        return ESP32SerialPortData(port, baud)
    ESP32SerialPortList.getPortNames().firstOrNull { it == port }?.let {
        return ESP32SerialPortData(
            port,
            baud
        )
    }

    return null
}


object ESP32SerialPortList {
    private val serialInterface = SerialNativeInterface()
    private val PORTNAMES_REGEXP: Pattern?
    private val PORTNAMES_PATH: String?

    init {
        when (SerialNativeInterface.getOsType()) {
            SerialNativeInterface.OS_LINUX -> {
                PORTNAMES_REGEXP = Pattern.compile("(ttyS|ttyUSB|ttyACM|ttyAMA|rfcomm|ttyO)[0-9]{1,3}")
                PORTNAMES_PATH = "/dev/"
            }
            SerialNativeInterface.OS_SOLARIS -> {
                PORTNAMES_REGEXP = Pattern.compile("[0-9]*|[a-z]*")
                PORTNAMES_PATH = "/dev/term/"
            }
            SerialNativeInterface.OS_MAC_OS_X -> {
                PORTNAMES_REGEXP = Pattern.compile("(tty|cu)\\..*")
                PORTNAMES_PATH = "/dev/"
            }
            SerialNativeInterface.OS_WINDOWS -> {
                PORTNAMES_REGEXP = Pattern.compile("")
                PORTNAMES_PATH = ""
            }
            else -> {
                PORTNAMES_REGEXP = null
                PORTNAMES_PATH = null
            }
        }
    }

    fun getPortNames() = getPortNames(PORTNAMES_PATH, PORTNAMES_REGEXP, PortNameComparator)

    private fun getPortNames(searchPath: String?, pattern: Pattern?, comparator: Comparator<String>?): List<String> {
        if (searchPath == null || pattern == null || comparator == null) {
            return listOf()
        }
        return if (SerialNativeInterface.getOsType() == SerialNativeInterface.OS_WINDOWS) {
            getWindowsPortNames(pattern, comparator)
        } else getUnixBasedPortNames(searchPath, pattern, comparator)
    }

    private fun getWindowsPortNames(pattern: Pattern, comparator: Comparator<String>): List<String> {
        val portNames = serialInterface.serialPortNames ?: return listOf()
        val ports = TreeSet(comparator)
        for (portName in portNames) {
            if (pattern.matcher(portName).find()) {
                ports.add(portName)
            }
        }
        return ports.toList()
    }

    private fun getUnixBasedPortNames(
        search: String,
        pattern: Pattern,
        comparator: Comparator<String>
    ): List<String> {
        val searchPath = when {
            search == "" -> search
            search.endsWith("/") -> search
            else -> "$search/"
        }
        var returnArray = listOf<String>()
        val dir = File(searchPath)
        if (dir.exists() && dir.isDirectory) {
            val files = dir.listFiles()
            if (files!!.isNotEmpty()) {
                val portsTree = TreeSet(comparator)
                for (file in files) {
                    val fileName = file.name
                    if (!file.isDirectory && !file.isFile && pattern.matcher(fileName).find()) {
                        val portName = searchPath + fileName
                        if (fileName.startsWith("ttyS")) {
                            val portHandle = serialInterface.openPort(portName, false)//Open port without TIOCEXCL
                            if (portHandle < 0 && portHandle != SerialNativeInterface.ERR_PORT_BUSY) {
                                continue
                            } else if (portHandle != SerialNativeInterface.ERR_PORT_BUSY) {
                                serialInterface.closePort(portHandle)
                            }
                        }
                        portsTree.add(portName)
                    }
                }
                returnArray = portsTree.toList()
            }
        }
        return returnArray
    }
}


object PortNameComparator : Comparator<String> {

    override fun compare(valueA: String, valueB: String): Int {

        if (valueA.equals(valueB, ignoreCase = true)) {
            return valueA.compareTo(valueB)
        }

        val minLength = min(valueA.length, valueB.length)

        var shiftA = 0
        var shiftB = 0

        var i = 0
        while (i < minLength) {
            val charA = valueA[i - shiftA]
            val charB = valueB[i - shiftB]
            if (charA != charB) {
                if (Character.isDigit(charA) && Character.isDigit(charB)) {
                    val resultsA = getNumberAndLastIndex(valueA, i - shiftA)
                    val resultsB = getNumberAndLastIndex(valueB, i - shiftB)

                    if (resultsA[0] != resultsB[0]) {
                        return resultsA[0] - resultsB[0]
                    }

                    if (valueA.length < valueB.length) {
                        i = resultsA[1]
                        shiftB = resultsA[1] - resultsB[1]
                    } else {
                        i = resultsB[1]
                        shiftA = resultsB[1] - resultsA[1]
                    }
                } else {
                    if (Character.toLowerCase(charA) - Character.toLowerCase(charB) != 0) {
                        return Character.toLowerCase(charA) - Character.toLowerCase(charB)
                    }
                }
            }
            i++
        }
        return valueA.compareTo(valueB, ignoreCase = true)
    }

    private fun getNumberAndLastIndex(str: String, startIndex: Int): IntArray {
        var numberValue = ""
        val returnValues = intArrayOf(-1, startIndex)
        for (i in startIndex until str.length) {
            returnValues[1] = i
            val c = str[i]
            if (Character.isDigit(c)) {
                numberValue += c
            } else {
                break
            }
        }
        try {
            returnValues[0] = Integer.valueOf(numberValue)
        } catch (ex: Exception) {
            //Do nothing
        }

        return returnValues
    }
}
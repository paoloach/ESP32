package it.achdjian.plugin.esp32.configurations.debuger.openocd

import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.openapi.util.Key
import com.intellij.util.concurrency.FutureResult
import com.jetbrains.cidr.cpp.execution.gdbserver.Utils
import it.achdjian.plugin.esp32.configurations.debuger.DownloadingStatus
import java.io.File

private val FAIL_STRINGS = arrayOf("** Programming Failed **", "communication failure", "** OpenOCD init failed **")

class OpenOCDDownloadFollower(private val downloadedFile: File) : FutureResult<DownloadingStatus>(),
    ProcessListener {

    companion object {
        val LOG = com.intellij.openapi.diagnostic.Logger.getInstance(OpenOCDDownloadFollower::class.java)
    }

    var actualStatus = DownloadingStatus.FLASH_PROGRAM_START

    override fun startNotified(event: ProcessEvent) {
    }

    override fun processTerminated(event: ProcessEvent) {
        try {
            if (!this.isDone) {
                this.set(actualStatus)
            }
        } catch (var3: Exception) {
            this.set(actualStatus)
        }
    }

    override fun processWillTerminate(event: ProcessEvent, willBeDestroyed: Boolean) {
    }

    override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        val text = event.text.trim { it <= ' ' }
        when {
            text.contains("no device found")-> {
                reset()
                set(DownloadingStatus.FLASH_NO_DEVICE_FOUND)
            }
            text.contains("JTAG scan chain interrogation failed") -> {
                reset()
                set(DownloadingStatus.FLASH_JTAG_ERROR)
            }
            FAIL_STRINGS.any { text.contains(it) } -> {
                reset()
                set(DownloadingStatus.FLASH_GENERIC_ERROR)
            }
            text.contains("** Programming Finished **") -> {
                actualStatus = DownloadingStatus.FLASH_PROGRAM_SUCCESS
                LOG.info("Programming successful")
            }
            text.contains("** Verified OK **") -> {
                actualStatus = DownloadingStatus.FLASH_VERIFY_SUCCESS
                Utils.markDownloaded(downloadedFile)
                LOG.info("Verify successful")
            }
            text.contains("Core 0 was reset")  && actualStatus == DownloadingStatus.FLASH_VERIFY_SUCCESS-> {
                reset()
                set(DownloadingStatus.FLASH_DOWNLOAD_SUCCESS)
                LOG.info("Core 0 reset")
            }
        }
    }
}
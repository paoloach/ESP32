package it.achdjian.plugin.esp32.configurations.debuger.openocd

import com.intellij.execution.filters.Filter
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.Project
import it.achdjian.plugin.esp32.configurations.debuger.showFailedDownloadNotification
import it.achdjian.plugin.esp32.configurations.debuger.showSuccessfulDownloadNotification

private val FAIL_STRINGS = arrayOf("** Programming Failed **","Verify Failer",  "communication failure", "** OpenOCD init failed **")
private val MY_COLOR_SCHEME = EditorColorsManager.getInstance().globalScheme
private val ERROR_TEXT_ATTRIBUTE = MY_COLOR_SCHEME.getAttributes(ConsoleViewContentType.ERROR_OUTPUT_KEY)

class OpenOCDErrorFilter(private val project: Project) : Filter {
    override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
        return if (FAIL_STRINGS.any { line.contains(it) }) {
            showFailedDownloadNotification(project)
            object : Filter.Result(0, line.length, null ,ERROR_TEXT_ATTRIBUTE ) {
                override fun getHighlighterLayer(): Int {
                    return 5000
                }
            }
        } else {
            if (line.contains("** Verified OK **")) {
                showSuccessfulDownloadNotification(project)
            }
            null
        }
    }
}
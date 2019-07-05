package it.achdjian.plugin.esp32.configurator

import com.intellij.ui.components.JBScrollPane
import io.mockk.MockKAnnotations
import it.achdjian.plugin.espparser.MainMenu
import it.achdjian.plugin.espparser.ReadFile
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.awt.*
import java.io.File
import javax.swing.JPanel
import javax.swing.JWindow


internal class WizardDataTest {

    @BeforeEach
    fun beforeEach() {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @Test
    fun createMenu() {
        val envPath = System.getenv("IDF_PATH")
        MatcherAssert.assertThat(envPath, CoreMatchers.notNullValue())
        val idfPath = File(envPath)
        MatcherAssert.assertThat(idfPath.exists(), CoreMatchers.`is`(true))
        val kConfig = File(idfPath, "Kconfig")
        MatcherAssert.assertThat(kConfig.exists(), CoreMatchers.`is`(true))
        val componentDir = File(idfPath, "components")
        val componentKConfig = componentDir.walk().filter { file->file.name=="Kconfig" }.toList()
        val componentKConfigProjbuild = componentDir.walk().filter { file->file.name=="Kconfig.projbuild" }.toList()


        val mainMenu = MainMenu(kConfig.readLines(), SourceList(), ReadFile())
        val entries = createMenuEntries(mainMenu, listOf())


        val w  = JWindow()

        val panel = JPanel()

        val menu = ESP32WizardPanel(panel, entries)
        val pane = JBScrollPane(menu)
        pane.preferredSize = Dimension(400,400)

        w.add(pane, BorderLayout.CENTER)

        menu.isVisible=true
        pane.isVisible=true
        w.isVisible=true

        w.setLocation(100, 100)
        w.setSize(800,800)
        Thread.sleep(10000000)
    }

}
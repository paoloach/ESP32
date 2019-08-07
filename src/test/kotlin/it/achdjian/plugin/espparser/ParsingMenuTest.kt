package it.achdjian.plugin.espparser

import io.mockk.MockKAnnotations
import it.achdjian.plugin.esp32.configurator.SourceList
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.io.File
import org.hamcrest.CoreMatchers.`is` as Is


class ParsingMenuTest {

    @Test
    fun parsing() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        val envPath = System.getenv("IDF_PATH")
        assertThat(envPath, notNullValue())
        val idfPath = File(envPath)
        assertThat(idfPath.exists(), Is(true))
        val kConfig = File(idfPath, "Kconfig")
        assertThat(kConfig.exists(), Is(true))

        val mainMenu = MainMenu(kConfig.readLines(), SourceList(envPath), ReadFile())
    }
}
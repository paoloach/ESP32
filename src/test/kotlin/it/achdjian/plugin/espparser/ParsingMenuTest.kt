package it.achdjian.plugin.espparser

import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.io.File
import org.hamcrest.CoreMatchers.`is` as Is

class ParsingMenuTest {

    @Test
    fun parsing() {
        val envPath = System.getenv("IDF_PATH")
        assertThat(envPath, notNullValue())
        val idfPath = File(envPath)
        assertThat(idfPath.exists(), Is(true))
        val kConfig = File(idfPath, "Kconfig")
        assertThat(kConfig.exists(), Is(true))
        val componentDir = File(idfPath, "components")
        val componentKConfig = componentDir.walk().filter { file->file.name=="Kconfig" }.toList()
        val componentKConfigProjbuild = componentDir.walk().filter { file->file.name=="Kconfig.projbuild" }.toList()
        val sourcesList = mapOf(
            "COMPONENT_KCONFIGS" to componentKConfig.toList(),
            "COMPONENT_KCONFIGS_PROJBUILD" to componentKConfigProjbuild.toList()
        )

        val mainMenu = MainMenu(kConfig.readLines(), sourcesList, ReadFile())
    }
}
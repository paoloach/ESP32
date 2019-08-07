package it.achdjian.plugin.espparser

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import it.achdjian.plugin.esp32.configurator.SourceList
import it.achdjian.plugin.esp32.entry_type.Value
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import org.hamcrest.CoreMatchers.`is` as Is

internal class MainMenuTest {
    @MockK
    lateinit var readFile: ReadFile

    val sourcesList = SourceList()
    //mapOf(
    //    "COMPONENT_KCONFIGS_PROJBUILD" to listOf(File("file1"), File("file2")),
    //    "COMPONENT_KCONFIGS" to listOf(File("file3"), File("file4"))
   // )

    val Kconfig1 = listOf(
        "mainmenu \"Espressif IoT Development Framework Configuration\"",
        "   config IDF_CMAKE",
        "       bool",
        "       option env=\"IDF_CMAKE\"",
        "   config IDF_TARGET",
        "       string",
        "       default \"IDF_TARGET_NOT_SET\" if IDF_TARGET_ENV=\"\"",
        "       default IDF_TARGET_ENV",
        "   menu \"SDK tool configuration\"",
        "       config TOOLPREFIX",
        "           string \"Compiler toolchain path/prefix\"",
        "           default \"xtensa-esp32-elf-\"",
        "           help",
        "               The prefix/path that is used to call the toolchain. The default setting assumes",
        "               a crosstool-ng gcc setup that is in your PATH.",


        "   endmenu  # SDK tool configuration",
        "  source \"\$COMPONENT_KCONFIGS_PROJBUILD\""
    )

    @BeforeEach
    fun beforeEach() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { readFile.read(File("file1")) } returns listOf(
            "menu \"ESP32-specific\"",
            "    config IDF_TARGET_ESP32",
            "        bool",
            "        default \"y\" if IDF_TARGET=\"esp32\"",
            "        default \"n\"",
            "    config ESP32_DEFAULT_CPU_FREQ_MHZ",
            "        int",
            "        default 80 if ESP32_DEFAULT_CPU_FREQ_80",
            "        default 160 if ESP32_DEFAULT_CPU_FREQ_160",
            "        default 240 if ESP32_DEFAULT_CPU_FREQ_240"
        )
        every { readFile.read(File("file2")) } returns listOf()
        every { readFile.read(File("file3")) } returns listOf()
        every { readFile.read(File("file4")) } returns listOf()
    }

    @Test
    fun mainMenuPrompt() {
        val mainMenu = MainMenu(Kconfig1, sourcesList, readFile)

        assertThat(mainMenu.prompt, Is("Espressif IoT Development Framework Configuration"))
    }

    @Test
    fun mainMenuNotPrompt() {
        val mainMenu = MainMenu(listOf("mainmenu "), sourcesList, readFile)

        assertThat(mainMenu.prompt, isEmptyString())
    }

    @Test
    fun configFromEnv() {
        val mainMenu = MainMenu(Kconfig1, sourcesList, readFile)

        assertThat(mainMenu.menuElements, hasKey("IDF_CMAKE"))
        assertThat(mainMenu.menuElements["IDF_CMAKE"], instanceOf(EspressifConfig::class.java))

        val espressifConfg = mainMenu.menuElements["IDF_CMAKE"] as EspressifConfig

        assertThat(espressifConfg.type, Is(ConfigType.Boolean))
        assertThat(espressifConfg.envVariable, Is("IDF_CMAKE"))
    }

    @Test
    fun configStringWithMultipleDefault() {
        val mainMenu = MainMenu(Kconfig1, sourcesList, readFile)
        assertThat(mainMenu.menuElements, hasKey("IDF_TARGET"))
        assertThat(mainMenu.menuElements["IDF_TARGET"], instanceOf(EspressifConfig::class.java))

        val espressifConfg = mainMenu.menuElements["IDF_TARGET"] as EspressifConfig

        val expected = EqualOper(SimpleExpression("IDF_TARGET_ENV"), SimpleExpression(""))

        assertThat(espressifConfg.type, Is(ConfigType.String))
        assertThat(
            espressifConfg.multiDefault, contains(
                Value(SimpleExpression("IDF_TARGET_NOT_SET"), expected),
                Value(SimpleExpression("IDF_TARGET_ENV"), emptyExpression)
            )
        )

    }

    @Test
    fun subMenu() {
        val mainMenu = MainMenu(Kconfig1, sourcesList, readFile)
        assertThat(mainMenu.menuElements, hasKey("SDK tool configuration"))
        assertThat(mainMenu.menuElements["SDK tool configuration"], instanceOf(EspressifMenu::class.java))
    }

    @Test
    fun stringConfigIntoSubmenu() {
        val mainMenu = MainMenu(Kconfig1, sourcesList, readFile)

        val espressifMenu = mainMenu.menuElements["SDK tool configuration"] as EspressifMenu
        val element = espressifMenu.allElements.first { it.name == "TOOLPREFIX" }
        assertThat(element, instanceOf(EspressifConfig::class.java))

        val espressifString = element as EspressifConfig

        assertThat(espressifString.type, Is(ConfigType.String))
        assertThat(
            espressifString.multiDefault,
            contains(Value(SimpleExpression("xtensa-esp32-elf-"), emptyExpression))
        )
    }

    @Test
    fun sourceFromAnotherFile() {
        val mainMenu = MainMenu(Kconfig1, sourcesList, readFile)
        assertThat(mainMenu.menuElements, hasKey("ESP32-specific"))
        assertThat(mainMenu.menuElements["ESP32-specific"], instanceOf(EspressifMenu::class.java))

        val espressifMenu = mainMenu.menuElements["ESP32-specific"] as EspressifMenu
        val element = espressifMenu.allElements.first { it.name == "IDF_TARGET_ESP32" }
        assertThat(element, instanceOf(EspressifConfig::class.java))
    }
}
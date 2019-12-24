package it.achdjian.plugin.espparser

import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import it.achdjian.plugin.esp32.configurator.SourceList
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class EspressifIfTest {
    @MockK
    lateinit var sourcesList: SourceList
    @MockK
    lateinit var readFile: ReadFile


    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @Test
    fun ifConfig() {
        val lines = listOf(
            " menuconfig FREERTOS_DEBUG_INTERNALS",
            "        bool \"Debug FreeRTOS internals\"",
            "        default n",
            "        help",
            "            Enable this option to show the menu with internal FreeRTOS debugging features.",
            "            This option does not change any code by itself, it just shows/hides some options.",
            "",
            "    if FREERTOS_DEBUG_INTERNALS",
            "",
            "        config FREERTOS_PORTMUX_DEBUG",
            "            bool \"Debug portMUX portENTER_CRITICAL/portEXIT_CRITICAL\"",
            "            depends on FREERTOS_DEBUG_INTERNALS",
            "            default n",
            "            help",
            "                If enabled, debug information (including integrity checks) will be printed",
            "                to UART for the port-specific MUX implementation.",
            "",
            "    endif # FREERTOS_DEBUG_INTERNALS",
            "",
            "    config FREERTOS_TASK_FUNCTION_WRAPPER",
            "        bool \"Enclose all task functions in a wrapper function\"",
            "        depends on OPTIMIZATION_LEVEL_DEBUG",
            "        default y",
            "        help",
            "            If enabled, all FreeRTOS task functions will be enclosed in a wrapper function.",
            "            If a task function mistakenly returns (i.e. does not delete), the call flow will",
            "            return to the wrapper function. The wrapper function will then log an error and",
            "            abort the application. This option is also required for GDB backtraces and C++",
            "            exceptions to work correctly inside top-level task functions.\n",
            "endmenu"
        )


        val menu = EspressifMenu(EspressifMenuNullElement(), "menu testmenu", sourcesList, ReadFile())
        var parser: EspressifMenuParser = menu
        lines.forEach {
            parser = parser.addLine(it)
        }
        assertThat(parser, Matchers.instanceOf(EspressifMenuNullElement::class.java))

        assertThat(
            menu.elements.map { it.name }, contains("FREERTOS_DEBUG_INTERNALS", "FREERTOS_TASK_FUNCTION_WRAPPER")
        )

        menu.menuConfigs.first { it.name=="FREERTOS_DEBUG_INTERNALS" }.let { subMenu->
            assertThat(subMenu.elements.map { it.name }, contains("FREERTOS_PORTMUX_DEBUG"))
            val config = subMenu.elements.first { it.name == "FREERTOS_PORTMUX_DEBUG" } as EspressifConfig
            assertThat(config.dependsOn, contains(SimpleExpression("FREERTOS_DEBUG_INTERNALS") as Expression))
        }

    }

    @Test
    fun nestedIf() {
        val lines = listOf(
            " menuconfig FREERTOS_DEBUG_INTERNALS",
            "        bool \"Debug FreeRTOS internals\"",
            "        default n",
            "        help",
            "            Enable this option to show the menu with internal FreeRTOS debugging features.",
            "            This option does not change any code by itself, it just shows/hides some options.",
            "",
            "    if FREERTOS_DEBUG_INTERNALS",
            "",
            "        config FREERTOS_PORTMUX_DEBUG",
            "            bool \"Debug portMUX portENTER_CRITICAL/portEXIT_CRITICAL\"",
            "            depends on FREERTOS_DEBUG_INTERNALS",
            "            default n",
            "            help",
            "                If enabled, debug information (including integrity checks) will be printed",
            "                to UART for the port-specific MUX implementation.",
            "         if !FREERTOS_UNICORE",
            "            config FREERTOS_PORTMUX_DEBUG_RECURSIVE",
            "                bool \"Debug portMUX Recursion\"",
            "                depends on FREERTOS_PORTMUX_DEBUG",
            "                default n",
            "                help",
            "                    If enabled, additional debug information will be printed for recursive",
            "                    portMUX usage.",
            "        endif #FREERTOS_UNICORE",
            "",
            "    endif # FREERTOS_DEBUG_INTERNALS",
            "",
            "    config FREERTOS_TASK_FUNCTION_WRAPPER",
            "        bool \"Enclose all task functions in a wrapper function\"",
            "        depends on OPTIMIZATION_LEVEL_DEBUG",
            "        default y",
            "        help",
            "            If enabled, all FreeRTOS task functions will be enclosed in a wrapper function.",
            "            If a task function mistakenly returns (i.e. does not delete), the call flow will",
            "            return to the wrapper function. The wrapper function will then log an error and",
            "            abort the application. This option is also required for GDB backtraces and C++",
            "            exceptions to work correctly inside top-level task functions.\n",
            "endmenu"
        )


        val menu = EspressifMenu(EspressifMenuNullElement(), "menu testmenu", sourcesList, ReadFile())
        var parser: EspressifMenuParser = menu
        lines.forEach {
            parser = parser.addLine(it)
        }
        assertThat(parser, Matchers.instanceOf(EspressifMenuNullElement::class.java))

        assertThat(
            menu.elements.map { it.name }, contains("FREERTOS_DEBUG_INTERNALS", "FREERTOS_TASK_FUNCTION_WRAPPER")
        )

        menu.menuConfigs.first { it.name=="FREERTOS_DEBUG_INTERNALS" }.let { subMenu->
            assertThat(subMenu.elements.map { it.name }, contains("FREERTOS_PORTMUX_DEBUG","FREERTOS_PORTMUX_DEBUG_RECURSIVE"))
            val config1 = subMenu.elements.first { it.name == "FREERTOS_PORTMUX_DEBUG" } as EspressifConfig
            assertThat(config1.dependsOn, contains(SimpleExpression("FREERTOS_DEBUG_INTERNALS") as Expression))

            val config2 = subMenu.elements.first { it.name == "FREERTOS_PORTMUX_DEBUG_RECURSIVE" } as EspressifConfig
            assertThat(config2.dependsOn, containsInAnyOrder(
                SimpleExpression("FREERTOS_DEBUG_INTERNALS") as Expression,
                SimpleExpression("FREERTOS_PORTMUX_DEBUG") as Expression,
                NotOper(SimpleExpression("FREERTOS_UNICORE")) as Expression)
            )
        }

    }

    @Test
    fun nestedIfMenuConfig() {

        val lines = listOf(
                    "        bool \"Support SPI to Ethernet Module\"",
                    "        default y",
                    "        help",
                    "            ESP-IDF can also support some SPI-Ethernet module.",
                    "",
                    "    if ETH_USE_SPI_ETHERNET",
                    "        menuconfig ETH_SPI_ETHERNET_DM9051",
                    "            bool \"Use DM9051\"",
                    "            default y",
                    "            help",
                    "                DM9051 is a fast Ethernet controller with an SPI interface.",
                    "                It's also integrated with a 10/100M PHY and MAC.",
                    "                Set true to enable DM9051 driver.",
                    "",
                    "        if ETH_SPI_ETHERNET_DM9051",
                    "            config ETH_DM9051_INT_GPIO",
                    "                int \"DM9051 Interrupt GPIO number\"",
                    "                default 4",
                    "                range 0 33",
                    "                help",
                    "                    Set the GPIO number used by DM9051's Interrupt pin.",
                    "        endif",
                    "    endif"
        )

        val menu = EspressifMenuConfig(EspressifMenuNullElement(), "menuconfig testmenu", sourcesList, readFile)
        var parser: EspressifMenuParser = menu
        lines.forEach {
            parser = parser.addLine(it)
        }
        assertThat(menu.configs.map { it.internalName }, containsInAnyOrder("ETH_SPI_ETHERNET_DM9051","ETH_DM9051_INT_GPIO"))
    }
}
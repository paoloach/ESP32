package it.achdjian.plugin.espparser

import gherkin.lexer.Es
import gherkin.lexer.No
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.system.measureNanoTime
import org.hamcrest.CoreMatchers.`is` as Is

internal class EspressifIfTest {
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


        val menu = EspressifMenu(EspressifMenuNullElement(), "menu testmenu", mapOf(), ReadFile())
        var parser: EspressifMenuParser = menu
        lines.forEach {
            parser = parser.addLine(it)
        }
        assertThat(parser, Matchers.instanceOf(EspressifMenuNullElement::class.java))

        assertThat(
            menu.elements.map { it.name }, contains("FREERTOS_DEBUG_INTERNALS", "FREERTOS_TASK_FUNCTION_WRAPPER")
        )

        menu.menuConfigs.get("FREERTOS_DEBUG_INTERNALS")?.let { subMenu ->
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


        val menu = EspressifMenu(EspressifMenuNullElement(), "menu testmenu", mapOf(), ReadFile())
        var parser: EspressifMenuParser = menu
        lines.forEach {
            parser = parser.addLine(it)
        }
        assertThat(parser, Matchers.instanceOf(EspressifMenuNullElement::class.java))

        assertThat(
            menu.elements.map { it.name }, contains("FREERTOS_DEBUG_INTERNALS", "FREERTOS_TASK_FUNCTION_WRAPPER")
        )

        menu.menuConfigs.get("FREERTOS_DEBUG_INTERNALS")?.let { subMenu ->
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
}
package it.achdjian.plugin.espparser

import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.hamcrest.CoreMatchers.`is` as Is

internal class EspressifChoiceTest {
    @Test
    fun SimpleTwoOption() {
        val lines = listOf(
            "    prompt \"Flash SPI mode\"",
            "    default FLASHMODE_DIO",
            "    help",
            "        Mode the flash chip is flashed in, as well as the default mode for the",
            "        binary to run in.",
            "        ",
            "    config FLASHMODE_QIO",
            "        bool \"QIO\"",
            "    config FLASHMODE_DIO",
            "        bool \"DIO\"",
            "endchoice"

        )
        val choice = EspressifChoice(EspressifMenuNullElement(),"FLASHMODE", listOf())
        var parser: EspressifMenuParser = choice

        lines.forEach {
            parser = parser.addLine(it)
        }

        assertThat(parser, instanceOf(EspressifMenuNullElement::class.java))

        assertThat(choice.prompt, Is("Flash SPI mode"))
        assertThat(
            choice.help,
            Is("Mode the flash chip is flashed in, as well as the default mode for the binary to run in.\n")
        )
        assertThat(choice.listConfig.map { it.name }, contains("FLASHMODE_QIO", "FLASHMODE_DIO"))
        assertThat(choice.defaultList, contains(Default("FLASHMODE_DIO")))
    }

    @Test
    fun SimpleWithDependsOn() {
        val lines = listOf(
            "        prompt \"Default baud rate\"",
            "        depends on !IDF_CMAKE",
            "        default ESPTOOLPY_BAUD_115200B",
            "        help",
            "            Default baud rate to use while communicating with the ESP chip. Can be overridden by",
            "            setting the ESPBAUD variable.",
            "",
            "            This value is ignored when using the CMake-based build system or idf.py.",
            "",
            "        config ESPTOOLPY_BAUD_115200B",
            "            bool \"115200 baud\"",
            "        config ESPTOOLPY_BAUD_230400B",
            "            bool \"230400 baud\"",
            "        config ESPTOOLPY_BAUD_921600B",
            "            bool \"921600 baud\"",
            "        config ESPTOOLPY_BAUD_2MB",
            "            bool \"2Mbaud\"",
            "        config ESPTOOLPY_BAUD_OTHER",
            "            bool \"Other baud rate\"",
            "endchoice"
        )

        val choice = EspressifChoice(EspressifMenuNullElement(),"ESPTOOLPY_BAUD", listOf())

        var parser: EspressifMenuParser = choice

        lines.forEach {
            parser = parser.addLine(it)
        }

        assertThat(parser, instanceOf(EspressifMenuNullElement::class.java))


        assertThat(choice.prompt, Is("Default baud rate"))
        assertThat(
            choice.help,
            Is("Default baud rate to use while communicating with the ESP chip. Can be overridden by setting the ESPBAUD variable.\n This value is ignored when using the CMake-based build system or idf.py.\n")
        )
        assertThat(choice.listConfig.map { it.name }, contains("ESPTOOLPY_BAUD_115200B", "ESPTOOLPY_BAUD_230400B","ESPTOOLPY_BAUD_921600B","ESPTOOLPY_BAUD_2MB","ESPTOOLPY_BAUD_OTHER"))
        assertThat(choice.defaultList, contains(Default("ESPTOOLPY_BAUD_115200B")))
        assertThat(choice.dependsOn, contains<Expression>(NotOper(SimpleExpression("IDF_CMAKE"))))
    }
    
    @Test
    fun boolInsteadOfPromt() {
        val lines = listOf(
                    "        bool \"Bootloader log verbosity\"",
                    "        default LOG_BOOTLOADER_LEVEL_INFO",
                    "        help",
                    "            Specify how much output to see in bootloader logs.",
                    "",
                    "        config LOG_BOOTLOADER_LEVEL_NONE",
                    "            bool \"No output\"",
                    "        config LOG_BOOTLOADER_LEVEL_ERROR",
                    "            bool \"Error\"",
                    "        config LOG_BOOTLOADER_LEVEL_WARN",
                    "            bool \"Warning\"",
                    "        config LOG_BOOTLOADER_LEVEL_INFO",
                    "            bool \"Info\"",
                    "        config LOG_BOOTLOADER_LEVEL_DEBUG",
                    "            bool \"Debug\"",
                    "        config LOG_BOOTLOADER_LEVEL_VERBOSE",
                    "            bool \"Verbose\"",
            "endchoice"
        )

        val choice = EspressifChoice(EspressifMenuNullElement(),"LOG_BOOTLOADER_LEVEL", listOf())

        var parser: EspressifMenuParser = choice

        lines.forEach {
            parser = parser.addLine(it)
        }

        assertThat(parser, instanceOf(EspressifMenuNullElement::class.java))


        assertThat(choice.prompt, Is("Bootloader log verbosity"))

    }

    @Test
    fun completedConfig() {
        val lines = listOf(
                    "            prompt \"Assertion level\"",
                    "            default OPTIMIZATION_ASSERTIONS_ENABLED",
                    "            help",
                    "                Assertions can be:",
                    "",
                    "                - Enabled. Failure will print verbose assertion details. This is the default.",
                    "",
                    "                - Set to \"silent\" to save code size (failed assertions will abort() but user",
                    "                  needs to use the aborting address to find the line number with the failed assertion.)",
                    "",
                    "                - Disabled entirely (not recommended for most configurations.) -DNDEBUG is added",
                    "                  to CPPFLAGS in this case.",
                    "",
                    "            config OPTIMIZATION_ASSERTIONS_ENABLED",
                    "                prompt \"Enabled\"",
                    "                bool",
                    "                help",
                    "                    Enable assertions. Assertion content and line number will be printed on failure.",
                    "",
                    "            config OPTIMIZATION_ASSERTIONS_SILENT",
                    "                prompt \"Silent (saves code size)\"",
                    "                bool",
                    "                help",
                    "                    Enable silent assertions. Failed assertions will abort(), user needs to",
                    "                    use the aborting address to find the line number with the failed assertion.",
                    "",
                    "            config OPTIMIZATION_ASSERTIONS_DISABLED",
                    "                prompt \"Disabled (sets -DNDEBUG)\"",
                    "                bool",
                    "                help",
                    "                    If assertions are disabled, -DNDEBUG is added to CPPFLAGS.",
                    "",
            "endchoice"
        )

        val choice = EspressifChoice(EspressifMenuNullElement(),"OPTIMIZATION_ASSERTION_LEVEL", listOf())

        var parser: EspressifMenuParser = choice

        lines.forEach {
            parser = parser.addLine(it)
        }

        assertThat(parser, instanceOf(EspressifMenuNullElement::class.java))


        assertThat(choice.prompt, Is("Assertion level"))
        assertThat(choice.defaultList, contains(Default("OPTIMIZATION_ASSERTIONS_ENABLED")))
        assertThat(choice.help, Is(
            "Assertions can be:\n" +
                    " - Enabled. Failure will print verbose assertion details. This is the default.\n" +
                    " - Set to \"silent\" to save code size (failed assertions will abort() but user needs to use the aborting address to find the line number with the failed assertion.)\n" +
                    " - Disabled entirely (not recommended for most configurations.) -DNDEBUG is added to CPPFLAGS in this case.\n"
        ))

        assertThat(choice.listConfig.size, Is(3))
        assertThat(choice.listConfig[0].name, Is("OPTIMIZATION_ASSERTIONS_ENABLED"))
        assertThat(choice.listConfig[0].text, Is("Enabled"))
        assertThat(choice.listConfig[1].name, Is("OPTIMIZATION_ASSERTIONS_SILENT"))
        assertThat(choice.listConfig[1].text, Is("Silent (saves code size)"))
        assertThat(choice.listConfig[2].name, Is("OPTIMIZATION_ASSERTIONS_DISABLED"))
        assertThat(choice.listConfig[2].text, Is("Disabled (sets -DNDEBUG)"))

    }
}
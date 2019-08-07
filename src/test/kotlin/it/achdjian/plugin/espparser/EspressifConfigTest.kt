package it.achdjian.plugin.espparser

import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import it.achdjian.plugin.esp32.configurator.SourceList
import it.achdjian.plugin.esp32.entry_type.Value
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.contains
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.hamcrest.CoreMatchers.`is` as Is

internal class EspressifConfigTest {
    @MockK
    lateinit var sourcesList: SourceList
    @MockK
    lateinit var readFile: ReadFile

    @BeforeEach
    fun beforeEach() {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }


    @Test
    fun oneEmptyLineFirstOfHelpToken() {
        val lines = listOf(
            "int \"Slave respond timeout (Milliseconds)\"",
            "        default 150",
            "        range 50 400",
            "",
            "        help",
            "                If master sends a frame which is not broadcast, it has to wait sometime for slave response.",
            "                if slave is not respond in this time, the master will process timeout error.",
            "",
            "config MB_MASTER_DELAY_MS_CONVERT"
        )
        val config = EspressifConfig(EspressifFakeParent(), "config MB_MASTER_TIMEOUT_MS_RESPOND", sourcesList,readFile)
        var parser: EspressifMenuParser = config

        lines.forEach {
            parser = parser.addLine(it)
        }

        assertThat(parser, CoreMatchers.instanceOf(EspressifFakeParent::class.java))

        assertThat(config.name, Is("MB_MASTER_TIMEOUT_MS_RESPOND"))
        assertThat(config.type, Is(ConfigType.Int))
        assertThat(config.text, Is("Slave respond timeout (Milliseconds)"))
        assertThat(config.multiDefault, contains(Value(SimpleExpression("150"))))
        assertThat(config.min, Is("50"))
        assertThat(config.max, Is("400"))

        assertThat(
            config.help,
            Is("If master sends a frame which is not broadcast, it has to wait sometime for slave response.if slave is not respond in this time, the master will process timeout error.")
        )


    }
}
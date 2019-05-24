package it.achdjian.plugin.espparser

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.instanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import org.hamcrest.CoreMatchers.`is` as Is

internal class EspressifMenuTest {
    @MockK
    lateinit var readFile: ReadFile
    val file = File("/tmp/source")

    @BeforeEach
    fun beforeEach() {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @Test
    fun sourceMenuConfig() {
        every { readFile.read(file) } returns listOf(
            "menuconfig AWS_IOT_SDK",
            "    bool \"Amazon Web Services IoT Platform\"",
            "    help",
            "        Select this option to enable support for the AWS IoT platform,",
            "        via the esp-idf component for the AWS IoT Device C SDK.",
            "",
            "config AWS_IOT_MQTT_HOST",
            "    string \"AWS IoT Endpoint Hostname\"",
            "    depends on AWS_IOT_SDK",
            "    default \"\"",
            "    help",
            "        Default endpoint host name to connect to AWS IoT MQTT/S gateway",
            "endmenu"
        )


        val menu = EspressifMenu(
            EspressifMenuNullElement(),
            "menu \"Component config\"",
            mapOf("COMPONENT_KCONFIGS" to listOf(file)),
            readFile
        )


        menu.addLine("source \"\$COMPONENT_KCONFIGS\"")
        assertThat(menu.elements.map { it.name }, contains("AWS_IOT_SDK"))
        assertThat(menu.elements[0], instanceOf(EspressifMenuConfig::class.java))
        val menuConfig = menu.elements[0] as EspressifMenuConfig

        assertThat(menuConfig.elements.map { it.name }, contains("AWS_IOT_MQTT_HOST"))
    }


    @Test
    fun configWithHelpwithALine() {
        val lines = listOf(
            "",
            "    config MB_MASTER_TIMEOUT_MS_RESPOND",
            "        int \"Slave respond timeout (Milliseconds)\"",
            "        default 150",
            "        range 50 400",
            "",
            "        help",
            "                If master sends a frame which is not broadcast, it has to wait sometime for slave response.",
            "                if slave is not respond in this time, the master will process timeout error.",
            "",
            "    config MB_MASTER_DELAY_MS_CONVERT",
            "        int \"Slave conversion delay (Milliseconds)\"",
            "        default 200",
            "        range 50 400",
            "",
            "        help",
            "                If master sends a broadcast frame, it has to wait conversion time to delay,",
            "                then master can send next frame.",
            "endmenu"
        )


        val menu = EspressifMenu(EspressifFakeParent(), "menu \"Modbus configuration\"", mapOf(), readFile)
        var parser: EspressifMenuParser = menu
        lines.forEach {
            parser = parser.addLine(it)
        }

        val configs = menu.elements.map { it as EspressifConfig }
        assertThat(configs.map { it.name }, contains("MB_MASTER_TIMEOUT_MS_RESPOND", "MB_MASTER_DELAY_MS_CONVERT"))
        assertThat(
            configs.map { it.help }, contains(
                "If master sends a frame which is not broadcast, it has to wait sometime for slave response.if slave is not respond in this time, the master will process timeout error.",
                "If master sends a broadcast frame, it has to wait conversion time to delay,then master can send next frame."
            )
        )

    }


    @Test
    fun subMenu() {
        val lines = listOf(
            "",
            "",
            "    config BT_ENABLED",
            "        bool \"Bluetooth\"",
            "        help",
            "            Select this option to enable Bluetooth and show the submenu with Bluetooth configuration choices.",
            "",
            "    menu \"Bluetooth controller\"",
            "        visible if BT_ENABLED",
            "",
            "        choice BTDM_CONTROLLER_MODE",
            "            prompt \"Bluetooth controller mode (BR/EDR/BLE/DUALMODE)\"",
            "            depends on BT_ENABLED",
            "            help",
            "                Specify the bluetooth controller mode (BR/EDR, BLE or dual mode).",
            "",
            "            config BTDM_CONTROLLER_MODE_BLE_ONLY",
            "                bool \"BLE Only\"",
            "",
            "            config BTDM_CONTROLLER_MODE_BR_EDR_ONLY",
            "                bool \"BR/EDR Only\"",
            "",
            "            config BTDM_CONTROLLER_MODE_BTDM",
            "                bool \"Bluetooth Dual Mode\"",
            "",
            "        endchoice",
            "        menu \"HCI UART(H4) Options\"",
            "            visible if BTDM_CONTROLLER_HCI_MODE_UART_H4",
            "",
            "            config BT_HCI_UART_NO",
            "                int \"UART Number for HCI\"",
            "                depends on BTDM_CONTROLLER_HCI_MODE_UART_H4",
            "                range 1 2",
            "                default 1",
            "                help",
            "                    Uart number for HCI. The available uart is UART1 and UART2.",
            "",
            "            config BT_HCI_UART_BAUDRATE",
            "                int \"UART Baudrate for HCI\"",
            "                depends on BTDM_CONTROLLER_HCI_MODE_UART_H4",
            "                range 115200 921600",
            "                default 921600",
            "                help",
            "                    UART Baudrate for HCI. Please use standard baudrate.",
            "",
            "        endmenu",
            "    endmenu",
            "    endmenu"
        )

        val menu = EspressifMenu(EspressifMenuNullElement(), "menu Bluetooth", mapOf(), readFile)
        var parser: EspressifMenuParser = menu
        lines.forEach {
            parser = parser.addLine(it)
        }
        assertThat(parser, instanceOf(EspressifMenuNullElement::class.java))
        assertThat(menu.subMenus.map { it.name }, contains("Bluetooth controller"))
        val subMenu = menu.subMenus[0]
        assertThat(subMenu.subMenus.map { it.name }, contains("HCI UART(H4) Options"))
        val subSubMenu = subMenu.subMenus[0]
        assertThat(subSubMenu.visibleIf, Is(SimpleExpression("BTDM_CONTROLLER_HCI_MODE_UART_H4") as Expression))
        assertThat(subSubMenu.elements.map { it.name }, contains("BT_HCI_UART_NO", "BT_HCI_UART_BAUDRATE"))


    }
}
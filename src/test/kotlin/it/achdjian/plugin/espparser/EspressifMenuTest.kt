package it.achdjian.plugin.espparser

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import it.achdjian.plugin.esp32.configurator.SourceList
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import org.hamcrest.CoreMatchers.`is` as Is

internal class EspressifMenuTest {
    @MockK
    lateinit var readFile: ReadFile

    @MockK
    lateinit var sourcesList: SourceList

    val file = File("/tmp/source")

    @BeforeEach
    fun beforeEach() {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }


    @Test
    fun esp8266SerialFlashConfig() {
        val lines = listOf("",
                "config ESPTOOLPY_PORT",
                "\tstring \"Default serial port\"",
                "\tdefault \"/dev/ttyUSB0\"",
                "\thelp",
                "\t\tThe serial port that's connected to the ESP chip. This can be overridden by setting the ESPPORT",
                "\t\tenvironment variable.",
                "",
                "choice ESPTOOLPY_BAUD",
                "\tprompt \"Default baud rate\"",
                "\tdefault ESPTOOLPY_BAUD_115200B",
                "\thelp",
                "\t\tDefault baud rate to use while communicating with the ESP chip. Can be overridden by",
                "\t\tsetting the ESPBAUD variable.",
                "",
                "config ESPTOOLPY_BAUD_115200B",
                "\tbool \"115200 baud\"",
                "config ESPTOOLPY_BAUD_230400B",
                "\tbool \"230400 baud\"",
                "config ESPTOOLPY_BAUD_921600B",
                "\tbool \"921600 baud\"",
                "config ESPTOOLPY_BAUD_2MB",
                "\tbool \"2Mbaud\"",
                "config ESPTOOLPY_BAUD_OTHER",
                "    bool \"Other baud rate\"",
                "endchoice",
                "",
                "config ESPTOOLPY_BAUD_OTHER_VAL",
                "\tint \"Other baud rate value\" if ESPTOOLPY_BAUD_OTHER",
                "\tdefault 115200",
                "",
                "config ESPTOOLPY_BAUD",
                "\tint",
                "\tdefault 115200 if ESPTOOLPY_BAUD_115200B",
                "\tdefault 230400 if ESPTOOLPY_BAUD_230400B",
                "\tdefault 921600 if ESPTOOLPY_BAUD_921600B",
                "\tdefault 2000000 if ESPTOOLPY_BAUD_2MB",
                "\tdefault ESPTOOLPY_BAUD_OTHER_VAL if ESPTOOLPY_BAUD_OTHER",
                "",
                "config ESPTOOLPY_COMPRESSED",
                "\tbool \"Use compressed upload\"",
                "\tdefault \"y\"",
                "\thelp",
                "\t\tThe flasher tool can send data compressed using zlib, letting the ROM on the ESP chip",
                "\t\tdecompress it on the fly before flashing it. For most payloads, this should result in a",
                "\t\tspeed increase.",
                "",
                "choice FLASHMODE",
                "\tprompt \"Flash SPI mode\"",
                "\tdefault FLASHMODE_QIO",
                "\thelp",
                "\t\tMode the flash chip is flashed in, as well as the default mode for the",
                "\t\tbinary to run in.",
                "",
                "\t\tThe esptool.py flashes firmware at DIO mode when user select \"QIO\", \"QOUT\" or \"DIO\" mode.",
                "",
                "config FLASHMODE_QIO",
                "\tbool \"QIO\"",
                "config FLASHMODE_QOUT",
                "\tbool \"QOUT\"",
                "config FLASHMODE_DIO",
                "\tbool \"DIO\"",
                "config FLASHMODE_DOUT",
                "\tbool \"DOUT\"",
                "endchoice",
                "",
                "# Note: we use esptool.py to flash bootloader in",
                "# dio mode for QIO/QOUT, bootloader then upgrades",
                "# itself to quad mode during initialisation",
                "config ESPTOOLPY_FLASHMODE",
                "\tstring",
                "\tdefault \"dio\" if FLASHMODE_QIO",
                "\tdefault \"dio\" if FLASHMODE_QOUT",
                "\tdefault \"dio\" if FLASHMODE_DIO",
                "\tdefault \"dout\" if FLASHMODE_DOUT",
                "",
                "config SPI_FLASH_MODE",
                "\thex",
                "\tdefault 0x0 if FLASHMODE_QIO",
                "\tdefault 0x1 if FLASHMODE_QOUT",
                "\tdefault 0x2 if FLASHMODE_DIO",
                "\tdefault 0x3 if FLASHMODE_DOUT\t",
                "",
                "choice ESPTOOLPY_FLASHFREQ",
                "\tprompt \"Flash SPI speed\"",
                "\tdefault ESPTOOLPY_FLASHFREQ_40M",
                "\thelp",
                "\t\tThe SPI flash frequency to be used.",
                "",
                "config ESPTOOLPY_FLASHFREQ_80M",
                "\tbool \"80 MHz\"",
                "config ESPTOOLPY_FLASHFREQ_40M",
                "\tbool \"40 MHz\"",
                "config ESPTOOLPY_FLASHFREQ_26M",
                "\tbool \"26 MHz\"",
                "config ESPTOOLPY_FLASHFREQ_20M",
                "\tbool \"20 MHz\"",
                "endchoice",
                "",
                "config ESPTOOLPY_FLASHFREQ",
                "\tstring",
                "\tdefault \"80m\" if ESPTOOLPY_FLASHFREQ_80M",
                "\tdefault \"40m\" if ESPTOOLPY_FLASHFREQ_40M",
                "\tdefault \"26m\" if ESPTOOLPY_FLASHFREQ_26M",
                "\tdefault \"20m\" if ESPTOOLPY_FLASHFREQ_20M",
                "",
                "config SPI_FLASH_FREQ",
                "\thex",
                "\tdefault 0xf if ESPTOOLPY_FLASHFREQ_80M",
                "\tdefault 0x0 if ESPTOOLPY_FLASHFREQ_40M",
                "\tdefault 0x1 if ESPTOOLPY_FLASHFREQ_26M",
                "\tdefault 0x2 if ESPTOOLPY_FLASHFREQ_20M",
                "",
                "choice ESPTOOLPY_FLASHSIZE",
                "\tprompt \"Flash size\"",
                "\tdefault ESPTOOLPY_FLASHSIZE_2MB",
                "\thelp",
                "\t\tSPI flash size, in megabytes. Users must not configure a larger value than their real flash size.",
                "\t\tOtherwise an unexpected result may cause if users read/write a wrong address of flash.",
                "",
                "config ESPTOOLPY_FLASHSIZE_1MB",
                "\tbool \"1 MB\"",
                "config ESPTOOLPY_FLASHSIZE_2MB",
                "\tbool \"2 MB\"",
                "config ESPTOOLPY_FLASHSIZE_4MB",
                "\tbool \"4 MB\"",
                "config ESPTOOLPY_FLASHSIZE_8MB",
                "\tbool \"8 MB\"",
                "config ESPTOOLPY_FLASHSIZE_16MB",
                "\tbool \"16 MB\"",
                "endchoice",
                "",
                "config ESPTOOLPY_FLASHSIZE",
                "\tstring",
                "\tdefault \"1MB\" if ESPTOOLPY_FLASHSIZE_1MB",
                "\tdefault \"2MB\" if ESPTOOLPY_FLASHSIZE_2MB",
                "\tdefault \"4MB\" if ESPTOOLPY_FLASHSIZE_4MB",
                "\tdefault \"8MB\" if ESPTOOLPY_FLASHSIZE_8MB",
                "\tdefault \"16MB\" if ESPTOOLPY_FLASHSIZE_16MB",
                "",
                "config SPI_FLASH_SIZE",
                "\thex",
                "\tdefault 0x100000 if ESPTOOLPY_FLASHSIZE_1MB",
                "\tdefault 0x200000 if ESPTOOLPY_FLASHSIZE_2MB",
                "\tdefault 0x400000 if ESPTOOLPY_FLASHSIZE_4MB",
                "\tdefault 0x800000 if ESPTOOLPY_FLASHSIZE_8MB",
                "\tdefault 0x1000000 if ESPTOOLPY_FLASHSIZE_16MB",
                "",
                "choice ESPTOOLPY_BEFORE",
                "    prompt \"Before flashing\"",
                "    default ESPTOOLPY_BEFORE_RESET",
                "    help",
                "        Configure whether esptool.py should reset the ESP32 before flashing.",
                "",
                "        Automatic resetting depends on the RTS & DTR signals being",
                "        wired from the serial port to the ESP32. Most USB development",
                "        boards do this internally.",
                "",
                "config ESPTOOLPY_BEFORE_RESET",
                "    bool \"Reset to bootloader\"",
                "config ESPTOOLPY_BEFORE_NORESET",
                "    bool \"No reset\"",
                "endchoice",
                "",
                "config ESPTOOLPY_BEFORE",
                "    string",
                "    default \"default_reset\" if ESPTOOLPY_BEFORE_RESET",
                "    default \"no_reset\" if ESPTOOLPY_BEFORE_NORESET",
                "",
                "choice ESPTOOLPY_AFTER",
                "    prompt \"After flashing\"",
                "    default ESPTOOLPY_AFTER_HARD_RESET",
                "    help",
                "        Configure whether esptool.py should reset the ESP8266 after flashing.",
                "",
                "        Automatic resetting depends on the RTS & DTR signals being",
                "        wired from the serial port to the ESP32. Most USB development",
                "        boards do this internally.",
                "",
                "config ESPTOOLPY_AFTER_HARD_RESET",
                "    bool \"Hard reset after flashing\"",
                "config ESPTOOLPY_AFTER_SOFT_RESET",
                "    bool \"Soft reset after flashing\"",
                "config ESPTOOLPY_AFTER_NORESET",
                "    bool \"Stay in bootloader\"",
                "endchoice",
                "",
                "config ESPTOOLPY_AFTER",
                "    string",
                "    default \"hard_reset\" if ESPTOOLPY_AFTER_HARD_RESET",
                "    default \"soft_reset\" if ESPTOOLPY_AFTER_SOFT_RESET",
                "    default \"no_reset\" if ESPTOOLPY_AFTER_NORESET",
                "",
                "choice MONITOR_BAUD",
                "    prompt \"'make monitor' baud rate\"",
                "    default MONITOR_BAUD_74880B",
                "    help",
                "        Baud rate to use when running 'make monitor' to view serial output",
                "        from a running chip.",
                "",
                "        Can override by setting the MONITORBAUD environment variable.",
                "",
                "config MONITOR_BAUD_9600B",
                "    bool \"9600 bps\"",
                "config MONITOR_BAUD_57600B",
                "    bool \"57600 bps\"",
                "config MONITOR_BAUD_74880B",
                "    bool \"74880 bps\"",
                "config MONITOR_BAUD_115200B",
                "    bool \"115200 bps\"",
                "config MONITOR_BAUD_230400B",
                "    bool \"230400 bps\"",
                "config MONITOR_BAUD_921600B",
                "    bool \"921600 bps\"",
                "config MONITOR_BAUD_2MB",
                "    bool \"2 Mbps\"",
                "config MONITOR_BAUD_OTHER",
                "    bool \"Custom baud rate\"",
                "",
                "endchoice",
                "",
                "config MONITOR_BAUD_OTHER_VAL",
                "    int \"Custom baud rate value\" if MONITOR_BAUD_OTHER",
                "    default 74880",
                "",
                "config MONITOR_BAUD",
                "    int",
                "    default 9600 if MONITOR_BAUD_9600B",
                "    default 57600 if MONITOR_BAUD_57600B",
                "    default 74880 if MONITOR_BAUD_74880B",
                "    default 115200 if MONITOR_BAUD_115200B",
                "    default 230400 if MONITOR_BAUD_230400B",
                "    default 921600 if MONITOR_BAUD_921600B",
                "    default 2000000 if MONITOR_BAUD_2MB",
                "    default MONITOR_BAUD_OTHER_VAL if MONITOR_BAUD_OTHER",
                "",
                "config ESPTOOLPY_ENABLE_TIME",
                "\tbool \"Enable monitor time information\"",
                "\tdefault n",
                "\thelp",
                "\t\tEnable this option, time string will be added at the head of serial input data line.",
                "",
                "config ESPTOOLPY_ENABLE_SAVELOG",
                "\tbool \"Enable console log save\"",
                "\tdefault n",
                "\thelp",
                "\t\tEnable this option, all console output will auto-save to current 'log' directory which same level as the 'build` directory; ",
                "\t\tif 'log' directory is not exist, will create it. ",
                "\t\tAll console output will be saved as a log filename, named PROJECT_NAME + timestamp.log, which PROJECT_NAME defined in your demo Makefile.",
                "",
                "endmenu\n")

        val menu = EspressifMenu(EspressifMenuNullElement(), "menu \"Serial flasher config\"", sourcesList, readFile)
        var parser: EspressifMenuParser = menu
        lines.forEach {
            parser = parser.addLine(it)
        }
        assertThat(parser, instanceOf(EspressifMenuNullElement::class.java))
        assertThat(menu.elements, hasSize(23))
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


        // mapOf("COMPONENT_KCONFIGS" to listOf(file)),
        val sourceList = sourcesList
        every { sourceList["COMPONENT_KCONFIGS"] }.returns( listOf(file));

        val menu = EspressifMenu(
            EspressifMenuNullElement(),
            "menu \"Component config\"",
            sourceList,
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


        val menu = EspressifMenu(EspressifFakeParent(), "menu \"Modbus configuration\"", sourcesList, readFile)
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

        val menu = EspressifMenu(EspressifMenuNullElement(), "menu Bluetooth", sourcesList, readFile)
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

    @Test
    fun menuWithIf() {
        val lines = listOf(
            "",
            "",
            "menuconfig USING_SPIFFS",
            " bool \"SPIFFS\"",
            "    select USING_ESP_VFS",
            "    help",
            "       Select this option to enable support for the SPIFFS.",
            "",
            "if USING_SPIFFS",
            "",
            "config SPIFFS_MAX_PARTITIONS",
            "    int \"Maximum Number of Partitions\"",
            "    default 3",
            "    range 1 10",
            "    help",
            "        Define maximum number of partitions that can be mounted.",
            "",
            "menu \"Debug Configuration\"",
            "",
            "config SPIFFS_DBG",
            "    bool \"Enable general SPIFFS debug\"",
            "    default \"n\"",
            "    help",
            "        Enabling this option will print general debug mesages to the console.",
            "",
            "endmenu",
            "",
            "endif",
            "endmenu"
        )

        val menu = EspressifMenu(EspressifMenuNullElement(), "menu Bluetooth", sourcesList, readFile)
        var parser: EspressifMenuParser = menu
        lines.forEach {
            parser = parser.addLine(it)
        }
        assertThat(parser, instanceOf(EspressifMenuNullElement::class.java))
        assertThat(menu.elements.map { it.name }, contains("USING_SPIFFS" ))
        val menuConfig = menu.elements[0] as EspressifMenuConfig
        assertThat(menuConfig.elements.map { it.name }, containsInAnyOrder("SPIFFS_MAX_PARTITIONS","Debug Configuration" ))
    }
}
package it.achdjian.plugin.espparser

import it.achdjian.plugin.esp32.configurator.SourceList
import java.io.File

class EspressifIf(
    val parent: EspressifMenuParser, line: String, val sourcesList: SourceList,
    val readFile: ReadFile
) : EspressifMenuParser {
    val condition: Expression
    val elements = mutableListOf<EspressifMenuElement>()
    val subIf = mutableListOf<EspressifIf>()

    init {
        condition = ExpressionParser(line.trim().substring(2)).expresison
    }

    override fun addLine(line: String): EspressifMenuParser {
        val trimmedLine = line.trim()
        when {
            trimmedLine.startsWith("#") -> return this
            trimmedLine.isEmpty() -> return this
            trimmedLine.startsWith("config") -> {
                val config = EspressifConfig(this, trimmedLine, sourcesList, readFile)
                elements.add(config)
                config.dependsOn.add(condition)
                return config
            }
            trimmedLine.startsWith("menu ") -> {
                val subMenu = EspressifMenu(this, trimmedLine, sourcesList, readFile)
                elements.add(subMenu)
                subMenu.dependsOn.add(condition)
                return subMenu
            }
            trimmedLine.startsWith("menuconfig ") -> {
                val subMenu = EspressifMenuConfig(this, trimmedLine, sourcesList, readFile)
                elements.add(subMenu)
                subMenu.dependsOn.add(condition)
                return subMenu
            }
            trimmedLine.startsWith("choice") -> {
                val configName = trimmedLine.substring(6).trim()
                val choice = EspressifChoice(this, configName, emptyList(), sourcesList, readFile)
                elements.add(choice);
                return choice
            }
            trimmedLine.startsWith("endif") -> {
                when (parent) {
                    is EspressifIf -> {
                        elements.forEach {
                            it.dependsOn.add(parent.condition)
                        }
                        parent.elements.addAll(elements)
                    }
                    is EspressifMenu -> {
                        elements.forEach {
                            if (it is EspressifConfig)
                                parent.addConfig(it)
                            else
                                parent.elements.add(it)
                        }
                    }
                    is EspressifMenuConfig -> elements.forEach { parent.elements.add(it) }

                }
                return parent
            }
            trimmedLine.startsWith("if") -> {
                val ifElement = EspressifIf(this, trimmedLine, sourcesList, readFile)
                subIf.add(ifElement)
                return ifElement
            }
            else -> throw UnknownToken(line)
        }
    }
}
package it.achdjian.plugin.espparser

import java.io.File

class EspressifMenu(
    val parent: EspressifMenuParser,
    line: String,
    val sourcesList: Map<String, List<File>>,
    val readFile: ReadFile,
    override val dependsOn: MutableSet<Expression> = mutableSetOf()
) : EspressifMenuElement, EspressifOwningConfig {
    val internalName: String

    init {
        internalName = removeDoubleQuotes(line.substring(4).trim())
    }

    override val name: String
        get() = internalName

    override val valid: Boolean
        get() = true
    override val veriableDepending: List<String>
        get() = varDepending


    override val configs: List<EspressifConfig>
        get() {
            val configElements = elements.filter { it is EspressifConfig }.map { it as EspressifConfig }.toMutableList()
            configElements.addAll(elements.map { it.configs }.flatten())
            configElements.addAll(subMenus.map { it.configs }.flatten())

            return configElements
        }


    val allElements: List<EspressifMenuElement>
        get() {
            val elements = ArrayList(this.elements)
            subMenus.forEach { elements.addAll(it.allElements) }
            return elements
        }
    val elements = mutableListOf<EspressifMenuElement>()
    val menuConfigs = mutableMapOf<String, EspressifMenuConfig>()
    private val configElements = mutableListOf<EspressifConfig>()
    val subMenus = mutableListOf<EspressifMenu>()
    var visibleIf: Expression = emptyExpression
    private val varDepending = mutableListOf<String>()


    override fun addLine(line: String): EspressifMenuParser {
        val trimmedLine = line.trim()
        when {
            trimmedLine.startsWith("#") -> return this
            trimmedLine.isEmpty() -> return this
            trimmedLine == "config" || trimmedLine.startsWith("config ") -> {
                return EspressifConfig(this, trimmedLine)
            }
            trimmedLine == "menuconfig" || trimmedLine.startsWith("menuconfig ") -> {
                return EspressifMenuConfig(this, trimmedLine);
            }
            trimmedLine == "choice" || trimmedLine.startsWith("choice ") -> {
                val configName = trimmedLine.substring(6).trim()
                val choice = EspressifChoice(this, configName, configElements)
                elements.add(choice)
                return choice
            }
            trimmedLine.startsWith("menu ") -> {
                val subMenu = EspressifMenu(this, trimmedLine, sourcesList, readFile)
                subMenus.add(subMenu)
                return subMenu
            }
            trimmedLine == "endmenu" || trimmedLine.startsWith("endmenu ") -> {
                return parent
            }
            trimmedLine.startsWith("visible if ") -> {
                if (visibleIf !is emptyExpression) {
                    throw RuntimeException("Multiple visible option in  trimmedLine $trimmedLine")
                } else {
                    visibleIf = ExpressionParser(trimmedLine.substring(10)).expresison
                }
            }
            trimmedLine == "depends on" || trimmedLine.startsWith("depends on ") -> {
                dependsOn.add(ExpressionParser(trimmedLine.substring(10).trim()).expresison)
            }
            trimmedLine.startsWith("if") -> {
                return EspressifIf(this, trimmedLine, sourcesList, readFile)
            }
            trimmedLine.startsWith("source ") -> {
                val source = removeDoubleQuotes(trimmedLine.substring(6).trim())
                if (source.isNotEmpty() && source[0] == '$') {
                    val envVariable = source.substring(1)
                    val sources = sourcesList[envVariable] ?: throw InvalidEnvVariable(envVariable)
                    sources.forEach {
                        val lines = readFile.read(it)
                        if (lines[0].startsWith("menu ")) {
                            val subMenu = EspressifMenu(this, lines[0], sourcesList, readFile)
                            var parser: EspressifMenuParser = subMenu
                            lines.subList(1, lines.size).forEach { line ->
                                try {
                                    parser = parser.addLine(line)
                                } catch (e: Exception) {
                                    val message = "parsing ${it.path} got the error ${e.message}"
                                    throw RuntimeException(message, e)
                                }
                            }
                            subMenus.add(subMenu)
                        } else if (lines[0].startsWith("menuconfig ")) {
                            var parser: EspressifMenuParser = this
                            lines.forEach { line ->
                                try {
                                    parser = parser.addLine(line)
                                } catch (e: Exception) {
                                    val message = "parsing ${it.path} got the error ${e.message}"
                                    throw RuntimeException(message, e)
                                }
                            }
                        }

                    }

                } else throw InvalidSourceValue(source)
            }
            else -> {
                throw UnexpectedToken(line)
            }
        }
        return this
    }

    override fun addConfig(config: EspressifConfig) {
        var ownerMenuConfig = false
        config.veriableDepending.forEach {
            menuConfigs[it]?.let {
                it.elements.add(config)
                ownerMenuConfig = true
            }
        }
        if (!ownerMenuConfig) {
            elements.add(config)
            configElements.add(config)
            if (config is EspressifMenuConfig)
                menuConfigs[config.name] = config
        }
    }

}

class UnexpectedToken(line: String) : RuntimeException("Unexpected line: $line")

package it.achdjian.plugin.espparser

data class ChoiceConfig(val type: ConfigType, val name: String, val text: String) {}

class EspressifChoice(
    val parent: EspressifMenuParser,
    override val name: String,
    private val configElements: List<EspressifConfig>,
    override val dependsOn: MutableSet<Expression> = mutableSetOf()
) : EspressifMenuElement {
    override val veriableDepending: List<String>
        get() = varDepending
    override val valid: Boolean
        get() = true

    override val configs: List<EspressifConfig>
        get() = listConfig

    var prompt = ""
    val defaultList = ArrayList<Default>()
    var help = ""
    private val escapedHelp: String get() = help.replace("\"", "\\\"").replace("$", "\\$")
    private var helpSpaces = 0
    var helping = false
    val listConfig = mutableListOf<EspressifConfig>()
    val varDepending = ArrayList<String>()

    override fun addLine(line: String): EspressifMenuParser {
        val trimmedLine = line.trim()

        if (helping && trimmedLine.isNotEmpty()) {
            val spaces = line.indexOfFirst { it != ' ' }
            if (help.isEmpty()) {
                helpSpaces = spaces
            } else {
                if (spaces < helpSpaces) {
                    helping = false
                }
            }
        }
        when {

            helping -> {
                if (trimmedLine.isEmpty())
                    help += "\n"
                else
                    if (help.isEmpty())
                        help = trimmedLine
                    else
                        help += " " + trimmedLine
                return this
            }
            trimmedLine.startsWith("#") -> return this
            trimmedLine.startsWith("endchoice") -> return parent
            (trimmedLine.startsWith("prompt") || trimmedLine.startsWith("bool")) -> {
                val start = trimmedLine.indexOf("\"", 4)
                if (start > 0) {
                    val end = trimmedLine.indexOf("\"", start + 1)
                    prompt = trimmedLine.substring(start + 1, end)
                } else {
                    prompt = trimmedLine.substring(6)
                }
                prompt = prompt.trim()
                return this
            }
            trimmedLine.startsWith("default") -> {
                defaultList.add(Default(trimmedLine.substring(8).trim()))
                return this
            }
            trimmedLine.startsWith("help") -> {
                help = trimmedLine.substring(4).trim()
                helping = true
                helpSpaces = line.indexOfFirst { it != ' ' }
                return this
            }
            trimmedLine.startsWith("config") -> {
                val configElement = EspressifConfig(this, trimmedLine)
                listConfig.add(configElement)
                return configElement
            }
            trimmedLine.startsWith("depends on") -> {
                dependsOn.add(ExpressionParser(trimmedLine.substring(10).trim()).expresison)
                return this
            }
            else -> {
                if (trimmedLine.isNotEmpty() && trimmedLine[0] != '#')
                    throw ParsingChoiceException(line)
            }
        }
        return this
    }


}

class ParsingChoiceException(line: String) : RuntimeException("error parsing choice option:  $line")

package it.achdjian.plugin.espparser

import it.achdjian.plugin.esp32.entry_type.Value
import java.io.File

open class EspressifConfig(
    val parent: EspressifMenuParser,
    line: String

) : EspressifMenuElement {
    val internalName: String
    override val dependsOn = mutableSetOf<Expression>()
    val promptIf = mutableSetOf<Expression>()

    init {
        if (line.startsWith("config"))
            internalName = line.substring(6).trim()
        else if (line.startsWith("menuconfig"))
            internalName = line.substring(10).trim()
        else throw RuntimeException("Error parsing $line")
    }

    override val configs: List<EspressifConfig>
        get() = listOf(this)

    override val name: String
        get() = internalName

    override val veriableDepending: List<String>
        get() {
            return dependsOn.filter { it is SimpleExpression }.map { (it as SimpleExpression).value }.distinct()
        }

    override val valid: Boolean
        get() = multiDefault.isEmpty()

    var text = ""
    var help = ""
    var type = ConfigType.Undefined
    var select = mutableListOf<String>()

    val multiDefault = mutableListOf<Value>()
    var helpText = false
    private var helpSpaces = 0
    var min = Int.MIN_VALUE.toLong()
    var max = Int.MAX_VALUE.toLong()
    var envVariable: String = ""

    private val escapedHelp: String get() = help.replace("\"", "\\\"").replace("$", "\\$")


    private fun createBooleanConfigEntry(output: File) {
        output.appendText("BoolConfigEntry(\"${text}\", \"${name}\", \"${escapedHelp}\", $multiDefault[\"true\"]")
        output.appendText(dependsOn.toString())
        if (select.isNotEmpty()) {
            output.appendText(",associated=listOf(")
            output.appendText(select.joinToString())
            output.appendText(")")
        }
        output.appendText(")\n")
    }


    private fun parseBoolDefault(value: String): Boolean? {
        if (value == "\"y\"")
            return true
        if (value == "y")
            return true
        if (value == "\"Y\"")
            return true
        if (value == "Y")
            return true
        if (value == "\"n\"")
            return false
        if (value == "\"N\"")
            return false
        if (value == "n")
            return false
        if (value == "N")
            return false
        if (value == "F")
            return false
        if (value == "f")
            return false
        if (value == "T")
            return true
        if (value == "t")
            return true
        return null

    }


    private fun addText(trimmedLine: String) {
        val start = trimmedLine.indexOf('"')
        if (start > 1) {
            val end = trimmedLine.indexOf('"', start + 1)
            text = trimmedLine.substring(start + 1, end)
            val startIf = trimmedLine.indexOf("if", end + 1)
            if (startIf > 0) {
                val depending = trimmedLine.substring(startIf + 2).trim()

                dependsOn.add(ExpressionParser(depending).expresison)
            }
        }
    }

    override fun addLine(line: String): EspressifMenuParser {
        val trimmedLine = line.trim()
        if (helpText && trimmedLine.isNotEmpty()) {
            val spaces = line.indexOfFirst { it != ' ' && it !='\t'}
            if (spaces <= helpSpaces) {
                helpText = false
            }
        }
        when {
            trimmedLine.isNotEmpty() && trimmedLine[0] == '#' ->
                return this
            helpText -> {
                help += trimmedLine
            }

            trimmedLine.startsWith("string") -> {
                type = ConfigType.String
                addText(trimmedLine)
            }
            trimmedLine.startsWith("int") -> {
                type = ConfigType.Int
                addText(trimmedLine)
            }
            trimmedLine.startsWith("hex") -> {
                type = ConfigType.Hex
                addText(trimmedLine)
            }
            trimmedLine.startsWith("bool") -> {
                type = ConfigType.Boolean
                addText(trimmedLine)
            }
            trimmedLine.startsWith("prompt") -> {
                text = trimmedLine.substring(6).trim()
                if (text.isNotEmpty()) {
                    if (text[0] == '"') {
                        val endText = text.indexOf('\"', 1)
                        val otherText = text.substring(endText + 1).trim()
                        text = text.substring(1, endText)
                        if (otherText.startsWith("if")) {
                            promptIf.add(ExpressionParser(otherText.substring(2).trim()).expresison)
                        }
                    }
                }
            }
            trimmedLine == "select" || trimmedLine.startsWith("select ") -> {
                val variableName = trimmedLine.substring(6).trim()
                select.add(variableName)
            }
            trimmedLine == "depends on" || trimmedLine.startsWith("depends on ") -> {
                dependsOn.add(ExpressionParser(trimmedLine.substring(10).trim()).expresison)
            }
            trimmedLine == "default" || trimmedLine.startsWith("default ") -> {
                val value = trimmedLine.substring(7).trim()
                var end: Int
                if (value[0] == '\"') {
                    end = value.indexOf("\"", 1)
                } else {
                    end = value.indexOf(" ", 1)
                    if (end < 0)
                        end = value.length - 1
                }
                val default = value.substring(0, end + 1)
                val ifString = value.substring(end + 1).trim()
                val ifCase = if (ifString.startsWith("if")) {
                    val parser = ExpressionParser(ifString.substring(3).trim())
                    parser.expresison
                } else {
                    emptyExpression
                }
                multiDefault.add(Value(ExpressionParser(removeDoubleQuotes(default.trim())).expresison, ifCase))
            }
            trimmedLine.startsWith("range ") -> {
                val tokens = trimmedLine.split(Regex("\\s+"))
                min = tokens[1].toLong()
                max = tokens[2].toLong()
            }
            trimmedLine == "help" || trimmedLine.startsWith("help ") -> {
                help = trimmedLine.substring(4).trim()
                helpText = true
                helpSpaces = line.indexOfFirst { it != ' ' }
            }
            trimmedLine.startsWith("option") -> {
                val optionValue = trimmedLine.substring(6).trim()
                if (optionValue.startsWith("env=")) {
                    envVariable = removeDoubleQuotes(optionValue.substring(4).trim())
                    if (envVariable=="IDF_TARGET"){
                        multiDefault.add(Value(SimpleExpression("esp32"),emptyExpression ))
                    }
                }
            }
            trimmedLine.startsWith("endchoice")->
                return parent.addLine(line)
            trimmedLine.startsWith("choice")->
                return parent.addLine(line)
            trimmedLine.isEmpty() -> return this
            else -> {
                if (parent is EspressifOwningConfig)
                    parent.addConfig(this)
                return parent.addLine(line)
            }
        }
        return this
    }

}


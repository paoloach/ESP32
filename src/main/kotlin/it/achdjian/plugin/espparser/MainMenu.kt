package it.achdjian.plugin.espparser

import java.io.File
import java.lang.RuntimeException
import java.util.*
import kotlin.collections.HashMap

class MainMenu(lines: List<String>, sourcesList: Map<String, List<File>>, readFile: ReadFile) : EspressifMenuParser {
    var prompt = ""
    val menuElements = mutableMapOf<String, EspressifMenuElement>()
    var sourcesList = mapOf<String, List<File>>()
    var readFile = ReadFile()

    init {

        parse(lines, sourcesList, readFile)
    }

    private fun parse(lines: List<String>, sourcesList: Map<String, List<File>>, readFile: ReadFile) {
        this.readFile = readFile
        this.sourcesList = sourcesList
        var parser: EspressifMenuParser=this
        lines.forEach {
            parser = parser.addLine(it)
        }
    }

    override fun addLine(line: String): EspressifMenuParser {
        val trimmedLine = line.trim()
        when {
            trimmedLine.startsWith("#") -> return this
            trimmedLine.isEmpty() -> return this
            trimmedLine.startsWith("mainmenu") -> {
                prompt = trimmedLine.substring(8).trim()
                if (prompt.isNotEmpty() && prompt[0] == '"') {
                    prompt = prompt.substring(1, prompt.length - 1)
                }
                return this
            }
            trimmedLine.startsWith("choice")  -> {
                val choice = EspressifChoice(this, trimmedLine, mutableListOf<EspressifConfig>())
                menuElements[choice.name] = choice
                return choice
            }
            trimmedLine.startsWith("config")  -> {
                val config = EspressifConfig(this, trimmedLine)
                menuElements[config.name] = config
                return config
            }
            trimmedLine.startsWith("menuconfig")  -> {
                val config = EspressifMenuConfig(this, trimmedLine)
                menuElements[config.name] = config
                return config
            }
            trimmedLine.startsWith("menu")  -> {
                val menu = EspressifMenu(this, trimmedLine, sourcesList, readFile)
                menuElements[menu.name] = menu
                return menu
            }
            trimmedLine.startsWith("source")  -> {
                val source = removeDoubleQuotes(trimmedLine.substring(6).trim())
                if (source.isNotEmpty() && source[0] == '$') {
                    val envVariable = source.substring(1)
                    val sources = sourcesList[envVariable] ?: throw InvalidEnvVariable(envVariable)
                    var parser: EspressifMenuParser = this
                    sources.forEach {
                        val fileLines = readFile.read(it)

                        try {
                            fileLines.forEach {
                                parser = parser.addLine(it)
                            }
                        } catch (e: Exception) {
                            val message = "parsing ${it.path} got the error ${e.message}"
                            throw RuntimeException(message, e)
                        }
                    }

                } else throw InvalidSourceValue(source)
            }
            else -> throw UnknownToken(line)
        }
        return this
    }
}

class UnknownToken(line: String) : RuntimeException("Found unknow token: $line") {

}

class InvalidEnvVariable(val envVariable: String) : Exception()

class InvalidSourceValue(val source: String) : Exception(source)

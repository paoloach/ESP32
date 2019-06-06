package it.achdjian.plugin.espparser

import java.io.File

class EspressifIf(val parent:EspressifMenuParser, line:String, val sourcesList: Map<String, List<File>>,
                  val readFile: ReadFile ) :EspressifMenuParser {
    val condition: Expression
    val elements = mutableListOf<EspressifMenuElement>()
    val subIf= mutableListOf<EspressifIf>()
    init {
        condition = ExpressionParser(line.trim().substring(2)).expresison
    }

    override fun addLine(line: String): EspressifMenuParser {
        val trimmedLine = line.trim()
        when {
            trimmedLine.startsWith("#")->return this
            trimmedLine.isEmpty()->return this
            trimmedLine.startsWith("config")->{
                val config=EspressifConfig(this, trimmedLine)
                elements.add(config)
                config.dependsOn.add(condition)
                return config
            }
            trimmedLine.startsWith("menu")->{
                val subMenu = EspressifMenu(this, trimmedLine, sourcesList, readFile)
                elements.add(subMenu)
                subMenu.dependsOn.add(condition)
                return subMenu
            }
            trimmedLine.startsWith("endif")->{
                if (parent is EspressifIf){
                    elements.forEach{
                        it.dependsOn.add(parent.condition)
                    }
                    parent.elements.addAll(elements)
                } else if (parent is EspressifMenu ){
                    elements.forEach{
                        if(it is EspressifConfig)
                            parent.addConfig(it)
                        else
                            parent.elements.add(it)
                    }

                }
                return parent
            }
            trimmedLine.startsWith("if")->{
                val ifElement = EspressifIf(this,  trimmedLine, sourcesList, readFile)
                subIf.add(ifElement)
                return ifElement
            }
            else->throw UnknownToken(line)
        }
    }
}
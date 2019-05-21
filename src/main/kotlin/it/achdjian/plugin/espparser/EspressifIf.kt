package it.achdjian.plugin.espparser

class EspressifIf(val parent:EspressifMenuParser, line:String) :EspressifMenuParser {
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
                val ifElement = EspressifIf(this,  trimmedLine)
                subIf.add(ifElement)
                return ifElement
            }
            else->throw UnknownToken(line)
        }
    }
}
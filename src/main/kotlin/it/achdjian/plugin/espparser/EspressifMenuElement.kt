package it.achdjian.plugin.espparser


interface EspressifMenuParser {
    fun addLine(line:String):EspressifMenuParser
}

interface EspressifOwningConfig  {
    fun addConfig(config: EspressifConfig)
}

interface EspressifMenuElement : EspressifMenuParser{
    val configs: List<EspressifConfig>
    val dependsOn: MutableSet<Expression>
    val name:String
    val valid: Boolean
    val veriableDepending: List<String>
}

class EspressifMenuNullElement(override val dependsOn: MutableSet<Expression> = mutableSetOf()) : EspressifMenuElement{
    override val configs: List<EspressifConfig>
        get() = listOf()

    override val name: String
        get() = "NO_NAME"
    override val veriableDepending: List<String>
        get() = emptyList()
    override val valid: Boolean
        get() = false

    override fun addLine(line: String) :EspressifMenuParser{
        val trimmedLine = line.trim()
        if (trimmedLine.isNotEmpty() && !trimmedLine.startsWith("#"))
            throw RuntimeException("Unable to add the trimmedLine $trimmedLine to the NullElement")
        else
            return this
    }

}
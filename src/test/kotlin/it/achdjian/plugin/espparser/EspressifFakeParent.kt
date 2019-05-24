package it.achdjian.plugin.espparser

class EspressifFakeParent:EspressifMenuElement {
    override fun addLine(line: String): EspressifMenuParser =  this

    override val configs: List<EspressifConfig>
        get() = listOf()
    override val dependsOn: MutableSet<Expression>
        get() = mutableSetOf()
    override val name: String
        get() = "Fake parent"
    override val valid: Boolean
        get() = true
    override val veriableDepending: List<String>
        get() = listOf()
}
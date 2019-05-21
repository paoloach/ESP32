package it.achdjian.plugin.espparser

class EspressifMenuConfig(parent:EspressifMenuParser, line:String) : EspressifConfig(parent, line) {
    val elements = ArrayList<EspressifMenuElement>()

    override val configs: List<EspressifConfig>
        get() {
            val list = mutableListOf<EspressifConfig>()
            elements.forEach {
                list.addAll(it.configs)
            }
            return list
        }
}
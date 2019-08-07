package it.achdjian.plugin.espparser

import it.achdjian.plugin.esp32.configurator.SourceList

class EspressifMenuConfig(
    parent: EspressifMenuParser, line: String,
    sourcesList: SourceList,
    readFile: ReadFile
) : EspressifConfig(parent, line, sourcesList, readFile) {
    val elements = ArrayList<EspressifMenuElement>()

    override val configs: List<EspressifConfig>
        get() {
            val list = mutableListOf<EspressifConfig>()
            elements.forEach {
                if (it is EspressifMenuConfig)
                    list.add(it)
                list.addAll(it.configs)
            }
            return list
        }
}
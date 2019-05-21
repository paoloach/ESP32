package it.achdjian.plugin.esp32.entry_type

import it.achdjian.plugin.espparser.EspressifConfig
import it.achdjian.plugin.espparser.Expression

class SubMenuEntry(
    text: String,
    val visibleIf: Expression,
    override val subMenu: List<ConfigurationEntry>
) :
    ConfigurationEntry(text, ""), MenuEntry {

    override val enabled: Boolean
        get() = eval(dependsOn) && eval(visibleIf)

    override fun addConfiguration(configurations: MutableList<Pair<String, String>>) {
        subMenu.forEach {
            it.addConfiguration(configurations)
        }
    }
}
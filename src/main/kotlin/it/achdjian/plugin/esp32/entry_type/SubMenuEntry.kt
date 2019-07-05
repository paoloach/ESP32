package it.achdjian.plugin.esp32.entry_type

import it.achdjian.plugin.espparser.Expression

class SubMenuEntry(
    text: String,
    private val visibleIf: Expression,
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

    override fun set(key: String, newValue: String) {
        subMenu.forEach { it.set(key, newValue) }
    }

    override fun addListenerToDepending(listener: (value: Boolean) -> Unit) {
        addListenerToDepending(dependsOn, listener)
        addListenerToDepending(visibleIf, listener)
    }
}
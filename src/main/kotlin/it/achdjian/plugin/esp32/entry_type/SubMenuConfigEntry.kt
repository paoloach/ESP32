package it.achdjian.plugin.esp32.entry_type

import it.achdjian.plugin.espparser.SimpleExpression

class SubMenuConfigEntry(
    val name: String,
    text: String,
    override val subMenu: List<ConfigurationEntry>,
    var values: List<Value>
) :
    ConfigurationEntry(text, ""), MenuEntry {
    val listeners = mutableListOf<(value: Boolean) -> Unit>()

    var value: Boolean
        get() {
            if (!enabled){
                return false
            }
            values.forEach {
                if (eval(it.condition)) {
                    return eval(it.value)
                }
            }
            return false
        }
        set(newVal) {
            values = listOf(Value(SimpleExpression(newVal.toString())))
            listeners.forEach { it(newVal) }
        }

    override fun addConfiguration(configurations: MutableList<Pair<String, String>>) {
        if (value)
            configurations.add(Pair(name, "1"))
        subMenu.forEach {
            it.addConfiguration(configurations)
        }
    }
}
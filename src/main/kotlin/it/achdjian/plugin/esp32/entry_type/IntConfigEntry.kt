package it.achdjian.plugin.esp32.entry_type

import it.achdjian.plugin.espparser.SimpleExpression

open class IntConfigEntry(
    text: String,
    configEntry: String,
    description: String,
    values: List<Value>,
    val min: Long,
    val max: Long
) : SdkConfigEntry(text, description, configEntry, values) {

    protected val listeners = ArrayList<(value: Int) -> Unit>()

    var value: Int
        get() {
            values.forEach {
                if (eval(it.condition)) {
                    return eval(it.value)
                }
            }
            return 0
        }
        set(newVal) {
            values = listOf(Value(SimpleExpression(newVal.toString())))
            listeners.forEach { it(newVal) }
        }


    override fun set(newValue: String) {
        value = newValue.toInt()
    }

    fun addListener(listener: (value: Int) -> Unit) {
        listeners.add(listener)
    }


    override fun addConfiguration(configurations: MutableList<Pair<String, String>>) {
        if (enabled) {
            configurations.add(Pair(configEntry, value.toString()))
        }
    }


}

class HexConfigEntry(
    text: String,
    configEntry: String,
    description: String,
    values: List<Value>,
    min: Long,
    max: Long
) : IntConfigEntry(text, configEntry, description, values, min, max) {

    override fun addConfiguration(configurations: MutableList<Pair<String, String>>) {
        if (enabled) {
            configurations.add(Pair(configEntry, "0x" + value.toString(16)))
        }
    }

    override fun set(newValue: String) {
        value = newValue.toInt(16)
    }
}



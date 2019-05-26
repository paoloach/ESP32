package it.achdjian.plugin.esp32.entry_type

import it.achdjian.plugin.espparser.SimpleExpression


open class StringConfigEntry(text: String, configEntry: String, description: String, values: List<Value>) :
    SdkConfigEntry(text, description, configEntry, values) {


    var value: String
        get() {
            values.forEach {
                if (eval(it.condition)) {
                    try {
                        return eval(it.value)
                    } catch (e: Exception) {
                        throw RuntimeException("Unable to evaluate ${it.value} for $text", e)
                    }
                }
            }
            return ""
        }
        set(newVal) {
            values = listOf(Value(SimpleExpression(newVal)))
        }


    override fun set(newValue: String) {
        value = newValue
    }

    override fun addConfiguration(configurations: MutableList<Pair<String, String>>) {
        if (enabled) {
            configurations.add(Pair(configEntry, "\"$value\""))
        }
    }

}



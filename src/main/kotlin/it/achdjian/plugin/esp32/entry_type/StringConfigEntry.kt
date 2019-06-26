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
            val value = SimpleExpression(newVal)
            if (values.size==1)
                values = listOf(Value(value))
//            else {
//                values
//                        .find { it.value == value }
//                        ?.let {
//                            if (it.condition is SimpleExpression){
//
//                            }
//                        }
//            }
        }


    override fun set(key: String, newValue: String) {
        if (key == configEntry)
            value = newValue
    }

    override fun addConfiguration(configurations: MutableList<Pair<String, String>>) {
        if (enabled) {
            configurations.add(Pair(configEntry, "\"$value\""))
        }
    }

}



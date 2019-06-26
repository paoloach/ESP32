package it.achdjian.plugin.esp32.entry_type

import it.achdjian.plugin.esp32.entry_type.ConfigElements.configElements
import it.achdjian.plugin.espparser.Expression
import it.achdjian.plugin.espparser.SimpleExpression

class ChoiceConfigEntry(
    text: String,
    configEntry: String,
    description: String,
    var choices: List<BoolConfigEntry>,
    default: List<Value>,
    dependsOn: Expression
) : SdkConfigEntry(text, description, configEntry, default) {

    override fun set(key:String, newValue: String) {
        choices.forEach { it.set(key, "n") }
        choices.forEach { it.set(key, newValue) }
        values = choices.firstOrNull{ it.value} ?.let { listOf(Value(SimpleExpression(it.configEntry))) } ?: values
    }

    var choiced: BoolConfigEntry? = null

    init {
        this.dependsOn = dependsOn
        if (enabled) {
            val def = default
            choices.forEach {
                it.value = it == def
            }
        } else {
            choices.forEach { it.value = false }
        }
        choices.forEach { it.choiceConfigEntry = this }
    }

    val default: BoolConfigEntry?
        get() {
            values.forEach {
                if (eval(it.condition)) {
                    if (it.value is SimpleExpression) {
                        val configName = it.value.toString()
                        val boolConfig = configElements[configName] ?: return null
                        if (boolConfig is BoolConfigEntry)
                            return boolConfig
                        else {
                            throw RuntimeException("default value has to be an boolean but it is ${boolConfig.javaClass.name}")
                        }
                    } else {
                        throw RuntimeException("default value has to be an Simple Expression but it is ${it.value}")
                    }
                }
            }
            return null
        }


    override fun isConfig(configKey: String): Boolean {
        return configEntry == configKey || choices.any { it.isConfig(configKey) }
    }


    override fun addConfiguration(configurations: MutableList<Pair<String, String>>) {
        if (enabled) {
            choiced?.addConfiguration(configurations)
        }
    }

}



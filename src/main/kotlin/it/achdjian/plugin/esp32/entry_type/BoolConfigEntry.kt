package it.achdjian.plugin.esp32.entry_type

import it.achdjian.plugin.espparser.Expression
import it.achdjian.plugin.espparser.SimpleExpression
import java.awt.event.ItemEvent
import java.awt.event.ItemListener

open class BoolConfigEntry(
    text: String,
    configEntry: String,
    description: String,
    values: List<Value>
) : SdkConfigEntry(text, description, configEntry, values), ItemListener {


    val forceTrueBy = mutableListOf<Expression>()

    private val listeners = ArrayList<(value: Boolean) -> Unit>()

    var value: Boolean
        get() {
            forceTrueBy.firstOrNull { eval(it) }?.let { return true }
            if (!enabled) {
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
            if (newVal) {

            }
        }

    override fun set(newValue: String) {
        value = newValue == "y"
    }


    override fun itemStateChanged(event: ItemEvent) {
        value = event.stateChange == ItemEvent.SELECTED
//        associated.forEach { it.value = value }
    }


    fun addListener(listener: (value: Boolean) -> Unit) {
        listeners.add(listener)
    }

    override fun addConfiguration(configurations: MutableList<Pair<String, String>>) {
        if (value and enabled) {
            configurations.add(Pair(configEntry, "1"))
            //associated.forEach { it.configEntry.forEach { ce -> configurations[ce] = "1" } }
        }
    }
}




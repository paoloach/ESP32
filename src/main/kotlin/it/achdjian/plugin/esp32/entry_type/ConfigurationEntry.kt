@file:Suppress("UNCHECKED_CAST")

package it.achdjian.plugin.esp32.entry_type

import it.achdjian.plugin.esp32.entry_type.ESP32ConfigElements.esp32configElements
import it.achdjian.plugin.espparser.*
import kotlin.reflect.KClass


abstract class ConfigurationEntry(val text: String, val description: String) {
    var dependsOn: Expression = emptyExpression

    open val enabled: Boolean
        get() = eval(dependsOn)

    open fun addListenerToDepending(listener: (value: Boolean) -> Unit) =
        addListenerToDepending(dependsOn, listener)

    abstract fun addConfiguration(configurations: MutableList<Pair<String, String>>)
    abstract fun set(key:String, newValue:String)

}


interface MenuEntry {
    val subMenu: List<ConfigurationEntry>
}

fun addListenerToDepending(
    expression: Expression,
    listener: (value: Boolean) -> Unit
) {
    when (expression) {
        is SimpleExpression -> {
            esp32configElements[expression.text]?.let {
                when (it) {
                    is BoolConfigEntry -> it.addListener(listener)
                    is IntConfigEntry -> {
                        // it.addListener ( listener )
                    }
                    else -> throw RuntimeException("Only to bool config it is possibile add a listener but ${it.configEntry} is type ${it::class.java.name}")
                }
            }
        }
        is NotOper -> addListenerToDepending(expression.expression, listener)
        is AndOper -> {
            addListenerToDepending(expression.left, listener)
            addListenerToDepending(expression.right, listener)
        }
        is OrOper -> {
            addListenerToDepending(expression.left, listener)
            addListenerToDepending(expression.right, listener)
        }
        is GTOper -> {
            addListenerToDepending(expression.left, listener)
            addListenerToDepending(expression.right, listener)
        }
        is EqualOper -> {
            addListenerToDepending(expression.left, listener)
            addListenerToDepending(expression.right, listener)
        }
        is NotEqualOper -> {
            addListenerToDepending(expression.left, listener)
            addListenerToDepending(expression.right, listener)
        }
    }
}

data class Value(val value: Expression, val condition: Expression) {
    constructor(value: Expression) : this(value, emptyExpression)
}

abstract class SdkConfigEntry(
    text: String,
    description: String,
    val configEntry: String,
    var values: List<Value>
) :
    ConfigurationEntry(text, description) {
    var promptIf: Expression = emptyExpression

    open fun isConfig(configKey: String) = configEntry == configKey
}

fun <T : Any> convertString(clazz: KClass<T>, value: String): T {
    return when {
        (clazz == String::class) -> value as T
        (clazz == Boolean::class) -> {
            convertoToBoolean(value) as T
        }
        (clazz == Int::class) -> {
            if (value.startsWith("0x")) {
                value.substring(2).toInt(16) as T
            } else {
                value.toInt() as T
            }
        }
        else -> value as T
    }
}

fun convertoToBoolean(value: String): Boolean {
    return (value == "y"
            || value == "Y"
            || value == "1"
            || value == "t"
            || value == "T"
            || value == "true"
            || value == "TRUE")
}

fun <T : Any> convertSimpleExpression(
    clazz: KClass<T>,
    expression: SimpleExpression,
    configElements: Map<String, SdkConfigEntry>
): T {
    configElements[expression.text]?.let {
        return when (it) {
            is BoolConfigEntry -> it.value as T
            is StringConfigEntry -> {
                val value = it.value
                if (value.isEmpty())
                    "" as T
                else
                    it.value as T
            }
            is IntConfigEntry -> it.value as T
            is HexConfigEntry -> it.value as T
            else -> throw RuntimeException("Uknown ConfigEntry type: ${it.javaClass.name}")
        }

    } ?: return convertString(clazz, expression.text)

}

fun evalBool(expression: Expression): Boolean {
    return eval(expression)
}

fun evalInt(expression: Expression): Int {
    return eval(expression)
}

fun evalString(expression: Expression): String {
    return eval(expression)
}

inline fun <reified T : Any> eval(expression: Expression): T {
    return eval(T::class, expression)
}

fun <T : Any> eval(clazz: KClass<T>, expression: Expression): T {
    when (expression) {
        is emptyExpression -> {
            return when (clazz) {
                Boolean::class -> true as T
                String::class -> "" as T
                Int::class -> 0 as T
                else -> throw RuntimeException("Unable to convert emptyExpression to ${clazz.simpleName}")
            }
        }
        is EnvironmentExpression -> {
            esp32configElements[expression.text]?.let {

                return it.text as T
            } ?: return "" as T
        }
        is SimpleExpression -> return convertSimpleExpression(clazz, expression, esp32configElements)
        is NotOper -> {
            return (!evalBool(expression.expression)) as T
        }
        is AndOper -> {
            return (evalBool(expression.left) && evalBool(expression.right)) as T
        }
        is OrOper -> {
            return (evalBool(expression.left) || evalBool(expression.right)) as T
        }
        is GTOper -> {
            return (evalInt(expression.left) > evalInt(expression.right)) as T
        }
        is GTEOper -> {
            return (evalInt(expression.left) >= evalInt(expression.right)) as T
        }
        is LTOper -> {
            return (evalInt(expression.left) < evalInt(expression.right)) as T
        }
        is LTEOper -> {
            return (evalInt(expression.left) <= evalInt(expression.right)) as T
        }
        is EqualOper -> {
            if (expression.left::class != expression.right::class) {
                return false as T
            }
            return (evalString(expression.left) == evalString(expression.right)) as T
        }
        is NotEqualOper -> {
            if (expression.left::class != expression.right::class) {
                return true as T
            }
            return (evalString(expression.left) != evalString(expression.right)) as T
        }
        else -> {
            throw RuntimeException("Undefined expression: ${expression.javaClass.name}")
        }
    }
}

object ESP32ConfigElements {
    var esp32configElements = mapOf<String, SdkConfigEntry>()
}

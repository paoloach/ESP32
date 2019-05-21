package it.achdjian.plugin.espparser

import java.lang.RuntimeException
import java.util.*


fun removeDoubleQuotes(value:String):String{
    if(value.isNotEmpty() && value[0] == '"'){
        return value.substring(1, value.length-1)
    } else
        return value
}

fun getVariableName(name: String): String {
    if (name.isEmpty())
        return name
    var variableName = name.replace("(","_").replace(")","_").split("_").map { it.toLowerCase() }.joinToString(separator = "") { it.capitalize() }
    variableName = variableName[0].toLowerCase().toString() + variableName.substring(1)
    return variableName
}


fun parseDependsOn(line: String, dependsOn: MutableList<String>): String {
    if (line.isEmpty()) {
        return "Fixed(true)"
    }

    var bracketCount = 0
    var bracketContent = ""
    var bracketOn = false
    var token = ""
    var tokens = Stack<String>()



    (line + " ").forEach {
        when {
            it == '(' -> {
                bracketCount++;
                bracketOn = true
            }
            it == ')' -> {
                if (bracketCount == 1) {
                    bracketOn = false
                    tokens.push(parseDependsOn(bracketContent, dependsOn))
                    applyOper(tokens, line)
                    bracketContent = ""
                } else {
                    bracketCount--
                }
            }
            bracketOn ->
                bracketContent += it
            it == '!' -> {
                tokens.push("!")
                token = ""
            }
            it == ' ' && token.isNotEmpty() -> {
                val operator = token
                dependsOn.add(operator)
                tokens.push(operator)
                applyOper(tokens, line)
                token = ""
            }
            else -> {
                token += it
            }
        }

    }
    if (tokens.size > 1) {
        throw RuntimeException("Parse error for line $line")
    }
    return tokens.pop()
}


fun applyOper(tokens: Stack<String>, line: String) {
    if (tokens.empty()) {
        throw RuntimeException("No operation available: line $line")
    }

    while (tokens.size > 1 && !isOperation(tokens.peek())) {
        val operator = tokens.pop()
        val previousToken = tokens.peek()
        when (previousToken) {
            "!" -> {
                tokens.pop()
                tokens.push("Not($operator)")
            }
            "&&" -> {
                tokens.pop()
                val leftOper = tokens.pop()
                val newToken = "And($leftOper, $operator)"
                tokens.push(newToken)
            }
            "||" -> {
                tokens.pop()
                val leftOper = tokens.pop()
                val newToken = "Or($leftOper, $operator)"
                tokens.push(newToken)
            }
            ">=" -> {
                tokens.pop()
                val leftOper = tokens.pop()
                val newToken = "GreatEqual($leftOper, $operator)"
                tokens.push(newToken)
            }
            else -> throw RuntimeException("Expected operation but got $previousToken. Entire line is $line")
        }
    }
}

fun isOperation(token: String) = token == "&&" || token == "||" || token == ">=" || token == "!"


fun calcDepends(dependsOn: List<String>): String {
    if (dependsOn.isEmpty())
        return ",NULL_DEPEND"
    else if (dependsOn.size == 1) {
        return "," + dependsOn[0]
    } else {
        return ",And(" + dependsOn.joinToString { it } + ")"
    }
}
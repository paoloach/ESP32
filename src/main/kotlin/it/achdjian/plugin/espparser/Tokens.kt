package it.achdjian.plugin.espparser

open class Token(val text:String)

class ValueToken(text:String) : Token(text)
class OpenBracket:Token("(")
class CloseBracket:Token(")")

open class Operator(text:String): Token(text)

open class UnaryOperatorToken(text:String): Operator(text)

class NotOperatorToken: UnaryOperatorToken("!")

open class BinaryOperator(text:String): Operator(text)

class AndOperator: BinaryOperator("&&")

class OrOperator: BinaryOperator("||")
class EqualOperator: BinaryOperator("==")
class GtOperator: BinaryOperator(">")
class GteOperator: BinaryOperator(">=")
class LtOperator: BinaryOperator("<")
class LteOperator: BinaryOperator("<=")

class InvalidTokeException(invalidToken:String) : RuntimeException("Invalid token: $invalidToken")


fun operatorSecondChar(c: Char, prevOper: Char, result: MutableList<Token>) : Boolean{
    when(prevOper){
        '=' -> {
            when(c){
                '=' -> result.add(EqualOperator())
                '>' -> result.add(GteOperator())
                '<' -> result.add(LteOperator())
                else -> {
                    result.add(EqualOperator())
                    return false
                }
            }
            return true
        }
        '>' -> {
            if (c == '=') {
                result.add(GteOperator())
                return true
            } else {
                result.add(GtOperator())
                return false
            }
        }
        '<' -> {
            if (c == '=') {
                result.add(LteOperator())
                return true
            } else {
                result.add(LtOperator())
                return false
            }
        }
        '&' -> {
            if (c == '&'){
                result.add(AndOperator())
                return true
            } else {
                throw InvalidTokeException("$prevOper$c")
            }
        }
        '|' -> {
            if (c == '|'){
                result.add(OrOperator())
                return true
            } else {
                throw InvalidTokeException("$prevOper$c")
            }
        }
        else -> throw InvalidTokeException("$prevOper$c")
    }
}


fun tokenize(expr: String): List<Token> {
    val result: MutableList<Token> = mutableListOf()
    var prevOper = ' '
    var tokenOper = false
    var varName =""
    var comment=false

    ("$expr ").forEach {
        if (!comment) {
            var consumed = false
            if (tokenOper) {
                consumed = operatorSecondChar(it, prevOper, result)
                tokenOper=false
            }


            if (!consumed)
                when (it) {
                    ' ', '\t' -> {
                        if (varName.isNotEmpty()) {
                            result.add(ValueToken(varName))
                            varName = ""
                        }
                    }
                    '!' -> {
                        if (varName.isNotEmpty()) {
                            result.add(ValueToken(varName))
                            varName = ""
                        }
                        result.add(NotOperatorToken())
                    }
                    '(' -> {
                        if (varName.isNotEmpty()) {
                            result.add(ValueToken(varName))
                            varName = ""
                        }
                        result.add(OpenBracket())
                    }
                    ')' -> {
                        if (varName.isNotEmpty()) {
                            result.add(ValueToken(varName))
                            varName = ""
                        }
                        result.add(CloseBracket())
                    }
                    '=', '>','<', '&', '|' -> {
                        prevOper = it
                        tokenOper = true
                        if (varName.isNotEmpty()) {
                            result.add(ValueToken(varName))
                            varName = ""
                        }
                    }
                    '#' -> {
                        comment = true
                    }
                    else -> {
                        varName += it
                    }
                }
            }
        }
    return result
    }


package it.achdjian.plugin.espparser

open class Token(val text: String)

class ValueToken(text: String) : Token(text)
class EnvironmentToken(text: String) : Token(text)
class OpenBracket : Token("(")
class CloseBracket : Token(")")

open class Operator(text: String) : Token(text)

open class UnaryOperatorToken(text: String) : Operator(text)

class NotOperatorToken : UnaryOperatorToken("!")

open class BinaryOperator(text: String) : Operator(text)

class AndOperator : BinaryOperator("&&")

class OrOperator : BinaryOperator("||")
class EqualOperator : BinaryOperator("==")
class NotEqualOperator : BinaryOperator("!=")
class GtOperator : BinaryOperator(">")
class GteOperator : BinaryOperator(">=")
class LtOperator : BinaryOperator("<")
class LteOperator : BinaryOperator("<=")

class InvalidTokeException(invalidToken: String) : RuntimeException("Invalid token: $invalidToken")


fun operatorSecondChar(c: Char, prevOper: Char, result: MutableList<Token>): Boolean {
    when (prevOper) {
        '=' -> {
            when (c) {
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
        '!' -> {
            return if (c == '='){
                        result.add(NotEqualOperator())
                        true
                    } else {
                        false;
                    }
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
            if (c == '&') {
                result.add(AndOperator())
                return true
            } else {
                throw InvalidTokeException("$prevOper$c")
            }
        }
        '|' -> {
            if (c == '|') {
                result.add(OrOperator())
                return true
            } else {
                throw InvalidTokeException("$prevOper$c")
            }
        }
        else -> throw InvalidTokeException("$prevOper$c")
    }
}

interface Tokenizer {
    fun newToken(c: Char): Tokenizer
}

class CommentTokenizer : Tokenizer {
    override fun newToken(c: Char): Tokenizer {
        return this
    }

}

class EnvironmentTokenizer(val result: MutableList<Token>, val previousTokenizer: Tokenizer) : Tokenizer {
    private enum class Status { END_BRACKET, BEGIN, END_WORLD }

    private var envName = ""

    private var status = Status.BEGIN
    override fun newToken(c: Char): Tokenizer {
        when (status) {
            Status.BEGIN -> {
                when (c) {
                    '(' -> status == Status.END_BRACKET
                    else -> {
                        envName+=c
                        status = Status.END_WORLD
                    }
                }
            }
            Status.END_BRACKET -> {
                if (c != ')') {
                    envName += c
                } else {
                    result.add(EnvironmentToken(envName))
                    return previousTokenizer
                }
            }
            Status.END_WORLD -> {
                if (c != ' ') {
                    envName += c
                } else {
                    result.add(EnvironmentToken(envName))
                    return previousTokenizer
                }
            }
        }

        return this
    }
}

class DoubleQuoteTokenizer(val parent: Tokenizer,val  result: MutableList<Token>) : Tokenizer {
    var text=""
    override fun newToken(c: Char): Tokenizer {
        return when(c){
            '\"' -> {
                result.add(SimpleExpression(text))
                parent
            }
            else -> {
                text += c
                this
            }
        }
    }

}

class NormalTokenizer(val result: MutableList<Token>) : Tokenizer {
    private var prevOper = ' '
    private var tokenOper = false
    private var varName = ""
    private val commentTokenizer = CommentTokenizer()
    override fun newToken(c: Char): Tokenizer {
        var consumed = false
        if (tokenOper) {
            consumed = operatorSecondChar(c, prevOper, result)
            tokenOper = false
        }


        if (!consumed)
            when (c) {
                '\"' ->{
                    return DoubleQuoteTokenizer(this, result)
                }
                ' ', '\t' -> {
                    if (varName.isNotEmpty()) {
                        result.add(ValueToken(varName))
                        varName = ""
                    }
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
                '=', '>', '<', '&', '|', '!' -> {
                    prevOper = c
                    tokenOper = true
                    if (varName.isNotEmpty()) {
                        result.add(ValueToken(varName))
                        varName = ""
                    }
                }
                '$' -> {
                    return EnvironmentTokenizer(result, this)
                }
                '#' -> {
                    return commentTokenizer
                }

                else -> {
                    varName += c
                }
            }
        return this
    }

}

fun tokenize(expr: String): List<Token> {
    val result: MutableList<Token> = mutableListOf()
    var tokenizer: Tokenizer = NormalTokenizer(result)

    ("$expr ").forEach {
        tokenizer = tokenizer.newToken(it)

    }
    return result
}


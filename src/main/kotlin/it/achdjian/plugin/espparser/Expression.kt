package it.achdjian.plugin.espparser


open class Expression(value: String) : Token(value)

open class SimpleExpression( value: String) : Expression(value) {
    override fun equals(other: Any?): Boolean = other is SimpleExpression && other.text == text
    override fun hashCode(): Int {
        return text.hashCode()
    }

    override fun toString(): String {
        return text
    }
}

class EnvironmentExpression(value: String) : SimpleExpression(value) {
    override fun toString(): String {
        if (text=="IDF_TARGET")
            return "esp32"
        else {
            return System.getenv(text) ?: ""
        }
    }
}

object emptyExpression : Expression("empty expression") {
    override fun equals(other: Any?): Boolean = other is emptyExpression
    override fun hashCode(): Int {
        return true.hashCode()
    }

    override fun toString(): String {
        return ""
    }
}

open class Operation : Expression("operation")

abstract class UnaryOperation(val expression: Expression) : Operation()

abstract class BinaryOperation(val left: Expression, val right: Expression) : Operation() {
    override fun hashCode(): Int {
        var result = left.hashCode()
        result = 31 * result + right.hashCode()
        return result
    }
}

class NotOper(expression: Expression) : UnaryOperation(expression) {

    override fun equals(other: Any?): Boolean = other is NotOper && other.expression == expression
    override fun hashCode(): Int {
        return expression.hashCode()
    }
}

class AndOper(left: Expression, right: Expression) : BinaryOperation(left, right) {
    override fun equals(other: Any?): Boolean = other is AndOper && other.left == left && other.right == other.right

}

class OrOper(left: Expression, right: Expression) : BinaryOperation(left, right) {
    override fun equals(other: Any?): Boolean = other is OrOper && other.left == left && other.right == other.right
}

class GTOper(left: Expression, right: Expression) : BinaryOperation(left, right) {
    override fun equals(other: Any?): Boolean = other is GTOper && other.left == left && other.right == other.right
}

class GTEOper(left: Expression, right: Expression) : BinaryOperation(left, right) {
    override fun equals(other: Any?): Boolean = other is GTOper && other.left == left && other.right == other.right
}

class LTOper(left: Expression, right: Expression) : BinaryOperation(left, right) {
    override fun equals(other: Any?): Boolean = other is GTOper && other.left == left && other.right == other.right
}

class LTEOper(left: Expression, right: Expression) : BinaryOperation(left, right) {
    override fun equals(other: Any?): Boolean = other is GTOper && other.left == left && other.right == other.right
}

class EqualOper(left: Expression, right: Expression) : BinaryOperation(left, right) {
    override fun equals(other: Any?): Boolean = other is EqualOper && other.left == left && other.right == other.right
}

class NotEqualOper(left: Expression, right: Expression) : BinaryOperation(left, right) {
    override fun equals(other: Any?): Boolean = other is NotEqualOper && other.left == left && other.right == other.right
    override fun toString() = "NotEqual from $left and $right"
}

class NotAvailableOperation : RuntimeException()
open class ParserError(message: String = "") : RuntimeException(message)
class NotMatchingBracket : ParserError("Not matching bracket")
class InvalidNotArgument(token: Token) : ParserError("Invalid not argument: ${token.text}")
class InvalidLeftArgument(token: Token) : ParserError("Invalid left argument: ${token.text}")
class InvalidRightArgument(token: Token) : ParserError("Invalid left argument: ${token.text}")
class InvalidExpressionError(token: Token) : ParserError("${token.text} is an invalid expression")

private fun extractInsideBracket(iter: ListIterator<Token>): List<Token> {
    val insideBracketList = mutableListOf<Token>()
    var bracketCount = 0
    while (iter.hasPrevious()) {
        val token = iter.next()
        when (token) {
            is OpenBracket -> bracketCount++
            is CloseBracket -> {
                if (bracketCount == 0) {
                    return insideBracketList
                } else {
                    bracketCount--
                }
            }
        }
        insideBracketList.add(token)
    }
    throw NotMatchingBracket()
}

fun List<Token>.applyUnary(): List<Token> {
    val result = mutableListOf<Token>()
    val iter = listIterator()

    while (iter.hasNext()) {
        when (val token = iter.next()) {
            is NotOperatorToken -> {
                val argument = iter.next()
                if (argument is Expression)
                    result.add(NotOper(argument))
                else
                    throw InvalidNotArgument(argument)
            }
            else -> result.add(token)
        }
    }

    return result
}

fun List<Token>.applyIntegerBinaryOperator(): List<Token> {
    val result = mutableListOf<Token>()
    val iter = listIterator()

    while (iter.hasNext()) {
        when (val token = iter.next()) {
            is GtOperator, is GteOperator, is LtOperator, is LteOperator, is EqualOperator, is NotEqualOperator -> {
                result.removeAt(result.lastIndex)
                iter.previous()
                val left = iter.previous()
                iter.next()
                iter.next()
                val right = iter.next()
                if (left !is Expression)
                    throw InvalidLeftArgument(left)
                if (right !is Expression)
                    throw InvalidRightArgument(right)
                when (token) {
                    is EqualOperator -> result.add(EqualOper(left, right))
                    is NotEqualOperator -> result.add(NotEqualOper(left, right))
                    is GtOperator -> result.add(GTOper(left, right))
                    is GteOperator -> result.add(GTEOper(left, right))
                    is LteOperator -> result.add(LTOper(left, right))
                    is LtOperator -> result.add(LTEOper(left, right))
                }

            }
            else -> result.add(token)
        }
    }

    return result
}

fun List<Token>.applyBooleanBinaryOperator(): List<Token> {
    val result = mutableListOf<Token>()
    val iter = listIterator()

    while (iter.hasNext()) {
        when (val token = iter.next()) {
            is AndOperator, is OrOperator -> {
                result.removeAt(result.lastIndex)
                iter.previous()
                val left = iter.previous()
                iter.next()
                iter.next()
                val right = iter.next()
                if (left !is Expression)
                    throw InvalidLeftArgument(left)
                if (right !is Expression)
                    throw InvalidRightArgument(right)
                when (token) {
                    is AndOperator -> result.add(AndOper(left, right))
                    is OrOperator -> result.add(OrOper(left, right))
                }
            }
            else -> result.add(token)
        }
    }

    return result
}

fun List<Token>.parse(): Expression {
    val tokenWithoutBracket = parseBracket()
        .applyUnary()
        .applyIntegerBinaryOperator()
        .applyBooleanBinaryOperator()

    if (tokenWithoutBracket.isEmpty())
        return emptyExpression
    if (tokenWithoutBracket.size > 1)
        throw ParserError()
    val expression = tokenWithoutBracket[0]
    if (expression !is Expression)
        throw InvalidExpressionError(expression)

    return expression
}


fun List<Token>.parseBracket(): List<Token> {
    val result = mutableListOf<Token>()

    val iter = listIterator()
    while (iter.hasNext()) {
        val token = iter.next()
        if (token is OpenBracket) {
            val expression = extractInsideBracket(iter).parse()
            result.add(expression)
        } else {
            result.add(token)
        }
    }
    return result
}


fun List<Token>.parseSimpleExpression() =
    map {
        if (it is ValueToken) {
            if (it.text.isNotEmpty() && it.text.startsWith('\"') && it.text.endsWith('\"')) {
                SimpleExpression(it.text.substring(1, it.text.length - 1))
            } else
                SimpleExpression(it.text)
        } else if (it is EnvironmentToken){
            EnvironmentExpression(it.text)
        } else
            it
    }

class ExpressionParser(expr: String) {
    val expresison: Expression

    init {
        try {
            expresison = parse(expr)
        } catch (e: NotAvailableOperation) {
            throw RuntimeException("Not available operation parsing line $expr", e)
        } catch (e: ParserError) {
            throw RuntimeException("Parser error for expression $expr", e)
        }
    }


    private fun parse(expr: String): Expression {
        if (expr.isEmpty()) {
            return emptyExpression
        }

        return tokenize(expr).parseSimpleExpression().parse()
    }
}

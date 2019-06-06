package it.achdjian.plugin.espparser

import java.lang.RuntimeException
import java.util.*


interface Expression {
    fun isValid(): Boolean
}

class SimpleExpression(val value: String) : Expression {
    override fun isValid(): Boolean = true
    override fun equals(other: Any?): Boolean = other is SimpleExpression && other.value == value
    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return value
    }
}

class UndefinedExpr : Expression {
    override fun isValid(): Boolean = false
    override fun equals(other: Any?): Boolean = other is UndefinedExpr
    override fun hashCode(): Int {
        return false.hashCode()
    }

    override fun toString(): String {
        return "Undefined expression"
    }
}

object emptyExpression : Expression {
    override fun isValid(): Boolean = true
    override fun equals(other: Any?): Boolean = other is emptyExpression
    override fun hashCode(): Int {
        return true.hashCode()
    }

    override fun toString(): String {
        return ""
    }
}


interface Operation : Expression {
    fun apply(tokens: Stack<Expression>)
}

class NotOper(val expression: Expression = UndefinedExpr()) : Operation {
    override fun isValid(): Boolean = expression.isValid()

    override fun apply(tokens: Stack<Expression>) {
        val oper = tokens.pop()
        val not = NotOper(oper)
        tokens.push(not)
    }

    override fun equals(other: Any?): Boolean  = other is NotOper && other.expression == expression
    override fun hashCode(): Int {
        return expression.hashCode()
    }
}

class AndOper(val left: Expression = UndefinedExpr(), val right: Expression = UndefinedExpr()) : Operation {
    override fun isValid(): Boolean = left.isValid() && right.isValid()

    override fun apply(tokens: Stack<Expression>) {
        if (tokens.size <2){
            throw ParserError()
        }
        val right = tokens.pop()
        val left = tokens.pop()
        val and = AndOper(left, right)
        tokens.push(and)
    }

    override fun equals(other: Any?): Boolean  = other is AndOper && other.left == left && other.right == other.right
    override fun hashCode(): Int {
        var result = left.hashCode()
        result = 31 * result + right.hashCode()
        return result
    }
}

class OrOper(val left: Expression = UndefinedExpr(), val right: Expression = UndefinedExpr()) : Operation {
    override fun isValid(): Boolean = left.isValid() && right.isValid()

    override fun apply(tokens: Stack<Expression>) {
        if (tokens.size <2){
            throw ParserError()
        }
        val right = tokens.pop()
        val left = tokens.pop()
        val or = OrOper(left, right)
        tokens.push(or)
    }

    override fun equals(other: Any?): Boolean  = other is OrOper && other.left == left && other.right == other.right

    override fun hashCode(): Int {
        var result = left.hashCode()
        result = 31 * result + right.hashCode()
        return result
    }
}

class GTOper(val left: Expression = UndefinedExpr(), val right: Expression = UndefinedExpr()) : Operation {
    override fun isValid(): Boolean = left.isValid() && right.isValid()

    override fun apply(tokens: Stack<Expression>) {
        if (tokens.size <2){
            throw ParserError()
        }
        val right = tokens.pop()
        val left = tokens.pop()
        val gt = GTOper(left, right)
        tokens.push(gt)
    }

    override fun hashCode(): Int {
        var result = left.hashCode()
        result = 31 * result + right.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean  = other is GTOper && other.left == left && other.right == other.right
}

class EqualOper(val left: Expression = UndefinedExpr(), val right: Expression = UndefinedExpr()) : Operation {
    override fun isValid(): Boolean = left.isValid() && right.isValid()

    override fun apply(tokens: Stack<Expression>) {
        if (tokens.size <2){
            throw ParserError()
        }
        val right = tokens.pop()
        val left = tokens.pop()
        val and = EqualOper(left, right)
        tokens.push(and)
    }

    override fun equals(other: Any?): Boolean  = other is EqualOper && other.left == left && other.right == other.right

    override fun hashCode(): Int {
        var result = left.hashCode()
        result = 31 * result + right.hashCode()
        return result
    }
}



class NotAvailableOperation : RuntimeException()
class ParserError : RuntimeException()

class ExpressionParser(expr: String) {
    private val tokens = Stack<Expression>()

    val expresison: Expression

    init {
        try {
            expresison = parse(expr)
        } catch (e: NotAvailableOperation) {
            throw RuntimeException("Not available operation parsing line $expr", e)
        } catch (e: ParserError) {
            throw RuntimeException("Parser error for expression $expr",  e)
        }
    }


    private fun parse(expr: String): Expression {
        if (expr.isEmpty()) {
            return emptyExpression
        }

        var bracketCount = 0
        var bracketContent = ""
        var bracketOn = false
        var token = ""
        var tokenOper=false
        var noOper = true
        var end=false

        ("$expr ").forEach {
            if (!end) {
                if (tokenOper) {
                    when (token) {
                        "=" -> {
                            tokens.push(EqualOper())
                            token = ""
                            noOper = true
                        }
                        ">" -> {
                            if (it == '=') {
                                noOper = false
                                tokens.push(GTOper())
                                token = ""
                            } else {
                                throw ParserError()
                            }
                        }
                        "&" -> {
                            if (it == '&') {
                                noOper = false
                                tokens.push(AndOper())
                                token = ""
                            } else {
                                throw ParserError()
                            }
                        }
                        "|" -> {
                            if (it == '|') {
                                noOper = false
                                tokens.push(OrOper())
                                token = ""
                            } else {
                                throw ParserError()
                            }
                        }
                        else -> throw ParserError()

                    }
                    tokenOper = false
                }
                if (noOper) {
                    when {
                        it == '(' -> {
                            bracketCount++
                            bracketOn = true
                        }
                        it == ')' -> {
                            if (bracketCount == 1) {
                                bracketOn = false
                                val parser = ExpressionParser(bracketContent)
                                tokens.push(parser.expresison)
                                applyOper()
                                bracketContent = ""
                                token = ""
                            } else {
                                bracketCount--
                            }
                        }
                        bracketOn ->
                            bracketContent += it
                        it == '!' -> {
                            tokens.push(NotOper())
                            token = ""
                        }
                        it == '=' || it == '>' || it == '&' || it == '|' -> {
                            if (token.trim().isNotEmpty()) {
                                tokens.push(SimpleExpression(removeDoubleQuotes(token.trim())))
                            }
                            token = it.toString()
                            tokenOper = true
                        }
                        it == ' ' && token.isNotEmpty() -> {
                            tokens.push(SimpleExpression(removeDoubleQuotes(token.trim())))
                            applyOper()
                            token = ""
                        }
                        it == '#' ->  end =true
                        else -> {
                            token += it
                        }
                    }

                } else {
                    noOper = true
                }
            }
        }
        if (tokens.size > 1) {
            throw ParserError()
        }
        return tokens.pop()
    }


    private fun applyOper() {
        if (tokens.empty()) {
            throw NotAvailableOperation()
        }

        while (tokens.size > 1 && tokens.peek().isValid()) {

            val operand = tokens.pop()
            val oper = tokens.pop() as? Operation ?: throw NotAvailableOperation()
            tokens.push(operand)
            oper.apply(tokens)
        }
    }


}
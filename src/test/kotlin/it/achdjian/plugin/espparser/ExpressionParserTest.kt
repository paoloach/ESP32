package it.achdjian.plugin.espparser

import org.hamcrest.CoreMatchers.`is` as Is
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.instanceOf
import org.hamcrest.Matchers.isEmptyString
import org.junit.jupiter.api.Test

internal class ExpressionParserTest {
    @Test
    fun empty() {
        val parser = ExpressionParser("")

        assertThat(parser.expresison, instanceOf(emptyExpression::class.java) )
    }

    @Test
    fun simplyName() {
        val parser = ExpressionParser("A_CONFIG")

        assertThat(parser.expresison, instanceOf(SimpleExpression::class.java) )

        val simpleExpression = parser.expresison as SimpleExpression

        assertThat(simpleExpression.value, Is("A_CONFIG"))
    }

    @Test
    fun NotSimplyName() {
        val parser = ExpressionParser("!A_CONFIG")

        assertThat(parser.expresison, instanceOf(NotOper::class.java) )

        val notOper = parser.expresison as NotOper

        assertThat(notOper.expression, instanceOf(SimpleExpression::class.java))
    }

    @Test
    fun OrSimplyName() {
        val parser = ExpressionParser("A_CONFIG || B_CONFIG")

        assertThat(parser.expresison, instanceOf(OrOper::class.java) )

        val orOper = parser.expresison as OrOper

        assertThat(orOper.left, instanceOf(SimpleExpression::class.java))
        assertThat(orOper.right, instanceOf(SimpleExpression::class.java))

        val left = orOper.left as SimpleExpression
        val right = orOper.right as SimpleExpression


        assertThat(left.value, Is("A_CONFIG"))
        assertThat(right.value, Is("B_CONFIG"))
    }

    @Test
    fun AndSimplyName() {
        val parser = ExpressionParser("A_CONFIG && B_CONFIG")

        assertThat(parser.expresison, instanceOf(AndOper::class.java) )

        val andOper = parser.expresison as AndOper

        assertThat(andOper.left, instanceOf(SimpleExpression::class.java))
        assertThat(andOper.right, instanceOf(SimpleExpression::class.java))

        val left = andOper.left as SimpleExpression
        val right = andOper.right as SimpleExpression


        assertThat(left.value, Is("A_CONFIG"))
        assertThat(right.value, Is("B_CONFIG"))
    }

    @Test
    fun GreaterEqualSimplyName() {
        val parser = ExpressionParser("A_CONFIG >= B_CONFIG")

        assertThat(parser.expresison, instanceOf(GTOper::class.java) )

        val gtOper = parser.expresison as GTOper

        assertThat(gtOper.left, instanceOf(SimpleExpression::class.java))
        assertThat(gtOper.right, instanceOf(SimpleExpression::class.java))

        val left = gtOper.left as SimpleExpression
        val right = gtOper.right as SimpleExpression


        assertThat(left.value, Is("A_CONFIG"))
        assertThat(right.value, Is("B_CONFIG"))
    }

    @Test
    fun BracketExpression() {
        val parser = ExpressionParser("A_CONFIG && (!B_CONFIG || C_CONFIG)")

        assertThat(parser.expresison, instanceOf(AndOper::class.java) )

        val andOper = parser.expresison as AndOper

        assertThat(andOper.left, instanceOf(SimpleExpression::class.java))
        assertThat(andOper.right, instanceOf(OrOper::class.java))

        val left = andOper.left as SimpleExpression
        val right = andOper.right as OrOper


        assertThat(left.value, Is("A_CONFIG"))
        assertThat(right.left, instanceOf(NotOper::class.java) )
        assertThat(right.right, instanceOf(SimpleExpression::class.java) )
    }

    @Test
    fun equalsToEmptyString() {
        val parser = ExpressionParser("IDF_TARGET_ENV=\"\"")

        assertThat(parser.expresison, instanceOf(EqualOper::class.java) )

        val andOper = parser.expresison as EqualOper

        assertThat(andOper.left, instanceOf(SimpleExpression::class.java))
        assertThat(andOper.right, instanceOf(SimpleExpression::class.java))

        val left = andOper.left as SimpleExpression
        val right = andOper.right as SimpleExpression


        assertThat(left.value, Is("IDF_TARGET_ENV"))
        assertThat(right.value, isEmptyString())
    }

    @Test
    fun expressionWithComment() {
        val parser = ExpressionParser("BTDM_CONTROLLER_MODE_BR_EDR_ONLY || BTDM_CONTROLLER_MODE_BTDM  # NOERROR")

        assertThat(parser.expresison, Is( OrOper(SimpleExpression("BTDM_CONTROLLER_MODE_BR_EDR_ONLY"), SimpleExpression("BTDM_CONTROLLER_MODE_BTDM"))  as Expression))
    }
}
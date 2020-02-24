package it.achdjian.plugin.espparser

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.instanceOf
import org.hamcrest.Matchers.isEmptyString
import org.junit.jupiter.api.Test
import org.hamcrest.CoreMatchers.`is` as Is

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

        assertThat(simpleExpression.text, Is("A_CONFIG"))
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


        assertThat(left.text, Is("A_CONFIG"))
        assertThat(right.text, Is("B_CONFIG"))
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


        assertThat(left.text, Is("A_CONFIG"))
        assertThat(right.text, Is("B_CONFIG"))
    }

    @Test
    fun GreaterEqualSimplyName() {
        val parser = ExpressionParser("A_CONFIG >= B_CONFIG")

        assertThat(parser.expresison, instanceOf(GTEOper::class.java) )

        val gteOper = parser.expresison as GTEOper

        assertThat(gteOper.left, instanceOf(SimpleExpression::class.java))
        assertThat(gteOper.right, instanceOf(SimpleExpression::class.java))

        val left = gteOper.left as SimpleExpression
        val right = gteOper.right as SimpleExpression


        assertThat(left.text, Is("A_CONFIG"))
        assertThat(right.text, Is("B_CONFIG"))
    }


    @Test
    fun NotANDGreaterEqualSimplyName2() {
        val parser = ExpressionParser("!FREERTOS_UNICORE  && ESP32_REV_MIN < 2")

        assertThat(parser.expresison, instanceOf(AndOper::class.java) )

        val andOper = parser.expresison as AndOper

        assertThat(andOper.left, instanceOf(NotOper::class.java))
        assertThat(andOper.right, instanceOf(LTEOper::class.java))

        val left = andOper.left as NotOper
        val right = andOper.right as LTEOper


        assertThat(right.left, instanceOf(SimpleExpression::class.java))
        assertThat(right.right, instanceOf(SimpleExpression::class.java))
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


        assertThat(left.text, Is("A_CONFIG"))
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


        assertThat(left.text, Is("IDF_TARGET_ENV"))
        assertThat(right.text, isEmptyString())
    }




    @Test
    fun OrBracketAnd() {
        val parser = ExpressionParser("(FLASHMODE_QIO || FLASHMODE_QOUT) && IDF_TARGET_ESP32")

        assertThat(parser.expresison, instanceOf(AndOper::class.java) )

        val andOper = parser.expresison as AndOper

        assertThat(andOper.left, instanceOf(OrOper::class.java))
        assertThat(andOper.right, instanceOf(SimpleExpression::class.java))

        val left = andOper.left as OrOper
        val orLeft = left.left as SimpleExpression
        val orRight = left.right as SimpleExpression
        val right = andOper.right as SimpleExpression


        assertThat(right.text, Is("IDF_TARGET_ESP32"))
        assertThat(orLeft.text, Is("FLASHMODE_QIO"))
        assertThat(orRight.text, Is("FLASHMODE_QOUT"))
    }

    @Test
    fun expressionWithComment() {
        val parser = ExpressionParser("BTDM_CONTROLLER_MODE_BR_EDR_ONLY || BTDM_CONTROLLER_MODE_BTDM  # NOERROR")

        assertThat(parser.expresison, Is( OrOper(SimpleExpression("BTDM_CONTROLLER_MODE_BR_EDR_ONLY"), SimpleExpression("BTDM_CONTROLLER_MODE_BTDM"))  as Expression))
    }
}
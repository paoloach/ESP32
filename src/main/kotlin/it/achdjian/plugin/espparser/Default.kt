package it.achdjian.plugin.espparser

class Default(line: String) {
    val value: String

    val condition: Expression

    init {
        val splitted = line.split(Regex("\\s+"))
        if (splitted.size == 1) {
            if (line.startsWith("CONFIG_")){
                value= line.substring(7)
            } else {
                value = line
            }
            condition = emptyExpression

        } else {
            value = splitted[0]

            if (splitted[1].compareTo("if", true) == 0) {
                val strCond = splitted.subList(2, splitted.size).joinToString(separator = " ")
                condition = ExpressionParser(strCond).expresison
            } else {
                throw RuntimeException("Unable to parse the default line: $line")
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other is Default) {
            return other.value == value && other.condition == condition
        } else return false
    }
}
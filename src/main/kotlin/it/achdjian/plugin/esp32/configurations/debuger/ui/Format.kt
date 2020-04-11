package it.achdjian.plugin.esp32.configurations.debuger.ui

enum class Format(val readableName: String, val sign: Char) {
    DEC("Decimal", 'D') {
        override fun format(value: Long, bits: Int) = java.lang.Long.toUnsignedString(value, 10)
    },
    HEX("Hex", 'H') {
        override fun format(value: Long, bits: Int) = bitGroupFormat(value, bits, 4, "0123456789ABCDEF", "0x")
    },
    OCT("Octal", 'O') {
        override fun format(value: Long, bits: Int) = bitGroupFormat(value, bits, 3, "01234567", "0")
    },
    BIN("Binary", 'B') {
        override fun format(value: Long, bits: Int) = bitGroupFormat(value, bits, 1, "01", "0b")
    };

    abstract fun format(var1: Long, var3: Int): String

    companion object {
        private fun bitGroupFormat(value: Long, bits: Int, bitsPerSymbol: Int, alphabet: String, prefix: String): String {
            var value = value
            var bits = bits
            val result = StringBuilder()
            val symCount = alphabet.length
            while (bits > 0) {
                result.append(alphabet[java.lang.Long.remainderUnsigned(value, symCount.toLong()).toInt()])
                value = java.lang.Long.divideUnsigned(value, symCount.toLong())
                bits -= bitsPerSymbol
            }
            return result.reverse().insert(0, prefix).toString()
        }
    }

}
package it.achdjian.plugin.esp32.configurations.debuger.ui

import java.util.*

enum class RegisterAccess {
    READ_ONLY, WRITE_ONLY, READ_WRITE, WRITEONCE, READ_WRITEONCE;

    val isReadable: Boolean
        get() = when (this) {
            WRITE_ONLY, WRITEONCE -> false
            else -> true
        }

    companion object {
        fun parse(s: String)=
            when (Objects.toString(s, "")) {
                "writeOnce" -> WRITEONCE
                "read-only" -> READ_ONLY
                "write-only"-> WRITE_ONLY
                "read-writeOnce"->READ_WRITEONCE
                else ->READ_ONLY
            }
    }
}

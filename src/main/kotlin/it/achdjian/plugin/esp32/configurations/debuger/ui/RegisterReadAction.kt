package it.achdjian.plugin.esp32.configurations.debuger.ui

import org.jetbrains.annotations.Contract

enum class RegisterReadAction {
    CLEAR, SET, MODIFY, MODIFY_EXTERNAL;

    companion object {
        @Contract("null->null")
        fun parse(s: String) =
            when(s) {
                "modifyExternal"-> MODIFY_EXTERNAL
                "modify"->MODIFY
                "set"->SET
                "clear"->CLEAR
                else->null
            }
    }
}

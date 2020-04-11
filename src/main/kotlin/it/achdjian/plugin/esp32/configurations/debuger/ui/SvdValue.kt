package it.achdjian.plugin.esp32.configurations.debuger.ui

abstract class SvdValue<T : SvdNode<*>> (id: String, name: String, description: String, val access: RegisterAccess, val readAction:RegisterReadAction?, val bitSize: Int, var format:Format) :
    SvdNode<T>(id,name,description) {

    @Volatile
    protected var isChanged = false


    abstract val defaultFormat: Format

    override fun setFormatFromSign(sign: Char) {
        format = ourFormatBySign[sign] as Format
    }

    companion object {
        private val ourFormatBySign = Format.values().map { it.sign to it }.toMap()
    }

}

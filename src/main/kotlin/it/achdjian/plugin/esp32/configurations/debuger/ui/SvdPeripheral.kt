package it.achdjian.plugin.esp32.configurations.debuger.ui

import java.io.PrintWriter

class SvdPeripheral(fileName: String, name: String, description: String, val myRegisterAccess: RegisterAccess?, val baseAddress: Long, val registerBitSize: Int) :
    SvdNode<SvdRegister>(id="$fileName|p:$name", name=name, description=description) {

    override fun exportCsv(writer: PrintWriter, prefix: String,  active: (BaseSvdNode)->Boolean) {
        super.exportCsv(writer, prefix + name, active)
    }

    val registerAccess: RegisterAccess
        get() = myRegisterAccess ?: RegisterAccess.READ_WRITE

}
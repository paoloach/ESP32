package it.achdjian.plugin.esp32.configurations.debuger.ui

import java.io.PrintWriter
import java.util.*


interface BaseSvdNode {
    fun exportCsv(writer: PrintWriter, prefix: String, active: (BaseSvdNode)->Boolean) ;
}

open class SvdNode< T :BaseSvdNode>(val id:String,val name: String="", val description: String="", open val displayValue: String="", var children:MutableList<T> = mutableListOf() ) : BaseSvdNode{

    open fun setFormatFromSign(sign: Char) {}
    override fun exportCsv(writer: PrintWriter, prefix: String, active: (BaseSvdNode)->Boolean) {
        children.filter { active(it) }.forEach { it.exportCsv(writer, prefix, active) }
    }

    companion object {
        val NAME_COMPARATOR = Comparator { a: SvdNode<*>, b: SvdNode<*> ->
                a.name.compareTo(b.name, ignoreCase = true)
            }
    }
}

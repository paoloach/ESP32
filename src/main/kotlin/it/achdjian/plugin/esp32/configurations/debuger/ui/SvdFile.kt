package it.achdjian.plugin.esp32.configurations.debuger.ui

class SvdFile(name: String, val location: String) :  SvdNode<SvdNode<*>>(name,name) {
    override fun toString(): String {
        return name
    }
}
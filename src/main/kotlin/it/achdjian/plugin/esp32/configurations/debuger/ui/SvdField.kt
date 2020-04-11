package it.achdjian.plugin.esp32.configurations.debuger.ui

class SvdField(val myParent: SvdRegister, name: String, description: String, access: RegisterAccess, readAction: RegisterReadAction?, val bitOffset: Int, size: Int) :
    SvdValue<SvdNode<*>>(myParent.id + "|" + name, name, description, access, readAction, size, Format.BIN) {
    private var value = 0L

    fun updateFromValue(registerValue: Long) {
        var newValue = registerValue shr bitOffset
        newValue = newValue and (-1L shl bitSize).inv()
        isChanged = value != newValue
        value = newValue
    }

    override val displayValue: String
        get() = if (access.isReadable && !myParent.isFailed) {
            format.format(value, this.bitSize)
        } else {
            "-"
        }

    override val defaultFormat: Format = Format.BIN

    fun markStalled() {
        isChanged = false
    }

}
package it.achdjian.plugin.esp32.configurations.debuger.ui

import com.jetbrains.cidr.execution.debugger.backend.LLMemoryHunk
import com.jetbrains.cidr.execution.debugger.memory.Address
import java.io.PrintWriter

open class SvdRegister(peripheralName: String, name: String, description: String, val address: Address, bitSize: Int, access: RegisterAccess, readAction: RegisterReadAction?) :
    SvdValue<SvdField>("$peripheralName|$name", name, description, access, readAction, bitSize, DEFAULT_FORMAT) {

    @Volatile
    private var value = 0L

    @Volatile
    private var failReason: String = "-"

    val isFailed = failReason == "-"

    override val defaultFormat =  DEFAULT_FORMAT


    override val displayValue = if (isFailed) failReason else format.format(value, bitSize)



    fun processValue(hunks: List<LLMemoryHunk>?, e: Throwable?) {
        e?.let {
            markNoValue(e.localizedMessage, true)
        }  ?: run {
            hunks?.let { hunk ->
                hunk.firstOrNull { it.component1().contains(address) } ?.let { found ->
                    val byteSize: Int = (this.bitSize + 7) / 8
                    val offset = address.minus(found.range.start).toInt()
                    val value = this.getValue(found, offset, byteSize)
                    this.isChanged = this.value != value
                    if (this.isChanged) {
                        this.value = value
                        val var9: Iterator<*> = this.children.iterator()
                        while (var9.hasNext()) {
                            val field: SvdField = var9.next() as SvdField
                            field.updateFromValue(value)
                        }
                    } else {
                        this.children.forEach { obj: SvdField -> obj.markStalled() }
                    }
                    failReason = "-"
                } ?: markNoValue("-", true)
            } ?: markNoValue("-", true)
        }
    }

    open protected fun getValue(hunk: LLMemoryHunk, offset: Int, byteSize: Int): Long {
        var value = 0L
        for (i in byteSize - 1 downTo 0) {
            value = value shl 8 or 255L and hunk.bytes[offset + i].toLong()
        }
        return value
    }

    fun markNoValue(reason: String, valueChange: Boolean) {
        isChanged = valueChange && reason != failReason
        failReason = reason
    }


    override fun exportCsv(writer: PrintWriter, prefix: String, active: (BaseSvdNode)->Boolean) {
        writer.print(prefix)
        writer.print(", ")
        writer.print(name)
        writer.print(", ")
        writer.print(displayValue)
        if (children.isNotEmpty()) {
            writer.print(", ")
            writer.println(children.joinToString(separator = "; ") { "${it.name}:${it.displayValue}" })
        }
    }

    companion object {
        private var DEFAULT_FORMAT = Format.HEX
    }

    init {
        if (!access.isReadable) {
            markNoValue("SVD write only", false)
        }
    }
}

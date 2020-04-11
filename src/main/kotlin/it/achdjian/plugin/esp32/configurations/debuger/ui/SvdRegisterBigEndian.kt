package it.achdjian.plugin.esp32.configurations.debuger.ui

import com.jetbrains.cidr.execution.debugger.backend.LLMemoryHunk
import com.jetbrains.cidr.execution.debugger.memory.Address

class SvdRegisterBigEndian(peripheralName: String, name: String, description: String, address: Address, bitSize: Int, access: RegisterAccess, readAction: RegisterReadAction?) :
    SvdRegister(peripheralName, name, description, address, bitSize, access, readAction) {
    override fun getValue(hunk: LLMemoryHunk, offset: Int, byteSize: Int): Long {
        var value = 0L
        for (i in 0 until byteSize) {
            value = value shl 8 or 255L and hunk.bytes[offset + i].toLong()
        }
        return value
    }
}

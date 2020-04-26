package it.achdjian.plugin.esp32.configurations.debuger.ui

import com.intellij.execution.ExecutionException
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Pair
import com.intellij.util.SmartList
import com.jetbrains.cidr.execution.debugger.CidrDebugProcess
import com.jetbrains.cidr.execution.debugger.backend.DebuggerCommandException
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriver
import com.jetbrains.cidr.execution.debugger.backend.LLMemoryHunk
import com.jetbrains.cidr.execution.debugger.memory.Address
import com.jetbrains.cidr.execution.debugger.memory.AddressRange
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean


fun createAdressCoalescer(nodes: Collection<SvdNode<*>>): SvdAddressCoalescer {
    val coalescer = SvdAddressCoalescer()
    coalescer.loadFrom(nodes)
    return coalescer
}

private fun calcSize(register: SvdRegister): Int {
    return (register.bitSize + 7) / 8
}

class SvdAddressCoalescer {
    companion object {
        val log = Logger.getInstance(SvdAddressCoalescer::class.java)
    }

    private val ranges = mutableListOf<Pair<AddressRange, List<SvdRegister>>>()

    fun loadFrom(nodes: Collection<SvdNode<*>>) {

        val sortedRegisters = nodes.filterIsInstance<SvdRegister>()
            .filter { it.access.isReadable }
            .sortedBy { it.address }

        val lastList: MutableList<SvdRegister> = SmartList()
        lateinit var lastStart: Address
        lateinit var lastEnd: Address
        sortedRegisters.firstOrNull()?.let { register ->
            lastStart = register.address
            lastEnd = register.address.plus(calcSize(register = register))
            lastList.add(register)
        }
        sortedRegisters.forEach { register ->
            val registerEnd = register.address.plus(calcSize(register = register))
            if (register.address in lastStart..lastEnd) {
                lastList.add(register)
                if (lastEnd < registerEnd) {
                    lastEnd = registerEnd
                }
            } else if (registerEnd in lastStart..lastEnd) {
                lastList.add(register)
                if (lastStart < register.address) {
                    lastStart = register.address
                }
            } else {
                ranges.add(Pair.create(AddressRange(lastStart, lastEnd.minus(1)), lastList))
                lastStart = register.address
                lastEnd = registerEnd
                lastList.add(register)
            }
        }
    }

    fun updateValues(process: CidrDebugProcess, cancelled: AtomicBoolean, updateListener: Runnable) {

        updateListener.run()

        ranges.forEach {
            val command = MemDumpCode(it.first, cancelled) { reason: String ->
                it.getSecond().forEach { register -> register.markNoValue(reason, false) }
            }
            val future: CompletableFuture<List<LLMemoryHunk>> = process.postCommand<List<LLMemoryHunk>>(command)
            future.whenComplete { hunks, exception ->
                try {
                    it.second.forEach { register ->
                        try {
                            register.processValue(hunks, exception)
                        } catch (e: Throwable) {
                            val message = e.message?.let { e.message } ?: "unknown cause"
                            register.markNoValue(message, true)
                            log.error(e)
                        }
                    }
                } finally {
                    updateListener.run()
                }
            }
        }
    }
}

private class MemDumpCode(
    val addressRange: AddressRange,
    val cancelled: AtomicBoolean,
    val rejectedHandler: (String) -> Unit
) :
    CidrDebugProcess.DebuggerCommand<List<LLMemoryHunk>?> {

    @Throws(
        ExecutionException::class,
        DebuggerCommandException::class
    )
    override fun call(driver: DebuggerDriver): List<LLMemoryHunk> {
        return if (cancelled.get()) {
            throw CancellationException("-")
        } else {
            driver.dumpMemory(addressRange)
        }
    }

    override fun rejected(reason: String) {
        rejectedHandler(reason)
    }
}
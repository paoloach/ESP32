package it.achdjian.plugin.esp32.configurations.debuger.ui

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ui.Messages
import com.intellij.util.SystemProperties
import com.jetbrains.cidr.execution.debugger.memory.Address
import org.jdom.Document
import org.jdom.Element
import org.jdom.Text
import org.jdom.input.SAXBuilder
import java.io.InputStream
import java.io.PrintWriter
import java.text.MessageFormat
import java.util.*

class SvdRoot : SvdNode<SvdFile>(id = "", name = "") {
    private val treeTableModel: SvdTreeTableModel = SvdTreeTableModel(this)
    var activeNodes = setOf<SvdNode<*>>()

    fun addFile(stream: InputStream?, shortName: String, fileName: String): SvdFile? {
        val document: Document
        try {
            document = SAXBuilder().build(stream)
        } catch (e: Exception) {
            Messages.showErrorDialog("$fileName: ${e.message}", "Root load error")
            return null
        }
        val svdFile = SvdFile(shortName, fileName)
        val flatList = mutableListOf<SvdNode<*>>()
        val endian: List<Element> = selectDomSubNodes(document.rootElement, "cpu", "endian")
        val bigEndian: Boolean = endian.isNotEmpty() && ("big" == endian[0].getTextTrim())
        val elements: MutableMap<String, Element> = TreeMap<String, Element>(java.lang.String.CASE_INSENSITIVE_ORDER)

        selectDomSubNodes(document.rootElement, "peripherals", "peripheral")
            .forEach { elements.put(it.getChildText("name"), it) }


        val grouped = TreeMap<String, SvdNode<SvdPeripheral>>(java.lang.String.CASE_INSENSITIVE_ORDER)


        elements.forEach { name, element ->
            try {
                val derivedFromName: String? = element.getAttributeValue("derivedFrom")
                val derivedFrom = derivedFromName?.let { elements[derivedFromName] }
                val description = loadDescription(element, derivedFrom)
                val groupName = getDomSubTagText(element, derivedFrom, "groupName")
                val baseAddress: Long = getDomSubTagValue(element, derivedFrom, "baseAddress", 0) { it.toLong() }
                val registerSize: Int = getDomSubTagValue(element, derivedFrom, "size", 32) { it.toInt() }
                val registerAccess: RegisterAccess =
                    getDomSubTagValue(element, derivedFrom, "access", RegisterAccess.READ_WRITE) {
                        RegisterAccess.parse(it)
                    }
                val peripheral = if (groupName.isEmpty()) {
                    val p = SvdPeripheral(shortName, name, description, registerAccess, baseAddress, registerSize)
                    flatList.add(p)
                    p
                } else {
                    val group = grouped.computeIfAbsent(groupName) {
                        SvdNode(id = "$shortName|g:$it", name = id)
                    }
                    val p = SvdPeripheral(shortName, name, description, registerAccess, baseAddress, registerSize)
                    group.children.add(p)
                    p
                }
                loadContent(peripheral, element, derivedFrom, bigEndian)
            } catch (e: RuntimeException) {
                log.warn(
                    MessageFormat.format(
                        "Svd parser error at {0}/{1}",
                        element.name,
                        element.getChildText("name")
                    ), e
                )
            }
        }

        grouped.values.filter { it.children.size == 1 }.forEach { flatList.add(it.children[0]) }
        grouped.values.filter { it.children.size > 1 }.forEach { flatList.add(it) }

        flatList.sortWith(NAME_COMPARATOR)

        svdFile.children = flatList
        children.add(svdFile)
        treeTableModel.notifyFileInserted(svdFile)
        return svdFile
    }


    fun isActive(child: BaseSvdNode): Boolean = activeNodes.contains(child)

    fun setActive(nodes: Set<SvdNode<*>>) {
        activeNodes = nodes
    }

    fun unloadFile(deleted: SvdFile) {
        val index = children.indexOf(deleted)
        if (index >= 0) {
            children.removeAt(index)
            treeTableModel.notifyFileDeleted(deleted, index)
        }
    }

    fun export(writer: PrintWriter) {
        writer.write("Peripheral, Register, Value, Fields" + SystemProperties.getLineSeparator())
        val fileNamePrefix: Boolean = children.size > 1

        children.forEach { file ->
            if (file.children.any { isActive(it) }) {
                val prefix = if (fileNamePrefix)
                                file.name + ":"
                              else
                                 ""
                file.exportCsv(writer, prefix) { isActive(it) }
            }
        }
    }

    companion object {
        val log = Logger.getInstance(SvdRoot::class.java)
    }
}

private fun loadContent(peripheral: SvdPeripheral, peripheralElement: Element, peripheralDerivedFrom: Element?, bigEndian: Boolean) {
    val elements = TreeMap<String, Element>(java.lang.String.CASE_INSENSITIVE_ORDER)

    selectDomSubNodes(peripheralElement, "registers", "register").forEach { elements[it.getChildText("name")] = it }
    selectDomSubNodes(peripheralDerivedFrom, "registers", "register").forEach { elements[it.getChildText("name")] = it }

    peripheral.children = elements.map {entry->
                        val element = entry.value
                        val name = entry.key
                        val derivedFromName: String? = element.getAttributeValue("derivedFrom")
                        val derivedFrom: Element? = derivedFromName?.let { elements[derivedFromName] }
                        val address =  Address.fromUnsignedLong( peripheral.baseAddress + getDomSubTagValue(element, derivedFrom, "addressOffset", 0L){it.toLong()})
                        val description = loadDescription(element, derivedFrom) + " (" + addressToString(address) + ")"
                        val registerSize = getDomSubTagValue(element, derivedFrom, "size", peripheral.registerBitSize){it.toInt()}
                        val registerAccess = getDomSubTagValue(element, derivedFrom, "access", peripheral.registerAccess){RegisterAccess.parse(it)}
                        val registerReadAction = getDomSubTagValue(element, derivedFrom, "access", null as RegisterReadAction){RegisterReadAction.parse(it)}
                        val register = if (bigEndian)
                                SvdRegisterBigEndian(peripheral.id, name, description, address, registerSize, registerAccess, registerReadAction)
                            else
                                SvdRegister(peripheral.id, name, description, address, registerSize, registerAccess, registerReadAction)
                        loadContent(register, element, derivedFrom)
                        register
                    }.sortedBy { it.address }.toMutableList()
}

fun addressToString(address: Address)= "0x" + address.unsignedLongValue.toString(16).padStart(8, '0')

private fun loadDescription(element: Element, derivedFrom: Element?): String {
    return Text.normalizeString(getDomSubTagText(element, derivedFrom, "description"))
}

private fun getDomSubTagText(element: Element, derivedFrom: Element?, tagName: String): String {
    return   getDomSubTagValue(element, derivedFrom, tagName, ""){it}
}

private fun <T> getDomSubTagValue(element: Element, derivedFrom: Element?, tagName: String, defaultValue: T, convert: (String) -> T): T {
    return element.getChildText(tagName)?.let { convert(it) } ?: run { derivedFrom?.let { convert(it.getChildText(tagName)) } ?: defaultValue }
}


private fun selectDomSubNodes(parent: Element?, vararg selectors: String): List<Element> {
    val lastParent = selectors.fold(parent) { childParent, selector -> childParent?.getChild(selector) }
    return lastParent?.getChildren(selectors.last()) ?: emptyList()
}

private fun loadContent(register: SvdRegister, peripheralElement: Element, peripheralDerivedFrom: Element?) {
    val elements= TreeMap<String, Element>(java.lang.String.CASE_INSENSITIVE_ORDER)

    selectDomSubNodes(peripheralElement, "fields", "field").forEach { elements[it.getChildText("name")] = it }
    selectDomSubNodes(peripheralDerivedFrom, "fields", "field").forEach { elements[it.getChildText("name")] = it }

    register.children = elements.map {
        var description: String
        val bitSize:Int
        val element = it.value
        val name = it.key
        val derivedFromName: String? = element.getAttributeValue("derivedFrom")
        val derivedFrom = derivedFromName ?.let { elements[derivedFromName] }
        val registerAccess = getDomSubTagValue(element, derivedFrom, "access", register.access){s->RegisterAccess.parse(s)}
        val registerReadAction = getDomSubTagValue(element, derivedFrom, "access", null){s->RegisterReadAction.parse(s)}
        var bitOffset = getDomSubTagValue(element, derivedFrom, "bitOffset",-1){s->s.toInt()}
        if (bitOffset >= 0) {
            bitSize = getDomSubTagValue(element, derivedFrom, "bitWidth", register.bitSize-bitOffset){s->s.toInt()}
        } else {
            bitOffset = getDomSubTagValue(element, derivedFrom, "lsb",-1){s->s.toInt()}
            if (bitOffset >= 0) {
                bitSize = getDomSubTagValue(element, derivedFrom, "msb",register.bitSize){s->s.toInt()}
            } else {
                description = getDomSubTagText(element, derivedFrom, "bitRange")
                description = description.replace("\\s|\\[|]".toRegex(), "")
                val columnIndex: Int = description.indexOf(':')
                if (columnIndex < 1) {
                    bitOffset = 0
                    bitSize = register.bitSize
                } else {
                    bitOffset = Integer.parseUnsignedInt(description.substring(0, columnIndex), 10)
                    bitSize =   description.substring(columnIndex + 1).toInt()-bitOffset+1
                }
            }
        }
        description = loadDescription(element, derivedFrom)
        description = if (bitSize == 1) {
            "$description [$bitOffset]"
        } else {
            description + " [" + bitOffset + ":" + (bitOffset + bitSize - 1) + "]"
        }
        SvdField(register, name, description, registerAccess, registerReadAction, bitOffset, bitSize)
    }.sortedBy { it.bitOffset }.toMutableList()

}
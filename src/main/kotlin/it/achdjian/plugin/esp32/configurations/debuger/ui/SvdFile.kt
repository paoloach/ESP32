package it.achdjian.plugin.esp32.configurations.debuger.ui

import java.nio.file.Path

class SvdFile(val path: Path) :  SvdNode<SvdFile>(path.toString(),path.toString()) {
    override fun toString(): String {
        return path.fileName?.toString() ?: path.toString()
    }
}
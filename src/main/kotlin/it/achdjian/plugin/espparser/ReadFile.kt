package it.achdjian.plugin.espparser

import java.io.File

class ReadFile {
    fun read(it: File):List<String> = it.readLines()
}
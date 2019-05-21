package it.achdjian.plugin.espparser

class Condition(toParse: String="") {
    val condition = parseDependsOn(toParse, ArrayList())
    val alwaysTrue = toParse==""
}
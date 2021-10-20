package org.igye.metamathparser

data class VisualizationData(
    val description: String,
    val assertionVarToContextVar: IntArray,
    val symbolsMap: Map<Int,String>,
    val variablesTypes: Map<String,String>,
) {
    fun statementToSymbols(stmt:IntArray):List<String> = stmt.map { numToSymbol(it) }

    fun statementToSymbols(stmt:Statement):List<String> = statementToSymbols(stmt.content)

    fun numToSymbol(n:Int): String = symbolsMap[n]!!
}

val emptyVisualizationData = VisualizationData(
    description = "",
    assertionVarToContextVar = IntArray(0),
    symbolsMap = emptyMap(),
    variablesTypes = emptyMap()
)
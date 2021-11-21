package org.igye.metamathparser

data class VisualizationData(
    val description: String,
    val localVarToGlobalVar: IntArray,
    val symbolsMap: Map<Int,String>,
    val variablesTypes: Map<String,String>,
) {
    fun numToSym(num:Int): String = symbolsMap[if (num >= 0) localVarToGlobalVar[num] else num]!!
}

val emptyVisualizationData = VisualizationData(
    description = "",
    localVarToGlobalVar = IntArray(0),
    symbolsMap = emptyMap(),
    variablesTypes = emptyMap()
)
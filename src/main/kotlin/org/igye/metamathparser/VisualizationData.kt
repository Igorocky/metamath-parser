package org.igye.metamathparser

data class VisualizationData(
    val description: String,
    val localVarToGlobalVar: IntArray,
    val symbolsMap: Map<Int,String>,
    val variablesTypes: Map<String,String>,
)

val emptyVisualizationData = VisualizationData(
    description = "",
    localVarToGlobalVar = IntArray(0),
    symbolsMap = emptyMap(),
    variablesTypes = emptyMap()
)
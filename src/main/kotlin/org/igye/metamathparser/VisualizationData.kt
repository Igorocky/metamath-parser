package org.igye.metamathparser

data class VisualizationData(
    val variablesTypes: Map<Int,Int>,
    val contextVarToAssertionVar: Map<Int,Int>,
    val assertionVarToContextVar: IntArray,
    val symbolsMap: Map<Int,String>
)

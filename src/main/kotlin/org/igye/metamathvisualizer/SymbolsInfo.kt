package org.igye.metamathvisualizer

data class SymbolsInfo(
    val constants: Map<Int, String>,
    val variables: Map<Int, String>,
    val varTypes: Map<String, String>,
)
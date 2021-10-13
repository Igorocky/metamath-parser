package org.igye.metamathvisualizer.dto

data class IndexElemDto(
    val id: Int,
    val type: String,
    val label: String,
    val hypotheses: List<List<String>>,
    val expression: List<String>,
    val varTypes: Map<String, String>,
)

package org.igye.metamathvisualizer.dto

data class AssertionDto(
    val type: String,
    val name: String,
    val description: String,
    val varTypes: Map<String, String>,
    val params: List<List<String>>,
    val retVal: List<String>,
    val proof: List<StackNodeDto>?,
)
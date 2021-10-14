package org.igye.metamathvisualizer.dto

data class StackNodeDto(
    val id: Int,
    var args: List<Int>,
    val type: String,
    val label: String,
    val params: List<List<String>>,
    val numOfTypes: Int,
    val retVal: List<String>,
    val substitution: Map<String, List<String>>,
    val expr: List<String>,
)
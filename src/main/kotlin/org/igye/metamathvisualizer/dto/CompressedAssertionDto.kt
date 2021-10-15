package org.igye.metamathvisualizer.dto

data class CompressedAssertionDto(
    val s: List<String>,
    val t: String,
    val n: String,
    val d: String,
    val v: Map<Int, Int>,
    val pa: List<List<Int>>,
    val r: List<Int>,
    val p: List<CompressedStackNodeDto>?,
)

package org.igye.metamathvisualizer.dto

data class CompressedStackNodeDto(
    val i: Int,
    val a: List<Int>?,
    val t: Int,
    val l: Int,
    val p: List<List<Int>?>?,
    val n: Int,
    val r: List<Int>?,
    val s: Map<Int, List<Int>>?,
    val e: List<Int>,
)

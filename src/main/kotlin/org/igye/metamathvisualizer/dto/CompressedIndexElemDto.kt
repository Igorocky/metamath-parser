package org.igye.metamathvisualizer.dto

data class CompressedIndexElemDto(
    val i: Int,
    val t: Int,
    val l: String,
    val h: List<List<Int>>,
    val e: List<Int>,
    val v: Map<Int, Int>,
)

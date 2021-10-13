package org.igye.metamathvisualizer.dto

data class CompressedIndexDto(
    val strings: List<String>,
    val elems: List<CompressedIndexElemDto>,
)

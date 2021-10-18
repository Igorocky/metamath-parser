package org.igye.metamathparser

data class Statement(
    val beginIdx:Int,
    val label:String,
    val type:Char,
    val content:IntArray,
)
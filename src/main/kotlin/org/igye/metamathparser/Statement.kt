package org.igye.metamathparser

import java.util.*

data class Statement(
    val beginIdx:Int = 0,
    val label:String = UUID.randomUUID().toString(),
    val type:Char,
    val content:IntArray,
)
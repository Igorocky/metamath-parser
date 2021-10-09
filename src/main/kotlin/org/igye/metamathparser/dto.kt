package org.igye.metamathparser

data class ParserInput(val text:String, val begin:Int) {
    fun charAt(i:Int): Char = text[toAbsolute(i)]
    fun proceed(n:Int) = this.copy(begin = begin+n)
    fun currPositionStr():String = begin.toString()
    fun toAbsolute(i:Int) = begin+i
}

data class ParserOutput<T>(val result:T, val end:Int)

data class ListOfConstants(val symbols:List<String>, val beginIdx:Int)
data class ListOfVariables(val symbols:List<String>, val beginIdx:Int)
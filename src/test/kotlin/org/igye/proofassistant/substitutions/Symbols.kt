package org.igye.proofassistant.substitutions

object Symbols {
    data class ConstPartsDto(val consts: String, val vars: String) {
        override fun toString(): String {
            return "ConstPartsDto(\n    c: $consts\n    v: $vars\n)"
        }
    }

    private val vars = setOf("a", "b", "c", "d", "e", "f", "g", "h", "ph", "ps", "ch", "th", "ta", "$0", "$1", "$2", "$3", "$4")
    private val consts = setOf("A", "B", "C", "D", "E", "F", "G", "H", "|-", "(", ")", "{", "}", "[", "]", "->", "=", "e.", "BaseSet", "`", "if", "U", "CPreHilOLD", ",", "<.", "+", "x.", ">.", "abs", "/\\", "+v", ".iOLD")
    init {
        if (vars.intersect(consts).isNotEmpty()) {
            throw RuntimeException("vars.intersect(consts).isNotEmpty()")
        }
    }
    private val intToSymbol: Map<Int, String> = vars.asSequence().mapIndexed { i, s -> i to s }.toMap().plus(
        consts.asSequence().mapIndexed { i, s -> -(i+1) to s }.toMap()
    )
    private val symbolToInt: Map<String, Int> = intToSymbol.asSequence().associate { (k,v) -> v to k }

    fun stmtToArr(stmt: String): IntArray = stmt.split(" ").map { symbolToInt[it]!! }.toIntArray()
    fun toInt(symb:String): Int = symbolToInt[symb]!!
    fun toSymb(num:Int): String = intToSymbol[num]!!

    fun constPartsToDto(stmt: IntArray, constParts: List<IntArray>): ConstPartsDto {
        fun append(sb:StringBuilder,num:Int,empty:Boolean) {
            if (sb.isNotEmpty()) {
                sb.append(" ")
            }
            val symb = toSymb(num)
            if (empty) {
                sb.append(" ".repeat(symb.length))
            } else {
                sb.append(symb)
            }
        }
        fun append(sb:StringBuilder,num:Int) = append(sb,num,false)
        fun appendEmpty(sb:StringBuilder,num:Int) = append(sb,num,true)
        val consts = StringBuilder()
        val vars = StringBuilder()
        if (constParts[0][0] != 0) {
            for(i in 0 .. constParts[0][0]-1) {
                append(vars, stmt[i])
                appendEmpty(consts, stmt[i])
            }
        }
        for (j in constParts.indices) {
            for(i in constParts[j][0] .. constParts[j][1]) {
                appendEmpty(vars, stmt[i])
                append(consts, stmt[i])
            }
            if (j < constParts.size-1) {
                for(i in constParts[j][1]+1 .. constParts[j+1][0]-1) {
                    append(vars, stmt[i])
                    appendEmpty(consts, stmt[i])
                }
            }
        }
        if (constParts.last()[1] != stmt.size-1) {
            for(i in constParts.last()[1]+1 .. stmt.size-1) {
                append(vars, stmt[i])
                appendEmpty(consts, stmt[i])
            }
        }
        return ConstPartsDto(consts = consts.toString(), vars = vars.toString())
    }
}
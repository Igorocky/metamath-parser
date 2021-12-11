package org.igye.proofassistant.substitutions

class Substitution(
    val size: Int,
    val parenthesesCounter: Array<ParenthesesCounter>,
) {
    val begins: IntArray = IntArray(size)
    val ends: IntArray = IntArray(size)
    val stmt: Array<IntArray> = Array(size){begins}
    val isDefined: BooleanArray = BooleanArray(size)
    val locks: IntArray = IntArray(size)

    fun isNotLocked(i: Int) = !isLocked(i)
    fun isLocked(i: Int) = locks[i] > -2

    fun unlock(level: Int = -1): Substitution {
        for (i in 0 until size) {
            if (locks[i] >= level) {
                locks[i] = -2
            }
        }
        return this
    }

    fun lock(level: Int = -1): Substitution {
        for (i in 0 until size) {
            if (isDefined[i] && isNotLocked(i)) {
                locks[i] = level
            }
        }
        return this
    }

    fun toString(varNames: (Int) -> String, symbols: (Int) -> String): String {
        return begins.indices.asSequence()
            .filter { isLocked(it) }
            .map { "${varNames(it)}: ${subExprToString(it, symbols)}" }
            .joinToString(prefix = "[", separator = " ; ", postfix = "]")
    }

    private fun subExprToString(i:Int, symbols: (Int) -> String): String {
        return stmt[i].indices.asSequence()
            .filter { begins[i] <= it && it <= ends[i] }
            .map { symbols(stmt[i][it]) }
            .joinToString(separator = " ")
    }
}

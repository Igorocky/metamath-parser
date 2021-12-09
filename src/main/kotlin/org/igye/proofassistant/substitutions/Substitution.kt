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

    fun unlock(hypIdx: Int = -1): Substitution {
        for (i in 0 until size) {
            if (locks[i] >= hypIdx) {
                locks[i] = -2
            }
        }
        return this
    }

    fun lock(hypIdx: Int = -1): Substitution {
        for (i in 0 until size) {
            if (isDefined[i] && isNotLocked(i)) {
                locks[i] = hypIdx
            }
        }
        return this
    }
}

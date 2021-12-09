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

    fun isNotLocked(i: Int) = locks[i] <= -2
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
            // TODO: 12/9/2021 don't overwrite other locks here
            locks[i] = if (isDefined[i]) hypIdx else -2
        }
        return this
    }
}

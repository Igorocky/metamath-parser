package org.igye.proofassistant.substitutions

class Substitution(
    val size: Int,
    val parenthesesCounter: Array<ParenthesesCounter>,
) {
    val begins: IntArray = IntArray(size)
    val ends: IntArray = IntArray(size)
    val isDefined: BooleanArray = BooleanArray(size)
    val locks: IntArray = IntArray(size)

    fun isNotLocked(i: Int) = locks[i] <= -2
    fun isLocked(i: Int) = locks[i] > -2

    fun unlock(): Substitution {
        for (i in 0 until size) {
            locks[i] = -2
        }
        return this
    }

    fun lock(): Substitution {
        for (i in 0 until size) {
            locks[i] = if (isDefined[i]) -1 else -2
        }
        return this
    }
}

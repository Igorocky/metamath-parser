package org.igye.proofassistant.substitutions

class ParenthesesCounter(
    private val roundBracketOpen:Int,
    private val roundBracketClose:Int,
    private val curlyBracketOpen:Int,
    private val curlyBracketClose:Int,
    private val squareBracketOpen:Int,
    private val squareBracketClose:Int,
) {
    private val parenStack = IntArray(30)
    private var stackIdx = -1
    private var failed = false

    fun reset() {
        stackIdx = -1
        failed = false
    }

    fun accept(symb: Int): Int {
        if (!failed) {
            if (symb == roundBracketOpen || symb == curlyBracketOpen || symb == squareBracketOpen) {
                parenStack[++stackIdx] = symb
            } else if (symb == roundBracketClose) {
                if (stackIdx >= 0 && parenStack[stackIdx] == roundBracketOpen) {
                    stackIdx--
                } else {
                    failed = true
                }
            } else if (symb == curlyBracketClose) {
                if (stackIdx >= 0 && parenStack[stackIdx] == curlyBracketOpen) {
                    stackIdx--
                } else {
                    failed = true
                }
            } else if (symb == squareBracketClose) {
                if (stackIdx >= 0 && parenStack[stackIdx] == squareBracketOpen) {
                    stackIdx--
                } else {
                    failed = true
                }
            }
        }

        return if (failed) {
            BR_FAILED
        } else if (stackIdx == -1) {
            BR_OK
        } else {
            BR_OPEN
        }
    }

    companion object {
        val BR_OPEN = 1
        val BR_OK = 2
        val BR_FAILED = 3
    }
}
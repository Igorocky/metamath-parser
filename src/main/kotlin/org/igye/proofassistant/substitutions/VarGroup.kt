package org.igye.proofassistant.substitutions

// TODO: 11/5/2021 move VarGroup to Assertion.proofAssistantData
class VarGroup(
    val asrtStmt:IntArray,
    var numOfVars:Int = 0,
    var varsBeginIdx:Int = 0,
    var exprBeginIdx:Int = 0,
    var exprEndIdx:Int = 0,
) {
    var numberOfStates = getNumberOfStates(exprBeginIdx, exprEndIdx)

    fun init(numOfVars:Int, varsBeginIdx:Int, exprBeginIdx:Int, exprEndIdx:Int) {
        this.exprBeginIdx = exprBeginIdx
        this.exprEndIdx = exprEndIdx
        this.numOfVars = numOfVars
        this.varsBeginIdx = varsBeginIdx
        numberOfStates = getNumberOfStates(exprBeginIdx, exprEndIdx)
    }

    private fun getNumberOfStates(exprBeginIdx:Int, exprEndIdx:Int): Long {
        return numberOfStates(numOfVars = numOfVars, subExprLength = exprEndIdx-exprBeginIdx+1)
    }

    companion object {
        fun numberOfStates(numOfVars:Int, subExprLength:Int): Long {
            val n = subExprLength - 1L
            val k = numOfVars - 1L

            var res = 1L
            var rem = 2L
            for (i in (n-k)+1 .. n) {
                res = Math.multiplyExact(res,i)
                while (rem <= k && res.mod(rem) == 0L) {
                    res /= rem++
                }
            }
            while (rem <= k) {
                res /= rem++
            }
            return res
        }
    }
}
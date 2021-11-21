package org.igye.proofassistant.substitutions

// TODO: 11/5/2021 move VarGroup to Assertion.proofAssistantData
class VarGroup(
    val asrtStmt:IntArray,
    val numOfVars:Int,
    val varsBeginIdx:Int,
    val exprBeginIdx:Int,
    val exprEndIdx:Int,
    var level:Int = -1,
) {
    val numberOfStates = numberOfStates(numOfVars = numOfVars, subExprLength = exprEndIdx-exprBeginIdx+1)

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
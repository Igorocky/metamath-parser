package org.igye.proofassistant

import org.junit.Assert.*
import org.junit.Test

internal class VarGroupTest {

    @Test
    fun numberOfStates_produces_correct_results() {
        //given
        fun numberOfStatesExpected(numOfVars:Int, subExprLength:Int): Long {
            val grp = VarGroup(
                asrtStmt = IntArray(numOfVars){it},
                numOfVars = numOfVars,
                varsBeginIdx = 0,
                exprBeginIdx = 0,
                exprEndIdx = subExprLength-1
            )
            val subExprBegins: IntArray = IntArray(numOfVars+1)
            for (i in 0 until numOfVars) {
                subExprBegins[i] = grp.exprBeginIdx+i
            }
            subExprBegins[numOfVars]=grp.exprEndIdx+1
            fun nextDelims(): Boolean {
                var i = subExprBegins.size-2
                while (i > 0) {
                    if (subExprBegins[i] < grp.exprEndIdx-(subExprBegins.size-2-i)) {
                        subExprBegins[i]++
                        for (j in i+1 until subExprBegins.size-1) {
                            subExprBegins[j]=subExprBegins[j-1]+1
                        }
                        break
                    }
                    i--
                }
                return i > 0
            }
            var cnt = 1L
            while (nextDelims()) {
                cnt++
            }
            return cnt
        }

        //then
        for (numOfVars in 1 .. 10) {
            for (subExprLength in numOfVars .. 40) {
                assertEquals(
                    numberOfStatesExpected(numOfVars = numOfVars, subExprLength = subExprLength),
                    VarGroup(
                        asrtStmt = IntArray(numOfVars){it},
                        numOfVars = numOfVars,
                        varsBeginIdx = 0,
                        exprBeginIdx = 0,
                        exprEndIdx = subExprLength-1
                    ).numberOfStates
                )
            }
        }
    }
}
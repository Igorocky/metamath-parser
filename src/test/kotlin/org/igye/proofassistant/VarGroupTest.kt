package org.igye.proofassistant

import org.junit.Assert.*
import org.junit.Test

internal class VarGroupTest {
    @Test
    fun nextDelims_processes_delimiters_as_expected() {
        //given
        val varGrp = VarGroup(
            asrtStmt = intArrayOf(0,1,2),
            numOfVars = 3,
            varsBeginIdx = 0,
            sameVarsIdxs = null,
            exprBeginIdx = 10,
            exprEndIdx = 14,
        )

        //when
        varGrp.init()
        //then
        assertTrue(intArrayOf(10,11,12,15).contentEquals(varGrp.subExprBegins))

        //when
        assertTrue(varGrp.nextDelims())
        //then
        assertTrue(intArrayOf(10,11,13,15).contentEquals(varGrp.subExprBegins))

        //when
        assertTrue(varGrp.nextDelims())
        //then
        assertTrue(intArrayOf(10,11,14,15).contentEquals(varGrp.subExprBegins))

        //when
        assertTrue(varGrp.nextDelims())
        //then
        assertTrue(intArrayOf(10,12,13,15).contentEquals(varGrp.subExprBegins))

        //when
        assertTrue(varGrp.nextDelims())
        //then
        assertTrue(intArrayOf(10,12,14,15).contentEquals(varGrp.subExprBegins))

        //when
        assertTrue(varGrp.nextDelims())
        //then
        assertTrue(intArrayOf(10,13,14,15).contentEquals(varGrp.subExprBegins))
        assertFalse(varGrp.nextDelims())
    }

    @Test
    fun doesntContradictItself_produces_correct_results() {
        //given
        var stmt = intArrayOf(0,1,2,3,4,5,6,7,8,9)
        //then
        assertTrue(
            VarGroup(
                asrtStmt = intArrayOf(0,1,2),
                numOfVars = 3,
                varsBeginIdx = 0,
                sameVarsIdxs = null,
                exprBeginIdx = 1,
                exprEndIdx = 8,
            ).doesntContradictItself(stmt)
        )

        //given
        stmt = intArrayOf(0,1,2,3,4,5,6,7,8,9)
        //then
        assertFalse(
            VarGroup(
                asrtStmt = intArrayOf(2,1,2),
                numOfVars = 3,
                varsBeginIdx = 0,
                sameVarsIdxs = intArrayOf(0,2),
                exprBeginIdx = 1,
                exprEndIdx = 8,
            ).doesntContradictItself(stmt)
        )

        //given
        stmt = intArrayOf(0,1,2,3,4,5,1,2,3,9)
        //then
        assertTrue(
            VarGroup(
                asrtStmt = intArrayOf(2,1,2),
                numOfVars = 3,
                varsBeginIdx = 0,
                sameVarsIdxs = intArrayOf(0,2),
                exprBeginIdx = 1,
                exprEndIdx = 8,
            ).doesntContradictItself(stmt)
        )

        //given
        stmt = intArrayOf(0,1,2,3,4,5,1,2,4,9)
        //then
        assertFalse(
            VarGroup(
                asrtStmt = intArrayOf(2,1,2),
                numOfVars = 3,
                varsBeginIdx = 0,
                sameVarsIdxs = intArrayOf(0,2),
                exprBeginIdx = 1,
                exprEndIdx = 8,
            ).doesntContradictItself(stmt)
        )
    }

    @Test
    fun doesntContradict_produces_correct_results() {
        //given
        var stmt = intArrayOf(0,1,2,3,4,5,1,2,3,9)
        //then
        val varGrp1 = VarGroup(
            asrtStmt = intArrayOf(0, 1, 2),
            numOfVars = 3,
            varsBeginIdx = 0,
            sameVarsIdxs = null,
            exprBeginIdx = 1,
            exprEndIdx = 8,
        )
        val varGrp2 = VarGroup(
            asrtStmt = intArrayOf(2, 1, 3),
            numOfVars = 3,
            varsBeginIdx = 0,
            sameVarsIdxs = null,
            exprBeginIdx = 1,
            exprEndIdx = 8,
        )
        assertTrue(varGrp1.doesntContradict(stmt, varGrp2))

        //given
        stmt = intArrayOf(0,1,1,3,4,5,1,2,3,9)
        //then
        val varGrp3 = VarGroup(
            asrtStmt = intArrayOf(0, 1, 2),
            numOfVars = 3,
            varsBeginIdx = 0,
            sameVarsIdxs = null,
            exprBeginIdx = 1,
            exprEndIdx = 8,
        )
        val varGrp4 = VarGroup(
            asrtStmt = intArrayOf(2, 1, 3),
            numOfVars = 3,
            varsBeginIdx = 0,
            sameVarsIdxs = null,
            exprBeginIdx = 1,
            exprEndIdx = 8,
        )
        assertFalse(varGrp3.doesntContradict(stmt, varGrp4))
    }

    @Test
    fun numberOfStates_produces_correct_results() {
        //given
        fun numberOfStatesExpected(numOfVars:Int, subExprLength:Int): Long {
            val grp = VarGroup(
                asrtStmt = IntArray(numOfVars){it},
                numOfVars = numOfVars,
                varsBeginIdx = 0,
                sameVarsIdxs = null,
                exprBeginIdx = 0,
                exprEndIdx = subExprLength-1
            )
            grp.init()
            var cnt = 1L
            while (grp.nextDelims()) {
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
                        sameVarsIdxs = null,
                        exprBeginIdx = 0,
                        exprEndIdx = subExprLength-1
                    ).numberOfStates
                )
            }
        }
    }
}
package org.igye.proofassistant

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

internal class VarGroupTest {
    @Test
    fun nextDelims_processes_delimiters_as_expected() {
        //given
        val varGrp = VarGroup(
            vars = intArrayOf(0,1,2),
            sameVarsIdxs = null,
            exprBeginIdx = 10,
            exprEndIdx = 14,
            subExprBegins = IntArray(4)
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
                vars = intArrayOf(0,1,2),
                sameVarsIdxs = null,
                exprBeginIdx = 1,
                exprEndIdx = 8,
                subExprBegins = intArrayOf(1,3,6,9)
            ).doesntContradictItself(stmt)
        )

        //given
        stmt = intArrayOf(0,1,2,3,4,5,6,7,8,9)
        //then
        assertFalse(
            VarGroup(
                vars = intArrayOf(2,1,2),
                sameVarsIdxs = intArrayOf(0,2),
                exprBeginIdx = 1,
                exprEndIdx = 8,
                subExprBegins = intArrayOf(1,3,6,9)
            ).doesntContradictItself(stmt)
        )

        //given
        stmt = intArrayOf(0,1,2,3,4,5,1,2,3,9)
        //then
        assertTrue(
            VarGroup(
                vars = intArrayOf(2,1,2),
                sameVarsIdxs = intArrayOf(0,2),
                exprBeginIdx = 1,
                exprEndIdx = 8,
                subExprBegins = intArrayOf(1,4,6,9)
            ).doesntContradictItself(stmt)
        )

        //given
        stmt = intArrayOf(0,1,2,3,4,5,1,2,4,9)
        //then
        assertFalse(
            VarGroup(
                vars = intArrayOf(2,1,2),
                sameVarsIdxs = intArrayOf(0,2),
                exprBeginIdx = 1,
                exprEndIdx = 8,
                subExprBegins = intArrayOf(1,4,6,9)
            ).doesntContradictItself(stmt)
        )
    }

    @Test
    fun doesntContradict_produces_correct_results() {
        //given
        var stmt = intArrayOf(0,1,2,3,4,5,1,2,3,9)
        //then
        val varGrp1 = VarGroup(
            vars = intArrayOf(0, 1, 2),
            sameVarsIdxs = null,
            exprBeginIdx = 1,
            exprEndIdx = 8,
            subExprBegins = intArrayOf(1, 4, 6, 9)
        )
        val varGrp2 = VarGroup(
            vars = intArrayOf(2, 1, 3),
            sameVarsIdxs = null,
            exprBeginIdx = 1,
            exprEndIdx = 8,
            subExprBegins = intArrayOf(1, 4, 6, 9)
        )
        assertTrue(varGrp1.doesntContradict(stmt, varGrp2))

        //given
        stmt = intArrayOf(0,1,1,3,4,5,1,2,3,9)
        //then
        val varGrp3 = VarGroup(
            vars = intArrayOf(0, 1, 2),
            sameVarsIdxs = null,
            exprBeginIdx = 1,
            exprEndIdx = 8,
            subExprBegins = intArrayOf(1, 4, 6, 9)
        )
        val varGrp4 = VarGroup(
            vars = intArrayOf(2, 1, 3),
            sameVarsIdxs = null,
            exprBeginIdx = 1,
            exprEndIdx = 8,
            subExprBegins = intArrayOf(1, 4, 6, 9)
        )
        assertFalse(varGrp3.doesntContradict(stmt, varGrp4))
    }
}
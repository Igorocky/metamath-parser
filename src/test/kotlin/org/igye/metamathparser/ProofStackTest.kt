package org.igye.metamathparser

import org.junit.Assert
import org.junit.Test

internal class ProofStackTest {
    @Test
    fun applySubstitution_applies_substitution_correctly() {
        //given
        val subs = mapOf(
            1 to intArrayOf(10),
            2 to intArrayOf(20,200),
            3 to intArrayOf(30,300,3000),
        )
        val proofStack = ProofStack()

        //then
        Assert.assertArrayEquals(intArrayOf(-1), proofStack.applySubstitution(intArrayOf(-1), subs))
        Assert.assertArrayEquals(intArrayOf(-1,-2), proofStack.applySubstitution(intArrayOf(-1,-2), subs))
        Assert.assertArrayEquals(intArrayOf(10), proofStack.applySubstitution(intArrayOf(1), subs))
        Assert.assertArrayEquals(intArrayOf(20,200), proofStack.applySubstitution(intArrayOf(2), subs))
        Assert.assertArrayEquals(intArrayOf(-1,30,300,3000,-1), proofStack.applySubstitution(intArrayOf(-1,3,-1), subs))
    }
}
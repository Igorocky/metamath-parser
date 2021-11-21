package org.igye.common

import org.igye.common.MetamathUtils.applySubstitution
import org.junit.Assert
import org.junit.Test

internal class MetamathUtilsTest {
    @Test
    fun applySubstitution_applies_substitution_correctly() {
        //given
        val subs = listOf(
            intArrayOf(10),
            intArrayOf(10),
            intArrayOf(20,200),
            intArrayOf(30,300,3000),
        )

        //then
        Assert.assertArrayEquals(intArrayOf(-1), applySubstitution(intArrayOf(-1), subs))
        Assert.assertArrayEquals(intArrayOf(-1,-2), applySubstitution(intArrayOf(-1,-2), subs))
        Assert.assertArrayEquals(intArrayOf(10), applySubstitution(intArrayOf(1), subs))
        Assert.assertArrayEquals(intArrayOf(20,200), applySubstitution(intArrayOf(2), subs))
        Assert.assertArrayEquals(intArrayOf(-1,30,300,3000,-1), applySubstitution(intArrayOf(-1,3,-1), subs))
    }
}
package org.igye.proofassistant

import org.junit.Assert.*
import org.junit.Test
import java.lang.Exception

internal class ProofAssistantTest {
    private val A = -1
    private val B = -2
    private val C = -3
    private val D = -4
    private val E = -5
    private val F = -6
    private val G = -7
    private val H = -8
    private val PP = -9
    private val BRO = -10
    private val BRC = -11
    private val ARR = -12
    private val a = 0
    private val b = 1
    private val c = 2
    private val d = 3

    @Test
    fun endsWith_test() {
        assertTrue(ProofAssistant.endsWith(what = intArrayOf(1,2,3,4,5), pattern = intArrayOf(1,2,3,4,5), begin = 2))
        assertTrue(ProofAssistant.endsWith(what = intArrayOf(1,2,3,4,5), pattern = intArrayOf(1,2,3,4,5), begin = 3))
        assertTrue(ProofAssistant.endsWith(what = intArrayOf(1,2,3,4,5,3,7,6), pattern = intArrayOf(0,0,3,7,6), begin = 2))
        assertTrue(ProofAssistant.endsWith(what = intArrayOf(1,2,3,4,5,3,7,6), pattern = intArrayOf(7,6), begin = 1))
        assertFalse(ProofAssistant.endsWith(what = intArrayOf(1,2,3,4,5,3,7,6), pattern = intArrayOf(0,0,3,7,6), begin = 1))
    }

    @Test
    fun iterateMatchingConstParts_one_option() {
        //given
        var cnt = 0
        val expectedConstParts = "[[1,1]]"
        val expectedMatchingConstParts = setOf(
            "[[1,1]]",
        )

        //when
        ProofAssistant.iterateMatchingConstParts(
            intArrayOf(A,ARR,B),
            intArrayOf(a,ARR,b)
        ) { constParts: List<IntArray>, matchingConstParts: Array<IntArray> ->
            //then
            assertEquals(expectedConstParts, constPartsToStr(constParts.toTypedArray()))
            assertTrue(expectedMatchingConstParts.contains(constPartsToStr(matchingConstParts)))
            cnt++
        }
        assertEquals(expectedMatchingConstParts.size,cnt)
    }

    @Test
    fun iterateMatchingConstParts_two_options() {
        //given
        var cnt = 0
        val expectedConstParts = "[[1,1]]"
        val expectedMatchingConstParts = setOf(
            "[[1,1]]",
            "[[3,3]]",
        )

        //when
        ProofAssistant.iterateMatchingConstParts(
            intArrayOf(A,ARR,B,ARR,C),
            intArrayOf(a,ARR,b)
        ) { constParts: List<IntArray>, matchingConstParts: Array<IntArray> ->
            //then
            assertEquals(expectedConstParts, constPartsToStr(constParts.toTypedArray()))
            assertTrue(expectedMatchingConstParts.contains(constPartsToStr(matchingConstParts)))
            cnt++
        }
        assertEquals(expectedMatchingConstParts.size,cnt)
    }

    @Test
    fun iterateMatchingConstParts_three_options() {
        //given
        var cnt = 0
        val expectedConstParts = "[[1,1],[3,3]]"
        val expectedMatchingConstParts = setOf(
            "[[1,1],[3,3]]",
            "[[1,1],[5,5]]",
            "[[3,3],[5,5]]",
        )

        //when
        ProofAssistant.iterateMatchingConstParts(
            intArrayOf(A,ARR,B,ARR,C,ARR,D),
            intArrayOf(a,ARR,b,ARR,c)
        ) { constParts: List<IntArray>, matchingConstParts: Array<IntArray> ->
            //then
            assertEquals(expectedConstParts, constPartsToStr(constParts.toTypedArray()))
            val constPartsStr = constPartsToStr(matchingConstParts)
            assertTrue(expectedMatchingConstParts.contains(constPartsStr))
            cnt++
        }
        assertEquals(expectedMatchingConstParts.size,cnt)
    }

    @Test
    fun iterateMatchingConstParts_asrt_starts_with_constant() {
        //given
        var cnt = 0
        val expectedConstParts = "[[0,0],[2,2]]"
        val expectedMatchingConstParts = setOf(
            "[[0,0],[2,2]]",
            "[[0,0],[4,4]]",
        )

        //when
        ProofAssistant.iterateMatchingConstParts(
            intArrayOf(PP,A,ARR,B,ARR,C),
            intArrayOf(PP,a,ARR,b)
        ) { constParts: List<IntArray>, matchingConstParts: Array<IntArray> ->
            //then
            assertEquals(expectedConstParts, constPartsToStr(constParts.toTypedArray()))
            val constPartsStr = constPartsToStr(matchingConstParts)
            assertTrue(expectedMatchingConstParts.contains(constPartsStr))
            cnt++
        }
        assertEquals(expectedMatchingConstParts.size,cnt)
    }

    @Test
    fun iterateMatchingConstParts_asrt_starts_with_constant_and_same_expression_is_present_inside_of_the_statement() {
        //given
        var cnt = 0
        val expectedConstParts = "[[0,0],[2,2]]"
        val expectedMatchingConstParts = setOf(
            "[[0,0],[2,2]]",
            "[[0,0],[5,5]]",
        )

        //when
        ProofAssistant.iterateMatchingConstParts(
            intArrayOf(PP,A,ARR,PP,B,ARR,C),
            intArrayOf(PP,a,ARR,b)
        ) { constParts: List<IntArray>, matchingConstParts: Array<IntArray> ->
            //then
            assertEquals(expectedConstParts, constPartsToStr(constParts.toTypedArray()))
            val constPartsStr = constPartsToStr(matchingConstParts)
            assertTrue(expectedMatchingConstParts.contains(constPartsStr))
            cnt++
        }
        assertEquals(expectedMatchingConstParts.size,cnt)
    }

    @Test
    fun iterateMatchingConstParts_there_are_sequences_of_more_than_one_constant() {
        //given
        var cnt = 0
        val expectedConstParts = "[[0,1],[3,3],[5,7],[9,9],[11,11]]"
        val expectedMatchingConstParts = setOf(
            "[[0,1],[3,3],[5,7],[9,9],[11,11]]",
        )

        //when
        ProofAssistant.iterateMatchingConstParts(
            intArrayOf(PP,BRO,A,ARR,B,BRC,ARR,BRO,B,ARR,C,BRC),
            intArrayOf(PP,BRO,a,ARR,b,BRC,ARR,BRO,a,ARR,b,BRC)
        ) { constParts: List<IntArray>, matchingConstParts: Array<IntArray> ->
            //then
            assertEquals(expectedConstParts, constPartsToStr(constParts.toTypedArray()))
            val constPartsStr = constPartsToStr(matchingConstParts)
            assertTrue(expectedMatchingConstParts.contains(constPartsStr))
            cnt++
        }
        assertEquals(expectedMatchingConstParts.size,cnt)
    }

    @Test
    fun iterateMatchingConstParts_few_options_and_asrt_ends_with_constant() {
        //given
        var cnt = 0
        val expectedConstParts = "[[1,1],[3,3]]"
        val expectedMatchingConstParts = setOf(
            "[[1,1],[5,5]]",
            "[[3,3],[5,5]]",
        )

        //when
        ProofAssistant.iterateMatchingConstParts(
            intArrayOf(A,ARR,B,ARR,C,ARR),
            intArrayOf(a,ARR,b,ARR)
        ) { constParts: List<IntArray>, matchingConstParts: Array<IntArray> ->
            //then
            assertEquals(expectedConstParts, constPartsToStr(constParts.toTypedArray()))
            val constPartsStr = constPartsToStr(matchingConstParts)
            assertTrue(expectedMatchingConstParts.contains(constPartsStr))
            cnt++
        }
        assertEquals(expectedMatchingConstParts.size,cnt)
    }

    @Test
    fun iterateMatchingConstParts_var_and_const() {
        //given
        var cnt = 0
        val expectedConstParts = "[[1,1]]"
        val expectedMatchingConstParts = setOf(
            "[[5,5]]" ,
        )

        //when
        ProofAssistant.iterateMatchingConstParts(
            intArrayOf(A,ARR,B,ARR,C,ARR),
            intArrayOf(a,ARR)
        ) { constParts: List<IntArray>, matchingConstParts: Array<IntArray> ->
            //then
            assertEquals(expectedConstParts, constPartsToStr(constParts.toTypedArray()))
            val constPartsStr = constPartsToStr(matchingConstParts)
            assertTrue(expectedMatchingConstParts.contains(constPartsStr))
            cnt++
        }
        assertEquals(expectedMatchingConstParts.size,cnt)
    }

    @Test
    fun iterateMatchingConstParts_gaps_between_some_constant_parts_are_less_than_number_of_variables_and_asrt_starts_with_constant() {
        //given
        var cnt = 0
        val expectedConstParts = "[[0,0],[4,4]]"
        val expectedMatchingConstParts = setOf(
            "[[0,0],[4,4]]",
        )

        //when
        ProofAssistant.iterateMatchingConstParts(
            intArrayOf(PP,A,B,C,ARR,A,B,D,ARR,C),
            intArrayOf(PP,a,b,c,ARR,a,b,d)
        ) { constParts: List<IntArray>, matchingConstParts: Array<IntArray> ->
            //then
            assertEquals(expectedConstParts, constPartsToStr(constParts.toTypedArray()))
            val constPartsStr = constPartsToStr(matchingConstParts)
            assertTrue(expectedMatchingConstParts.contains(constPartsStr))
            cnt++
        }
        assertEquals(expectedMatchingConstParts.size,cnt)
    }

    @Test
    fun iterateMatchingConstParts_gaps_between_some_constant_parts_are_less_than_number_of_variables_and_asrt_starts_with_non_constant() {
        if (true) throw Exception()
        //given
        var cnt = 0
        val expectedConstParts = "[[0,0],[2,2]]"
        val expectedMatchingConstParts = setOf(
            "[[0,0],[2,2]]",
            "[[0,0],[5,5]]",
        )

        //when
        ProofAssistant.iterateMatchingConstParts(
            intArrayOf(PP,A,ARR,PP,B,ARR,C),
            intArrayOf(PP,a,b,c,ARR,a,b,d)
        ) { constParts: List<IntArray>, matchingConstParts: Array<IntArray> ->
            //then
            assertEquals(expectedConstParts, constPartsToStr(constParts.toTypedArray()))
            val constPartsStr = constPartsToStr(matchingConstParts)
            assertTrue(expectedMatchingConstParts.contains(constPartsStr))
            cnt++
        }
        assertEquals(expectedMatchingConstParts.size,cnt)
    }

    private fun constPartsToStr(constParts:Array<IntArray>): String {
        return "[" + constParts.asSequence().map { "[${it[0]},${it[1]}]" }.joinToString(separator = ",") + "]"
    }
}
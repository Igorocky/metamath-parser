package org.igye.proofassistant

import org.junit.Assert.*
import org.junit.Test

internal class ProofAssistantTest {
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
        testIterateMatchingConstParts(IterateMatchingConstPartsTestData(
            asrtStmt = intArrayOf(a,ARR,b),
            expectedConstParts = "[[1,1]]",
            stmt = intArrayOf(A,ARR,B),
            expectedMatchingConstParts = setOf(
                "[[1,1]]",
            )
        ))
    }

    @Test
    fun iterateMatchingConstParts_two_options() {
        testIterateMatchingConstParts(IterateMatchingConstPartsTestData(
            asrtStmt = intArrayOf(a,ARR,b),
            expectedConstParts = "[[1,1]]",
            stmt = intArrayOf(A,ARR,B,ARR,C),
            expectedMatchingConstParts = setOf(
                "[[1,1]]",
                "[[3,3]]",
            )
        ))
    }

    @Test
    fun iterateMatchingConstParts_three_options() {
        testIterateMatchingConstParts(IterateMatchingConstPartsTestData(
            asrtStmt = intArrayOf(a,ARR,b,ARR,c),
            expectedConstParts = "[[1,1],[3,3]]",
            stmt = intArrayOf(A,ARR,B,ARR,C,ARR,D),
            expectedMatchingConstParts = setOf(
                "[[1,1],[3,3]]",
                "[[1,1],[5,5]]",
                "[[3,3],[5,5]]",
            )
        ))
    }

    @Test
    fun iterateMatchingConstParts_asrt_starts_with_constant() {
        testIterateMatchingConstParts(IterateMatchingConstPartsTestData(
            asrtStmt = intArrayOf(PP,a,ARR,b),
            expectedConstParts = "[[0,0],[2,2]]",
            stmt = intArrayOf(PP,A,ARR,B,ARR,C),
            expectedMatchingConstParts = setOf(
                "[[0,0],[2,2]]",
                "[[0,0],[4,4]]",
            )
        ))
    }

    @Test
    fun iterateMatchingConstParts_asrt_starts_with_constant_and_same_expression_is_present_inside_of_the_statement() {
        testIterateMatchingConstParts(IterateMatchingConstPartsTestData(
            asrtStmt = intArrayOf(PP,a,ARR,b),
            expectedConstParts = "[[0,0],[2,2]]",
            stmt = intArrayOf(PP,A,ARR,PP,B,ARR,C),
            expectedMatchingConstParts = setOf(
                "[[0,0],[2,2]]",
                "[[0,0],[5,5]]",
            )
        ))
    }

    @Test
    fun iterateMatchingConstParts_there_are_sequences_of_more_than_one_constant() {
        testIterateMatchingConstParts(IterateMatchingConstPartsTestData(
            asrtStmt = intArrayOf(PP,BRO,a,ARR,b,BRC,ARR,BRO,a,ARR,b,BRC),
            expectedConstParts = "[[0,1],[3,3],[5,7],[9,9],[11,11]]",
            stmt = intArrayOf(PP,BRO,A,ARR,B,BRC,ARR,BRO,B,ARR,C,BRC),
            expectedMatchingConstParts = setOf(
                "[[0,1],[3,3],[5,7],[9,9],[11,11]]",
            )
        ))
    }

    @Test
    fun iterateMatchingConstParts_few_options_and_asrt_ends_with_constant() {
        testIterateMatchingConstParts(IterateMatchingConstPartsTestData(
            asrtStmt = intArrayOf(a,ARR,b,ARR),
            expectedConstParts = "[[1,1],[3,3]]",
            stmt = intArrayOf(A,ARR,B,ARR,C,ARR),
            expectedMatchingConstParts = setOf(
                "[[1,1],[5,5]]",
                "[[3,3],[5,5]]",
            )
        ))
    }

    @Test
    fun iterateMatchingConstParts_var_and_const() {
        testIterateMatchingConstParts(IterateMatchingConstPartsTestData(
            asrtStmt = intArrayOf(a,ARR),
            expectedConstParts = "[[1,1]]",
            stmt = intArrayOf(A,ARR,B,ARR,C,ARR),
            expectedMatchingConstParts = setOf(
                "[[5,5]]",
            )
        ))
    }

    @Test
    fun iterateMatchingConstParts_gaps_between_some_constant_parts_are_less_than_number_of_variables_and_asrt_starts_with_constant() {
        testIterateMatchingConstParts(IterateMatchingConstPartsTestData(
            asrtStmt = intArrayOf(PP,a,b,c,ARR,a,b,d),
            expectedConstParts = "[[0,0],[4,4]]",
            stmt = intArrayOf(PP,A,B,C,ARR,A,B,D,ARR,C),
            expectedMatchingConstParts = setOf(
                "[[0,0],[4,4]]",
            )
        ))
    }

    @Test
    fun iterateMatchingConstParts_gaps_between_some_constant_parts_are_less_than_number_of_variables_and_asrt_starts_with_non_constant() {
        testIterateMatchingConstParts(IterateMatchingConstPartsTestData(
            asrtStmt = intArrayOf(a,b,c,ARR,a,b,d),
            expectedConstParts = "[[3,3]]",
            stmt = intArrayOf(A,ARR,B,ARR,C,D,E),
            expectedMatchingConstParts = setOf(
                "[[3,3]]",
            )
        ))

        testIterateMatchingConstParts(IterateMatchingConstPartsTestData(
            asrtStmt = intArrayOf(a,b,c,ARR,a,b,d),
            expectedConstParts = "[[3,3]]",
            stmt = intArrayOf(A,ARR,B,C,D),
            expectedMatchingConstParts = emptySet()
        ))

        testIterateMatchingConstParts(IterateMatchingConstPartsTestData(
            asrtStmt = intArrayOf(BRO,a,b,BRC),
            expectedConstParts = "[[0,0],[3,3]]",
            stmt = intArrayOf(BRO,A,BRC),
            expectedMatchingConstParts = emptySet()
        ))
    }

    @Test
    fun iterateSubstitutions_test1() {
        testIterateSubstitutions(
            IterateSubstitutionsTestData(
            asrtStmt = intArrayOf(PP,a,ARR,b),
            numOfVars = 2,
            stmt = intArrayOf(PP,A,ARR,B),
            expectedSubstitutions = listOf(
                mapOf(
                    0 to intArrayOf(A),
                    1 to intArrayOf(B),
                )
            )
        ))
    }

    fun testIterateMatchingConstParts(testData: IterateMatchingConstPartsTestData) {
        //given
        var cnt = 0

        //when
        ProofAssistant.iterateMatchingConstParts(
            testData.stmt,
            testData.asrtStmt
        ) { constParts: List<IntArray>, matchingConstParts: Array<IntArray> ->
            //then
            assertEquals(testData.expectedConstParts, constPartsToStr(constParts.toTypedArray()))
            val matchingConstPartsStr = constPartsToStr(matchingConstParts)
            assertTrue(testData.expectedMatchingConstParts.contains(matchingConstPartsStr))
            cnt++
        }
        assertEquals(testData.expectedMatchingConstParts.size,cnt)
    }

    fun testIterateSubstitutions(testData: IterateSubstitutionsTestData) {
        //given
        testData.expectedSubstitutions.forEach {
            assertTrue(it.size == testData.numOfVars)
            assertTrue(it.keys.all { it < testData.numOfVars })
            for ((_,v) in it) {
                assertTrue(v.isNotEmpty())
            }
        }
        var cnt = 0
        val expectedSubsStr: Set<String> = testData.expectedSubstitutions.map { subst ->
            subst.keys.asSequence().sorted().map {varNum -> substToStr(varNum, 0, subst[varNum]!!.size-1, subst[varNum]!!) }.joinToString(separator = ", ")
        }.toSet()

        //when
        ProofAssistant.iterateSubstitutions(
            testData.stmt,
            testData.asrtStmt,
            testData.numOfVars
        ) { subs: IntArray ->
            //then
            val actualSubsStr = actualSubstToStr(subs, testData.stmt)
            assertTrue(expectedSubsStr.contains(actualSubsStr))
            cnt++
        }
        assertEquals(testData.expectedSubstitutions.size,cnt)
    }

    private fun actualSubstToStr(subs:IntArray, stmt:IntArray): String {
        return (0 until subs.size/2).asSequence().map {varNum ->
            substToStr(varNum, subs[varNum*2], subs[varNum*2+1], stmt)
        }.joinToString(separator = ", ")
    }

    private fun substToStr(varNum:Int, stmtBegin:Int, stmtEnd:Int, stmt:IntArray): String {
        val sb = StringBuilder()
        sb.append(varNum).append(" ->")
        for (i in stmtBegin .. stmtEnd) {
            sb.append(" ").append(stmt[i])
        }
        return sb.toString()
    }

    private fun constPartsToStr(constParts:Array<IntArray>): String {
        return "[" + constParts.asSequence().map { "[${it[0]},${it[1]}]" }.joinToString(separator = ",") + "]"
    }

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

    data class IterateMatchingConstPartsTestData(
        val asrtStmt: IntArray,
        val expectedConstParts: String,
        val stmt: IntArray,
        val expectedMatchingConstParts: Set<String>
    )

    data class IterateSubstitutionsTestData(
        val asrtStmt: IntArray,
        val numOfVars: Int,
        val stmt: IntArray,
        val expectedSubstitutions: List<Map<Int,IntArray>>
    )
}
package org.igye.proofassistant

import org.junit.Assert.*
import org.junit.Ignore
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
            asrtStmt = "a -> b",
            expectedConstParts = "[[1,1]]",
            stmt = "A -> B",
            expectedMatchingConstParts = setOf(
                "[[1,1]]",
            )
        ))
    }

    @Test
    fun iterateMatchingConstParts_two_options() {
        testIterateMatchingConstParts(IterateMatchingConstPartsTestData(
            asrtStmt = "a -> b",
            expectedConstParts = "[[1,1]]",
            stmt = "A -> B -> C",
            expectedMatchingConstParts = setOf(
                "[[1,1]]",
                "[[3,3]]",
            )
        ))
    }

    @Test
    fun iterateMatchingConstParts_three_options() {
        testIterateMatchingConstParts(IterateMatchingConstPartsTestData(
            asrtStmt = "a -> b -> c",
            expectedConstParts = "[[1,1],[3,3]]",
            stmt = "A -> B -> C -> D",
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
            asrtStmt = "|- a -> b",
            expectedConstParts = "[[0,0],[2,2]]",
            stmt = "|- A -> B -> C",
            expectedMatchingConstParts = setOf(
                "[[0,0],[2,2]]",
                "[[0,0],[4,4]]",
            )
        ))
    }

    @Test
    fun iterateMatchingConstParts_asrt_starts_with_constant_and_same_expression_is_present_inside_of_the_statement() {
        testIterateMatchingConstParts(IterateMatchingConstPartsTestData(
            asrtStmt = "|- a -> b",
            expectedConstParts = "[[0,0],[2,2]]",
            stmt = "|- A -> |- B -> C",
            expectedMatchingConstParts = setOf(
                "[[0,0],[2,2]]",
                "[[0,0],[5,5]]",
            )
        ))
    }

    @Test
    fun iterateMatchingConstParts_there_are_sequences_of_more_than_one_constant() {
        testIterateMatchingConstParts(IterateMatchingConstPartsTestData(
            asrtStmt = "|- ( a -> b ) -> ( a -> b )",
            expectedConstParts = "[[0,1],[3,3],[5,7],[9,9],[11,11]]",
            stmt = "|- ( A -> B ) -> ( A -> B )",
            expectedMatchingConstParts = setOf(
                "[[0,1],[3,3],[5,7],[9,9],[11,11]]",
            )
        ))
    }

    @Test
    fun iterateMatchingConstParts_few_options_and_asrt_ends_with_constant() {
        testIterateMatchingConstParts(IterateMatchingConstPartsTestData(
            asrtStmt = "a -> b ->",
            expectedConstParts = "[[1,1],[3,3]]",
            stmt = "A -> B -> C ->",
            expectedMatchingConstParts = setOf(
                "[[1,1],[5,5]]",
                "[[3,3],[5,5]]",
            )
        ))
    }

    @Test
    fun iterateMatchingConstParts_var_and_const() {
        testIterateMatchingConstParts(IterateMatchingConstPartsTestData(
            asrtStmt = "a ->",
            expectedConstParts = "[[1,1]]",
            stmt = "A -> B -> C ->",
            expectedMatchingConstParts = setOf(
                "[[5,5]]",
            )
        ))
    }

    @Test
    fun iterateMatchingConstParts_gaps_between_some_constant_parts_are_less_than_number_of_variables_and_asrt_starts_with_constant() {
        testIterateMatchingConstParts(IterateMatchingConstPartsTestData(
            asrtStmt = "|- a b c -> a b d",
            expectedConstParts = "[[0,0],[4,4]]",
            stmt = "|- A B C -> A B D -> C",
            expectedMatchingConstParts = setOf(
                "[[0,0],[4,4]]",
            )
        ))
    }

    @Test
    fun iterateMatchingConstParts_gaps_between_some_constant_parts_are_less_than_number_of_variables_and_asrt_starts_with_non_constant() {
        testIterateMatchingConstParts(IterateMatchingConstPartsTestData(
            asrtStmt = "a b c -> a b d",
            expectedConstParts = "[[3,3]]",
            stmt = "A -> B -> C D E",
            expectedMatchingConstParts = setOf(
                "[[3,3]]",
            )
        ))

        testIterateMatchingConstParts(IterateMatchingConstPartsTestData(
            asrtStmt = "a b c -> a b d",
            expectedConstParts = "[[3,3]]",
            stmt = "A -> B C D",
            expectedMatchingConstParts = emptySet()
        ))

        testIterateMatchingConstParts(IterateMatchingConstPartsTestData(
            asrtStmt = "( a b )",
            expectedConstParts = "[[0,0],[3,3]]",
            stmt = "( a )",
            expectedMatchingConstParts = emptySet()
        ))
    }

    @Test
    fun iterateSubstitutions_one_option() {
        testIterateSubstitutions(IterateSubstitutionsTestData(
            asrtStmt = "|- a -> b",
            stmt = "|- A -> B",
            expectedSubstitutions = listOf(
                setOf("a: A", "b: B"),
            )
        ))
    }

    @Test
    fun iterateSubstitutions_two_options() {
        testIterateSubstitutions(IterateSubstitutionsTestData(
            asrtStmt = "|- a -> b",
            stmt = "|- A -> B -> C",
            expectedSubstitutions = listOf(
                setOf("a: A", "b: B -> C"),
                setOf("a: A -> B", "b: C"),
            )
        ))
    }

    @Test
    fun iterateSubstitutions_zero_options() {
        testIterateSubstitutions(IterateSubstitutionsTestData(
            asrtStmt = "|- a -> b",
            stmt = "|- A = B",
            expectedSubstitutions = listOf()
        ))
    }

    @Test
    fun iterateSubstitutions_one_variable_repeats() {
        testIterateSubstitutions(IterateSubstitutionsTestData(
            asrtStmt = "|- a -> b",
            stmt = "|- A -> B -> A -> B",
            expectedSubstitutions = listOf(
                setOf("a: A", "b: B -> A -> B"),
                setOf("a: A -> B", "b: A -> B"),
                setOf("a: A -> B -> A", "b: B"),
            )
        ))

        testIterateSubstitutions(IterateSubstitutionsTestData(
            asrtStmt = "|- a -> a",
            stmt = "|- A -> B -> A -> B",
            expectedSubstitutions = listOf(
                setOf("a: A -> B"),
            )
        ))
    }

    @Test
    fun iterateSubstitutions_case1_from_set_mm() {
//        val stmt = "|- ( ( A e. ( BaseSet ` if ( U e. CPreHilOLD , U , <. <. + , x. >. , abs >. ) ) /\\ B e. ( BaseSet ` if ( U e. CPreHilOLD , U , <. <. + , x. >. , abs >. ) ) /\\ C e. ( BaseSet ` if ( U e. CPreHilOLD , U , <. <. + , x. >. , abs >. ) ) ) -> ( ( A ( +v ` if ( U e. CPreHilOLD , U , <. <. + , x. >. , abs >. ) ) B ) ( .iOLD ` if ( U e. CPreHilOLD , U , <. <. + , x. >. , abs >. ) ) C ) = ( ( A ( .iOLD ` if ( U e. CPreHilOLD , U , <. <. + , x. >. , abs >. ) ) C ) + ( B ( .iOLD ` if ( U e. CPreHilOLD , U , <. <. + , x. >. , abs >. ) ) C ) ) )"
//        stmt.split(" ").asSequence().filter { !symbolToInt.containsKey(it) }.toSet().forEach { println(it) }

        testIterateSubstitutions(IterateSubstitutionsTestData(
            asrtStmt = "|- ( ( a e. f /\\ b e. f /\\ c e. f ) -> ( ( a e b ) d c ) = ( ( a d c ) + ( b d c ) ) )",
            stmt = "|- ( ( A e. ( BaseSet ` if ( U e. CPreHilOLD , U , <. <. + , x. >. , abs >. ) ) /\\ B e. ( BaseSet ` if ( U e. CPreHilOLD , U , <. <. + , x. >. , abs >. ) ) /\\ C e. ( BaseSet ` if ( U e. CPreHilOLD , U , <. <. + , x. >. , abs >. ) ) ) -> ( ( A ( +v ` if ( U e. CPreHilOLD , U , <. <. + , x. >. , abs >. ) ) B ) ( .iOLD ` if ( U e. CPreHilOLD , U , <. <. + , x. >. , abs >. ) ) C ) = ( ( A ( .iOLD ` if ( U e. CPreHilOLD , U , <. <. + , x. >. , abs >. ) ) C ) + ( B ( .iOLD ` if ( U e. CPreHilOLD , U , <. <. + , x. >. , abs >. ) ) C ) ) )",
            expectedSubstitutions = listOf(
                setOf(
                    "a: A",
                    "b: B",
                    "c: C",
                    "d: ( .iOLD ` if ( U e. CPreHilOLD , U , <. <. + , x. >. , abs >. ) )",
                    "e: ( +v ` if ( U e. CPreHilOLD , U , <. <. + , x. >. , abs >. ) )",
                    "f: ( BaseSet ` if ( U e. CPreHilOLD , U , <. <. + , x. >. , abs >. ) )"
                ),
            )
        ))
    }

    @Ignore
    @Test
    fun iterateSubstitutions_case2_from_set_mm() {
        if (true) return
        testIterateSubstitutions(IterateSubstitutionsTestData(
            asrtStmt = "|- ( $0 -> ( ( $1 -> $2 ) -> ( ( ( $3 -> $1 ) -> ( $2 -> $4 ) ) -> ( $1 -> $4 ) ) ) )",
            stmt = "|- ( ( ( ( ph -> ps ) -> ( ph -> ps ) ) -> ( ( ( ( ( ph -> ps ) -> ch ) -> ( ph -> ps ) ) -> ( ( ph -> ps ) -> ( ph -> ps ) ) ) -> ( ( ph -> ps ) -> ( ph -> ps ) ) ) ) -> ( ( ( ( ( ph -> ps ) -> ( ph -> ps ) ) -> ( ( ( ( ( ph -> ps ) -> ch ) -> ( ph -> ps ) ) -> ( ( ph -> ps ) -> ( ph -> ps ) ) ) -> ( ( ph -> ps ) -> ( ph -> ps ) ) ) ) -> ( ( ( ph -> ps ) -> ( ph -> ps ) ) -> ( ( ( ( ( ph -> ps ) -> ch ) -> ( ph -> ps ) ) -> ( ( ph -> ps ) -> ( ph -> ps ) ) ) -> ( ( ph -> ps ) -> ( ph -> ps ) ) ) ) ) -> ( ( ( ( ( ( ph -> ps ) -> ( ph -> ps ) ) -> ( ( ( ( ( ph -> ps ) -> ch ) -> ( ph -> ps ) ) -> ( ( ph -> ps ) -> ( ph -> ps ) ) ) -> ( ( ph -> ps ) -> ( ph -> ps ) ) ) ) -> ( ( ( ph -> ps ) -> ( ph -> ps ) ) -> ( ( ( ( ( ph -> ps ) -> ch ) -> ( ph -> ps ) ) -> ( ( ph -> ps ) -> ( ph -> ps ) ) ) -> ( ( ph -> ps ) -> ( ph -> ps ) ) ) ) ) -> ( ( ( ( ph -> ps ) -> ( ph -> ps ) ) -> ( ( ( ( ( ph -> ps ) -> ch ) -> ( ph -> ps ) ) -> ( ( ph -> ps ) -> ( ph -> ps ) ) ) -> ( ( ph -> ps ) -> ( ph -> ps ) ) ) ) -> ( ( ( ph -> ps ) -> ( ph -> ps ) ) -> ( ( ( ( ( ph -> ps ) -> ch ) -> ( ph -> ps ) ) -> ( ( ph -> ps ) -> ( ph -> ps ) ) ) -> ( ( ph -> ps ) -> ( ph -> ps ) ) ) ) ) ) -> ( ( ( ( ph -> ps ) -> ( ph -> ps ) ) -> ( ( ( ( ( ph -> ps ) -> ch ) -> ( ph -> ps ) ) -> ( ( ph -> ps ) -> ( ph -> ps ) ) ) -> ( ( ph -> ps ) -> ( ph -> ps ) ) ) ) -> ( ( ( ph -> ps ) -> ( ph -> ps ) ) -> ( ( ( ( ( ph -> ps ) -> ch ) -> ( ph -> ps ) ) -> ( ( ph -> ps ) -> ( ph -> ps ) ) ) -> ( ( ph -> ps ) -> ( ph -> ps ) ) ) ) ) ) ) )",
            expectedSubstitutions = listOf(
                setOf(
                    "a: A",
                    "b: B",
                    "c: C",
                    "d: ( .iOLD ` if ( U e. CPreHilOLD , U , <. <. + , x. >. , abs >. ) )",
                    "e: ( +v ` if ( U e. CPreHilOLD , U , <. <. + , x. >. , abs >. ) )",
                    "f: ( BaseSet ` if ( U e. CPreHilOLD , U , <. <. + , x. >. , abs >. ) )"
                ),
            )
        ))
    }

    fun testIterateMatchingConstParts(testData: IterateMatchingConstPartsTestData) {
        //given
        var cnt = 0

        //when
        ProofAssistant.iterateMatchingConstParts(
            Symbols.stmtToArr(testData.stmt),
            Symbols.stmtToArr(testData.asrtStmt)
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
        var cnt = 0
        val expectedSubsStr: Set<String> = testData.expectedSubstitutions.map { subst ->
            subst.asSequence().sortedBy{Symbols.toInt(it.split(":")[0].trim())!!}.joinToString(separator = ", ")
        }.toSet()
        val stmt = Symbols.stmtToArr(testData.stmt)

        //when
        ProofAssistant.iterateSubstitutions(
            stmt = stmt,
            asrtStmt = Symbols.stmtToArr(testData.asrtStmt),
            parenCounter = {
                ParenthesesCounter(
                    roundBracketOpen = Symbols.toInt("("),
                    roundBracketClose = Symbols.toInt(")"),
                    curlyBracketOpen = Symbols.toInt("{"),
                    curlyBracketClose = Symbols.toInt("}"),
                    squareBracketOpen = Symbols.toInt("["),
                    squareBracketClose = Symbols.toInt("]"),
                )
            }
        ) { subs: Substitution ->
            //then
            val asrtStmtStr = testData.asrtStmt
            val stmtStr = testData.stmt
            val actualSubsStr = actualSubstToStr(subs)
            assertTrue(expectedSubsStr.contains(actualSubsStr))
            cnt++
        }
        assertEquals(testData.expectedSubstitutions.size,cnt)
    }

    private fun actualSubstToStr(subs:Substitution): String {
        return (0 until subs.begins.size).asSequence().filter { subs.levels[it] < Int.MAX_VALUE }.map {varNum ->
            substToStr(varNum, subs.begins[varNum], subs.ends[varNum], subs.stmt)
        }.joinToString(separator = ", ")
    }

    private fun substToStr(varNum:Int, stmtBegin:Int, stmtEnd:Int, stmt:IntArray): String {
        val sb = StringBuilder()
        sb.append(Symbols.toSymb(varNum)).append(":")
        for (i in stmtBegin .. stmtEnd) {
            sb.append(" ").append(Symbols.toSymb(stmt[i]))
        }
        return sb.toString()
    }

    private fun constPartsToStr(constParts:Array<IntArray>): String {
        return "[" + constParts.asSequence().map { "[${it[0]},${it[1]}]" }.joinToString(separator = ",") + "]"
    }

    data class IterateMatchingConstPartsTestData(
        val asrtStmt: String,
        val expectedConstParts: String,
        val stmt: String,
        val expectedMatchingConstParts: Set<String>
    )

    data class IterateSubstitutionsTestData(
        val asrtStmt: String,
        val stmt: String,
        val expectedSubstitutions: List<Set<String>>
    )
}
package org.igye.metamathparser

import org.igye.metamathparser.ExpressionProcessor.splitEncodedProof
import org.igye.metamathparser.ExpressionProcessor.strToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.*

internal class ExpressionProcessorTest {
    @Test
    fun successfully_processes_metamath_file_with_noncompressed_valid_proofs() {
        //when
        val assertions: Map<String, Assertion> =
            Parsers.traverseMetamathFile(text = Utils.readStringFromClassPath("/demo0.mm"), ExpressionProcessor).getAssertions()

        //then
        assertEquals(setOf("tze","tpl","weq","wim","a1","a2","mp","th1"), assertions.keys)
    }

    @Test(expected = MetamathParserException::class)
    fun fails_to_processes_metamath_file_with_noncompressed_invalid_proofs() {
        //when
        Parsers.traverseMetamathFile(text = Utils.readStringFromClassPath("/demo0-with-incorrect-proof.mm"), ExpressionProcessor)
    }

    @Test
    fun successfully_processes_metamath_file_with_compressed_valid_proofs() {
        //when
        val assertions: Map<String, Assertion> =
            Parsers.traverseMetamathFile(text = Utils.readStringFromClassPath("/set-reduced.mm"), ExpressionProcessor).getAssertions()

        //then
        assertEquals(193, assertions.size)
        assertTrue(assertions.containsKey("mp2"))
        assertTrue(assertions.containsKey("id"))
        assertTrue(assertions.containsKey("dfbi1ALT"))
    }

    @Test
    fun strToInt_shouldParseNumbersFromCompressedProofs() {
        assertEquals(1, strToInt("A"))
        assertEquals(2, strToInt("B"))
        assertEquals(20, strToInt("T"))
        assertEquals(21, strToInt("UA"))
        assertEquals(22, strToInt("UB"))
        assertEquals(40, strToInt("UT"))
        assertEquals(41, strToInt("VA"))
        assertEquals(42, strToInt("VB"))
        assertEquals(120, strToInt("YT"))
        assertEquals(121, strToInt("UUA"))
        assertEquals(620, strToInt("YYT"))
        assertEquals(621, strToInt("UUUA"))
    }

    @Test
    fun splitEncodedProof_shouldSplitEncodedProofIntoParts() {
        //when
        val parts: List<String> = splitEncodedProof("ABCZUACZYYWA")

        //then
        assertEquals(Arrays.asList("A", "B", "C", "Z", "UA", "C", "Z", "YYWA"), parts)
    }
}
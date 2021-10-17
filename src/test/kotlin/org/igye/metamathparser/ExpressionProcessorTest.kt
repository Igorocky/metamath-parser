package org.igye.metamathparser

import org.igye.common.Utils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

internal class ExpressionProcessorTest {
    @Test
    fun successfully_processes_metamath_file_with_noncompressed_proofs() {
        //when
        val assertions: Map<String, Assertion> =
            Parsers.parseMetamathFile(text = Utils.readStringFromClassPath("/demo0.mm"), ExpressionProcessor).getAssertions()

        //then
        assertEquals(setOf("tze","tpl","weq","wim","a1","a2","mp","th1","th2"), assertions.keys)
    }

    @Test
    fun successfully_processes_metamath_file_with_compressed_proofs() {
        //when
        val assertions: Map<String, Assertion> =
            Parsers.parseMetamathFile(text = Utils.readStringFromClassPath("/set-reduced.mm"), ExpressionProcessor).getAssertions()

        //then
        assertEquals(193, assertions.size)
        assertTrue(assertions.containsKey("mp2"))
        assertTrue(assertions.containsKey("id"))
        assertTrue(assertions.containsKey("dfbi1ALT"))
    }
}
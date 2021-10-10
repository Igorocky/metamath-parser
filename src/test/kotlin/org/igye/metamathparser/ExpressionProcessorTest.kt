package org.igye.metamathparser

import org.junit.Assert
import org.junit.Test

internal class ExpressionProcessorTest {
    @Test
    fun successfully_processes_metamath_file_with_noncompressed_valid_proofs() {
        //when
        val assertions: Map<String, Assertion> =
            Parsers.traverseMetamathFile(text = Utils.readStringFromClassPath("/demo0.mm"), ExpressionProcessor)

        //then
        Assert.assertEquals(setOf("tze","tpl","weq","wim","a1","a2","mp","th1"), assertions.keys)
    }

    @Test(expected = MetamathParserException::class)
    fun fails_to_processes_metamath_file_with_noncompressed_invalid_proofs() {
        //when
        val assertions: Map<String, Assertion> =
            Parsers.traverseMetamathFile(text = Utils.readStringFromClassPath("/demo0-with-incorrect-proof.mm"), ExpressionProcessor)
    }
}
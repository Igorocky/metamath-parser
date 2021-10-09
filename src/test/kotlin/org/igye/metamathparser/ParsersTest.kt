package org.igye.metamathparser

import org.junit.Assert.assertEquals
import org.junit.Test


internal class ParsersTest {
    @Test
    fun parseConstantStmt_parses_list_of_constants() {
        //when
        val parserOutput: ParserOutput<ConstantStmt> =
            Parsers.parseConstantStmt(ParserInput(text = "\$c 0 + = -> ( ) term wff |- \$.", begin = 0))

        //then
        assertEquals(
            listOf("0", "+", "=", "->", "(", ")", "term", "wff", "|-"),
            parserOutput.result.symbols
        )
        assertEquals(29, parserOutput.end)
    }

    @Test
    fun parseVariableStmt_parses_list_of_variables() {
        //when
        val parserOutput: ParserOutput<VariableStmt> =
            Parsers.parseVariableStmt(ParserInput(text = "\$v t r s P Q \$.", begin = 0))

        //then
        assertEquals(
            listOf("t", "r", "s", "P", "Q"),
            parserOutput.result.symbols
        )
        assertEquals(14, parserOutput.end)
    }

    @Test
    fun parseLabeledSequence_parses_labeled_list_of_symbols() {
        //when
        val parserOutput: ParserOutput<LabeledSequence> =
            Parsers.parseLabeledSequence(ParserInput(text = "maj \$e |- ( P -> Q ) \$.", begin = 0))

        //then
        val labeledSequence = parserOutput.result
        assertEquals("maj", labeledSequence.label)
        assertEquals('e', labeledSequence.seqType)
        assertEquals(listOf("|-", "(", "P", "->", "Q", ")"), labeledSequence.symbols)
        assertEquals(22, parserOutput.end)
    }
}
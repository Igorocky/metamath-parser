package org.igye.metamathparser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test


internal class ParsersTest {
    @Test
    fun parseSequenceOfSymbols_parses_sequence_of_symbols_with_proof() {
        //when
        val parserOutput: ParserOutput<SequenceOfSymbols> =
            Parsers.parseSequenceOfSymbols(ParserInput(
                text = "\$p |- t = t \$=\n" +
                    "       tt tze tpl tt weq \n" +
                    "     \$.",
                begin = 0
            ))

        //then
        val sequenceOfSymbols = parserOutput.result
        assertEquals(0, sequenceOfSymbols.beginIdx)
        assertEquals(47, parserOutput.end)

        assertEquals('p', sequenceOfSymbols.seqType)
        assertEquals(listOf("|-", "t", "=", "t"), sequenceOfSymbols.symbols)
        assertEquals(listOf("tt", "tze", "tpl", "tt", "weq",), sequenceOfSymbols.proof)
    }

    @Test
    fun parseLabeledSequence_parses_labeled_list_of_symbols() {
        //when
        val parserOutput: ParserOutput<LabeledSequenceOfSymbols> =
            Parsers.parseLabeledSequenceOfSymbols(ParserInput(text = "maj \$e |- ( P -> Q ) \$.", begin = 0))

        //then
        val labeledSequence = parserOutput.result
        assertEquals(0, labeledSequence.beginIdx)
        assertEquals(4, labeledSequence.sequence.beginIdx)
        assertEquals(22, parserOutput.end)

        assertEquals("maj", labeledSequence.label)
        assertEquals('e', labeledSequence.sequence.seqType)
        assertEquals(listOf("|-", "(", "P", "->", "Q", ")"), labeledSequence.sequence.symbols)
        assertNull(labeledSequence.sequence.proof)
    }

    @Test
    fun parseComment_parses_comment() {
        //when
        val parserOutput: ParserOutput<Comment> =
            Parsers.parseComment(ParserInput(text = "\$( demo0.mm  1-Jan-04 \$)", begin = 0))

        //then
        val comment = parserOutput.result
        assertEquals(0, comment.beginIdx)
        assertEquals(23, parserOutput.end)
        assertEquals(" demo0.mm  1-Jan-04 ", comment.text)
    }
}
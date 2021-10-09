package org.igye.metamathparser

import org.junit.Assert.assertEquals
import org.junit.Test


internal class ParsersTest {
    @Test
    fun parseLabeledSequence_parses_labeled_list_of_symbols() {
        //when
        val parserOutput: ParserOutput<LabeledSequenceOfSymbols> =
            Parsers.parseLabeledSequence(ParserInput(text = "maj \$e |- ( P -> Q ) \$.", begin = 0))

        //then
        val labeledSequence = parserOutput.result
        assertEquals(0, labeledSequence.beginIdx)
        assertEquals(4, labeledSequence.sequence.beginIdx)
        assertEquals(22, parserOutput.end)

        assertEquals("maj", labeledSequence.label)
        assertEquals('e', labeledSequence.sequence.seqType)
        assertEquals(listOf("|-", "(", "P", "->", "Q", ")"), labeledSequence.sequence.symbols)
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
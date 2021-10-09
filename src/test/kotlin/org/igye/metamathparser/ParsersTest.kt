package org.igye.metamathparser

import org.junit.Assert.*
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
        assertEquals(23, comment.endIdx)
        assertEquals(23, parserOutput.end)
        assertEquals(" demo0.mm  1-Jan-04 ", comment.text)
    }

    @Test
    fun extractComments_separates_comments_and_regular_text() {
        //when
        val (comments: List<Comment>, nonComments: List<NonComment>) = Parsers.extractComments(Utils.readStringFromClassPath("/demo0.mm"))

        //then
        val expectedComments = listOf(
            " demo0.mm  1-Jan-04 ",
            "\n" +
                    "                           ~~ PUBLIC DOMAIN ~~\n" +
                    "This work is waived of all rights, including copyright, according to the CC0\n" +
                    "Public Domain Dedication.  http://creativecommons.org/publicdomain/zero/1.0/\n" +
                    "\n" +
                    "Norman Megill - email: nm at alum.mit.edu\n" +
                    "\n",
            " This file is the introductory formal system example described\n" +
                    "   in Chapter 2 of the Meamath book. ",
            " Declare the constant symbols we will use ",
            " Declare the metavariables we will use ",
            " Specify properties of the metavariables ",
            " Define \"term\" (part 1) ",
            " Define \"term\" (part 2) ",
            " Define \"wff\" (part 1) ",
            " Define \"wff\" (part 2) ",
            " State axiom a1 ",
            " State axiom a2 ",
            " Define the modus ponens inference rule ",
            " Prove a theorem ",
            " Here is its proof: ",
            " A theorem with invalid proof (two proof steps were swapped in comparison to the previous theorem) ",
            " Here is its proof: ",
        )
        assertEquals(expectedComments.size, comments.size)
        val actualComments = comments.map { it.text.filter { ch -> ch != '\r' } }
        assertEquals(expectedComments, actualComments)
    }


}
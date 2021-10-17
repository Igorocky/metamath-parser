package org.igye.metamathparser

import org.igye.common.Utils
import org.junit.Assert.*
import org.junit.Test


internal class ParsersTest {
    @Test
    fun parseSequenceOfSymbols_parses_sequence_of_symbols_with_uncompressed_proof() {
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
        assertEquals(listOf("tt", "tze", "tpl", "tt", "weq",), sequenceOfSymbols.uncompressedProof)
    }

    @Test
    fun parseSequenceOfSymbols_parses_sequence_of_symbols_with_compressed_proof() {
        //when
        val parserOutput: ParserOutput<SequenceOfSymbols> =
            Parsers.parseSequenceOfSymbols(ParserInput(
                text = "\$p |-\n" +
                        "                ( ( ph <-> ps ) <-> -. ( ( ph -> ps ) -> -. ( ps -> ph ) ) ) \$=\n" +
                        "    ( wch wth wb wi wn df-bi ax-1 ax-mp ax-3 ax-2 ) ABEZABFBAFGFGZFNMFGFGZMNEZA\n" +
                        "    BHCDCFFZOPFZCDIRGZQGZFZQRFSPOFZSFZFZUASUBISUCTFZFZUDUAFUEUFTGZUCGZFZUEUHUIM\n" +
                        "    NHUHUGIJTUCKJUESIJSUCTLJJRQKJJJ \$.",
                begin = 0
            ))

        //then
        val sequenceOfSymbols = parserOutput.result
        assertNotNull(sequenceOfSymbols.compressedProof)
        assertEquals(
            listOf("wch", "wth", "wb", "wi", "wn", "df-bi", "ax-1", "ax-mp", "ax-3", "ax-2"),
            sequenceOfSymbols.compressedProof!!.labels
        )
        assertEquals(
            "ABEZABFBAFGFGZFNMFGFGZMNEZABHCDCFFZOPFZCDIRGZQGZFZQRFSPOFZSFZFZUASUBISUCTFZFZUDUAFUEUFTGZUCGZFZUEUHUIMNHUHUGIJTUCKJUESIJSUCTLJJRQKJJJ",
            sequenceOfSymbols.compressedProof!!.proof
        )
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
        assertNull(labeledSequence.sequence.uncompressedProof)
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

    @Test
    fun traverseMetamathFile_traverses_metamath_file_correctly() {
        //given
        val expressins = ArrayList<List<String>>()

        //when
        Parsers.parseMetamathFile(text = Utils.readStringFromClassPath("/demo0.mm")) { ctx, expr ->
            expressins.add(
                when (expr) {
                    is SequenceOfSymbols -> expr.symbols
                    is LabeledSequenceOfSymbols -> listOf("<<${expr.label}:${expr.sequence.seqType}>>", *expr.sequence.symbols.toTypedArray())
                    else -> throw MetamathParserException()
                }
            )
            ctx
        }

        //then
        val expectedExpressions: List<List<String>> = listOf(
            "0 + = -> ( ) term wff |-",
            "t r s P Q",
            "<<tt:f>> term t",
            "<<tr:f>> term r",
            "<<ts:f>> term s",
            "<<wp:f>> wff P",
            "<<wq:f>> wff Q",
            "<<tze:a>> term 0",
            "<<tpl:a>> term ( t + r )",
            "<<weq:a>> wff t = r",
            "<<wim:a>> wff ( P -> Q )",
            "<<a1:a>> |- ( t = r -> ( t = s -> r = s ) )",
            "<<a2:a>> |- ( t + 0 ) = t",
            "<<min:e>> |- P",
            "<<maj:e>> |- ( P -> Q )",
            "<<mp:a>> |- Q",
            "<<th1:p>> |- t = t",
            "<<th2:p>> |- t = t",
        ).map { it.split(' ') }
        assertEquals(expectedExpressions, expressins)
    }


}
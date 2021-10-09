package org.igye.metamathparser

import org.junit.Assert.assertEquals
import org.junit.Test


internal class ParsersTest {
    @Test
    fun parseListOfConstants_parses_list_of_constants() {
        //when
        val parserOutput: ParserOutput<ListOfConstants> =
            Parsers.parseListOfConstants(ParserInput(text = "\$c 0 + = -> ( ) term wff |- \$.", begin = 0))

        //then
        assertEquals(
            listOf("0", "+", "=", "->", "(", ")", "term", "wff", "|-"),
            parserOutput.result.symbols
        )
        assertEquals(29, parserOutput.end)
    }
}
package org.igye.metamathparser

import java.util.*
import kotlin.collections.ArrayList

object Parsers {
    private const val SPACE = ' '

    fun parseListOfConstants(inp:ParserInput): ParserOutput<ListOfConstants> {
        if (inp.charAt(0) != '$' || inp.charAt(1) != 'c') {
            throw MetamathParserException("Cannot parse list of constants at ${inp.currPositionStr()}")
        }
        val listOfSymbols = parseListOfSymbols(inp.proceed(2))
        return ParserOutput(
            result = ListOfConstants(symbols = listOfSymbols.result, beginIdx = inp.begin),
            end = listOfSymbols.end
        )
    }

    fun parseListOfSymbols(inp:ParserInput): ParserOutput<List<String>> {
        val symbols = ArrayList<String>()
        val currSymbol = StringBuffer()
        var i = 0
        var currChar = inp.charAt(i)
        while (currChar != '$') {
            if (currChar == SPACE) {
                if (currSymbol.isNotEmpty()) {
                    symbols.add(currSymbol.toString())
                    currSymbol.setLength(0)
                }
            } else {
                currSymbol.append(currChar)
            }
            i += 1
            currChar = inp.charAt(i)
        }
        if (inp.charAt(i+1) != '.') {
            throw MetamathParserException("List of symbols must end with '$.' at ${inp.currPositionStr()}")
        }
        return ParserOutput(result = Collections.unmodifiableList(symbols), end = inp.toAbsolute(i+1))
    }
}
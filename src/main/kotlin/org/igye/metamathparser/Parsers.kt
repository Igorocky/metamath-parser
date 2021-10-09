package org.igye.metamathparser

import java.util.*
import kotlin.collections.ArrayList

data class ParserInput(val text:String, val begin:Int) {
    fun charAtRel(i:Int): Char = text[toAbsolute(i)]
    fun charAt(i:Int): Char = text[i]
    fun proceed(n:Int) = this.copy(begin = begin+n)
    fun currPositionStr():String = begin.toString()
    fun toAbsolute(i:Int) = begin+i
}

data class ParserOutput<T>(val result:T, val end:Int)

data class NonLabeledSequence(val beginIdx:Int, val seqType:Char, val symbols:List<String>)
data class LabeledSequence(val beginIdx:Int, val label:String, val seqType:Char, val symbols:List<String>)
data class ConstantStmt(val beginIdx:Int, val symbols:List<String>)
data class VariableStmt(val beginIdx:Int, val symbols:List<String>)
data class DisjointStmt(val beginIdx:Int, val symbols:List<String>)
data class FloatingStmt(val beginIdx:Int, val label:String, val typecode:String, val variable:String)

object Parsers {
    fun parseConstantStmt(inp:ParserInput): ParserOutput<ConstantStmt> {
        if (inp.charAtRel(0) != '$' || inp.charAtRel(1) != 'c') {
            throw MetamathParserException("Cannot parse list of constants at ${inp.currPositionStr()}")
        }
        val listOfSymbols = parseListOfSymbols(inp.proceed(2))
        return ParserOutput(
            result = ConstantStmt(symbols = listOfSymbols.result, beginIdx = inp.begin),
            end = listOfSymbols.end
        )
    }

    fun parseVariableStmt(inp:ParserInput): ParserOutput<VariableStmt> {
        if (inp.charAtRel(0) != '$' || inp.charAtRel(1) != 'v') {
            throw MetamathParserException("Cannot parse list of variables at ${inp.currPositionStr()}")
        }
        val listOfSymbols = parseListOfSymbols(inp.proceed(2))
        return ParserOutput(
            result = VariableStmt(symbols = listOfSymbols.result, beginIdx = inp.begin),
            end = listOfSymbols.end
        )
    }

    fun parseDisjointStmt(inp:ParserInput): ParserOutput<DisjointStmt> {
        if (inp.charAtRel(0) != '$' || inp.charAtRel(1) != 'd') {
            throw MetamathParserException("Cannot parse DisjointStmt at ${inp.currPositionStr()}")
        }
        val listOfSymbols = parseListOfSymbols(inp.proceed(2))
        return ParserOutput(
            result = DisjointStmt(symbols = listOfSymbols.result, beginIdx = inp.begin),
            end = listOfSymbols.end
        )
    }

    fun parseLabeledSequence(inp:ParserInput): ParserOutput<LabeledSequence> {
        val labelParserOutput = parsePrintable(inp)
        if (labelParserOutput.result.isEmpty()) {
            throw MetamathParserException("A label was expected at ${inp.currPositionStr()}")
        }
        val spaceParserOutput = parseWhitespace(inp.proceed(labelParserOutput.result.length))
        if (spaceParserOutput.result.isEmpty()) {
            throw MetamathParserException("A whitespace was expected between the label and the beginning of a list at ${inp.currPositionStr()}")
        }
        if (inp.charAt(spaceParserOutput.end+1) != '$') {
            throw MetamathParserException("Cannot parse labeled sequence at ${inp.currPositionStr()}")
        }
        val seqType = inp.charAt(spaceParserOutput.end+2)
        if (!inp.charAt(spaceParserOutput.end+3).isWhitespace()) {
            throw MetamathParserException("A whitespace was expected between the beginning of a list at its first element at ${inp.currPositionStr()}")
        }
        val listOfSymbols = parseListOfSymbols(inp.proceed(spaceParserOutput.end+3-inp.begin))
        return ParserOutput(
            result = LabeledSequence(
                beginIdx = inp.begin,
                label = labelParserOutput.result,
                seqType = seqType,
                symbols = listOfSymbols.result
            ),
            end = listOfSymbols.end
        )
    }

    private fun parsePrintable(inp:ParserInput): ParserOutput<String> {
        return parseWhile(inp) {!it.isWhitespace()}
    }

    private fun parseWhitespace(inp:ParserInput): ParserOutput<String> {
        return parseWhile(inp) {it.isWhitespace()}
    }

    private fun parseWhile(inp:ParserInput, predicate: (Char) -> Boolean): ParserOutput<String> {
        val result = StringBuffer()
        var i = 0
        var currChar = inp.charAtRel(i)
        while (predicate(currChar)) {
            result.append(currChar)
            i += 1
            currChar = inp.charAtRel(i)
        }
        return ParserOutput(result = result.toString(), end = inp.toAbsolute(i-1))
    }

    private fun parseListOfSymbols(inp:ParserInput): ParserOutput<List<String>> {
        val symbols = ArrayList<String>()
        val currSymbol = StringBuffer()
        var i = 0
        var currChar = inp.charAtRel(i)
        while (currChar != '$') {
            if (currChar.isWhitespace()) {
                if (currSymbol.isNotEmpty()) {
                    symbols.add(currSymbol.toString())
                    currSymbol.setLength(0)
                }
            } else {
                currSymbol.append(currChar)
            }
            i += 1
            currChar = inp.charAtRel(i)
        }
        if (inp.charAtRel(i+1) != '.') {
            throw MetamathParserException("A list of symbols must end with '$.' at ${inp.currPositionStr()}")
        }
        if (symbols.isEmpty()) {
            throw MetamathParserException("A list of symbols is expected to have at least one element at ${inp.currPositionStr()}")
        }
        return ParserOutput(result = Collections.unmodifiableList(symbols), end = inp.toAbsolute(i+1))
    }
}
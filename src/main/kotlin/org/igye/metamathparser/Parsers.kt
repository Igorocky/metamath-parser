package org.igye.metamathparser

import java.util.*
import kotlin.collections.ArrayList

data class ParserInput(val text:String, val begin:Int) {
    fun charAtRel(i:Int): Char = text[toAbsolute(i)]
    fun charAt(i:Int): Char = text[i]
    fun proceed(n:Int) = this.copy(begin = begin+n)
    fun proceedTo(n:Int) = this.copy(begin = n)
    fun currPositionStr():String = "'${text.substring(begin, 20)}...'"
    fun toAbsolute(i:Int) = begin+i
}

data class ParserOutput<T>(val result:T, val end:Int)

data class Comment(val text:String, val beginIdx:Int)
data class SequenceOfSymbols(val seqType:Char, val symbols:List<String>, val beginIdx:Int)
data class LabeledSequenceOfSymbols(val label:String, val sequence:SequenceOfSymbols, val beginIdx:Int)

object Parsers {

    fun parseComment(inp:ParserInput): ParserOutput<Comment> {
        if (inp.charAtRel(0) != '$' || inp.charAtRel(1) != '(') {
            throw MetamathParserException("Cannot parse Comment at ${inp.currPositionStr()}")
        }
        val commentText = collectWhile(inp.proceed(2)) { str, i -> !(str[i] == '$' && str[i + 1] == ')') }
        return ParserOutput(
            result = Comment(text = commentText.result, beginIdx = inp.begin),
            end = commentText.end+2
        )
    }

    fun parseSequenceOfSymbols(inp:ParserInput): ParserOutput<SequenceOfSymbols> {
        if (inp.charAtRel(0) != '$') {
            throw MetamathParserException("Cannot parse SequenceOfSymbols at ${inp.currPositionStr()}")
        }
        val seqType = inp.charAtRel(1)
        if (!inp.charAtRel(2).isWhitespace()) {
            throw MetamathParserException("A whitespace was expected between the beginning of a sequence at its first element at ${inp.currPositionStr()}")
        }
        val listOfSymbols = parseListOfSymbols(inp.proceed(2))
        return ParserOutput(
            result = SequenceOfSymbols(
                beginIdx = inp.begin,
                seqType = seqType,
                symbols = listOfSymbols.result
            ),
            end = listOfSymbols.end
        )
    }

    fun parseLabeledSequence(inp:ParserInput): ParserOutput<LabeledSequenceOfSymbols> {
        val label = parsePrintable(inp)
        if (label.result.isEmpty()) {
            throw MetamathParserException("A label was expected at ${inp.currPositionStr()}")
        }
        val space = parseWhitespace(inp.proceed(label.result.length))
        if (space.result.isEmpty()) {
            throw MetamathParserException("A whitespace was expected between the label and the beginning of a sequence at ${inp.currPositionStr()}")
        }
        if (inp.charAt(space.end+1) != '$') {
            throw MetamathParserException("Cannot parse labeled sequence at ${inp.currPositionStr()}")
        }
        val sequenceOfSymbols = parseSequenceOfSymbols(inp.proceedTo(space.end + 1))
        return ParserOutput(
            result = LabeledSequenceOfSymbols(
                label = label.result,
                sequence = sequenceOfSymbols.result,
                beginIdx = inp.begin
            ),
            end = sequenceOfSymbols.end
        )
    }

    private fun parsePrintable(inp:ParserInput): ParserOutput<String> {
        return collectWhile(inp) { str, i -> !str[i].isWhitespace()}
    }

    private fun parseWhitespace(inp:ParserInput): ParserOutput<String> {
        return collectWhile(inp) {str, i -> str[i].isWhitespace()}
    }

    private fun collectWhile(inp:ParserInput, predicate: (String, Int) -> Boolean): ParserOutput<String> {
        val result = StringBuffer()
        var i = inp.begin
        while (predicate(inp.text, i)) {
            result.append(inp.charAt(i))
            i += 1
        }
        return ParserOutput(result = result.toString(), end = i-1)
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
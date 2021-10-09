package org.igye.metamathparser

import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.min

data class ParserInput(val text:String, val begin:Int) {
    fun charAtRel(i:Int): Char = text[toAbsolute(i)]
    fun charAt(i:Int): Char = text[i]
    fun proceed(n:Int) = this.copy(begin = begin+n)
    fun proceedTo(n:Int) = this.copy(begin = n)
    fun currPositionStr():String {
        val lengthToShow = 20
        val ellipsis = if (text.length < begin+lengthToShow) "" else "..."
        return "'${text.substring(begin, min(text.length, begin+lengthToShow))}${ellipsis}'"
    }
    fun currPositionStr(i:Int):String = this.proceedTo(i).currPositionStr()
    fun currPositionStrRel(i:Int):String = this.proceedTo(begin+i).currPositionStr()
    fun toAbsolute(i:Int) = begin+i
}

data class ParserOutput<T>(val result:T, val end:Int)

data class Comment(val text:String, val beginIdx:Int, val endIdx:Int)
data class NonComment(val text:String, val beginIdx:Int, val endIdx:Int)
interface Expression
data class SequenceOfSymbols(val seqType:Char, val symbols:List<String>, val proof:List<String>?, val beginIdx:Int): Expression
data class LabeledSequenceOfSymbols(val label:String, val sequence:SequenceOfSymbols, val beginIdx:Int): Expression

object Parsers {

    fun traverseMetamathFile(
        text:String,
        exprProc: (MetamathContext,Expression) -> MetamathContext
    ):Map<String,Assertion> {
        val (_, code: List<NonComment>) = extractComments(text)
        return traverseBlock(
            inp = ParserInput(text = code.asSequence().map { it.text }.joinToString(separator = " "), begin = 0),
            exprProc = exprProc
        ).result
    }

    private fun traverseBlock(
        inp:ParserInput,
        context:MetamathContext = MetamathContext(),
        exprProc: (MetamathContext,Expression) -> MetamathContext
    ):ParserOutput<Map<String,Assertion>> {
        var idx = inp.begin
        val text = inp.text
        var ctx = context
        while (true) {
            while (idx < text.length && text[idx].isWhitespace()) {
                idx++
            }
            if (!(idx < text.length)) {
                return ParserOutput(result = ctx.assertions, end = idx-1)
            }

            if (idx+1 < text.length) {
                if (text[idx] == '$') {
                    if (text[idx+1] == '{') {
                        val assertionsFromBlock = traverseBlock(
                            inp = inp.proceedTo(idx + 2),
                            context = ctx,
                            exprProc = exprProc
                        )
                        ctx = ctx.copy(assertions = ctx.assertions.plus(assertionsFromBlock.result))
                        idx = assertionsFromBlock.end+1
                    } else if (text[idx+1] == '}') {
                        return ParserOutput(result = ctx.assertions, end = idx+1)
                    } else {
                        val sequenceOfSymbols = parseSequenceOfSymbols(inp.proceedTo(idx))
                        ctx = exprProc(ctx, sequenceOfSymbols.result)
                        idx = sequenceOfSymbols.end+1
                    }
                } else {
                    val sequenceOfSymbols = parseLabeledSequenceOfSymbols(inp.proceedTo(idx))
                    ctx = exprProc(ctx, sequenceOfSymbols.result)
                    idx = sequenceOfSymbols.end+1
                }
            } else {
                throw MetamathParserException()
            }
        }
    }

    fun extractComments(text: String): Pair<List<Comment>,List<NonComment>> {
        val comments = ArrayList<Comment>()
        val nonComments = ArrayList<NonComment>()
        var idx = 0
        while (idx < text.length) {
            while (idx < text.length && text[idx].isWhitespace()) {
                idx++
            }
            if (!(idx < text.length)) {
                break
            }

            if (idx+1 < text.length && text[idx] == '$' && text[idx+1] == '(') {
                val comment = parseComment(ParserInput(text, idx))
                comments.add(comment.result)
                idx = comment.end + 1
            } else {
                val beginOfNonCommentIdx = idx
                while (idx < text.length && !(idx+1 < text.length && text[idx] == '$' && text[idx+1] == '(')) {
                    idx++
                }
                if (beginOfNonCommentIdx < idx) {
                    nonComments.add(NonComment(
                        text = text.substring(beginOfNonCommentIdx,idx),
                        beginIdx = beginOfNonCommentIdx,
                        endIdx = idx-1
                    ))
                }
            }
        }
        return Pair(comments, nonComments)
    }

    fun parseComment(inp:ParserInput): ParserOutput<Comment> {
        if (inp.charAtRel(0) != '$' || inp.charAtRel(1) != '(') {
            throw MetamathParserException("Cannot parse Comment at ${inp.currPositionStr()}")
        }
        val commentText = collectWhile(inp.proceed(2)) { str, i -> !(str[i] == '$' && str[i + 1] == ')') }
        return ParserOutput(
            result = Comment(text = commentText.result, beginIdx = inp.begin, endIdx = commentText.end+2),
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
        val proof = if (inp.charAt(listOfSymbols.end) == '=') {
            parseSequenceOfSymbols(inp.proceedTo(listOfSymbols.end-1))
        } else {
            null
        }
        return ParserOutput(
            result = SequenceOfSymbols(
                beginIdx = inp.begin,
                seqType = seqType,
                symbols = listOfSymbols.result,
                proof = proof?.result?.symbols
            ),
            end = proof?.end?:listOfSymbols.end
        )
    }

    fun parseLabeledSequenceOfSymbols(inp:ParserInput): ParserOutput<LabeledSequenceOfSymbols> {
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
        if (inp.charAtRel(i+1) != '.' && inp.charAtRel(i+1) != '=') {
            throw MetamathParserException("A list of symbols must end with '$.' or '$=' at ${inp.currPositionStrRel(i)}")
        }
        if (symbols.isEmpty()) {
            throw MetamathParserException("A list of symbols is expected to have at least one element at ${inp.currPositionStr()}")
        }
        return ParserOutput(result = Collections.unmodifiableList(symbols), end = inp.toAbsolute(i+1))
    }
}
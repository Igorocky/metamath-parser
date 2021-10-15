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
data class NonComment(val text:String, val beginIdx:Int, val endIdx:Int, val lastComment: Comment?)
interface Expression
data class CompressedProof(val labels:List<String>, val proof:String)
data class SequenceOfSymbols(
    val seqType:Char, val symbols:List<String>, val uncompressedProof:List<String>?, val compressedProof:CompressedProof?, val beginIdx:Int): Expression
data class LabeledSequenceOfSymbols(val label:String, val sequence:SequenceOfSymbols, val beginIdx:Int): Expression

object Parsers {

    fun traverseMetamathFile(text:String, exprProc: (MetamathContext,Expression) -> Unit):MetamathContext {
        val (_, code: List<NonComment>) = extractComments(text)
        val comments = ArrayList<Comment>()
        val sb = StringBuilder()
        for (nonComment in code) {
            if (nonComment.lastComment != null) {
                comments.add(nonComment.lastComment.copy(beginIdx = sb.length))
            }
            sb.append(" ").append(nonComment.text)
        }
        val ctx = MetamathContext()
        traverseBlock(
            inp = ParserInput(text = sb.toString(), begin = 0),
            context = ctx,
            comments = comments,
            exprProc = exprProc
        )
        return ctx
    }

    private fun traverseBlock(
        inp:ParserInput,
        context:MetamathContext,
        comments: List<Comment>,
        exprProc: (MetamathContext,Expression) -> Unit
    ):ParserOutput<Unit> {
        var idx = inp.begin
        val text = inp.text
        while (true) {
            while (idx < text.length && text[idx].isWhitespace()) {
                idx++
            }
            if (!(idx < text.length)) {
                return ParserOutput(result = Unit, end = idx-1)
            }

            if (idx+1 < text.length) {
                if (text[idx] == '$') {
                    if (text[idx+1] == '{') {
                        val childContext = context.createChildContext()
                        val assertionsFromBlock = traverseBlock(
                            inp = inp.proceedTo(idx + 2),
                            context = childContext,
                            comments = comments,
                            exprProc = exprProc
                        )
                        idx = assertionsFromBlock.end+1
                    } else if (text[idx+1] == '}') {
                        return ParserOutput(result = Unit, end = idx+1)
                    } else {
                        val sequenceOfSymbols = parseSequenceOfSymbols(inp.proceedTo(idx))
                        exprProc(context, sequenceOfSymbols.result)
                        idx = sequenceOfSymbols.end+1
                    }
                } else {
                    val sequenceOfSymbols = parseLabeledSequenceOfSymbols(inp.proceedTo(idx))
                    context.lastComment = findLastCommentInBetween(comments, inp.begin, idx)
                    exprProc(context, sequenceOfSymbols.result)
                    idx = sequenceOfSymbols.end+1
                }
            } else {
                throw MetamathParserException()
            }
        }
    }

    private fun findLastCommentInBetween(comments: List<Comment>, begin: Int, end: Int): String? {
        var min = 0
        var max = comments.size-1
        var i = (min + max) / 2
        while (min < max && !(begin <= comments[i].beginIdx && comments[i].beginIdx <= end)) {
            if (comments[i].beginIdx < begin) {
                min = i+1
            } else if (end < comments[i].beginIdx) {
                max = i-1
            }
            i = (min + max) / 2
        }
        while (i+1 < comments.size && begin <= comments[i+1].beginIdx && comments[i+1].beginIdx <= end) {
            i++
        }
        return if (i < comments.size && begin <= comments[i].beginIdx && comments[i].beginIdx <= end) {
            comments[i].text
        } else {
            null
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
                        endIdx = idx-1,
                        lastComment = if (comments.isEmpty()) null else comments.last()
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
        var uncompressedProof:ParserOutput<SequenceOfSymbols>? = null
        var compressedProof:ParserOutput<CompressedProof>? = null
        if (inp.charAt(listOfSymbols.end) == '=') {
            val whitespace = parseWhitespace(inp.proceedTo(listOfSymbols.end + 1))
            val beginOfProof = listOfSymbols.end + whitespace.result.length + 1
            if (inp.charAt(beginOfProof) == '(') {
                compressedProof = parseCompressedProof(inp.proceedTo(beginOfProof))
            } else {
                uncompressedProof = parseSequenceOfSymbols(inp.proceedTo(listOfSymbols.end-1))
            }
        }
        return ParserOutput(
            result = SequenceOfSymbols(
                beginIdx = inp.begin,
                seqType = seqType,
                symbols = listOfSymbols.result,
                uncompressedProof = uncompressedProof?.result?.symbols,
                compressedProof = compressedProof?.result
            ),
            end = uncompressedProof?.end?:compressedProof?.end?:listOfSymbols.end
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

    fun parseCompressedProof(inp:ParserInput): ParserOutput<CompressedProof> {
        if (inp.charAt(inp.begin) != '(') {
            throw MetamathParserException("Compressed proof must begin with '('")
        }
        val labels = parseListOfSymbolsUntil(inp.proceed(1), ')')
        var i = labels.end+2
        var currChar = inp.charAt(i)
        val proof = StringBuilder()
        while (currChar != '$') {
            if (!currChar.isWhitespace()) {
                proof.append(currChar)
            }
            i++
            currChar = inp.charAt(i)
        }
        if (inp.charAt(i+1) != '.') {
            throw MetamathParserException("A proof must end with '$.' at ${inp.currPositionStr(i)}")
        }
        return ParserOutput(
            result = CompressedProof(
                labels = labels.result,
                proof = proof.toString()
            ),
            end = i+1
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
        val symbols = parseListOfSymbolsUntil(inp, '$')

        if (inp.charAt(symbols.end+2) != '.' && inp.charAt(symbols.end+2) != '=') {
            throw MetamathParserException("A list of symbols must end with '$.' or '$=' at ${inp.currPositionStrRel(symbols.end)}")
        }
        if (symbols.result.isEmpty()) {
            throw MetamathParserException("A list of symbols is expected to have at least one element at ${inp.currPositionStr()}")
        }
        return ParserOutput(result = Collections.unmodifiableList(symbols.result), end = symbols.end+2)
    }

    private fun parseListOfSymbolsUntil(inp:ParserInput, endChar:Char): ParserOutput<List<String>> {
        val symbols = ArrayList<String>()
        val currSymbol = StringBuffer()
        var i = 0
        var currChar = inp.charAtRel(i)
        while (currChar != endChar) {
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
        return ParserOutput(result = Collections.unmodifiableList(symbols), end = inp.toAbsolute(i-1))
    }
}
package org.igye.proofassistant

import org.igye.metamathparser.*
import org.igye.metamathparser.Parsers.parseMetamathFile
import org.igye.proofassistant.ProofAssistant.mkStmt
import org.igye.proofassistant.substitutions.ParenthesesCounter
import org.igye.proofassistant.substitutions.Substitutions
import java.io.File

fun main() {
    val context = parseMetamathFile(File("C:\\igye\\books\\metamath/set.mm"))
    val assertions: Map<String, Assertion> = context.getAssertions()
    val parenCounterProducer = {
        ParenthesesCounter(
            roundBracketOpen = context.getNumberBySymbol("("),
            roundBracketClose = context.getNumberBySymbol(")"),
            curlyBracketOpen = context.getNumberBySymbol("{"),
            curlyBracketClose = context.getNumberBySymbol("}"),
            squareBracketOpen = context.getNumberBySymbol("["),
            squareBracketClose = context.getNumberBySymbol("]"),
        )
    }

    val stmt1: IntArray = mkStmt("wff ( sqrt ` 2 ) e/ QQ", context::getNumberBySymbol)
    val stmt2: IntArray = mkStmt("wff ( y e. NN -> y e. CC )", context::getNumberBySymbol)

    ProofAssistant.findFullMatches(stmt2, assertions, parenCounterProducer)


    println("done")
}

object ProofAssistant {

    fun findFullMatches(stmt: IntArray, assertions: Map<String, Assertion>, parenCounterProducer: () -> ParenthesesCounter) {
        for ((label,assertion) in assertions) {
            Substitutions.iterateSubstitutions(
                stmt = stmt,
                asrtStmt = assertion.statement.content,
                parenCounterProducer = parenCounterProducer
            ) {
                if (it.isDefined.filter { !it }.isEmpty()) {
                    println()
                }
            }
        }
    }

    fun mkStmt(str:String, symbToInt:(String) -> Int): IntArray {
        return str.trim().split(' ').asSequence()
            .map(symbToInt)
            .toList()
            .toIntArray()
    }

    fun prove(expr: String, ctx: MetamathContext): StackNode {
        val allowedStatementsTypes: Set<Int> = setOf("wff", "setvar", "class").map { ctx.getNumberBySymbol(it) }.toSet()
        val stmtToProve = Statement(
            type = 'p',
            content = expr.split("\\s".toRegex()).map { ctx.getNumberBySymbol(it) }.toIntArray()
        )
        if (!allowedStatementsTypes.contains(stmtToProve.content[0])) {
            throw MetamathParserException("!allowedStatementsTypes.contains(stmtToProve.content[0])")
        }

        return ConstStackNode(Statement(type = 'n',content = intArrayOf()))
    }


}
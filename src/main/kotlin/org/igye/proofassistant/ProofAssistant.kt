package org.igye.proofassistant

import org.igye.common.ContinueInstr.CONTINUE
import org.igye.common.MetamathUtils.applySubstitution
import org.igye.common.MetamathUtils.toString
import org.igye.metamathparser.MetamathContext
import org.igye.metamathparser.MetamathParentheses
import org.igye.metamathparser.MetamathParserException
import org.igye.metamathparser.Parsers.parseMetamathFile
import org.igye.proofassistant.proof.*
import org.igye.proofassistant.substitutions.Substitutions
import java.io.File

fun main() {
    val ctx = parseMetamathFile(File("C:\\igye\\books\\metamath/set.mm"))
    val p1 = ProofAssistant.prove("wff ( y e. NN -> y e. CC )", ctx)

    println("done")
}

object ProofAssistant {

    fun prove(expr: String, ctx: MetamathContext): VarProofNode {
        val allowedStatementsTypes: Set<Int> = setOf("wff", "setvar", "class").map { ctx.getNumberBySymbol(it) }.toSet()
        val result = VarProofNode(
            value = mkStmt(expr, ctx::getNumberBySymbol),
            valueStr = expr
        )
        if (!allowedStatementsTypes.contains(result.value[0])) {
            throw MetamathParserException("!allowedStatementsTypes.contains(result.value[0])")
        }

        ctx.parentheses = MetamathParentheses(
            roundBracketOpen = ctx.getNumberBySymbol("("),
            roundBracketClose = ctx.getNumberBySymbol(")"),
            curlyBracketOpen = ctx.getNumberBySymbol("{"),
            curlyBracketClose = ctx.getNumberBySymbol("}"),
            squareBracketOpen = ctx.getNumberBySymbol("["),
            squareBracketClose = ctx.getNumberBySymbol("]"),
        )

        val statementsToProve = ArrayList<VarProofNode>()
        statementsToProve.add(result)

        while (statementsToProve.isNotEmpty() && !result.isProved) {
            val currStmt = statementsToProve.removeLast()
            for (foundProof in findProof(stmt = currStmt, ctx = ctx)) {
                currStmt.proofs.add(foundProof)
                if (foundProof is CalculatedProofNode) {
                    statementsToProve.addAll(foundProof.args)
                }
                markProved(foundProof)
            }
        }

        if (!result.isProved) {
            throw ProofAssistantException("!result.isProved")
        }

        return result
    }

    fun markProved(proofNode: ProofNode) {
        var node: VarProofNode? = if (proofNode is ConstProofNode) {
            proofNode.provesWhat
        } else if (proofNode is CalculatedProofNode && proofNode.args.isEmpty()) {
            proofNode.result
        } else {
            null
        }
        while (node != null) {
            if (node is VarProofNode) {
                node.isProved = true
                if (node.argOf != null) {
                    val calcNode = node.argOf!!
                    if (calcNode.args.all { it.isProved }) {
                        node = calcNode.result
                    } else {
                        break
                    }
                } else {
                    break
                }
            } else {
                break
            }
        }
    }

    fun findProof(stmt: VarProofNode, ctx: MetamathContext): List<ProofNode> {
        val result = ArrayList<ProofNode>()
        ctx.iterateHypotheses { hyp->
            if (hyp.type == 'f' && hyp.content.contentEquals(stmt.value)) {
                result.add(
                    ConstProofNode(
                        stmt = hyp,
                        valueStr = toString(hyp.content, ctx),
                        provesWhat = stmt
                    )
                )
            }
            CONTINUE
        }

        for (assertion in ctx.getAssertions().values) {
            Substitutions.iterateSubstitutions(
                stmt = stmt.value,
                asrtStmt = assertion.statement.content,
                parenCounterProducer = ctx.parentheses!!::createParenthesesCounter
            ) { subs ->
                if (subs.begins.size == assertion.numberOfVariables && subs.isDefined.filter { !it }.isEmpty()) {
                    val subsList = ArrayList<IntArray>(subs.begins.size)
                    for (i in subs.begins.indices) {
                        subsList.add(subs.stmt.copyOfRange(fromIndex = subs.begins[i], toIndex = subs.ends[i]+1))
                    }
                    val calculatedProofNode = CalculatedProofNode(
                        args = assertion.hypotheses.asSequence().map {
                            val value = applySubstitution(it.content, subsList)
                            VarProofNode(
                                value = value,
                                valueStr = toString(value, ctx),
                            )
                        }.toList(),
                        substitution = subsList,
                        assertion = assertion,
                        result = stmt
                    )
                    calculatedProofNode.args.forEach { it.argOf = calculatedProofNode }
                    result.add(calculatedProofNode)
                }
                CONTINUE
            }
        }

        return result
    }

    fun mkStmt(str:String, symbToInt:(String) -> Int): IntArray {
        return str.trim().split(' ').asSequence()
            .map(symbToInt)
            .toList()
            .toIntArray()
    }

}
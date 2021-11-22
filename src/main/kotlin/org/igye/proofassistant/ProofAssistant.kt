package org.igye.proofassistant

import org.igye.common.ContinueInstr.CONTINUE
import org.igye.common.MetamathUtils.applySubstitution
import org.igye.common.MetamathUtils.toJson
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

    println("p1 = ${toJson(p1)}")
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

        while (statementsToProve.isNotEmpty() && result.proofLength < 0) {
            val currStmt = statementsToProve.removeLast()
            if (currStmt.proofLength < 0 && !currStmt.isCanceled) {
                val foundProofs = findProof(stmt = currStmt, ctx = ctx)
                for (foundProof in foundProofs) {
                    currStmt.proofs.add(foundProof)
                    if (foundProof is CalculatedProofNode) {
                        statementsToProve.addAll(foundProof.args)
                    }
                }
                for (foundProof in foundProofs) {
                    markProved(foundProof)
                }
            }
        }

        if (result.proofLength < 0) {
            println("-----------------------------")
            println(toJson(result))
            println("-----------------------------")
            throw ProofAssistantException("result.proveLength < 0")
        }

        return result
    }

    fun markProved(proofNode: ProofNode) {
        if (proofNode.proofLength >= 0 || proofNode.isCanceled) {
            return
        }
        var toBeMarkedAsProved: VarProofNode? = if (proofNode is ConstProofNode) {
            proofNode.proofLength = 0
            proofNode.provesWhat
        } else if (proofNode is CalculatedProofNode && proofNode.args.isEmpty()) {
            proofNode.proofLength = 0
            proofNode.result
        } else {
            null
        }
        var proofLength = 1
        while (toBeMarkedAsProved != null) {
            if (toBeMarkedAsProved.proofLength >= 0) {
                throw ProofAssistantException("toBeMarkedAsProved.proofLength >= 0")
            }
            toBeMarkedAsProved.proofLength = proofLength
            if (toBeMarkedAsProved.argOf != null) {
                val calcNode = toBeMarkedAsProved.argOf!!
                if (calcNode.args.all { it.proofLength >= 0 }) {
                    calcNode.proofLength = calcNode.args.asSequence().map { it.proofLength+1 }.sum()
                    toBeMarkedAsProved = calcNode.result
                    proofLength = calcNode.proofLength+1
                } else {
                    break
                }
            } else {
                break
            }
        }
        if (toBeMarkedAsProved != null) {
            cancelNotProved(toBeMarkedAsProved)
        }
    }

    fun cancelNotProved(provedNode: VarProofNode) {
        val rootsToStartCancellingFrom = ArrayList<VarProofNode>()
        rootsToStartCancellingFrom.add(provedNode)
        while (rootsToStartCancellingFrom.isNotEmpty()) {
            val currProvedNode = rootsToStartCancellingFrom.removeLast()
            val nodesToCancel = ArrayList<ProofNode>()
            val proofToRemain = currProvedNode.proofs.asSequence().filter { it.proofLength >= 0 }.minByOrNull { it.proofLength }!!
            for (proof in currProvedNode.proofs) {
                if (proof != proofToRemain) {
                    nodesToCancel.add(proof)
                }
            }
            currProvedNode.proofs.removeIf{ it != proofToRemain }
            while (nodesToCancel.isNotEmpty()) {
                val currNode = nodesToCancel.removeLast()
                currNode.isCanceled = true
                if (currNode is VarProofNode) {
                    nodesToCancel.addAll(currNode.proofs)
                } else if (currNode is CalculatedProofNode) {
                    nodesToCancel.addAll(currNode.args)
                }
            }

            if (proofToRemain is VarProofNode) {
                rootsToStartCancellingFrom.add(proofToRemain)
            } else if (proofToRemain is CalculatedProofNode) {
                rootsToStartCancellingFrom.addAll(proofToRemain.args)
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
                if (subs.begins.size == assertion.numberOfVariables && subs.isDefined.all { it }) {
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
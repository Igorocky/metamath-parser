package org.igye.proofassistant

import org.igye.common.ContinueInstr.CONTINUE
import org.igye.common.ContinueInstr.STOP
import org.igye.common.MetamathUtils
import org.igye.common.MetamathUtils.applySubstitution
import org.igye.common.MetamathUtils.collectAllVariables
import org.igye.common.MetamathUtils.toJson
import org.igye.common.MetamathUtils.toString
import org.igye.metamathparser.MetamathContext
import org.igye.metamathparser.MetamathParentheses
import org.igye.metamathparser.MetamathParserException
import org.igye.metamathparser.Parsers.parseMetamathFile
import org.igye.metamathparser.Statement
import org.igye.proofassistant.proof.*
import org.igye.proofassistant.proof.ProofNodeState.PROVED
import org.igye.proofassistant.substitutions.Substitutions
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

fun main() {
    val ctx = parseMetamathFile(File("C:\\igye\\books\\metamath/set.mm"))
    val p1 = ProofAssistant.prove("wff ( y e. NN -> y e. CC )", ctx)

    println("p1 = ${toJson(p1)}")
}

object ProofAssistant {

    fun createProvableAssertion(varProofNode: InstVarProofNode, ctx: MetamathContext): String {
        val essentialHypotheses: List<Statement> = ctx.getHypotheses { it.type == 'e'}
        val variables = collectAllVariables(essentialHypotheses, varProofNode.value)
        val (floatingHypotheses, variablesTypes) = MetamathUtils.collectFloatingHypotheses(variables, ctx)
        val mandatoryHypotheses: List<Statement> = (floatingHypotheses + essentialHypotheses).sortedBy { it.beginIdx }

        val proofLabels: MutableList<String> = ArrayList(mandatoryHypotheses.map { it.label })
        val labelToIdx = HashMap<String,Int>()
        for (i in proofLabels.indices) {
            labelToIdx[proofLabels[i]] = i
        }

        fun labelToInt(label:String): Int {
            if (!labelToIdx.containsKey(label)) {
                labelToIdx[label] = proofLabels.size
                proofLabels.add(label)
            }
            return labelToIdx[label]!! + 1
        }

        val proofStepsStack = Stack<Int>()
        val nodesToProcess = Stack<InstVarProofNode>()
        nodesToProcess.push(varProofNode)
        while (nodesToProcess.isNotEmpty()) {
            val curNodeProof = nodesToProcess.pop().proofs[0]
            if (curNodeProof is CalculatedProofNode) {
                proofStepsStack.push(labelToInt(curNodeProof.assertion.statement.label))
                curNodeProof.args.forEach { nodesToProcess.push(it) }
            } else if (curNodeProof is ConstProofNode) {
                proofStepsStack.push(labelToInt(curNodeProof.stmt.label))
            } else {
                throw ProofAssistantException("Unexpected type of curNodeProof: " + curNodeProof.javaClass.canonicalName)
            }
        }

        val proofSteps = ArrayList<Int>()
        while (proofStepsStack.isNotEmpty()) {
            proofSteps.add(proofStepsStack.pop())
        }

        return "\$p " + varProofNode.value.asSequence().map { ctx.getSymbolByNumber(it) }.joinToString(" ") +
                " $= ( " + proofLabels.drop(mandatoryHypotheses.size).joinToString(" ") + " ) " +
                proofSteps.asSequence().map { intToStr(it) }.joinToString("") + " $."
    }

    fun prove(expr: String, ctx: MetamathContext, label: String = UUID.randomUUID().toString()): ValProofNode {
        val allowedStatementsTypes: Set<Int> = setOf("wff", "setvar", "class").map { ctx.getNumberBySymbol(it) }.toSet()
        val result = PendingProofNode(stmt = mkStmt(expr, ctx))
        if (!allowedStatementsTypes.contains(result.stmt.value[0])) {
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

        val proofContext = ProofContext()
        proofContext.addStatementToProve(result)

        while (result.state != PROVED && proofContext.hasStatementsToProve()) {
            val currStmtToProve: ProofNode = proofContext.getNextStatementToProve()

            val constProof = findConstant(currStmtToProve, ctx, proofContext)
            if (constProof != null) {
                proofContext.constProofFound(nodeToProve = currStmtToProve, constProof = constProof)
            } else {

            }

            val foundProofContinuations = findProofContinuation(stmt = currStmtToProve, ctx = ctx)
            for (proofContinuation in foundProofContinuations) {
                if (proofContinuation is ConstProofNode) {

                } else if (proofContinuation is InstVarProofNode) {
                    val existingSameStatementProved: InstVarProofNode? = provedStatements[proofContinuation.stmt]
                    if (existingSameStatementProved != null) {
                        val ref = replaceInstWithRef(notProvedStatement = proofContinuation, existingSameStatementProved = existingSameStatementProved)
                        markProved(ref)
                    }
                    currStmtToProve.proofs.add(proofContinuation)
                    if (proofContinuation is CalculatedProofNode) {
                        statementsToProve.addAll(proofContinuation.args)
                    }
                } else {
                    throw ProofAssistantException("Unexpected type of proofContinuation: ${proofContinuation.javaClass.canonicalName}")
                }

            }
            for (foundProof in foundProofContinuations) {
                markProved(foundProof)
            }

            val existingSameStatementProved: InstVarProofNode? = provedStatements[currStmtToProve.stmt]
            if (existingSameStatementProved != null) {
                val ref = replaceInstWithRef(notProvedStatement = currStmtToProve, existingSameStatementProved = existingSameStatementProved)
                markProved(ref)
            } else {

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

    private fun replaceInstWithRef(notProvedStatement: InstVarProofNode, existingSameStatementProved: InstVarProofNode): RefVarProofNode {
        if (notProvedStatement.proofs.isNotEmpty()) {
            throw ProofAssistantException("notProvedStatement.proofs.isNotEmpty()")
        }
        val ref = RefVarProofNode(stmt = notProvedStatement.stmt, ref = existingSameStatementProved, argOf = notProvedStatement.argOf)
        notProvedStatement.argOf!!.args.removeIf { it === notProvedStatement }
        notProvedStatement.argOf!!.args.add(ref)
        existingSameStatementProved.isReused = true
        return ref
    }

    private fun intToStr(i: Int): String {
        if (i == 0) {
            throw ProofAssistantException("i == 0")
        }
        var i = i
        val sb = StringBuilder()
        var base = 21
        //65 is A, 85 is U
        while (i > 0) {
            sb.append((i % base + if (sb.length == 0) 64 else 85).toChar())
            i /= base
            if (base == 21) {
                base = 6
            }
        }
        return sb.reverse().toString()
    }

    private fun markProved(proofNode: ProofNode) {
        if (proofNode.isCanceled) {
            throw AssumptionDoesntHoldException()
        }
        var toBeMarkedAsProved: ValProofNode? = if (proofNode is ConstProofNode) {
            proofNode.proofLength = 0
            proofNode.provesWhat
        } else if (proofNode is CalculatedProofNode && proofNode.args.isEmpty()) {
            proofNode.proofLength = 0
            proofNode.result
        } else if (proofNode is RefVarProofNode) {
            proofNode
        } else {
            null
        }
        var proofLength = if (toBeMarkedAsProved is RefVarProofNode) 0 else 1
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
        if (toBeMarkedAsProved is InstVarProofNode) {
            cancelNotProved(toBeMarkedAsProved)
        }
    }

    private fun cancelNotProved(provedNode: InstVarProofNode) {
        val rootsToStartCancellingFrom = ArrayList<InstVarProofNode>()
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
                if (currNode is InstVarProofNode) {
                    nodesToCancel.addAll(currNode.proofs)
                } else if (currNode is CalculatedProofNode) {
                    nodesToCancel.addAll(currNode.args)
                }
            }

            if (proofToRemain is InstVarProofNode) {
                rootsToStartCancellingFrom.add(proofToRemain)
            } else if (proofToRemain is CalculatedProofNode) {
                for (arg in proofToRemain.args) {
                    if (arg is InstVarProofNode) {
                        rootsToStartCancellingFrom.add(arg)
                    }
                }
            }
        }
    }

    private fun findConstant(nodeToProve: ProofNode, ctx: MetamathContext, proofContext: ProofContext): ConstProofNode? {
        var result: ConstProofNode? = null
        ctx.iterateHypotheses { hyp->
            if ((hyp.type == 'f' || hyp.type == 'e') && hyp.content.contentEquals(nodeToProve.stmt.value)) {
                result = ConstProofNode(src = hyp, stmt = nodeToProve.stmt, proofContext = proofContext)
                STOP
            } else {
                CONTINUE
            }
        }
        return result
    }

    private fun findProofContinuation(stmt: InstVarProofNode, ctx: MetamathContext): List<ProofNode> {
        val result = ArrayList<ProofNode>()
        ctx.iterateHypotheses { hyp->
            if ((hyp.type == 'f' || hyp.type == 'e') && hyp.content.contentEquals(stmt.value)) {
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
                            InstVarProofNode(
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

    private fun mkStmt(stmt: IntArray, ctx: MetamathContext): Stmt = Stmt(
        value = stmt,
        valueStr = stmt.asSequence().map { ctx.getSymbolByNumber(it) }.joinToString(" ")
    )

    private fun mkStmt(str:String, ctx: MetamathContext): Stmt = mkStmt(
        stmt = str.trim().split(' ').asSequence()
            .map { ctx.getNumberBySymbol(it) }
            .toList()
            .toIntArray(),
        ctx = ctx
    )

}
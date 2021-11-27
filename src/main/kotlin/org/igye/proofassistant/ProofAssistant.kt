package org.igye.proofassistant

import org.igye.common.ContinueInstr.CONTINUE
import org.igye.common.ContinueInstr.STOP
import org.igye.common.MetamathUtils
import org.igye.common.MetamathUtils.applySubstitution
import org.igye.common.MetamathUtils.collectAllVariables
import org.igye.common.MetamathUtils.toJson
import org.igye.metamathparser.MetamathContext
import org.igye.metamathparser.MetamathParentheses
import org.igye.metamathparser.MetamathParserException
import org.igye.metamathparser.Parsers.parseMetamathFile
import org.igye.metamathparser.Statement
import org.igye.proofassistant.proof.*
import org.igye.proofassistant.proof.prooftree.CalcProofNode
import org.igye.proofassistant.proof.prooftree.ConstProofNode
import org.igye.proofassistant.proof.prooftree.PendingProofNode
import org.igye.proofassistant.proof.prooftree.ProofNode
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

    fun createProvableAssertion(varProofNode: ProofNode, ctx: MetamathContext): String {
        val essentialHypotheses: List<Statement> = ctx.getHypotheses { it.type == 'e'}
        val variables = collectAllVariables(essentialHypotheses, varProofNode.stmt.value)
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
        val nodesToProcess = Stack<ProofNode>()
        nodesToProcess.push(varProofNode)
//        while (nodesToProcess.isNotEmpty()) {
//            val curNodeProof = nodesToProcess.pop().proofs[0]
//            if (curNodeProof is CalculatedProofNode) {
//                proofStepsStack.push(labelToInt(curNodeProof.assertion.statement.label))
//                curNodeProof.args.forEach { nodesToProcess.push(it) }
//            } else if (curNodeProof is ConstProofNode) {
//                proofStepsStack.push(labelToInt(curNodeProof.stmt.label))
//            } else {
//                throw ProofAssistantException("Unexpected type of curNodeProof: " + curNodeProof.javaClass.canonicalName)
//            }
//        }

        val proofSteps = ArrayList<Int>()
        while (proofStepsStack.isNotEmpty()) {
            proofSteps.add(proofStepsStack.pop())
        }

//        return "\$p " + varProofNode.value.asSequence().map { ctx.getSymbolByNumber(it) }.joinToString(" ") +
//                " $= ( " + proofLabels.drop(mandatoryHypotheses.size).joinToString(" ") + " ) " +
//                proofSteps.asSequence().map { intToStr(it) }.joinToString("") + " $."
        return "111111"
    }

    fun prove(expr: String, ctx: MetamathContext): ProofNode {
        val allowedStatementsTypes: Set<Int> = setOf("wff", "setvar", "class").map { ctx.getNumberBySymbol(it) }.toSet()
        val stmtToProve = mkStmt(expr, ctx)
        if (!allowedStatementsTypes.contains(stmtToProve.value[0])) {
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
        proofContext.addStatementToProve(PendingProofNode(stmt = stmtToProve))

        while (proofContext.getProved(stmtToProve) == null && proofContext.hasStatementsToProve()) {
            val currStmtToProve: PendingProofNode = proofContext.getNextStatementToProve()

            val constProof = findConstant(currStmtToProve.stmt, ctx)
            if (constProof != null) {
                proofContext.proofFoundForNodeToBeProved(nodeToBeProved = currStmtToProve, foundProof = constProof)
            } else {
                val matchingAssertions = findMatchingAssertions(currStmtToProve.stmt, ctx)
                for (asrtNode: CalcProofNode in matchingAssertions) {
                    createArgsForCalcNode(asrtNode, proofContext, ctx)
                    if (asrtNode.args.all { it.state == ProofNodeState.PROVED }) {
                        proofContext.proofFoundForNodeToBeProved(nodeToBeProved = currStmtToProve, foundProof = asrtNode)
                        break
                    } else {
                        currStmtToProve.proofs.add(asrtNode)
                        asrtNode.dependants.add(currStmtToProve)
                        asrtNode.state = ProofNodeState.WAITING
                    }
                }
            }

            if (currStmtToProve.state == ProofNodeState.TO_BE_PROVED) {
                proofContext.markWaiting(currStmtToProve)
            }
        }

        return proofContext.getProved(stmtToProve)?:proofContext.getWaiting(stmtToProve)!!
    }

    private fun createArgsForCalcNode(asrtNode: CalcProofNode, proofContext: ProofContext, ctx: MetamathContext) {
        for (arg in asrtNode.assertion.hypotheses) {
            val argStmt = mkStmt(applySubstitution(arg.content, asrtNode.substitution), ctx)
            val existingProof = proofContext.getProved(argStmt)
                ?: proofContext.getWaiting(argStmt)
                ?: proofContext.getToBeProved(argStmt)
            if (existingProof != null) {
                existingProof.dependants.add(asrtNode)
                asrtNode.args.add(existingProof)
            } else {
                val constProof = findConstant(argStmt, ctx)
                if (constProof != null) {
                    proofContext.markProved(constProof)
                    constProof.dependants.add(asrtNode)
                    asrtNode.args.add(constProof)
                } else {
                    val pendingNode = PendingProofNode(stmt = argStmt)
                    proofContext.addStatementToProve(pendingNode)
                    pendingNode.dependants.add(asrtNode)
                    asrtNode.args.add(pendingNode)
                }
            }
        }
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

    private fun findConstant(stmt: Stmt, ctx: MetamathContext): ConstProofNode? {
        var result: ConstProofNode? = null
        ctx.iterateHypotheses { hyp->
            if ((hyp.type == 'f' || hyp.type == 'e') && hyp.content.contentEquals(stmt.value)) {
                result = ConstProofNode(src = hyp, stmt = stmt)
                STOP
            } else {
                CONTINUE
            }
        }
        return result
    }

    private fun findMatchingAssertions(stmt: Stmt, ctx: MetamathContext): List<CalcProofNode> {
        val result = ArrayList<CalcProofNode>()
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
                    result.add(
                        CalcProofNode(
                            stmt = stmt,
                            substitution = subsList,
                            assertion = assertion,
                            args = ArrayList(assertion.hypotheses.size),
                        )
                    )
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
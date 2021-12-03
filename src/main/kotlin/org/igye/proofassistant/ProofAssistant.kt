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
import org.igye.proofassistant.proof.ProofNodeState.PROVED
import org.igye.proofassistant.proof.ProofNodeState.WAITING
import org.igye.proofassistant.proof.prooftree.CalcProofNode
import org.igye.proofassistant.proof.prooftree.ConstProofNode
import org.igye.proofassistant.proof.prooftree.PendingProofNode
import org.igye.proofassistant.proof.prooftree.ProofNode
import org.igye.proofassistant.substitutions.Substitutions
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.absoluteValue

fun main() {
    val ctx = parseMetamathFile(File("C:\\igye\\books\\metamath/set.mm"))
    val p1 = ProofAssistant.prove("wff ( y e. NN -> y e. CC )", ctx)

    println("p1 = ${toJson(p1)}")
}

object ProofAssistant {
    private val SAVE_CURR_VALUE_CMD = Int.MIN_VALUE

    fun createProvableAssertion(proofNode: ProofNode, ctx: MetamathContext): String {
        val essentialHypotheses: List<Statement> = ctx.getHypotheses { it.type == 'e'}
        val variables = collectAllVariables(essentialHypotheses, proofNode.stmt.value)
        val (floatingHypotheses, _) = MetamathUtils.collectFloatingHypotheses(variables, ctx)
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

        val savedNodeLabelToInt = HashMap<String,Int>()
        fun savedNodeLabelToInt(label:String): Int {
            if (!savedNodeLabelToInt.containsKey(label)) {
                savedNodeLabelToInt[label] = savedNodeLabelToInt.size+1
            }
            return -savedNodeLabelToInt[label]!!
        }

        val proofSteps = ArrayList<Int>()
        val nodesToProcess = Stack<Any>()
        nodesToProcess.push(proofNode)
        while (nodesToProcess.isNotEmpty()) {
            val curNodeToProcess = nodesToProcess.pop()
            if (curNodeToProcess is ConstProofNode) {
                proofSteps.add(labelToInt(curNodeToProcess.src.label))
            } else if (curNodeToProcess is CalcProofNode) {
                if (curNodeToProcess.label != null) {
                    proofSteps.add(savedNodeLabelToInt(curNodeToProcess.label!!))
                } else {
                    nodesToProcess.push(Optional.of(curNodeToProcess))
                    for (i in curNodeToProcess.args.size-1 downTo 0) {
                        nodesToProcess.push(curNodeToProcess.args[i])
                    }
                }
            } else if (curNodeToProcess is Optional<*>) {
                val calcNode = curNodeToProcess.get() as CalcProofNode
                proofSteps.add(labelToInt(calcNode.assertion.statement.label))
                calcNode.removeDependantIf { it.state != PROVED }
                if (calcNode.getDependants().size > 1) {
                    calcNode.label = UUID.randomUUID().toString()
                    savedNodeLabelToInt(calcNode.label!!)
                    proofSteps.add(SAVE_CURR_VALUE_CMD)
                }
            } else {
                throw AssumptionDoesntHoldException()
            }
        }

        for (i in proofSteps.indices) {
            val cmd = proofSteps.get(i)
            if (cmd < 0 && cmd != SAVE_CURR_VALUE_CMD) {
                proofSteps.set(i, proofLabels.size + cmd.absoluteValue)
            }
        }

        return "\$p " + proofNode.stmt.value.asSequence().map { ctx.getSymbolByNumber(it) }.joinToString(" ") +
                " $= ( " + proofLabels.drop(mandatoryHypotheses.size).joinToString(" ") + " ) " +
                proofSteps.asSequence().map { intToStr(it) }.joinToString("") + " $."
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

        val proofContext = ProofContext(PendingProofNode(stmt = stmtToProve))

        while (proofContext.getProved(stmtToProve) == null && proofContext.hasNewStatements()) {
            val currStmtToProve: PendingProofNode = proofContext.getNextStatementToProve()

            val constProof = findConstant(currStmtToProve.stmt, ctx)
            if (constProof != null) {
                proofContext.proofFoundForNodeToBeProved(nodeToBeProved = currStmtToProve, foundProof = constProof)
            } else {
                val matchingAssertions = findMatchingAssertions(currStmtToProve.stmt, ctx)
                for (asrtNode: CalcProofNode in matchingAssertions) {
                    createArgsForCalcNode(asrtNode, proofContext, ctx)
                    if (asrtNode.args.all { it.state == PROVED }) {
                        proofContext.proofFoundForNodeToBeProved(nodeToBeProved = currStmtToProve, foundProof = asrtNode)
                        break
                    } else {
                        currStmtToProve.proofs.add(asrtNode)
                        asrtNode.addDependant(currStmtToProve)
                        asrtNode.state = WAITING
                    }
                }
            }

            if (currStmtToProve.state == ProofNodeState.NEW) {
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
                ?: proofContext.getNew(argStmt)
            if (existingProof != null) {
                existingProof.addDependant(asrtNode)
                asrtNode.args.add(existingProof)
            } else {
                val constProof = findConstant(argStmt, ctx)
                if (constProof != null) {
                    proofContext.markProved(constProof)
                    constProof.addDependant(asrtNode)
                    asrtNode.args.add(constProof)
                } else {
                    val pendingNode = PendingProofNode(stmt = argStmt)
                    proofContext.addNewStatement(pendingNode)
                    pendingNode.addDependant(asrtNode)
                    asrtNode.args.add(pendingNode)
                }
            }
        }
    }

    fun intToStr(iArg: Int): String {
        if (iArg == SAVE_CURR_VALUE_CMD) {
            return "Z"
        }
        if (iArg == 0) {
            throw ProofAssistantException("i == 0")
        }
        val sb = StringBuilder()
        sb.append((65 + (iArg-1) % 20).toChar())
        var i = (iArg-1) / 20
        while (i > 0) {
            //85 is U
            sb.append(((i-1) % 5 + 85).toChar())
            i = (i-1) / 5
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
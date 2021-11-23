package org.igye.metamathparser

import org.igye.common.ContinueInstr
import org.igye.common.MetamathUtils.applySubstitution
import org.igye.common.Utils.subList
import org.igye.proofassistant.substitutions.ParenthesesCounter
import org.igye.proofassistant.substitutions.Substitution
import org.igye.proofassistant.substitutions.Substitutions

var parenCounterProducer: (() -> ParenthesesCounter)? = null

class ProofStack {
    private var nodeCounter = 0;
    private val stack = ArrayList<StackNode>()

    fun size() = stack.size

    fun getLast() = stack.last()

    fun put(constStmt: Statement) {
        if (constStmt.type != 'f' && constStmt.type != 'e') {
            throw MetamathParserException("constStmt.type != 'f' && constStmt.type != 'e'")
        }
        put(ConstStackNode(stmt = constStmt))
    }

    fun apply(assertion: Assertion) {
        if (stack.size < assertion.hypotheses.size) {
            throw MetamathParserException("stack.size < assertion.hypotheses.size")
        }
        val baseStackIdx = stack.size - assertion.hypotheses.size
        val substitution = ArrayList<IntArray>(assertion.numberOfVariables)
        for (i in 0 until assertion.hypotheses.size) {
            val hypothesis = assertion.hypotheses[i]
            if (hypothesis.type == 'f') {
                substitution.add(drop(1, stack[baseStackIdx+i].value))
            }
        }
        for (i in 0 until assertion.hypotheses.size) {
            if (assertion.hypotheses[i].type == 'e') {
                validateSubstitution(
                    asrtStmt = assertion.hypotheses[i].content,
                    stmt = stack[baseStackIdx+i].value,
                    actualSubstitution = substitution,
                    visualizationData = assertion.visualizationData
                )
                if (!stack[baseStackIdx+i].value.contentEquals(applySubstitution(assertion.hypotheses[i].content, substitution))) {
                    throw MetamathParserException("stack.value != assertion.hypothesis")
                }
            }
        }
        val result = CalculatedStackNode(
            args = subList(stack, baseStackIdx, stack.size),
            substitution = substitution,
            assertion = assertion,
            value = applySubstitution(assertion.statement.content, substitution)
        )
        validateSubstitution(
            asrtStmt = assertion.statement.content,
            stmt = result.value,
            actualSubstitution = substitution,
            visualizationData = assertion.visualizationData
        )
        (0 until assertion.hypotheses.size).forEach { stack.removeAt(stack.size-1) }
        put(result)
    }

    private fun drop(n:Int, arr:IntArray):IntArray {
        val res = IntArray(arr.size-n)
        for (i in 0 until res.size) {
            res[i] = arr[i+n]
        }
        return res
    }

    fun put(node: StackNode) {
        if (node.getId() < 0) {
            node.setId(++nodeCounter)
        }
        stack.add(node)
    }

    private fun numsToSymbols(
        stmt: IntArray,
        isAssertion: Boolean,
        visualizationData: VisualizationData
    ): String {
        return stmt.asSequence()
            .map {
                if (it < 0) {
                    visualizationData.symbolsMap[it]?:"#$it"
                } else if (isAssertion) {
                    "$$it"
                } else {
                    visualizationData.symbolsMap[it]?:"$$it"
                }
            }
            .joinToString(separator = " ")
    }

    private fun validateSubstitution(
        asrtStmt: IntArray,
        stmt: IntArray,
        actualSubstitution: ArrayList<IntArray>,
        visualizationData: VisualizationData
    ) {
        if (parenCounterProducer == null) {
            return
        }
//        println(
//            "validateSubstitution: " +
//                "asrtStmt=${numsToSymbols(asrtStmt, true, visualizationData)}; " +
//                "stmt=${numsToSymbols(stmt, false, visualizationData)}; " +
//                "${System.currentTimeMillis()}"
//        )
        val varsPresentInAsrt = IntArray(actualSubstitution.size)
        var atLeastOneVarIsPresent = false
        for (i in 0 until asrtStmt.size) {
            if (asrtStmt[i] >= 0) {
                varsPresentInAsrt[asrtStmt[i]] = 1
                atLeastOneVarIsPresent = true
            }
        }
        if (!atLeastOneVarIsPresent) {
            return
        }
        var matchFound = false
        Substitutions.iterateSubstitutions(
            stmt = stmt,
            asrtStmt = asrtStmt,
            parenCounterProducer = parenCounterProducer!!
        ) subsConsumer@{ subs: Substitution ->
            for (varNum in 0 until actualSubstitution.size) {
                if (varsPresentInAsrt[varNum] == 1
                    && !contentEquals(
                        stmt1 = actualSubstitution[varNum], begin1 = 0, end1 = actualSubstitution[varNum].size-1,
                        stmt2 = stmt, begin2 = subs.begins[varNum], end2 = subs.ends[varNum]
                    )
                ) {
                    return@subsConsumer ContinueInstr.CONTINUE
                }
            }
            matchFound = true
            return@subsConsumer ContinueInstr.STOP
        }
        if (!matchFound) {
            throw MetamathParserException("!matchFound")
        }
    }

    private fun contentEquals(stmt1: IntArray, begin1:Int, end1:Int, stmt2: IntArray, begin2:Int, end2:Int): Boolean {
        val length = end1-begin1
        if (length != end2-begin2) {
            return false
        }
        for (i in 0 .. length) {
            if (stmt1[begin1+i] != stmt2[begin2+i]) {
                return false
            }
        }
        return true
    }
}
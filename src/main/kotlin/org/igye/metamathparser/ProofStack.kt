package org.igye.metamathparser

import org.igye.common.Utils.subList
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class ProofStack {
    private var nodeCounter = 0;
    private val stack = ArrayList<StackNode>()

    fun size() = stack.size

    fun getLast() = stack.last()

    fun put(constStmt: Statement) {
        if (constStmt.type != 'f' && constStmt.type != 'e') {
            throw MetamathParserException("constStmt.type != 'f' && constStmt.type != 'e'")
        }
        put(StackNode(stmt = constStmt, value = constStmt.content))
    }

    fun apply(assertion: Assertion) {
        if (stack.size < assertion.hypotheses.size) {
            throw MetamathParserException("stack.size < assertion.hypotheses.size")
        }
        val baseStackIdx = stack.size - assertion.hypotheses.size
        val substitution = HashMap<Int,IntArray>()
        for (i in 0 until assertion.hypotheses.size) {
            if (assertion.hypotheses[i].type == 'f') {
                substitution.put(
                    assertion.hypotheses[i].content[1],
                    drop(1, stack[baseStackIdx+i].value)
                )
            }
        }
        for (i in 0 until assertion.hypotheses.size) {
            if (assertion.hypotheses[i].type == 'e') {
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

    fun applySubstitution(value:IntArray, substitution: Map<Int,IntArray>): IntArray {
        var resultSize = 0
        for (i in value) {
            if (i < 0) {
                resultSize++
            } else {
                resultSize += substitution[i]!!.size
            }
        }
        val res = IntArray(resultSize)
        var s = 0
        var t = 0
        while (s < value.size) {
            if (value[s] < 0) {
                res[t] = value[s]
                t++
            } else {
                val newExpr: IntArray = substitution[value[s]]!!
                newExpr.copyInto(destination = res, destinationOffset = t)
                t += newExpr.size
            }
            s++
        }
        return res
    }
}
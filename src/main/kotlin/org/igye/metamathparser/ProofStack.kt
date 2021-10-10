package org.igye.metamathparser

import java.util.Collections.unmodifiableList

class ProofStack {
    private var nodeCounter = 0;
    private val stack = ArrayList<StackNode>()

    fun size() = stack.size

    fun get(i:Int) = stack.get(i)

    fun apply(constStmt: SequenceOfSymbols) {
        if (constStmt.seqType != 'f' && constStmt.seqType != 'e') {
            throw MetamathParserException("constStmt.seqType != 'f' && constStmt.seqType != 'e'")
        }
        add(StackNode(value = constStmt.symbols))
    }

    fun apply(assertion: Assertion) {
        if (stack.size < assertion.hypotheses.size) {
            throw MetamathParserException("stack.size < assertion.hypotheses.size")
        }
        val baseStackIdx = stack.size - assertion.hypotheses.size
        val substitution = HashMap<String,List<String>>()
        for (i in 0 until assertion.hypotheses.size) {
            if (assertion.hypotheses[i].sequence.seqType == 'f') {
                substitution.put(
                    assertion.hypotheses[i].sequence.symbols[1],
                    stack[baseStackIdx+i].value.drop(1)
                )
            }
        }
        for (i in 0 until assertion.hypotheses.size) {
            if (assertion.hypotheses[i].sequence.seqType == 'e') {
                if (stack[baseStackIdx+i].value != applySubstitution(assertion.hypotheses[i].sequence.symbols, substitution)) {
                    throw MetamathParserException("stack.value != assertion.hypothesis")
                }
            }
        }
        val result = CalculatedStackNode(
            args = unmodifiableList(stack.subList(baseStackIdx, stack.size)),
            substitution = substitution,
            assertion = assertion,
            value = applySubstitution(assertion.assertion.sequence.symbols, substitution)
        )
        (0 until assertion.hypotheses.size).forEach { stack.removeAt(stack.size-1) }
        add(result)
    }

    fun add(node: StackNode) {
        if (node.getId() < 0) {
            node.setId(nodeCounter++)
        }
        stack.add(node)
    }

    private fun applySubstitution(value:List<String>, substitution: HashMap<String,List<String>>): List<String> {
        val res = ArrayList<String>()
        value.forEach {
            val subSeq = substitution[it]
            if (subSeq != null) {
                res.addAll(subSeq)
            } else {
                res.add(it)
            }
        }
        return res
    }
}
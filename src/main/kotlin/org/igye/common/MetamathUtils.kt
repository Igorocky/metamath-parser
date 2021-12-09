package org.igye.common

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import org.igye.metamathparser.Assertion
import org.igye.metamathparser.MetamathContext
import org.igye.metamathparser.MetamathParserException
import org.igye.metamathparser.Statement
import org.igye.proofassistant.proof.*
import org.igye.proofassistant.proof.prooftree.CalcProofNode
import org.igye.proofassistant.proof.prooftree.ConstProofNode
import org.igye.proofassistant.proof.prooftree.PendingProofNode
import org.igye.proofassistant.proof.prooftree.ProofNode

object MetamathUtils {
    private val MAPPER = ObjectMapper()
    init {
        MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL)
        MAPPER.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
    }

    fun toJson(obj: Any): String {
        return MAPPER.writeValueAsString(obj)
    }

    fun toJson(node: ProofNode): String {
        return MAPPER.writeValueAsString(toDto(node))
    }

    fun toDto(node: ProofNode): ProofNodeDto {
        return when (node) {
            is ConstProofNode -> toDto(node)
            is CalcProofNode -> toDto(node)
            is PendingProofNode -> toDto(node)
        }
    }

    fun toDto(node: ConstProofNode): ConstProofNodeDto {
        return ConstProofNodeDto(
            c = node.stmt.valueStr,
            label = node.src.label,
            hash = System.identityHashCode(node),
            state = node.state,
        )
    }

    fun toDto(node: PendingProofNode): PendingProofNodeDto {
        return PendingProofNodeDto(
            w = node.stmt.valueStr,
            hash = System.identityHashCode(node),
            state = node.state,
            proofs = node.proofs.map { toDto(it) }
        )
    }

    fun toDto(node: CalcProofNode): CalcProofNodeDto {
        return CalcProofNodeDto(
            a = node.stmt.valueStr,
            label = node.assertion.statement.label,
            hash = System.identityHashCode(node),
            state = node.state,
            args = node.args.map { toDto(it) }
        )
    }

    fun toString(stmt: IntArray, ctx: MetamathContext):String {
        return stmt.asSequence().map(ctx::getSymbolByNumber).joinToString(separator = " ")
    }

    fun toString(assertion: Assertion):String {
        return assertion.statement.content.asSequence().map(assertion.visualizationData::numToSym).joinToString(separator = " ") +
                " <=== " + assertion.hypotheses.asSequence()
            .map { it.content.asSequence().map(assertion.visualizationData::numToSym).joinToString(separator = " ") }
            .joinToString(separator = " ::: ")

    }

    fun applySubstitution(value:IntArray, substitution: List<IntArray>): IntArray {
        var resultSize = 0
        for (i in value) {
            if (i < 0) {
                resultSize++
            } else {
                resultSize += substitution[i].size
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
                val newExpr: IntArray = substitution[value[s]]
                newExpr.copyInto(destination = res, destinationOffset = t)
                t += newExpr.size
            }
            s++
        }
        return res
    }

    fun collectFloatingHypotheses(variables:Set<Int>, ctx: MetamathContext): Pair<List<Statement>, HashMap<Int, Int>> {
        val variablesTypes = HashMap<Int,Int>()
        val floatingHypotheses = ctx.getHypotheses {
            when (it.type) {
                'f' -> {
                    val varNum = it.content[1]
                    if (variables.contains(varNum)) {
                        if (variablesTypes.containsKey(varNum)) {
                            throw MetamathParserException("variablesTypes.containsKey(varName)")
                        } else {
                            variablesTypes[varNum] = it.content[0]
                            true
                        }
                    } else {
                        false
                    }
                }
                else -> false
            }
        }
        if (variablesTypes.keys != variables) {
            throw MetamathParserException("types.keys != variables")
        }
        return Pair(floatingHypotheses, variablesTypes)
    }

    fun collectAllVariables(essentialHypotheses: List<Statement>, assertionStatement: IntArray):Set<Int> {
        val variables = HashSet<Int>()
        for (hypothesis in essentialHypotheses) {
            for (i in hypothesis.content) {
                if (i >= 0) {
                    variables.add(i)
                }
            }
        }
        for (i in assertionStatement) {
            if (i >= 0) {
                variables.add(i)
            }
        }
        return variables
    }

    fun mkStmt(stmt: IntArray, ctx: MetamathContext): Stmt = Stmt(
        value = stmt,
        valueStr = stmt.asSequence().map { ctx.getSymbolByNumber(it) }.joinToString(" ")
    )

    fun mkStmt(str:String, ctx: MetamathContext): Stmt = mkStmt(
        stmt = str.trim().split(' ').asSequence()
            .map { ctx.getNumberBySymbol(it) }
            .toList()
            .toIntArray(),
        ctx = ctx
    )
}
package org.igye.common

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import org.igye.metamathparser.Assertion
import org.igye.metamathparser.MetamathContext
import org.igye.metamathparser.MetamathParserException
import org.igye.metamathparser.Statement
import org.igye.proofassistant.proof.*

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
            is VarProofNode -> toDto(node)
            is CalculatedProofNode -> toDto(node)
            else -> ProofNodeDto(
                u = "Unexpected type of proof node: ${node.javaClass.canonicalName}",
                proofLength = node.proofLength,
                isCanceled = node.isCanceled
            )
        }
    }

    fun toDto(node: ConstProofNode): ProofNodeDto {
        return ProofNodeDto(
            f = node.valueStr,
            proofLength = node.proofLength,
            isCanceled = node.isCanceled,
        )
    }

    fun toDto(node: VarProofNode): ProofNodeDto {
        return ProofNodeDto(
            v = node.valueStr,
            proofLength = node.proofLength,
            isCanceled = node.isCanceled,
            proofs = node.proofs.map { toDto(it) }
        )
    }

    fun toDto(node: CalculatedProofNode): ProofNodeDto {
        return ProofNodeDto(
            a = node.valueStr,
            proofLength = node.proofLength,
            isCanceled = node.isCanceled,
            args = node.args.map { toDto(it) }
        )
    }

    fun toString(stmt: IntArray, ctx: MetamathContext):String {
        return stmt.asSequence().map(ctx::getSymbolByNumber).joinToString(separator = " ")
    }

    fun toString(assertion: Assertion):String {
        return assertion.hypotheses.asSequence()
            .map { it.content.asSequence().map(assertion.visualizationData::numToSym).joinToString(separator = " ") }
            .joinToString(separator = " ::: ") + " ===> " +
                assertion.statement.content.asSequence().map(assertion.visualizationData::numToSym).joinToString(separator = " ")
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
}
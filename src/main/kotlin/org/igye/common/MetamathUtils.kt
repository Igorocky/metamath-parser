package org.igye.common

import org.igye.metamathparser.Assertion
import org.igye.metamathparser.MetamathContext

object MetamathUtils {

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
}
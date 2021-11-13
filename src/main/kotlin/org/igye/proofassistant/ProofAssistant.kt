package org.igye.proofassistant

import org.igye.metamathparser.*
import java.util.*

object ProofAssistant {
    fun prove(expr: String, ctx: MetamathContext): StackNode {
        val allowedStatementsTypes: Set<Int> = setOf("wff", "setvar", "class").map { ctx.getNumberBySymbol(it) }.toSet()
        val stmtToProve = Statement(
            type = 'p',
            content = expr.split("\\s".toRegex()).map { ctx.getNumberBySymbol(it) }.toIntArray()
        )
        if (!allowedStatementsTypes.contains(stmtToProve.content[0])) {
            throw MetamathParserException("!allowedStatementsTypes.contains(stmtToProve.content[0])")
        }

        return ConstStackNode(Statement(type = 'n',content = intArrayOf()))
    }

    fun iterateSubstitutions(stmt:IntArray, asrtStmt:IntArray, consumer: ((Substitution) -> Unit)) {
        val numOfVariables = asrtStmt.asSequence().filter { it >= 0 }.maxOrNull()!!+1
        iterateMatchingConstParts(stmt, asrtStmt) matchingConstPartsConsumer@{ constParts: List<IntArray>, matchingConstParts: Array<IntArray> ->
            val varGroups = createVarGroups(stmt, asrtStmt, constParts, matchingConstParts)
            val subs = Substitution(
                stmt = stmt,
                begins = IntArray(numOfVariables),
                ends = IntArray(numOfVariables),
                levels = IntArray(numOfVariables){Int.MAX_VALUE}
            )
            iterateSubstitutions(
                currSubs = subs,
                varGroups = varGroups,
                currGrpIdx = 0,
                currVarIdx = 0,
                subExprBeginIdx = varGroups[0].exprBeginIdx,
                consumer = consumer
            )
        }
    }

    fun iterateSubstitutions(
        currSubs:Substitution,
        varGroups:List<VarGroup>, currGrpIdx:Int, currVarIdx:Int, subExprBeginIdx:Int,
        consumer: ((Substitution) -> Unit)
    ) {
        val stmt = currSubs.stmt
        val grp = varGroups[currGrpIdx]
        val varNum = grp.asrtStmt[grp.varsBeginIdx + currVarIdx]
        val level = grp.level+currVarIdx
        val maxSubExprLength = grp.exprEndIdx-subExprBeginIdx+1-(grp.numOfVars-currVarIdx-1)

        fun invokeNext(subExprLength:Int) {
            if (currVarIdx < grp.numOfVars-1) {
                iterateSubstitutions(
                    currSubs = currSubs,
                    varGroups = varGroups,
                    currGrpIdx = currGrpIdx,
                    currVarIdx = currVarIdx+1,
                    subExprBeginIdx = subExprBeginIdx+subExprLength,
                    consumer = consumer
                )
            } else if (currGrpIdx < varGroups.size-1) {
                iterateSubstitutions(
                    currSubs = currSubs,
                    varGroups = varGroups,
                    currGrpIdx = currGrpIdx+1,
                    currVarIdx = 0,
                    subExprBeginIdx = varGroups[currGrpIdx+1].exprBeginIdx,
                    consumer = consumer
                )
            } else {
                consumer(currSubs)
            }
        }

        if (currSubs.levels[varNum] >= level) {
            currSubs.levels[varNum] = level
            if (currVarIdx == grp.numOfVars-1) {
                currSubs.begins[varNum] = subExprBeginIdx
                currSubs.ends[varNum] = grp.exprEndIdx
                invokeNext(subExprLength = maxSubExprLength)
            } else {
                currSubs.levels[varNum] = level
                currSubs.begins[varNum] = subExprBeginIdx
                var subExprLength = 1
                while (subExprLength <= maxSubExprLength) {
                    currSubs.ends[varNum] = subExprBeginIdx+subExprLength-1
                    invokeNext(subExprLength = subExprLength)
                    subExprLength++
                }
            }
            currSubs.levels[varNum] = Int.MAX_VALUE
        } else {
            val existingSubExprBeginIdx = currSubs.begins[varNum]
            val existingSubLength = currSubs.ends[varNum]- existingSubExprBeginIdx +1
            if (existingSubLength <= maxSubExprLength
                && (currVarIdx < grp.numOfVars-1 || existingSubLength == maxSubExprLength)) {
                var checkedLength = 0
                while (checkedLength < existingSubLength
                    && stmt[existingSubExprBeginIdx+checkedLength] == stmt[subExprBeginIdx+checkedLength]) {
                    checkedLength++
                }
                if (checkedLength == existingSubLength) {
                    invokeNext(subExprLength = checkedLength)
                }
            }
        }
    }

    fun createVarGroups(stmt:IntArray, asrtStmt:IntArray, constParts: List<IntArray>, matchingConstParts: Array<IntArray>):List<VarGroup> {
        val result = ArrayList<VarGroup>()
        if (constParts[0][0] > 0) {
            result.add(
                VarGroup(
                    asrtStmt = asrtStmt,
                    numOfVars = constParts[0][0],
                    varsBeginIdx = 0,
                    exprBeginIdx = 0,
                    exprEndIdx = matchingConstParts[0][0]
            ))
        }
        for (i in 0 .. constParts.size-2) {
            result.add(
                VarGroup(
                    asrtStmt = asrtStmt,
                    numOfVars = constParts[i+1][0] - constParts[i][1] - 1,
                    varsBeginIdx = constParts[i][1]+1,
                    exprBeginIdx = matchingConstParts[i][1]+1,
                    exprEndIdx = matchingConstParts[i+1][0]-1
                ))
        }
        val lastConstPart = constParts.last()
        if (lastConstPart[1] != asrtStmt.size-1) {
            result.add(
                VarGroup(
                    asrtStmt = asrtStmt,
                    numOfVars = asrtStmt.size - lastConstPart[1] - 1,
                    varsBeginIdx = lastConstPart[1]+1,
                    exprBeginIdx = matchingConstParts.last()[1]+1,
                    exprEndIdx = stmt.size-1
                ))
        }
        Collections.sort(result, compareBy { it.numberOfStates })
        var level = 0
        for (varGroup in result) {
            varGroup.level=level
            level+=varGroup.numOfVars
        }
        return result
    }

    fun iterateMatchingConstParts(
        stmt: IntArray,
        asrtStmt: IntArray,
        consumer: (constParts: List<IntArray>, matchingConstParts: Array<IntArray>) -> Unit
    ) {
        // TODO: 10/23/2021 move constParts to Assertion
        val constParts: ArrayList<IntArray> = ArrayList()
        for (i in 0 until asrtStmt.size) {
            if (asrtStmt[i] < 0) {
                if (constParts.isEmpty() || constParts.last()[1] >= 0) {
                    constParts.add(intArrayOf(i,-1))
                }
            } else if (constParts.isNotEmpty() && constParts.last()[1] < 0) {
                constParts.last()[1] = i-1
            }
        }
        if (constParts.isNotEmpty() && constParts.last()[1] < 0) {
            constParts.last()[1] = asrtStmt.size-1
        }
        val matchingConstParts = Array(constParts.size){ intArrayOf() }
        if (findLeftmostMatchingConstParts(0, constParts, matchingConstParts, stmt, asrtStmt)) {
            consumer(constParts, matchingConstParts)
            while (nextMatchingConstParts(stmt, asrtStmt, constParts, matchingConstParts)) {
                consumer(constParts, matchingConstParts)
            }
        }
    }

    fun nextMatchingConstParts(
        stmt: IntArray,
        asrtStmt: IntArray,
        constParts: ArrayList<IntArray>,
        matchingConstParts: Array<IntArray>
    ): Boolean {
        val asrtEndsWithConst = constParts.last()[1] == asrtStmt.size-1
        var p = matchingConstParts.size-1
        while (p >= 0) {
            if (p == 0 && constParts[0][0] == 0) {
                return false
            } else if (p == constParts.size-1 && asrtEndsWithConst) {
                p--
                continue
            }
            val nextMatch = findFirstSubSeq(
                where = stmt, startIdx = matchingConstParts[p][0]+1,
                what = asrtStmt, begin = constParts[p][0], end = constParts[p][1]
            )
            if (nextMatch != null && (p < constParts.size-1 || stmt.size - nextMatch[1] >= asrtStmt.size - constParts.last()[1])) {
                matchingConstParts[p] = nextMatch
                if (p < matchingConstParts.size-1) {
                    val allRemainingPartsFound = findLeftmostMatchingConstParts(
                        firstConstPartIdx = p+1,
                        constParts = constParts,
                        matchingConstParts = matchingConstParts,
                        stmt = stmt,
                        asrtStmt = asrtStmt
                    )
                    if (!allRemainingPartsFound) {
                        continue
                    } else {
                        break
                    }
                } else {
                    break
                }
            }
            p--
        }
        return p >= 0
    }

    private fun lengthOfGap(leftConstPartIdx:Int, constParts: List<IntArray>, asrtStmtSize: Int): Int {
        if (leftConstPartIdx < 0) {
            return constParts[0][0]
        } else if (leftConstPartIdx < constParts.size-1) {
            return constParts[leftConstPartIdx+1][0] - constParts[leftConstPartIdx][1] - 1
        } else {
            return asrtStmtSize - constParts[leftConstPartIdx][1] - 1
        }
    }

    private fun findLeftmostMatchingConstParts(
        firstConstPartIdx: Int,
        constParts: List<IntArray>,
        matchingConstParts: Array<IntArray>,
        stmt: IntArray,
        asrtStmt: IntArray
    ): Boolean {
        for (i in firstConstPartIdx until constParts.size) {
            val startIdx = lengthOfGap(i - 1, constParts, asrtStmt.size) + (if (i == 0) 0 else matchingConstParts[i - 1][1] + 1)
            val match = if (i == constParts.size-1 && constParts[i][1] == asrtStmt.size-1) {
                val len = asrtStmt.size - constParts[i][0]
                if (
                    startIdx + len <= stmt.size
                    && endsWith(what = stmt, pattern = asrtStmt, begin = constParts[i][0])
                ) {
                    intArrayOf(stmt.size-len, stmt.size-1)
                } else {
                    null
                }
            } else {
                findFirstSubSeq(where = stmt, startIdx = startIdx, what = asrtStmt, begin = constParts[i][0], end = constParts[i][1])
            }
            if (match == null || i == 0 && constParts[0][0] == 0 && match[0] != 0) {
                return false
            }
            matchingConstParts[i] = match
        }
        if (stmt.size - matchingConstParts.last()[1] < asrtStmt.size - constParts.last()[1]) {
            return false
        }
        return true
    }

    private fun findFirstSubSeq(where:IntArray, startIdx:Int, what:IntArray, begin:Int, end:Int): IntArray? {
        val len = end - begin + 1
        val maxS = where.size-len
        var i = 0
        var s = startIdx
        var e = s-1
        while (s <= maxS && e < s) {
            if (where[s+i] == what[begin+i]) {
                if (i == len-1) {
                    e = s+i
                } else {
                    i++
                }
            } else {
                s++
                i = 0
            }
        }
        return if (s <= e) intArrayOf(s,e) else null
    }

    fun endsWith(what:IntArray, pattern:IntArray, begin:Int): Boolean {
        val len = pattern.size - begin
        val whatBegin = what.size - len
        for (i in 0 until len) {
            if (what[whatBegin+i] != pattern[begin+i]) {
                return false
            }
        }
        return true
    }
}
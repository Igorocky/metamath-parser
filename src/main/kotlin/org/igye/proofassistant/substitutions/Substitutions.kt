package org.igye.proofassistant.substitutions

import java.util.*

object Substitutions {

    fun iterateSubstitutions(stmt:IntArray, asrtStmt:IntArray, parenCounterProducer: () -> ParenthesesCounter, consumer: ((Substitution) -> Unit)) {
        val maxVarNumberOrNull = asrtStmt.asSequence().filter { it >= 0 }.maxOrNull()
        if (maxVarNumberOrNull == null) {
            if (asrtStmt.contentEquals(stmt)) {
                consumer(
                    Substitution(
                        stmt = stmt,
                        begins = IntArray(0),
                        ends = IntArray(0),
                        isDefined = BooleanArray(0),
                        parenthesesCounter = emptyArray(),
                    )
                )
            }
        } else {
            val numOfVariables = maxVarNumberOrNull+1
            iterateMatchingConstParts(
                stmt = stmt,
                asrtStmt = asrtStmt,
                parenCounterProducer = parenCounterProducer,
            ) { constParts: ConstParts, matchingConstParts: ConstParts ->
                val varGroups = createVarGroups(stmt, asrtStmt, constParts, matchingConstParts)
                val subs = Substitution(
                    stmt = stmt,
                    begins = IntArray(numOfVariables),
                    ends = IntArray(numOfVariables),
                    isDefined = BooleanArray(numOfVariables){false},
                    parenthesesCounter = Array(numOfVariables){parenCounterProducer()},
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
    }

    private fun iterateSubstitutions(
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

        if (!currSubs.isDefined[varNum]) {
            currSubs.isDefined[varNum] = true
            currSubs.begins[varNum] = subExprBeginIdx
            if (currVarIdx == grp.numOfVars-1) {
                currSubs.ends[varNum] = grp.exprEndIdx
                invokeNext(subExprLength = maxSubExprLength)
            } else {
                val parenthesesCounter = currSubs.parenthesesCounter[varNum]
                parenthesesCounter.reset()
                var subExprLength = 1
                var end = subExprBeginIdx
                while (subExprLength <= maxSubExprLength) {
                    currSubs.ends[varNum] = end
                    val brStatus = parenthesesCounter.accept(stmt[end])
                    if (brStatus == ParenthesesCounter.BR_OK) {
                        invokeNext(subExprLength = subExprLength)
                    } else if (brStatus == ParenthesesCounter.BR_FAILED) {
                        break
                    }
                    subExprLength++
                    end++
                }
            }
            currSubs.isDefined[varNum] = false
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

    private fun createVarGroups(stmt:IntArray, asrtStmt:IntArray, constParts: ConstParts, matchingConstParts: ConstParts):List<VarGroup> {
        val result = ArrayList<VarGroup>()
        if (constParts.size == 0) {
            result.add(
                VarGroup(
                    asrtStmt = asrtStmt,
                    numOfVars = asrtStmt.size,
                    varsBeginIdx = 0,
                    exprBeginIdx = 0,
                    exprEndIdx = stmt.size-1,
                    level = 0
                )
            )
        } else {
            if (constParts.begins[0] > 0) {
                result.add(
                    VarGroup(
                        asrtStmt = asrtStmt,
                        numOfVars = constParts.begins[0],
                        varsBeginIdx = 0,
                        exprBeginIdx = 0,
                        exprEndIdx = matchingConstParts.begins[0]
                    )
                )
            }
            for (i in 0 .. constParts.size-2) {
                result.add(
                    VarGroup(
                        asrtStmt = asrtStmt,
                        numOfVars = constParts.begins[i+1] - constParts.ends[i] - 1,
                        varsBeginIdx = constParts.ends[i]+1,
                        exprBeginIdx = matchingConstParts.ends[i]+1,
                        exprEndIdx = matchingConstParts.begins[i+1]-1
                    )
                )
            }
            val lastConstPart = constParts.size-1
            if (constParts.ends[lastConstPart] != asrtStmt.size-1) {
                result.add(
                    VarGroup(
                        asrtStmt = asrtStmt,
                        numOfVars = asrtStmt.size - constParts.ends[lastConstPart] - 1,
                        varsBeginIdx = constParts.ends[lastConstPart]+1,
                        exprBeginIdx = matchingConstParts.ends[lastConstPart]+1,
                        exprEndIdx = stmt.size-1
                    )
                )
            }
            Collections.sort(result, compareBy { it.numberOfStates })
            var level = 0
            for (varGroup in result) {
                varGroup.level=level
                level+=varGroup.numOfVars
            }
        }
        return result
    }

    private fun createConstParts(stmt: IntArray): ConstParts {
        val constParts: MutableList<IntArray> = ArrayList()
        for (i in stmt.indices) {
            if (stmt[i] < 0) {
                if (constParts.isEmpty() || constParts.last()[1] >= 0) {
                    constParts.add(intArrayOf(i,-1))
                }
            } else if (constParts.isNotEmpty() && constParts.last()[1] < 0) {
                constParts.last()[1] = i-1
            }
        }
        if (constParts.isNotEmpty() && constParts.last()[1] < 0) {
            constParts.last()[1] = stmt.size-1
        }
        val result = ConstParts(
            begins = IntArray(constParts.size),
            ends = IntArray(constParts.size),
            parenCounters = emptyArray(),
            remainingMinLength = Array(constParts.size){0},
        )
        var remainingMinLength = 0
        for (i in constParts.indices.reversed()) {
            result.begins[i] = constParts[i][0]
            result.ends[i] = constParts[i][1]
            remainingMinLength += (result.ends[i]-result.begins[i]+1) + lengthOfGap(
                leftConstPartIdx = i,
                constParts = constParts,
                stmt.size
            )
            result.remainingMinLength[i] = remainingMinLength
        }
        return result
    }

    private fun iterateMatchingConstParts(
        stmt: IntArray,
        asrtStmt: IntArray,
        constParts: ConstParts,
        matchingConstParts: ConstParts,
        idxToMatch: Int,
        consumer: (constParts: ConstParts, matchingConstParts: ConstParts) -> Unit
    ) {
        fun invokeNext() {
            iterateMatchingConstParts(
                stmt = stmt,
                asrtStmt = asrtStmt,
                constParts = constParts,
                matchingConstParts = matchingConstParts,
                idxToMatch = idxToMatch+1,
                consumer = consumer,
            )
        }

        if (idxToMatch == constParts.size) {
            if (constParts.size > 0 && matchingConstParts.ends[idxToMatch-1] != stmt.size-1) {
                if (constParts.ends[idxToMatch-1] == asrtStmt.size-1) {
                    return
                }
                val matchingRemainingGap = stmt.size - matchingConstParts.ends[idxToMatch-1]
                val remainingGap = asrtStmt.size - constParts.ends[idxToMatch-1]
                if (remainingGap > matchingRemainingGap) {
                    return
                }
                val parenCounter = matchingConstParts.parenCounters[idxToMatch]
                parenCounter.reset()
                var parenState = ParenthesesCounter.BR_OK
                for (i in matchingConstParts.ends[idxToMatch-1]+1 .. stmt.size-1) {
                    parenState = parenCounter.accept(stmt[i])
                    if (parenState == ParenthesesCounter.BR_FAILED) {
                        return
                    }
                }
                if (parenState != ParenthesesCounter.BR_OK) {
                    return
                }
            }
            consumer(constParts, matchingConstParts)
        } else {
            if (idxToMatch == 0 && constParts.begins[idxToMatch] == 0) {
                for (i in 0 .. constParts.ends[idxToMatch]) {
                    if (asrtStmt[i] != stmt[i]) {
                        return
                    }
                }
                matchingConstParts.begins[idxToMatch]=0
                matchingConstParts.ends[idxToMatch]=constParts.ends[idxToMatch]
                invokeNext()
            } else {
                var begin = if (idxToMatch > 0) matchingConstParts.ends[idxToMatch-1]+1 else 0
                val maxBegin = stmt.size - constParts.remainingMinLength[idxToMatch]
                val parenCounter = matchingConstParts.parenCounters[idxToMatch]
                parenCounter.reset()
                var parenState = ParenthesesCounter.BR_OK
                val numOfVars = if (idxToMatch > 0) {
                    constParts.begins[idxToMatch] - constParts.ends[idxToMatch-1] - 1
                } else {
                    constParts.begins[0]
                }
                val partLen = constParts.ends[idxToMatch] - constParts.begins[idxToMatch] + 1
                for (i in 1 .. numOfVars) {
                    parenState = parenCounter.accept(stmt[begin])
                    begin++
                }
                while (begin <= maxBegin && parenState != ParenthesesCounter.BR_FAILED) {
                    if (parenState == ParenthesesCounter.BR_OK) {
                        var matchedLen = 0
                        while (matchedLen < partLen) {
                            if (asrtStmt[constParts.begins[idxToMatch] + matchedLen] != stmt[begin + matchedLen]) {
                                break
                            }
                            matchedLen++
                        }
                        if (matchedLen == partLen) {
                            matchingConstParts.begins[idxToMatch]=begin
                            matchingConstParts.ends[idxToMatch]=begin+partLen-1
                            invokeNext()
                        }
                    }
                    parenState = parenCounter.accept(stmt[begin])
                    begin++
                }
            }

        }
    }

    fun iterateMatchingConstParts(
        stmt: IntArray,
        asrtStmt: IntArray,
        parenCounterProducer: () -> ParenthesesCounter,
        consumer: (constParts: ConstParts, matchingConstParts: ConstParts) -> Unit
    ) {
        // TODO: 10/23/2021 move constParts and matchingConstParts to Assertion
        val constParts = createConstParts(asrtStmt)
        val matchingConstParts = ConstParts(
            begins = IntArray(constParts.begins.size),
            ends = IntArray(constParts.begins.size),
            parenCounters = Array(constParts.begins.size+1){parenCounterProducer()},
            remainingMinLength = emptyArray()
        )
        iterateMatchingConstParts(
            stmt = stmt,
            asrtStmt = asrtStmt,
            constParts = constParts,
            matchingConstParts = matchingConstParts,
            idxToMatch = 0,
            consumer = consumer
        )
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
}
package org.igye.proofassistant

import org.igye.metamathparser.*

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

    private fun findPossibleSubstitutions(stmt: IntArray, assertion: Assertion): List<IntArray> {
        val asrtStmt = assertion.statement.content
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



        val matchingConstParts = ArrayList<IntArray>(constParts.size)
        for (i in 0 until constParts.size) {
            val nextMatch = findFirstSubSeq(
                where = stmt, startIdx = lengthOfGap(i-1, constParts, asrtStmt.size) + (if (matchingConstParts.isEmpty()) 0 else matchingConstParts.last()[1]+1),
                what = asrtStmt, begin = constParts[i][0], end = constParts[i][1]
            )
            if (nextMatch == null) {
                break
            }
            matchingConstParts[i] = nextMatch
        }
        if (matchingConstParts.size != constParts.size) {
            return emptyList()
        }

        val possibleSubs = ArrayList<IntArray>()

        var s = 0
        var a = 0
        while (s < stmt.size && a < asrtStmt.size) {
            if (asrtStmt[a] < 0) {
                if (asrtStmt[a] == stmt[s]) {

                }
            }
        }

        return emptyList()
    }

    private fun findPossibleSubstitutions(
        stmt: IntArray,
        asrtStmt: IntArray,
        numOfVariables:Int,
        constParts: ArrayList<IntArray>,
        matchingConstParts: ArrayList<IntArray>,
    ): List<IntArray> {
        val varGroups:MutableList<Triple<IntArray,IntArray,IntArray>> = ArrayList()
        for (i in 0 until constParts.size) {
            if (i == 0 && constParts[0][0] != 0) {
                val vars = intArrayOf(0, constParts[0][0] - 1)
                varGroups.add(Triple(
                    vars,
                    intArrayOf(0,matchingConstParts[0][0]-1),
                    IntArray(vars[1]-vars[0]+1) { -1 }
                ))
            }
            if (i == constParts.size-1 && constParts.last()[1] != asrtStmt.size-1) {
                val vars = intArrayOf(constParts.last()[1] + 1, asrtStmt.size - 1)
                varGroups.add(Triple(
                    vars,
                    intArrayOf(matchingConstParts.last()[1]+1, stmt.size-1),
                    IntArray(vars[1]-vars[0]+1) { -1 }
                ))
            }
            if (i < constParts.size-1) {
//                varGroups.add(
//                    Triple(
//                    intArrayOf(constParts[i][1]+1, constParts[i+1][0]-1),
//                    intArrayOf(matchingConstParts[i][1]+1, matchingConstParts[i+1][0]-1)
//                )
//                )
            }
        }

//        fun nextVarGroupState(subs:Array<IntArray>, grp:Pair<IntArray,IntArray>, state:)

        return emptyList()
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
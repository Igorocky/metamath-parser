package org.igye.proofassistant

class VarGroup(
    val vars:IntArray,
    val sameVarsIdxs: IntArray?,
    val exprBeginIdx:Int, val exprEndIdx:Int,
    val subExprBegins: IntArray
) {
    fun init() {
        for (i in 0 until vars.size) {
            subExprBegins[i] = exprBeginIdx+i
        }
        subExprBegins[vars.size]=exprEndIdx+1
    }

    fun doesntContradict(stmt: IntArray, otherVarGroup: VarGroup):Boolean {
        for (i in 0 until vars.size) {
            for (j in 0 until otherVarGroup.vars.size) {
                if (vars[i] == otherVarGroup.vars[j]) {
                    val begin1 = subExprBegins[i]
                    val end1 = subExprBegins[i+1]-1
                    val begin2 = otherVarGroup.subExprBegins[j]
                    val end2 = otherVarGroup.subExprBegins[j+1]-1
                    if (!contentEquals(stmt, begin1, end1, begin2, end2)) {
                        return false
                    }
                }
            }
        }
        return true
    }

    fun doesntContradictItself(stmt: IntArray):Boolean {
        if (sameVarsIdxs == null) {
            return true
        } else {
            for (i in 0 until sameVarsIdxs.size/2) {
                val idx = i*2
                if (!contentEquals(stmt, sameVarsIdxs[idx], sameVarsIdxs[idx+1])) {
                    return false
                }
            }
            return true
        }
    }

    private fun contentEquals(stmt: IntArray, var1Idx:Int, var2Idx:Int): Boolean {
        val begin1 = subExprBegins[var1Idx]
        val end1 = subExprBegins[var1Idx+1]-1
        val begin2 = subExprBegins[var2Idx]
        val end2 = subExprBegins[var2Idx+1]-1
        return contentEquals(stmt, begin1, end1, begin2, end2)
    }

    private fun contentEquals(stmt: IntArray, begin1:Int, end1:Int, begin2:Int, end2:Int): Boolean {
        val length = end1-begin1
        if (length != end2-begin2) {
            return false
        }
        for (i in 0 .. length) {
            if (stmt[begin1+i] != stmt[begin2+i]) {
                return false
            }
        }
        return true
    }

    fun nextDelims(): Boolean {
        var i = subExprBegins.size-2
        while (i > 0) {
            if (subExprBegins[i] < exprEndIdx-(subExprBegins.size-2-i)) {
                subExprBegins[i]++
                for (j in i+1 until subExprBegins.size-1) {
                    subExprBegins[j]=subExprBegins[j-1]+1
                }
                break
            }
            i--
        }
        return i > 0
    }
}
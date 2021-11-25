package org.igye.proofassistant.proof

import org.igye.common.MetamathUtils
import org.igye.metamathparser.Assertion
import org.igye.metamathparser.Statement
import java.util.*
import kotlin.collections.ArrayList

abstract class ProofNode(val stmt: Stmt) {
    var parent: ProofNode? = null
    var proofLength: Int = -1
    var isCanceled: Boolean = false

    override fun toString(): String {
        return stmt.toString()
    }
}

class ConstProofNode(val src: Statement, stmt: Stmt):ProofNode(stmt = stmt) {
    init {
        proofLength = 0
    }
}

class CalcProofNode(
    stmt: Stmt,
    val args: MutableList<ProofNode>,
    val substitution: List<IntArray>,
    val assertion: Assertion,
):ProofNode(stmt = Stmt(value = stmt.value, valueStr = stmt.valueStr + " <<<<< " + MetamathUtils.toString(assertion)))

class PendingProofNode(stmt: Stmt, val proofs: MutableList<ProofNode> = ArrayList()): ProofNode(stmt = stmt)

class ValProofNode(
    stmt: Stmt,
    val label: String = UUID.randomUUID().toString(),
    var proof: ProofNode?,
    val usedBy: MutableList<ProofNode> = ArrayList()
):ProofNode(stmt = stmt)

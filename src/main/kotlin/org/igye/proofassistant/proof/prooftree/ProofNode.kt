package org.igye.proofassistant.proof.prooftree

import org.igye.common.MetamathUtils
import org.igye.metamathparser.Assertion
import org.igye.metamathparser.Statement
import org.igye.proofassistant.proof.ProofContext
import org.igye.proofassistant.proof.ProofNodeState
import org.igye.proofassistant.proof.Stmt

sealed class ProofNode(val proofContext: ProofContext, val stmt: Stmt) {
    var state: ProofNodeState = ProofNodeState.TO_BE_PROVED
    val parents: MutableList<ProofNode> = ArrayList()

    override fun toString(): String {
        return stmt.toString()
    }
}

class ConstProofNode(proofContext: ProofContext, stmt: Stmt, val src: Statement):ProofNode(stmt = stmt, proofContext = proofContext)

class CalcProofNode(
    proofContext: ProofContext,
    stmt: Stmt,
    val assertion: Assertion,
    val substitution: List<IntArray>,
    val args: MutableList<ProofNode>
):ProofNode(
    stmt = Stmt(value = stmt.value, valueStr = stmt.valueStr + " <<<<< " + MetamathUtils.toString(assertion)),
    proofContext = proofContext
) {
    var label: String? = null
}

class PendingProofNode(
    stmt: Stmt,
    proofContext: ProofContext,
):ProofNode(stmt = stmt, proofContext = proofContext) {
    val proofs: MutableList<ProofNode> = ArrayList()
}

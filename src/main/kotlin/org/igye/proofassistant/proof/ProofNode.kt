package org.igye.proofassistant.proof

import org.igye.common.MetamathUtils
import org.igye.metamathparser.Assertion
import org.igye.metamathparser.Statement
import java.util.*
import kotlin.collections.ArrayList

abstract class ProofNode(
    val stmt: Stmt,
    val proofContext: ProofContext,
) {
    var parent: ProofNode? = null
    var state: ProofNodeState = ProofNodeState.TO_BE_PROVED
    var proofLength: Int = -1

    override fun toString(): String {
        return stmt.toString()
    }
}

class ConstProofNode(
    val src: Statement,
    stmt: Stmt,
    proofContext: ProofContext,
):ProofNode(stmt = stmt, proofContext = proofContext) {
    init {
        state = ProofNodeState.PROVED
        proofLength = 0
    }
}

class CalcProofNode(
    stmt: Stmt,
    val args: MutableList<ProofNode>,
    val substitution: List<IntArray>,
    val assertion: Assertion,
    proofContext: ProofContext,
):ProofNode(
    stmt = Stmt(value = stmt.value, valueStr = stmt.valueStr + " <<<<< " + MetamathUtils.toString(assertion)),
    proofContext = proofContext
)

class PendingProofNode(
    stmt: Stmt,
    val proofs: MutableList<ProofNode> = ArrayList(),
    proofContext: ProofContext,
): ProofNode(stmt = stmt, proofContext = proofContext)

class ValProofNode(
    stmt: Stmt,
    val label: String = UUID.randomUUID().toString(),
    var proof: ProofNode?,
    val usedBy: MutableList<ProofNode> = ArrayList(),
    proofContext: ProofContext,
):ProofNode(stmt = stmt, proofContext = proofContext)

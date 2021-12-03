package org.igye.proofassistant.proof.prooftree

import org.igye.common.MetamathUtils
import org.igye.metamathparser.Assertion
import org.igye.metamathparser.Statement
import org.igye.proofassistant.proof.ProofNodeState
import org.igye.proofassistant.proof.Stmt
import java.util.*

sealed class ProofNode(val stmt: Stmt) {
    var state: ProofNodeState = ProofNodeState.NEW
    var dist: Int = -1
    private val dependants: MutableList<ProofNode> = ArrayList()

    fun removeDependantIf(predicate: (ProofNode) -> Boolean) {
        dependants.removeIf(predicate)
    }

    fun addDependants(newDependants: Collection<ProofNode>) {
        dependants.addAll(newDependants)
    }

    fun addDependant(newDep: ProofNode) {
        dependants.add(newDep)
    }

    fun getDependants(): List<ProofNode> {
        return Collections.unmodifiableList(dependants)
    }

    override fun toString(): String {
        return stmt.toString()
    }
}

class ConstProofNode(stmt: Stmt, val src: Statement):ProofNode(stmt = stmt)

class CalcProofNode(
    stmt: Stmt,
    val assertion: Assertion,
    val substitution: List<IntArray>,
    val args: MutableList<ProofNode>
):ProofNode(
    stmt = Stmt(value = stmt.value, valueStr = stmt.valueStr + " <<<<< " + MetamathUtils.toString(assertion)),
) {
    var label: String? = null
}

class PendingProofNode(stmt: Stmt):ProofNode(stmt = stmt) {
    val proofs: MutableList<CalcProofNode> = ArrayList()
}

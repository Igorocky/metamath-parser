package org.igye.proofassistant.proof

class InstVarProofNode(
    val proofs: MutableList<ProofNode> = ArrayList(),
    val reusedBy: MutableList<RefVarProofNode> = ArrayList(),
    stmt: Stmt,
):VarProofNode(stmt = stmt)
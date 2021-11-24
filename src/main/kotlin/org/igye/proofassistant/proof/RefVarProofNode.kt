package org.igye.proofassistant.proof

class RefVarProofNode(
    var argOf: CalculatedProofNode? = null,
    val ref: InstVarProofNode,
    stmt: Stmt,
):ProofNode(stmt = stmt)
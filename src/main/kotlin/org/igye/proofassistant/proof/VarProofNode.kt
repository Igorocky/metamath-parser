package org.igye.proofassistant.proof

sealed class VarProofNode(
    var argOf: CalculatedProofNode? = null,
    stmt: Stmt,
):ProofNode(stmt = stmt)
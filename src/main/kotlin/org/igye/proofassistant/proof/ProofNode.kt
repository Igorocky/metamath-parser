package org.igye.proofassistant.proof

abstract class ProofNode(
    val stmt: Stmt,
    var proofLength: Int = -1,
    var isCanceled: Boolean = false,
)
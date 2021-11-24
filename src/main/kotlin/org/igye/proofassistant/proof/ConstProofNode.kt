package org.igye.proofassistant.proof

import org.igye.metamathparser.Statement

class ConstProofNode(
    val src: Statement,
    val provesWhat: InstVarProofNode,
    stmt: Stmt,
):ProofNode(stmt = stmt)
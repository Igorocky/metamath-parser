package org.igye.proofassistant.proof

import org.igye.metamathparser.Statement

class ConstProofNode(
    val stmt: Statement,
    val provesWhat: VarProofNode,
    valueStr: String,
):ProofNode(
    value = stmt.content,
    valueStr = valueStr,
) {
    override fun toString(): String {
        return valueStr
    }
}
package org.igye.proofassistant.proof

import org.igye.common.MetamathUtils
import org.igye.metamathparser.Assertion

class CalculatedProofNode(
    val args: List<VarProofNode>,
    val substitution: List<IntArray>,
    val assertion: Assertion,
    val result: VarProofNode
):ProofNode(value = result.value, valueStr = MetamathUtils.toString(assertion)) {
    override fun toString(): String {
        return valueStr
    }
}
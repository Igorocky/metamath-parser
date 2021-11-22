package org.igye.proofassistant.proof

class VarProofNode(
    val proofs: MutableList<ProofNode> = ArrayList(),
    var argOf: CalculatedProofNode? = null,
    value: IntArray,
    valueStr: String,
):ProofNode(value = value, valueStr = valueStr) {
    override fun toString(): String {
        return valueStr
    }
}
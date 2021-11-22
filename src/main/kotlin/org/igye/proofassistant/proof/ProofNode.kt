package org.igye.proofassistant.proof

abstract class ProofNode(
    val value: IntArray,
    val valueStr: String,
    var proofLength: Int = -1,
    var isCanceled: Boolean = false,
)
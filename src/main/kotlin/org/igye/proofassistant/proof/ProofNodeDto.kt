package org.igye.proofassistant.proof

class ProofNodeDto(
    val v: String? = null,
    val a: String? = null,
    val f: String? = null,
    val u: String? = null,
    val proofLength: Int,
    val isCanceled: Boolean,
    val proofs: List<ProofNodeDto> = emptyList(),
    val args: List<ProofNodeDto> = emptyList(),
)
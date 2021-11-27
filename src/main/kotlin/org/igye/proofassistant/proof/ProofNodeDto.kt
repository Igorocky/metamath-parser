package org.igye.proofassistant.proof

class ProofNodeDto(
    val c: String? = null,
    val a: String? = null,
    val w: String? = null,
    val hash: Int,
    val state: ProofNodeState,
    val proofs: List<ProofNodeDto> = emptyList(),
    val args: List<ProofNodeDto> = emptyList(),
)
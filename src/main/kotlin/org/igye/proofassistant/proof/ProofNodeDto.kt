package org.igye.proofassistant.proof

import com.fasterxml.jackson.annotation.JsonPropertyOrder

@JsonPropertyOrder(value = ["a", "c", "w"])
sealed class ProofNodeDto(
    val hash: Int,
    val state: ProofNodeState,
)

class CalcProofNodeDto(
    val a: String,
    hash: Int,
    state: ProofNodeState,
    val args: List<ProofNodeDto>,
): ProofNodeDto(hash = hash, state = state)

class ConstProofNodeDto(
    val c: String,
    val label: String,
    hash: Int,
    state: ProofNodeState,
): ProofNodeDto(hash = hash, state = state)

class PendingProofNodeDto(
    val w: String,
    val proofs: List<ProofNodeDto>,
    hash: Int,
    state: ProofNodeState,
): ProofNodeDto(hash = hash, state = state)
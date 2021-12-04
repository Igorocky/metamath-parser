package org.igye.proofassistant

import org.igye.proofassistant.substitutions.ConstParts

data class ProofAssistantData(
    val constParts: ConstParts,
    val matchingConstParts: ConstParts,
)

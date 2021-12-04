package org.igye.proofassistant

import org.igye.proofassistant.substitutions.ConstParts
import org.igye.proofassistant.substitutions.Substitution
import org.igye.proofassistant.substitutions.VarGroup

data class ProofAssistantData(
    val constParts: ConstParts,
    val matchingConstParts: ConstParts,
    val varGroups: MutableList<VarGroup>,
    val substitution: Substitution,
)

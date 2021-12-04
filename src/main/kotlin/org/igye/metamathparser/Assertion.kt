package org.igye.metamathparser

import org.igye.proofassistant.ProofAssistantData

data class Assertion(
    val numberOfVariables: Int,
    val hypotheses:List<Statement>,
    val statement:Statement,
    val proofData: ProofData,
    var proofAssistantData: ProofAssistantData? = null,
    val visualizationData: VisualizationData = emptyVisualizationData,
)
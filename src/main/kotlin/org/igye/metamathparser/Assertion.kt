package org.igye.metamathparser

data class Assertion(
    val numberOfVariables: Int,
    val hypotheses:List<Statement>,
    val statement:Statement,
    val proofData: ProofData,
    val visualizationData: VisualizationData = emptyVisualizationData,
)
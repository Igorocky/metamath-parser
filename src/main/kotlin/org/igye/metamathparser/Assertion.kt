package org.igye.metamathparser

data class Assertion(
    val numberOfPlaceholders: Int,
    val hypotheses:List<Statement>,
    val statement:Statement,
    val proofData: ProofData,
    val visualizationData: VisualizationData? = null,
)
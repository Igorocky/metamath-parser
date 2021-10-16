package org.igye.metamathparser

data class Assertion(
    val description: String,
    val hypotheses:List<LabeledSequenceOfSymbols>,
    val assertion:LabeledSequenceOfSymbols,
    val assertionsReferencedFromProof:List<Any>,
    val visualizationData: VisualizationData? = null,
) {
    init {
        if (!assertionsReferencedFromProof.all { it is LabeledSequenceOfSymbols || it is Assertion }) {
            throw MetamathParserException("!proofData.all { it is LabeledSequenceOfSymbols || it is Assertion }")
        }
    }
}
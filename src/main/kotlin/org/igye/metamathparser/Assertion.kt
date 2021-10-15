package org.igye.metamathparser

data class Assertion(
    val description: String,
    val context: MetamathContext,
    val hypotheses:List<LabeledSequenceOfSymbols>,
    val assertion:LabeledSequenceOfSymbols,
    val proof: StackNode? = null
)
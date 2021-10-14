package org.igye.metamathparser

data class Assertion(
    val context: MetamathContext,
    val hypotheses:List<LabeledSequenceOfSymbols>,
    val assertion:LabeledSequenceOfSymbols,
    val proof: StackNode? = null
)
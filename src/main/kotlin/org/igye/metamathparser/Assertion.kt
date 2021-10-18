package org.igye.metamathparser

data class Assertion(
    val description: String,
    val hypotheses:List<Statement>,
    val statement:Statement,
    val compressedProof:String?,
    val assertionsReferencedFromProof:List<Any>,
    val visualizationData: VisualizationData? = null,
) {
    init {
        if (!assertionsReferencedFromProof.all { it is Statement || it is Assertion }) {
            throw MetamathParserException("!assertionsReferencedFromProof.all { it is Statement || it is Assertion }")
        }
    }

    fun innerStatementToSymbols(stmt:IntArray):List<String> = stmt.map { innerNumToSymbol(it) }

    fun innerStatementToSymbols(stmt:Statement):List<String> = stmt.content.map { innerNumToSymbol(it) }

    fun innerNumToSymbol(n:Int): String =
        if (n < 0)
            visualizationData!!.symbolsMap[n]!!
        else
            visualizationData!!.symbolsMap[visualizationData.assertionVarToContextVar[n-1]]!!
}
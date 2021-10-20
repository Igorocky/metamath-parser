package org.igye.metamathparser

data class ProofData(
    val statementToProve:Statement,
    val compressedProof:String?,
    val assertionsReferencedFromProof:List<Any>,
) {
    init {
        if (!assertionsReferencedFromProof.all { it is Statement || it is Assertion }) {
            throw MetamathParserException("!assertionsReferencedFromProof.all { it is Statement || it is Assertion }")
        }
    }
}
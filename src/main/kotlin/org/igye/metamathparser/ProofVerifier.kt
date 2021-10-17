package org.igye.metamathparser

object ProofVerifier {

    fun verifyProof(theorem: Assertion): StackNode {
        val proofStack = ProofStack()
        if (theorem.assertion.sequence.uncompressedProof != null) {
            eval(
                assertionsReferencedFromProof = theorem.assertionsReferencedFromProof,
                proofStack = proofStack,
            )
        } else {
            eval(
                compressedProof = theorem.assertion.sequence.compressedProof!!,
                assertionsReferencedFromProof = theorem.assertionsReferencedFromProof,
                proofStack = proofStack,
            )
        }
        if (proofStack.size() != 1) {
            throw MetamathParserException("proofStack.size() != 1")
        }
        val result: StackNode = proofStack.getLast()
        if (theorem.assertion.sequence.symbols != result.value) {
            throw MetamathParserException("theorem.symbols != result.value")
        }
        return result
    }

    private fun eval(assertionsReferencedFromProof:List<Any>, proofStack:ProofStack) {
        for (step in assertionsReferencedFromProof) {
            if (step is LabeledSequenceOfSymbols) {
                proofStack.put(step)
            } else {
                proofStack.apply(step as Assertion)
            }
        }
    }

    private fun eval(
        compressedProof:CompressedProof,
        assertionsReferencedFromProof:List<Any>,
        proofStack:ProofStack,
    ) {
        val args = ArrayList<Any>(assertionsReferencedFromProof)
        val proof: List<String> = splitEncodedProof(compressedProof.proof)

        for (step in proof) {
            if ("Z".equals(step)) {
                args.add(proofStack.getLast())
            } else {
                val arg = args[strToInt(step)-1]
                if (arg is StackNode) {
                    proofStack.put(arg)
                } else if (arg is LabeledSequenceOfSymbols) {
                    proofStack.put(arg)
                } else {
                    proofStack.apply(arg as Assertion)
                }
            }
        }
    }

    fun splitEncodedProof(proofStr: String): List<String> {
        val result = ArrayList<String>()
        val sb = StringBuilder()
        for (i in 0 until proofStr.length) {
            val c = proofStr[i]
            if (c == 'Z') {
                result.add("Z")
            } else {
                sb.append(c)
                if ('A' <= c && c <= 'T') {
                    result.add(sb.toString())
                    sb.setLength(0)
                }
            }
        }
        if (sb.length > 0) {
            throw MetamathParserException("sb.length() > 0")
        }
        return result
    }

    fun strToInt(str: String): Int {
        var result = 0
        var base = 1
        for (i in str.length - 2 downTo 0) {
            result += base * charToInt(str[i])
            base *= 5
        }
        return result * 20 + charToInt(str[str.length - 1])
    }

    private fun charToInt(c: Char): Int {
        return if ('A' <= c && c <= 'T') {
            c.code - 64 //65 is A
        } else {
            c.code - 84 //85 is U
        }
    }
}
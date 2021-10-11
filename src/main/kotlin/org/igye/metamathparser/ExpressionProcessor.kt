package org.igye.metamathparser

object ExpressionProcessor: ((MetamathContext,Expression) -> Unit) {
    override fun invoke(ctx: MetamathContext, expr: Expression) {
        when (expr) {
            is SequenceOfSymbols -> when (expr.seqType) {
                'c' -> ctx.addConstants(expr.symbols.toSet())
                'v' -> ctx.addVariables(expr.symbols.toSet())
                'd' -> ctx
                else -> throw MetamathParserException()
            }
            is LabeledSequenceOfSymbols -> when (expr.sequence.seqType) {
                'f' -> ctx.addHypothesis(expr.label, expr)
                'e' -> ctx.addHypothesis(expr.label, expr)
                'a' -> ctx.addAssertion(expr.label, createAssertion(ctx, expr))
                'p' -> processTheorem(ctx, expr)
                else -> throw MetamathParserException()
            }
            else -> throw MetamathParserException()
        }
    }

    private fun processTheorem(ctx: MetamathContext, expr: LabeledSequenceOfSymbols) {
        val theorem = createAssertion(ctx, expr)
        verify(theorem, ctx)
        ctx.addAssertion(expr.label, theorem)
    }

    private fun createAssertion(ctx: MetamathContext, expr: LabeledSequenceOfSymbols): Assertion {
        val variables = getAllVariablesUsed(ctx, expr.sequence)
        val hypotheses = ctx.getHypotheses {
            when (it.sequence.seqType) {
                'f' -> variables.contains(it.sequence.symbols[1])
                'e' -> true
                else -> throw MetamathParserException("Unexpected type of a hypothesis: ${it.sequence.seqType}")
            }
        }.sortedBy { it.beginIdx }
        return Assertion(hypotheses = hypotheses, assertion = expr)
    }

    private fun getAllVariablesUsed(ctx: MetamathContext, expr: SequenceOfSymbols):Set<String> {
        val result = HashSet<String>()
        expr.symbols.asSequence().filter { ctx.variableExists(it) }.forEach { result.add(it) }
        ctx.getHypotheses { it.sequence.seqType == 'e' }.forEach { essential ->
            essential.sequence.symbols.asSequence()
                .filter { ctx.variableExists(it) }
                .forEach { result.add(it) }
        }
        return result
    }

    private fun verify(theorem: Assertion, ctx: MetamathContext): StackNode {
        val proofStack = ProofStack()
        if (theorem.assertion.sequence.uncompressedProof != null) {
            eval(theorem.assertion.sequence.uncompressedProof, proofStack, ctx)
        } else {
            eval(
                compressedProof = theorem.assertion.sequence.compressedProof!!,
                mandatoryHypotheses = theorem.hypotheses.map { it.sequence },
                proofStack = proofStack,
                ctx = ctx
            )
        }
        if (proofStack.size() != 1) {
            throw MetamathParserException("proofStack.size() != 1")
        }
        val result: StackNode = proofStack.get(0)
        if (theorem.assertion.sequence.symbols != result.value) {
            throw MetamathParserException("theorem.symbols != result.value")
        }
        return result
    }

    private fun eval(uncompressedProof:List<String>, proofStack:ProofStack, ctx: MetamathContext) {
        for (label in uncompressedProof) {
            apply(label, proofStack, ctx)
        }
    }

    private fun eval(
        compressedProof:CompressedProof,
        mandatoryHypotheses:List<SequenceOfSymbols>,
        proofStack:ProofStack,
        ctx: MetamathContext
    ) {
        val args = ArrayList<Any>(mandatoryHypotheses)
        compressedProof.labels.forEach { args.add(ctx.getHypothesis(it)?.sequence?:ctx.getAssertions()[it]!!) }
        val proof: List<String> = splitEncodedProof(compressedProof.proof)

        for (step in proof) {
            if ("Z".equals(step)) {
                args.add(proofStack.get(proofStack.size()-1))
            } else {
                val arg = args[strToInt(step)-1]
                if (arg is StackNode) {
                    proofStack.add(arg)
                } else if (arg is SequenceOfSymbols) {
                    proofStack.apply(arg)
                } else {
                    proofStack.apply((arg as Assertion))
                }
            }
        }
    }

    private fun apply(label:String, proofStack:ProofStack, ctx: MetamathContext) {
        val hypothesis: LabeledSequenceOfSymbols? = ctx.getHypothesis(label)
        if (hypothesis != null) {
            proofStack.apply(hypothesis.sequence)
        } else {
            proofStack.apply(ctx.getAssertions()[label]!!)
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
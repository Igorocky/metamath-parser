package org.igye.metamathparser

object ExpressionProcessor: ((MetamathContext,Expression) -> MetamathContext) {
    override fun invoke(ctx: MetamathContext, expr: Expression): MetamathContext {
        return when (expr) {
            is SequenceOfSymbols -> when (expr.seqType) {
                'c' -> ctx.addConstants(expr.symbols)
                'v' -> ctx.addVariables(expr.symbols)
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

    private fun processTheorem(ctx: MetamathContext, expr: LabeledSequenceOfSymbols): MetamathContext {
        verify(expr.sequence, ctx)
        return ctx.addAssertion(expr.label, createAssertion(ctx, expr))
    }

    private fun createAssertion(ctx: MetamathContext, expr: LabeledSequenceOfSymbols): Assertion {
        val variables = getAllVariablesUsed(ctx, expr.sequence);
        val hypotheses = ctx.hypotheses.values.asSequence()
            .filter {
                when (it.sequence.seqType) {
                    'f' -> variables.contains(it.sequence.symbols[1])
                    'e' -> true
                    else -> throw MetamathParserException("Unexpected type of a hypothesis: ${it.sequence.seqType}")
                }
            }
            .sortedBy { it.beginIdx }
            .toList()
        return Assertion(hypotheses = hypotheses, assertion = expr)
    }

    private fun getAllVariablesUsed(ctx: MetamathContext, expr: SequenceOfSymbols):Set<String> {
        val result = HashSet<String>()
        expr.symbols.asSequence().filter { ctx.variables.contains(it) }.forEach { result.add(it) }
        ctx.hypotheses.values.asSequence().filter { it.sequence.seqType == 'e' }.forEach { essential ->
            essential.sequence.symbols.asSequence()
                .filter { ctx.variables.contains(it) }
                .forEach { result.add(it) }
        }
        return result
    }

    private fun verify(theorem: SequenceOfSymbols, ctx: MetamathContext): StackNode {
        val proofStack = ProofStack()
        for (label in theorem.proof!!) {
            val hypothesis = ctx.hypotheses[label]
            if (hypothesis != null) {
                proofStack.apply(hypothesis.sequence)
            } else {
                proofStack.apply(ctx.assertions[label]!!)
            }
        }
        if (proofStack.size() != 1) {
            throw MetamathParserException("proofStack.size() != 1")
        }
        val result: StackNode = proofStack.get(0)
        if (theorem.symbols != result.value) {
            throw MetamathParserException("theorem.symbols != result.value")
        }
        return result
    }
}
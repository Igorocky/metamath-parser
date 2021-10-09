package org.igye.metamathparser

class ExpressionProcessor: ((MetamathContext,Expression) -> MetamathContext) {
    override fun invoke(ctx: MetamathContext, expr: Expression): MetamathContext {
        return when (expr) {
            is SequenceOfSymbols -> when(expr.seqType) {
                'c' -> processConstants(ctx, expr)
                'v' -> processVariables(ctx, expr)
                else -> throw MetamathParserException()
            }
            is LabeledSequenceOfSymbols -> when(expr.sequence.seqType) {
                'f' -> processFloating(ctx, expr)
                'e' -> processEssential(ctx, expr)
                'a' -> processAxiom(ctx, expr)
                'p' -> processTheorem(ctx, expr)
                else -> throw MetamathParserException()
            }
            else -> throw MetamathParserException()
        }
    }

    private fun processConstants(ctx: MetamathContext, expr: SequenceOfSymbols): MetamathContext {

    }

    private fun processVariables(ctx: MetamathContext, expr: SequenceOfSymbols): MetamathContext {

    }

    private fun processFloating(ctx: MetamathContext, expr: LabeledSequenceOfSymbols): MetamathContext {

    }

    private fun processEssential(ctx: MetamathContext, expr: LabeledSequenceOfSymbols): MetamathContext {

    }

    private fun processAxiom(ctx: MetamathContext, expr: LabeledSequenceOfSymbols): MetamathContext {

    }

    private fun processTheorem(ctx: MetamathContext, expr: LabeledSequenceOfSymbols): MetamathContext {

    }
}
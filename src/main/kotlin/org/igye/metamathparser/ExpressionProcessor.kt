package org.igye.metamathparser

object ExpressionProcessor: ((MetamathContext,Expression) -> Unit) {
    override fun invoke(ctx: MetamathContext, expr: Expression) {
        when (expr) {
            is SequenceOfSymbols -> when (expr.seqType) {
                'c' -> ctx.addConstants(expr.symbols.toSet())
                'v' -> processVariableStmt(ctx, expr)
                'd' -> ctx
                else -> throw MetamathParserException()
            }
            is LabeledSequenceOfSymbols -> when (expr.sequence.seqType) {
                'f' -> processFloatingStmt(ctx, expr)
                'e' -> ctx.addHypothesis(expr.label, expr)
                'a', 'p' -> ctx.addAssertion(expr.label, createAssertion(ctx, expr))
                else -> throw MetamathParserException()
            }
            else -> throw MetamathParserException()
        }
    }

    private fun processVariableStmt(ctx: MetamathContext, expr: SequenceOfSymbols) {
        if (expr.symbols.any { ctx.isConstant(it) }) {
            throw MetamathParserException("expr.symbols.any { ctx.isConstant(it) }")
        }
        ctx.addVariables(expr.symbols.toSet())
    }

    private fun processFloatingStmt(ctx: MetamathContext, expr: LabeledSequenceOfSymbols) {
        val symbols = expr.sequence.symbols
        if (symbols.size != 2) {
            throw MetamathParserException("expr.sequence.symbols.size != 2")
        }
        if (!( ctx.isConstant(symbols[0]) && !ctx.isConstant(symbols[1]) )) {
            throw MetamathParserException("!( ctx.isConstant(symbols[0]) && !ctx.isConstant(symbols[1]) )")
        }
        ctx.addHypothesis(expr.label, expr)
    }

    private fun createAssertion(ctx: MetamathContext, expr: LabeledSequenceOfSymbols): Assertion {
        val variables = getAllVariablesFromExprAndAllEssentialHypotheses(ctx, expr.sequence)
        val variablesTypes = HashMap<String,String>()
        val hypotheses: List<LabeledSequenceOfSymbols> = ctx.getHypotheses {
            when (it.sequence.seqType) {
                'f' -> {
                    val varName = it.sequence.symbols[1]
                    if (variables.contains(varName)) {
                        if (variablesTypes.containsKey(varName)) {
                            throw MetamathParserException("variablesTypes.containsKey(varName)")
                        } else {
                            variablesTypes[varName] = it.sequence.symbols[0]
                            true
                        }
                    } else {
                        false
                    }
                }
                'e' -> true
                else -> throw MetamathParserException("Unexpected type of a hypothesis: ${it.sequence.seqType}")
            }
        }.sortedBy { it.beginIdx }
        if (variablesTypes.keys != variables) {
            throw MetamathParserException("types.keys != variables")
        }
        val assertionsReferencedFromProof: List<Any> = getAssertionsReferencedFromProof(
            ctx = ctx,
            mandatoryHypotheses = hypotheses,
            uncompressedProof = expr.sequence.uncompressedProof,
            compressedProof = expr.sequence.compressedProof,
        )
        addVarTypes(variablesTypes, assertionsReferencedFromProof)
        return Assertion(
            description = ctx.lastComment?.trim()?:"",
            hypotheses = hypotheses,
            assertion = expr,
            assertionsReferencedFromProof = assertionsReferencedFromProof,
            visualizationData = VisualizationData(
                variablesTypes = variablesTypes
            )
        )
    }

    private fun addVarTypes(variablesTypes: MutableMap<String,String>, assertionsReferencedFromProof: List<Any>) {
        for (assertion in assertionsReferencedFromProof) {
            if (assertion is LabeledSequenceOfSymbols && assertion.sequence.seqType == 'f') {
                val type = assertion.sequence.symbols[0]
                val varName = assertion.sequence.symbols[1]
                if (variablesTypes.containsKey(varName) && variablesTypes[varName] != type) {
                    throw MetamathParserException("variablesTypes.containsKey(varName) && variablesTypes[varName] != type")
                }
                variablesTypes[varName] = type
            }
        }
    }

    private fun getAssertionsReferencedFromProof(
        ctx: MetamathContext,
        mandatoryHypotheses: List<LabeledSequenceOfSymbols>,
        uncompressedProof:List<String>?,
        compressedProof:CompressedProof?
    ): List<Any> {
        if (compressedProof != null) {
            val dataReferencedFromProof = ArrayList<Any>(mandatoryHypotheses)
            compressedProof.labels.forEach { dataReferencedFromProof.add(ctx.getHypothesis(it)?:ctx.getAssertions()[it]!!) }
            return dataReferencedFromProof
        } else if (uncompressedProof != null) {
            return uncompressedProof.map { ctx.getHypothesis(it)?:ctx.getAssertions()[it]!! }
        } else {
            return emptyList()
        }
    }

    private fun getAllVariablesFromExprAndAllEssentialHypotheses(ctx: MetamathContext, expr: SequenceOfSymbols):Set<String> {
        val variables = HashSet<String>()
        for (symbol in expr.symbols) {
            if (!ctx.isConstant(symbol)) {
                variables.add(symbol)
            }
        }
        ctx.getHypotheses { it.sequence.seqType == 'e' }.forEach {
            for (symbol in it.sequence.symbols) {
                if (!ctx.isConstant(symbol)) {
                    variables.add(symbol)
                }
            }
        }
        return variables
    }

}
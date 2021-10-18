package org.igye.metamathparser

object ExpressionProcessor: ((MetamathContext,Expression) -> Unit) {
    override fun invoke(ctx: MetamathContext, expr: Expression) {
        when (expr) {
            is SequenceOfSymbols -> when (expr.seqType) {
                'c' -> processConstantStmt(ctx, expr)
                'v' -> processVariableStmt(ctx, expr)
                'd' -> ctx
                else -> throw MetamathParserException()
            }
            is LabeledSequenceOfSymbols -> when (expr.sequence.seqType) {
                'f' -> processFloatingStmt(ctx, expr)
                'e' -> processEssentialStmt(ctx, expr)
                'a', 'p' -> ctx.addAssertion(expr.label, createAssertion(ctx, expr))
                else -> throw MetamathParserException()
            }
            else -> throw MetamathParserException()
        }
    }

    private fun processConstantStmt(ctx: MetamathContext, expr: SequenceOfSymbols) {
        ctx.addConstants(expr.symbols.toSet())
    }

    private fun processVariableStmt(ctx: MetamathContext, expr: SequenceOfSymbols) {
        ctx.addVariables(expr.symbols.toSet())
    }

    private fun processFloatingStmt(ctx: MetamathContext, expr: LabeledSequenceOfSymbols) {
        val symbols = expr.sequence.symbols
        if (symbols.size != 2) {
            throw MetamathParserException("expr.sequence.symbols.size != 2")
        }
        val stmt = Statement(
            beginIdx = expr.beginIdx,
            label = expr.label,
            type = expr.sequence.seqType,
            content = symbolsToNumbers(ctx, expr.sequence.symbols)
        )
        if (!( stmt.content[0] < 0 && stmt.content[1] > 0 )) {
            throw MetamathParserException("!( stmt.content[0] < 0 && stmt.content[1] > 0 )")
        }
        ctx.addHypothesis(expr.label, stmt)
    }

    private fun processEssentialStmt(ctx: MetamathContext, expr: LabeledSequenceOfSymbols) {
        ctx.addHypothesis(
            expr.label,
            Statement(
                beginIdx = expr.beginIdx,
                label = expr.label,
                type = expr.sequence.seqType,
                content = symbolsToNumbers(ctx, expr.sequence.symbols)
            )
        )
    }

    private fun createAssertion(ctx: MetamathContext, expr: LabeledSequenceOfSymbols): Assertion {
        val essentialHypotheses: List<Statement> = ctx.getHypotheses { it.type == 'e'}
        val assertionStatement = Statement(
            beginIdx = expr.beginIdx,
            label = expr.label,
            type = expr.sequence.seqType,
            content = symbolsToNumbers(ctx, expr.sequence.symbols)
        )
        val variables = getAllVariablesFromExprAndAllEssentialHypotheses(essentialHypotheses, assertionStatement)
        val variablesTypes = HashMap<Int,Int>()
        val floatingHypotheses = ctx.getHypotheses {
            when (it.type) {
                'f' -> {
                    val varNum = it.content[1]
                    if (variables.contains(varNum)) {
                        if (variablesTypes.containsKey(varNum)) {
                            throw MetamathParserException("variablesTypes.containsKey(varName)")
                        } else {
                            variablesTypes[varNum] = it.content[0]
                            true
                        }
                    } else {
                        false
                    }
                }
                else -> false
            }
        }
        if (variablesTypes.keys != variables) {
            throw MetamathParserException("types.keys != variables")
        }
        val mandatoryHypotheses: List<Statement> = (floatingHypotheses + essentialHypotheses).sortedBy { it.beginIdx }
        val assertionVarToContextVar = IntArray(floatingHypotheses.size + 1)
        val contextVarToAssertionVar = HashMap<Int,Int>()
        for (i in 0 until mandatoryHypotheses.size) {
            val hypothesis = mandatoryHypotheses[i]
            if (hypothesis.type == 'f') {
                assertionVarToContextVar[i+1] = hypothesis.content[1]
                contextVarToAssertionVar[hypothesis.content[1]] = i+1
            }
        }
        val assertionsReferencedFromProof: List<Any> = getAssertionsReferencedFromProof(
            ctx = ctx,
            mandatoryHypotheses = mandatoryHypotheses,
            uncompressedProof = expr.sequence.uncompressedProof,
            compressedProof = expr.sequence.compressedProof,
        )
        addVarTypes(variablesTypes, assertionsReferencedFromProof)
        val (
            mandatoryHypothesesInner: List<Statement>,
            assertionStatementInner: Statement,
            allNums: Set<Int>
        ) = collectAllSymbols(
            mandatoryHypotheses, assertionStatement, assertionsReferencedFromProof, contextVarToAssertionVar
        )
        return Assertion(
            description = ctx.lastComment?.trim()?:"",
            hypotheses = mandatoryHypothesesInner,
            statement = assertionStatementInner,
            assertionsReferencedFromProof = assertionsReferencedFromProof,
            compressedProof = expr.sequence.compressedProof?.proof,
            visualizationData = VisualizationData(
                variablesTypes = variablesTypes,
                contextVarToAssertionVar = contextVarToAssertionVar,
                assertionVarToContextVar = assertionVarToContextVar,
                symbolsMap = allNums.associate { it to ctx.getSymbolByNumber(it) }
            )
        )
    }

    private fun collectAllSymbols(
        mandatoryHypotheses: List<Statement>,
        assertionStatement: Statement,
        assertionsReferencedFromProof: List<Any>,
        contextVarToAssertionVar: Map<Int,Int>
    ): Triple<List<Statement>, Statement, Set<Int>> {
        val allNums = HashSet<Int>()
        val mandatoryHypothesesInner = ArrayList<Statement>(mandatoryHypotheses.size)
        for (mandatoryHypothesis in mandatoryHypotheses) {
            mandatoryHypothesesInner.add(mandatoryHypothesis.copy(
                content = createContentInner(
                    content = mandatoryHypothesis.content,
                    contextVarToAssertionVar = contextVarToAssertionVar,
                    allNums = allNums
                )
            ))
        }
        val assertionStatementInner: Statement = assertionStatement.copy(
            content = createContentInner(
                content = assertionStatement.content,
                contextVarToAssertionVar = contextVarToAssertionVar,
                allNums = allNums
            )
        )
        for (assertion in assertionsReferencedFromProof) {
            if (assertion is Statement && assertion.type == 'f') {
                allNums.add(assertion.content[0])
                allNums.add(assertion.content[1])
            }
        }
        return Triple(mandatoryHypothesesInner, assertionStatementInner, allNums)
    }

    fun createContentInner(content: IntArray, contextVarToAssertionVar: Map<Int,Int>, allNums: MutableSet<Int>): IntArray {
        val contentInner = IntArray(content.size)
        for (i in 0 until content.size) {
            val num = content[i]
            allNums.add(num)
            if (num < 0) {
                contentInner[i] = num
            } else {
                contentInner[i] = contextVarToAssertionVar[num]!!
            }
        }
        return contentInner
    }

    private fun putAssertionVars(stmt: IntArray, contextVarToAssertionVar: Map<Int,Int>): IntArray {
        val result = IntArray(stmt.size)
        for (i in 0 until stmt.size) {
            if (stmt[i] < 0) {
                result[i] = stmt[i]
            } else {
                result[i] = contextVarToAssertionVar[stmt[i]]!!
            }
        }
        return result
    }

    private fun addVarTypes(variablesTypes: MutableMap<Int,Int>, assertionsReferencedFromProof: List<Any>) {
        for (assertion in assertionsReferencedFromProof) {
            if (assertion is Statement && assertion.type == 'f') {
                if (variablesTypes.containsKey(assertion.content[1]) && variablesTypes[assertion.content[1]] != assertion.content[0]) {
                    throw MetamathParserException("variablesTypes.containsKey(varName) && variablesTypes[varName] != type")
                }
                variablesTypes[assertion.content[1]] = assertion.content[0]
            }
        }
    }

    private fun getAssertionsReferencedFromProof(
        ctx: MetamathContext,
        mandatoryHypotheses: List<Statement>,
        uncompressedProof:List<String>?,
        compressedProof:CompressedProof?
    ): List<Any> {
        if (compressedProof != null) {
            val result = ArrayList<Any>(mandatoryHypotheses)
            compressedProof.labels.forEach { result.add(ctx.getHypothesis(it)?:ctx.getAssertions()[it]!!) }
            return result
        } else if (uncompressedProof != null) {
            return uncompressedProof.map { ctx.getHypothesis(it)?:ctx.getAssertions()[it]!! }
        } else {
            return emptyList()
        }
    }

    private fun getAllVariablesFromExprAndAllEssentialHypotheses(
        essentialHypotheses: List<Statement>,
        assertionStatement: Statement
    ):Set<Int> {
        val variables = HashSet<Int>()
        for (hypothesis in essentialHypotheses) {
            for (i in hypothesis.content) {
                if (i > 0) {
                    variables.add(i)
                }
            }
        }
        for (i in assertionStatement.content) {
            if (i > 0) {
                variables.add(i)
            }
        }
        return variables
    }



    private fun symbolsToNumbers(ctx: MetamathContext, symbols: List<String>): IntArray {
        val content = IntArray(symbols.size)
        for (i in 0 until content.size) {
            content[i] = ctx.getNumberBySymbol(symbols[i])
        }
        return content
    }
}
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
        if (!( stmt.content[0] < 0 && stmt.content[1] >= 0 )) {
            throw MetamathParserException("!( stmt.content[0] < 0 && stmt.content[1] >= 0 )")
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
        val variables = collectAllVariables(essentialHypotheses, assertionStatement)
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
        val assertionsReferencedFromProof: List<Any> = getAssertionsReferencedFromProof(
            ctx = ctx,
            mandatoryHypotheses = mandatoryHypotheses,
            uncompressedProof = expr.sequence.uncompressedProof,
            compressedProof = expr.sequence.compressedProof,
        )
        addVarTypes(variablesTypes, assertionsReferencedFromProof)
        return renumberVariables(Assertion(
            hypotheses = mandatoryHypotheses,
            statement = assertionStatement,
            proofData = ProofData(
                statementToProve = assertionStatement,
                compressedProof = expr.sequence.compressedProof?.proof,
                assertionsReferencedFromProof = assertionsReferencedFromProof,
            ),
            numberOfPlaceholders = variables.size,
            visualizationData = VisualizationData(
                description = ctx.lastComment?.trim()?:"",
                variablesTypes = variablesTypes.asSequence().associate { ctx.getSymbolByNumber(it.key) to ctx.getSymbolByNumber(it.value) },
                symbolsMap = createNumToSymbolMap(mandatoryHypotheses, assertionStatement, assertionsReferencedFromProof, ctx)
            )
        ))
    }

    private fun renumberVariables(
        symbolsMap: Map<Int,String>,
        contextVarToAssertionVar: Map<Int,Int>,
        numberOfLocalVariables: Int
    ):Map<Int,String> {
        return symbolsMap.mapKeys { if (it.key < 0 || it.key >= numberOfLocalVariables) it.key else contextVarToAssertionVar[it.key]!! }
    }

    private fun renumberVariables(statement: Statement, contextVarToAssertionVar: HashMap<Int,Int>):Statement {
        return statement.copy(content = renumberVariables(statement.content, contextVarToAssertionVar))
    }
    private fun renumberVariables(assertion: Assertion):Assertion {
        val assertionVarToContextVar = IntArray(assertion.numberOfPlaceholders)
        val contextVarToAssertionVar = HashMap<Int,Int>()
        var assertionVarNum = 0
        for (hypothesis in assertion.hypotheses) {
            if (hypothesis.type == 'f') {
                assertionVarToContextVar[assertionVarNum] = hypothesis.content[1]
                contextVarToAssertionVar[hypothesis.content[1]] = assertionVarNum
                assertionVarNum++
            }
        }
        return assertion.copy(
            hypotheses = assertion.hypotheses.map { renumberVariables(it, contextVarToAssertionVar) },
            statement = renumberVariables(assertion.statement, contextVarToAssertionVar),
            visualizationData = assertion.visualizationData?.copy(
                symbolsMap = renumberVariables(
                    symbolsMap = assertion.visualizationData.symbolsMap,
                    contextVarToAssertionVar = contextVarToAssertionVar,
                    numberOfLocalVariables = assertion.numberOfPlaceholders
                )
            )
        )
    }

    private fun createNumToSymbolMap(
        mandatoryHypotheses: List<Statement>,
        assertionStatement: Statement,
        assertionsReferencedFromProof: List<Any>,
        ctx: MetamathContext
    ): Map<Int, String> {
        val allNums = HashSet<Int>()
        for (mandatoryHypothesis in mandatoryHypotheses) {
            mandatoryHypothesis.content.forEach { allNums.add(it) }
        }
        assertionStatement.content.forEach { allNums.add(it) }
        for (assertion in assertionsReferencedFromProof) {
            if (assertion is Statement && assertion.type == 'f') {
                allNums.add(assertion.content[0])
                allNums.add(assertion.content[1])
            }
        }
        return allNums.associate { it to ctx.getSymbolByNumber(it) }
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

    private fun renumberVariables(stmt: IntArray, contextVarToAssertionVar: Map<Int,Int>): IntArray {
        val result = IntArray(stmt.size)
        for (i in 0 until stmt.size) {
            val num = stmt[i]
            if (num < 0) {
                result[i] = num
            } else {
                result[i] = contextVarToAssertionVar[num]!!
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

    private fun collectAllVariables(essentialHypotheses: List<Statement>, assertionStatement: Statement):Set<Int> {
        val variables = HashSet<Int>()
        for (hypothesis in essentialHypotheses) {
            for (i in hypothesis.content) {
                if (i >= 0) {
                    variables.add(i)
                }
            }
        }
        for (i in assertionStatement.content) {
            if (i >= 0) {
                variables.add(i)
            }
        }
        return variables
    }

    private fun symbolsToNumbers(ctx: MetamathContext, symbols: List<String>): IntArray {
        val result = IntArray(symbols.size)
        for (i in 0 until result.size) {
            result[i] = ctx.getNumberBySymbol(symbols[i])
        }
        return result
    }
}
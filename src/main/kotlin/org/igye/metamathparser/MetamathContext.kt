package org.igye.metamathparser

class MetamathContext(
    private val parent:MetamathContext? = null,
    var lastComment: String? = null
) {
    private var rootContext:MetamathContext? = null

    private val constants:MutableList<String>? = if (parent == null) ArrayList() else null
    private val constantSymbolToNumber:MutableMap<String,Int>? = if (parent == null) HashMap() else null

    private var variables:MutableList<String>? = null
    private var variableSymbolToNumber:MutableMap<String,Int>? = null

    private var hypotheses:MutableMap<String,Statement>? = null
    private val assertions:MutableMap<String,Assertion>? = if (parent == null) HashMap() else null

    fun createChildContext(): MetamathContext {
        return MetamathContext(parent = this)
    }

    fun getRootContext(): MetamathContext {
        if (rootContext == null) {
            rootContext = parent?.getRootContext() ?: this
        }
        return rootContext!!
    }

    private var assertionsCache:MutableMap<String,Assertion>? = null
    fun getAssertions(): MutableMap<String, Assertion> {
        if (assertionsCache == null) {
            assertionsCache = if (parent == null) {
                assertions!!
            } else {
                parent.getAssertions()
            }
        }
        return assertionsCache!!
    }

    fun getHypothesis(name:String):Statement? {
        return hypotheses?.get(name)?:parent?.getHypothesis(name)
    }

    fun getHypotheses(filter:(Statement) -> Boolean):List<Statement> {
        val result = ArrayList<Statement>()
        var ctx: MetamathContext? = this
        while (ctx != null) {
            if (ctx.hypotheses != null) {
                for (hypothesis in ctx.hypotheses!!.values) {
                    if (filter(hypothesis)) {
                        result.add(hypothesis)
                    }
                }
            }
            ctx = ctx.parent
        }
        return result
    }

    fun addConstants(constants:Set<String>) {
        if (parent != null) {
            throw MetamathParserException("Constant declaration is allowed only in the outermost block.")
        }
        for (symbol in constants) {
            if (getConstantSymbolNumber(symbol) != null || getVariableSymbolNumber(symbol) != null) {
                throw MetamathParserException("Cannot redeclare symbol '$symbol'")
            }
            this.constants!!.add(symbol)
            constantSymbolToNumber!![symbol] = -this.constants.size
        }
    }

    fun addVariables(variables:Set<String>) {
        if (this.variables == null) {
            this.variables = ArrayList()
            variableSymbolToNumber = HashMap()
        }
        for (symbol in variables) {
            if (getConstantSymbolNumber(symbol) != null || getVariableSymbolNumber(symbol) != null) {
                throw MetamathParserException("Cannot redeclare symbol '$symbol'")
            }
            this.variables!!.add(symbol)
            variableSymbolToNumber!![symbol] = getNumberOfVariables()
        }
    }

    fun getNumberBySymbol(symbol: String): Int =
        getConstantSymbolNumber(symbol)?:getVariableSymbolNumber(symbol)
            ?:throw MetamathParserException("Could not find symbol with name $symbol")

    private fun getConstantSymbolNumber(symbol: String): Int? = getRootContext().constantSymbolToNumber?.get(symbol)

    private fun getVariableSymbolNumber(symbol: String): Int? =
        variableSymbolToNumber?.get(symbol)?:parent?.getVariableSymbolNumber(symbol)

    fun getSymbolByNumber(n:Int): String {
        if (n < 0) {
            return getRootContext().constants?.get(-n - 1) ?: throw MetamathParserException("A constant with number $n was not registered.")
        } else {
            return variables?.get(n - 1) ?: parent?.getSymbolByNumber(n) ?: throw MetamathParserException("A variable with number $n was not registered.")
        }
    }

    private fun getNumberOfVariables(): Int {
        return variables?.size?:0 + (parent?.getNumberOfVariables()?:0)
    }

    // TODO: 10/18/2021 get name from stmt.label
    fun addHypothesis(name:String, stmt:Statement) {
        if (this.hypotheses == null) {
            this.hypotheses = HashMap()
        }
        this.hypotheses!![name] = stmt
    }

    fun addAssertion(name:String, expr:Assertion) {
        getAssertions()[name] = expr
    }
}
package org.igye.metamathparser

import org.igye.common.ContinueInstr

class MetamathContext(
    private val parent:MetamathContext? = null,
):MetamathContextI<MetamathContext> {
    private var rootContext:MetamathContext? = null

    private val constants:MutableList<String>? = if (parent == null) ArrayList() else null
    private val constantToNumber:MutableMap<String,Int>? = if (parent == null) HashMap() else null

    private var variables:MutableList<String>? = null
    private var variableToNumber:MutableMap<String,Int>? = null

    private var hypotheses:MutableMap<String,Statement>? = null
    private val assertions:MutableMap<String,Assertion>? = if (parent == null) HashMap() else null

    private var lastCommentP: String? = null

    val parentheses: MetamathParentheses by lazy {
        MetamathParentheses(
            roundBracketOpen = getNumberBySymbol("("),
            roundBracketClose = getNumberBySymbol(")"),
            curlyBracketOpen = getNumberBySymbol("{"),
            curlyBracketClose = getNumberBySymbol("}"),
            squareBracketOpen = getNumberBySymbol("["),
            squareBracketClose = getNumberBySymbol("]"),
        )
    }

    override fun createChildContext(): MetamathContext {
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

    fun iterateHypotheses(consumer: (Statement) -> ContinueInstr) {
        var ctx: MetamathContext? = this
        while (ctx != null) {
            if (ctx.hypotheses != null) {
                for (hyp in ctx.hypotheses!!.values) {
                    if (consumer(hyp) == ContinueInstr.STOP) {
                        return
                    }
                }
            }
            ctx = ctx.parent
        }
    }

    fun getHypotheses(filter:(Statement) -> Boolean):List<Statement> {
        val result = ArrayList<Statement>()
        iterateHypotheses {
            if (filter(it)) {
                result.add(it)
            }
            ContinueInstr.CONTINUE
        }
        return result
    }

    fun addConstants(constants:Set<String>) {
        if (parent != null) {
            throw MetamathParserException("Constant declaration is allowed only in the outermost block.")
        }
        for (symbol in constants) {
            if (getConstantNumber(symbol) != null || getVariableNumber(symbol) != null) {
                throw MetamathParserException("Cannot redeclare symbol '$symbol'")
            }
            this.constants!!.add(symbol)
            constantToNumber!![symbol] = -this.constants.size
        }
    }

    fun addVariables(variables:Set<String>) {
        if (this.variables == null) {
            this.variables = ArrayList()
            variableToNumber = HashMap()
        }
        for (symbol in variables) {
            if (getConstantNumber(symbol) != null || getVariableNumber(symbol) != null) {
                throw MetamathParserException("Cannot redeclare symbol '$symbol'")
            }
            variableToNumber!![symbol] = getNumberOfVariables()
            this.variables!!.add(symbol)
        }
    }

    fun getNumberBySymbol(symbol: String): Int =
        getConstantNumber(symbol)?:getVariableNumber(symbol)
            ?:throw MetamathParserException("Could not find symbol with name $symbol")

    private fun getConstantNumber(symbol: String): Int? = getRootContext().constantToNumber?.get(symbol)

    private fun getVariableNumber(symbol: String): Int? = variableToNumber?.get(symbol)?:parent?.getVariableNumber(symbol)

    fun getSymbolByNumber(n:Int): String {
        if (n < 0) {
            return getRootContext().constants?.get(-n - 1) ?: throw MetamathParserException("A constant with number $n was not registered.")
        } else {
            val varIdx = n - (parent?.getNumberOfVariables()?:0)
            return if (0 <= varIdx && varIdx < variables!!.size) {
                variables!![varIdx]
            } else {
                parent?.getSymbolByNumber(n) ?: throw MetamathParserException("A variable with number $n was not registered.")
            }
        }
    }

    private fun getNumberOfVariables(): Int {
        return (variables?.size?:0) + (parent?.getNumberOfVariables()?:0)
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

    fun getLastComment() = lastCommentP

    override fun setLastComment(str: String?) {
        lastCommentP = str
    }
}
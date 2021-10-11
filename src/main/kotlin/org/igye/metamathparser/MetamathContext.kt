package org.igye.metamathparser

class MetamathContext(
    private val parent:MetamathContext? = null
) {
    private val constants:MutableSet<String>? = if (parent == null) HashSet() else null
    private var variables:MutableSet<String>? = null
    private var hypotheses:MutableMap<String,LabeledSequenceOfSymbols>? = null
    private val assertions:MutableMap<String,Assertion>? = if (parent == null) HashMap() else null

    fun createChildContext(): MetamathContext {
        return MetamathContext(parent = this)
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

    fun getHypothesis(name:String):LabeledSequenceOfSymbols? {
        return hypotheses?.get(name)?:parent?.getHypothesis(name)
    }

    fun getHypotheses(filter:(LabeledSequenceOfSymbols) -> Boolean):List<LabeledSequenceOfSymbols> {
        val result = ArrayList<LabeledSequenceOfSymbols>()
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

    fun variableExists(name:String): Boolean {
        return variables?.contains(name)?:false || parent?.variableExists(name)?:false
    }

    fun addConstants(constants:Set<String>) {
        if (parent != null) {
            throw MetamathParserException("Constant declaration is allowed only in the outermost block.")
        }
        this.constants!!.addAll(constants)
    }

    fun addVariables(variables:Set<String>) {
        if (this.variables == null) {
            this.variables = HashSet()
        }
        this.variables!!.addAll(variables)
    }

    fun addHypothesis(name:String, expr:LabeledSequenceOfSymbols) {
        if (this.hypotheses == null) {
            this.hypotheses = HashMap()
        }
        this.hypotheses!![name] = expr
    }

    fun addAssertion(name:String,expr:Assertion) {
        getAssertions()[name] = expr
    }

    fun addAssertions(assertions:Map<String,Assertion>) {
        getAssertions().putAll(assertions)
    }
}
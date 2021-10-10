package org.igye.metamathparser

data class MetamathContext(
    val parent:MetamathContext? = null,
    val constants:Set<String> = emptySet(),
    val variables:Set<String> = emptySet(),
    val hypotheses:Map<String,LabeledSequenceOfSymbols> = emptyMap(),
    val assertions:Map<String,Assertion> = emptyMap(),
) {
    fun addConstants(constants:Collection<String>):MetamathContext {
        return copy(
            parent = this,
            constants = this.constants.plus(constants)
        )
    }

    fun addVariables(variables:Collection<String>):MetamathContext {
        return copy(
            parent = this,
            variables = this.variables.plus(variables)
        )
    }

    fun addHypothesis(name:String, expr:LabeledSequenceOfSymbols):MetamathContext {
        return copy(
            parent = this,
            hypotheses = this.hypotheses.plus(name to expr)
        )
    }

    fun addAssertion(name:String, assertion: Assertion):MetamathContext {
        return copy(
            parent = this,
            assertions = this.assertions.plus(name to assertion)
        )
    }

    fun addAssertions(assertions:Map<String,Assertion>):MetamathContext {
        return copy(
            parent = this,
            assertions = this.assertions.plus(assertions)
        )
    }
}
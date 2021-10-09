package org.igye.metamathparser

data class MetamathContext(
    val parent:MetamathContext? = null,
    val constants:Set<String> = emptySet(),
    val variables:Set<String> = emptySet(),
    val assertions:Map<String,Assertion> = emptyMap(),
)
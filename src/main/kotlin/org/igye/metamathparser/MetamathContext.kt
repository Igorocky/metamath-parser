package org.igye.metamathparser

data class MetamathContext(
    val parent:MetamathContext? = null,
    val constants:Set<String> = emptySet(),
    val variables:Set<String> = emptySet(),
    val floating:Map<String,List<String>> = emptyMap(),
    val essential:Map<String,List<String>> = emptyMap(),
    val assertions:Map<String,Assertion> = emptyMap(),
)
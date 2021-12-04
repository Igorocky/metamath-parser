package org.igye.proofassistant.substitutions

class Substitution(
    val begins: IntArray,
    val ends: IntArray,
    val isDefined: BooleanArray,
    val parenthesesCounter: Array<ParenthesesCounter>,
)

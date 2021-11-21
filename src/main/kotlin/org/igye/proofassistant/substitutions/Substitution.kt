package org.igye.proofassistant.substitutions

class Substitution(
    var stmt: IntArray,
    val begins: IntArray,
    val ends: IntArray,
    val levels: IntArray,
    val parenthesesCounter: Array<ParenthesesCounter>,
)

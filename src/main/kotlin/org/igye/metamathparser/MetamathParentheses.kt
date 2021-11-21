package org.igye.metamathparser

import org.igye.proofassistant.substitutions.ParenthesesCounter

class MetamathParentheses(
    val roundBracketOpen:Int,
    val roundBracketClose:Int,
    val curlyBracketOpen:Int,
    val curlyBracketClose:Int,
    val squareBracketOpen:Int,
    val squareBracketClose:Int,
) {
    fun createParenthesesCounter() = ParenthesesCounter(
        roundBracketOpen = roundBracketOpen,
        roundBracketClose = roundBracketClose,
        curlyBracketOpen = curlyBracketOpen,
        curlyBracketClose = curlyBracketClose,
        squareBracketOpen = squareBracketOpen,
        squareBracketClose = squareBracketClose,
    )
}
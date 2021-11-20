package org.igye.proofassistant

import org.igye.metamathparser.MetamathParserException

class ConstParts(
    val begins:IntArray,
    val ends:IntArray,
    val parenCounters: Array<ParenthesesCounter>,
    val remainingMinLength: Array<Int>
) {
    init {
        if (begins.size != ends.size) {
            throw MetamathParserException("begins.size != ends.size")
        }
    }

    val size = begins.size
}
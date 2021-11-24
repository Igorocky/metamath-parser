package org.igye.proofassistant.proof

import java.util.*

class Stmt(val value: IntArray, val valueStr: String) {
    override fun equals(other: Any?): Boolean {
        return other is Stmt && value.contentEquals(other.value)
    }

    override fun hashCode(): Int {
        return Arrays.hashCode(value)
    }

    override fun toString(): String {
        return valueStr
    }
}
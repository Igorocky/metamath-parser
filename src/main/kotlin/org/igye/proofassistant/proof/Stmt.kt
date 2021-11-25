package org.igye.proofassistant.proof

import java.util.*

class Stmt(val value: IntArray, val valueStr: String) {
    private val hash: Int = Arrays.hashCode(value)

    override fun equals(other: Any?): Boolean {
        return other is Stmt && this.hash == other.hash && value.contentEquals(other.value)
    }

    override fun hashCode(): Int {
        return hash
    }

    override fun toString(): String {
        return valueStr
    }
}
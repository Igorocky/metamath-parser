package org.igye.metamathparser

class CalculatedStackNode(
    val args: List<StackNode>,
    val substitution: List<IntArray>,
    val assertion: Assertion,
    value: IntArray
):StackNode(value = value) {
    override fun toString(): String {
        return "Calculated: ${value.joinToString(separator = " ")}"
    }
}
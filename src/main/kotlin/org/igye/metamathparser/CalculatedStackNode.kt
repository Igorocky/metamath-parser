package org.igye.metamathparser

class CalculatedStackNode(
    val args: List<StackNode>,
    val substitution: Map<String,List<String>>,
    val assertion: Assertion,
    value: List<String>
):StackNode(value) {
    override fun toString(): String {
        return "Calculated: ${value.joinToString(separator = " ")}"
    }
}
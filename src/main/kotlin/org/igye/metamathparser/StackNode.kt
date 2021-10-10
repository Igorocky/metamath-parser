package org.igye.metamathparser

open class StackNode(val value: List<String>) {
    private var id = -1

    fun setId(id:Int) {
        if (this.id != -1) {
            throw MetamathParserException("this.id != -1")
        }
        this.id = id
    }

    fun getId() = id

    override fun toString(): String {
        return "Const: ${value.joinToString(separator = " ")}"
    }
}
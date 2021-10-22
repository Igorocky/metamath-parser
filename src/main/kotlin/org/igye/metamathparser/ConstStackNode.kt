package org.igye.metamathparser

open class ConstStackNode(val stmt: Statement):StackNode(value = stmt.content) {

    override fun toString(): String {
        return "Const node: ${stmt.content.joinToString(separator = " ")}"
    }
}
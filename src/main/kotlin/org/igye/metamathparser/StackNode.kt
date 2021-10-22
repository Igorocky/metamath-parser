package org.igye.metamathparser

abstract class StackNode(val value: IntArray) {
    private var id = -1

    fun setId(id:Int) {
        if (this.id != -1) {
            throw MetamathParserException("this.id != -1")
        }
        this.id = id
    }

    fun getId() = id
}
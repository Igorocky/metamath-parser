package org.igye.metamathparser

interface MetamathContextI<IMPL> {
    fun createChildContext(): IMPL
    fun setLastComment(str:String?)
}
package org.igye.metamathparser

object Utils {
    fun readStringFromClassPath(path: String): String {
        return Utils::class.java.getResource(path).readText()
    }
}
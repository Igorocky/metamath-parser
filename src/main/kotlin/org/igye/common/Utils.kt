package org.igye.common

import java.io.InputStream

object Utils {
    fun readStringFromClassPath(path: String): String {
        return Utils::class.java.getResource(path).readText()
    }

    fun inputStreamFromClassPath(path: String): InputStream {
        return Utils::class.java.getResource(path).openStream()
    }

    fun <T> subList(list:List<T>, beginIdx:Int, endIdx:Int):List<T> {
        val res = ArrayList<T>()
        for (i in beginIdx until endIdx) {
            res.add(list[i])
        }
        return res
    }
}
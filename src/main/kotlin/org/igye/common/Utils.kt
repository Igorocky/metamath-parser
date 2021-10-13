package org.igye.common

import java.io.InputStream

object Utils {
    fun readStringFromClassPath(path: String): String {
        return Utils::class.java.getResource(path).readText()
    }

    fun inputStreamFromClassPath(path: String): InputStream {
        return Utils::class.java.getResource(path).openStream()
    }
}
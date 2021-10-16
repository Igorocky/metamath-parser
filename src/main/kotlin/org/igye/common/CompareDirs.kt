package org.igye.common

import java.io.File

fun main() {
    Utils.compareDirs(
        File("C:\\igye\\temp\\metamath\\old"),
        File("C:\\igye\\temp\\metamath\\new"),
    ).forEach { println(it) }
}
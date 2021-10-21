package org.igye.common

import java.io.File
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

    fun compareDirs(dir1: File, dir2: File): List<String> {
        fun save(file: File, files: MutableList<File>, dirs: MutableList<File>) {
            if (file.isDirectory) dirs.add(file) else files.add(file)
        }
        fun getAllChildFiles(dir: File): Set<String> {
            val files = ArrayList<File>()
            val dirs = ArrayList<File>()
            var currDir = dir
            currDir.listFiles().forEach { save(it, files, dirs) }
            while (dirs.isNotEmpty()) {
                currDir = dirs.removeAt(0)
                currDir.listFiles().forEach { save(it, files, dirs) }
            }
            return files.asSequence().map { it.toRelativeString(dir) }.toSet()
        }
        fun getContent(dir: File, fileRelPath: String): ByteArray = (File(dir.absolutePath + "/" + fileRelPath)).readBytes()
        fun contentEquals(dir1: File, dir2: File, fileRelPath: String) =
            getContent(dir1,fileRelPath).contentEquals(getContent(dir2,fileRelPath))

        val result = ArrayList<String>()
        val files1: Set<String> = getAllChildFiles(dir1)
        val files2: Set<String> = getAllChildFiles(dir2)
        result.add("${files1.size} files expected, ${files2.size} files actually present.")
        (files1 - files2).forEach { result.add("missing   $it") }
        (files2 - files1).forEach { result.add("redundant $it") }
        files1.intersect(files2).asSequence()
            .filter { !contentEquals(dir1,dir2,it) }
//            .take(10)
            .forEach { result.add("different $it") }
        return result
    }
}
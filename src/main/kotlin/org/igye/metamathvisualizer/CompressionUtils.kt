package org.igye.metamathvisualizer

import org.igye.metamathvisualizer.dto.*
import java.util.function.Function
import java.util.stream.Collectors

object CompressionUtils {

    fun compress(assertionDto: AssertionDto): CompressedAssertionDto2 {
        return compress(compress1(assertionDto))
    }

    private fun compress(dto: CompressedAssertionDto): CompressedAssertionDto2 {
        return CompressedAssertionDto2(
            s=dto.s.joinToString(separator = " "),
                    t=dto.t,
                    n=dto.n,
                    d=dto.d,
                    v=compressMapOfIntsToStr(dto.v),
                    pa=compressListOfListOfInts(dto.pa),
                    r=compressListOfIntsToStr(dto.r),
                    p= dto.p.asSequence().map(CompressionUtils::compress).toList(),
        )
    }

    private fun compress1(dto: AssertionDto): CompressedAssertionDto {
        val strings: Pair<List<String>, Map<String, Int>> = buildStrMap(dto)
        val strMap: Map<String, Int> = strings.second
        return CompressedAssertionDto(
            s = strings.first,
                    t = dto.type,
                    n = dto.name,
                    d = dto.description,
                    v = compress(dto.varTypes, strMap),
                    pa = compress2(dto.params, strMap),
                    r = compress(dto.retVal, strMap)!!,
                    p = dto.proof.stream().map { p -> compress(p, strMap) }.collect(Collectors.toList()),
        )
    }

    private fun buildStrMap(dto: AssertionDto): Pair<List<String>, Map<String, Int>> {
        val counts: MutableMap<String, Int> = HashMap()
        for ((key, value) in dto.varTypes.entries) {
            updateCounts(counts, key)
            updateCounts(counts, value)
        }
        updateCounts2(counts, dto.params)
        updateCounts(counts, dto.retVal)
        if (dto.proof != null) {
            for (proof in dto.proof) {
                updateCounts(counts, proof)
            }
        }
        return buildStrMap(counts)
    }

    private fun updateCounts(counts: MutableMap<String, Int>, dto: StackNodeDto) {
        updateCounts(counts, dto.type)
        updateCounts(counts, dto.label)
        updateCounts2(counts, dto.params)
        updateCounts(counts, dto.retVal)
        updateCounts2(counts, dto.substitution)
        updateCounts(counts, dto.expr)
    }

    private fun compress(dto: StackNodeDto, strMap: Map<String, Int>): CompressedStackNodeDto {
        return CompressedStackNodeDto(
            i = dto.id,
                    a = dto.args,
                    t = strMap[dto.type]!!,
                    l = strMap[dto.label]!!,
                    p = compress2(dto.params, strMap),
                    r = compress(dto.retVal, strMap),
                    n = dto.numOfTypes,
                    s = dto.substitution.entries.asSequence()
                        .associate { (k,v) -> strMap[k]!! to compress(v, strMap)!! },
            e = compress(dto.expr, strMap)!!,
        )
    }

    private fun compress(dto: CompressedStackNodeDto): String {
        val result = ArrayList<String>()
        result.add(dto.i.toString())
        result.add(if (dto.a == null) "" else compressListOfIntsToStr(dto.a))
        result.add(dto.t.toString())
        result.add(dto.l.toString())
        result.add(if (dto.p == null) "" else compressListOfListOfInts(dto.p))
        result.add(dto.n.toString())
        result.add(if (dto.r == null) "" else compressListOfIntsToStr(dto.r))
        result.add((if (dto.s == null) "" else compressMapOfIntListToStr(dto.s))!!)
        result.add(compressListOfIntsToStr(dto.e))
        return compressListOfStrings(result)
    }

    private fun buildStrMap(dto: IndexDto): Pair<List<String>, Map<String, Int>> {
        val counts: MutableMap<String, Int> = HashMap()
        for (indexElemDto in dto.elems) {
            updateCounts(counts, indexElemDto.type)
            updateCounts2(counts, indexElemDto.hypotheses)
            updateCounts(counts, indexElemDto.expression)
            updateCounts(counts, indexElemDto.varTypes)
        }
        return buildStrMap(counts)
    }

    fun compress(dto: IndexDto): CompressedIndexDto2 {
        return compress(compress1(dto))
    }

    private fun compress(dto: CompressedIndexDto): CompressedIndexDto2 {
        return CompressedIndexDto2(
            strings= dto.strings.joinToString(separator = " "),
                    elems= dto.elems.stream().map(CompressionUtils::compress).collect(Collectors.toList()),
        )
    }

    private fun compress1(dto: IndexDto): CompressedIndexDto {
        val strings: Pair<List<String>, Map<String, Int>> = buildStrMap(dto)
        val strMap: Map<String, Int> = strings.second
        return CompressedIndexDto(
            strings = strings.first,
                    elems = dto.elems.stream().map { e -> compress(e, strMap) }.toList(),
        )
    }

    private fun compress(dto: IndexElemDto, strMap: Map<String, Int>): CompressedIndexElemDto {
        return CompressedIndexElemDto(
            i = dto.id,
                    t = strMap[dto.type]!!,
                    l = dto.label,
                    h = compress2(dto.hypotheses, strMap),
                    e = compress(dto.expression, strMap)!!,
                    v = compress(dto.varTypes, strMap),
        )
    }

    private fun compress(dto: CompressedIndexElemDto): String {
        val res: MutableList<String> = ArrayList()
        res.add(dto.i.toString())
        res.add(dto.t.toString())
        res.add(dto.l)
        res.add(compressListOfListOfInts(dto.h))
        res.add(compressListOfIntsToStr(dto.e))
        res.add(compressMapOfIntsToStr(dto.v))
        return compressListOfStrings(res)
    }

    private fun compressListOfListOfInts(list: List<List<Int>>): String {
        return list.stream().map({ compressListOfIntsToStr(it) }).collect(Collectors.joining(" "))
    }

    fun intToStr(i: Int): String {
        var i = i
        val sb = StringBuilder()
        return if (i == 0) {
            "#"
        } else {
            val base = 46
            while (i > 0) {
                sb.append((i % base + if (sb.length == 0) 35 else 81).toChar())
                i /= base
            }
            sb.reverse().toString()
        }
    }

    fun compressListOfStrings(strings: List<String>): String {
        val sb = StringBuilder()
        for (i in strings.indices) {
            if (i != 0) {
                sb.append(166.toChar())
            }
            sb.append(strings[i])
        }
        return sb.toString()
    }

    private fun compressListOfIntsToStr(ints: List<Int>): String {
        val sb = StringBuilder()
        for (i in ints) {
            sb.append(intToStr(i))
        }
        return sb.toString()
    }

    private fun compressMapOfIntsToStr(map: Map<Int, Int>): String {
        val sb = StringBuilder()
        for ((key, value) in map) {
            sb.append(intToStr(key))
            sb.append(intToStr(value))
        }
        return sb.toString()
    }

    private fun compressMapOfIntListToStr(map: Map<Int, List<Int>>): String {
        val sb = StringBuilder()
        for ((key, value) in map) {
            sb.append(intToStr(key))
            sb.append(" ")
            sb.append(compressListOfIntsToStr(value))
            sb.append(" ")
        }
        return sb.toString().trim { it <= ' ' }
    }

    private fun compress(list: List<String>, strMap: Map<String, Int>): List<Int> {
        return list.asSequence().map { key: String ->
            strMap[key]!!
        }.toList()
    }

    private fun compress2(list: List<List<String>>, strMap: Map<String, Int>): List<List<Int>> {
        return list.asSequence()
            .map {
                compress(
                    it,
                    strMap
                )
            }.toList()
    }

    private fun compress(map: Map<String, String>, strMap: Map<String, Int>): Map<Int, Int> {
        return map.entries.stream().collect(Collectors.toMap(
            Function { (key): Map.Entry<String, String> ->
                strMap[key]!!
            },
            Function { (_, value): Map.Entry<String, String> ->
                strMap[value]!!
            }
        ))
    }

    private fun updateCounts(counts: MutableMap<String, Int>, list: List<String>?) {
        if (list != null) {
            for (str in list) {
                updateCounts(counts, str)
            }
        }
    }

    private fun updateCounts2(counts: MutableMap<String, Int>, list: List<List<String>>?) {
        if (list != null) {
            for (le in list) {
                for (str in le) {
                    updateCounts(counts, str)
                }
            }
        }
    }

    private fun updateCounts2(counts: MutableMap<String, Int>, data: Map<String, List<String>>?) {
        if (data != null) {
            for ((key, value) in data) {
                updateCounts(counts, key)
                for (str in value) {
                    updateCounts(counts, str)
                }
            }
        }
    }

    private fun updateCounts(counts: MutableMap<String, Int>, data: Map<String, String>?) {
        if (data != null) {
            for ((key, value) in data) {
                updateCounts(counts, key)
                updateCounts(counts, value)
            }
        }
    }

    private fun updateCounts(counts: MutableMap<String, Int>, str: String?) {
        if (str != null) {
            counts[str] = counts.getOrDefault(str, 0) + 1
        }
    }

    private fun buildStrMap(counts: Map<String, Int>): Pair<List<String>, Map<String, Int>> {
        val strings = counts.entries.stream()
            .sorted(compareByDescending { (_, value) -> value })
            .map { (key, _) -> key }
            .collect(Collectors.toList())
        val strMap: MutableMap<String, Int> = HashMap()
        for (i in strings.indices) {
            strMap[strings[i]] = i
        }
        return Pair(strings, strMap)
    }
}
package org.igye.metamathvisualizer

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import org.igye.common.DebugTimer
import org.igye.common.Utils.inputStreamFromClassPath
import org.igye.common.Utils.readStringFromClassPath
import org.igye.metamathparser.*
import org.igye.metamathvisualizer.dto.*
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Function
import java.util.stream.Collectors
import kotlin.collections.HashMap

fun main() {
    val assertions: Map<String, Assertion> = DebugTimer.run("parseMetamathFile") {
        Parsers.parseMetamathFile(
            text = File("C:\\igye\\books\\metamath\\set.mm").readText(),
//            text = File("C:\\igye\\projects\\kotlin\\metamath-parser\\src\\test\\resources\\set-reduced.mm").readText(),
            ExpressionProcessor
        ).getAssertions()
    }
    DebugTimer.run("generateProofExplorer") {
        MetamathVisualizer.generateProofExplorer(
            assertions = assertions.values,
            version = "v8",
            numOfThreads = 8,
            pathToDirToSaveTo = "C:\\igye\\temp\\metamath\\new"
//        pathToDirToSaveTo = "C:\\igye\\temp\\metamath-reduced\\new"
        )
    }
    println(DebugTimer.getStats())
}

object MetamathVisualizer {
    private val filePathSeparatorRegex = "/|\\\\".toRegex()
    private val MAPPER = ObjectMapper()
    init {
        MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL)
        MAPPER.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
    }

    fun generateProofExplorer(
        assertions: Collection<Assertion>, version: String, numOfThreads: Int, pathToDirToSaveTo: String
    ) {
        println("Writing common files...")
        val dirToSaveTo = File(pathToDirToSaveTo)
        if (dirToSaveTo.exists()) {
            throw MetamathParserException("The directory already exists: " + dirToSaveTo.absolutePath)
        }
        val versionDir = File(dirToSaveTo, version)
        versionDir.mkdirs()
        copyUiFileToDir("/ui/css/styles.css", versionDir)
        copyUiBinFileToDir("/ui/img/favicon.ico", versionDir)
        copyUiFileToDir("/ui/js/lib/react.production-16.8.6.min.js", versionDir)
        copyUiFileToDir("/ui/js/lib/react-dom.production-16.8.6.min.js", versionDir)
        copyUiFileToDir("/ui/js/lib/material-ui.production-4.11.0.min.js", versionDir)
        copyUiFileToDir("/ui/js/utils/react-imports.js", versionDir)
        copyUiFileToDir("/ui/js/utils/data-functions.js", versionDir)
        copyUiFileToDir("/ui/js/utils/svg-functions.js", versionDir)
        copyUiFileToDir("/ui/js/utils/rendering-functions.js", versionDir)
        copyUiFileToDir("/ui/js/utils/all-imports.js", versionDir)
        copyUiFileToDir("/ui/js/components/LicenseDialogView.js", versionDir)
        copyUiFileToDir("/ui/js/components/Pagination.js", versionDir)
        copyUiFileToDir("/ui/js/components/Expression.js", versionDir)
        copyUiFileToDir("/ui/js/components/Assertion.js", versionDir)
        copyUiFileToDir("/ui/js/components/ConstProofNode.js", versionDir)
        copyUiFileToDir("/ui/js/components/RuleProofNode.js", versionDir)
        copyUiFileToDir("/ui/js/components/MetamathAssertionView.js", versionDir)
        copyUiFileToDir("/ui/js/components/MetamathIndexTable.js", versionDir)
        copyUiFileToDir("/ui/js/components/MetamathIndexView.js", versionDir)

        val queue: Queue<Assertion> = ConcurrentLinkedQueue(assertions)
        val executorService = Executors.newFixedThreadPool(numOfThreads)
        val filesWrittenAtomic = AtomicInteger()
        val indexElems: MutableMap<Int, IndexElemDto> = ConcurrentSkipListMap()
        val errorOccurred = AtomicReference<Exception?>(null)
        for (i in 0 until numOfThreads) {
            executorService.submit {
                var assertion = queue.poll()
                while (assertion != null && errorOccurred.get() == null) {
                    try {
                        val dto: AssertionDto = DebugTimer.run("visualizeAssertion") { visualizeAssertion(assertion) }
                        DebugTimer.run("createAssertionHtmlFile") {
                            createAssertionHtmlFile(
                                version, dto, dirToSaveTo, createRelPathToSaveTo(dto.name)
                            )
                        }
                        indexElems[assertion.statement.beginIdx] = createIndexElemDto(dto)
                    } catch (e: Exception) {
                        println("Error - ${e.message}")
                        e.printStackTrace()
                        errorOccurred.set(e)
                    }
                    assertion = queue.poll()
                    val filesWritten = filesWrittenAtomic.incrementAndGet()
                    val pct = filesWritten * 1.0 / assertions.size * 100
                    println(
                        "Writing DTOs - "
                                + BigDecimal(pct).setScale(1, RoundingMode.HALF_UP) + "%" +
                                " (" + filesWritten + " of " + assertions.size + "). "
                    )
                }
            }
        }
        executorService.shutdown()
        executorService.awaitTermination(1, TimeUnit.DAYS)
        if (errorOccurred.get() != null) {
            val err = errorOccurred.get()
            throw err!!
        }
        println("Writing index...")
        createHtmlFile(
            version,
            ".",
            "MetamathIndexView",
            CompressionUtils.compress(buildIndex(indexElems.values)),
            File(dirToSaveTo, "index.html")
        )
    }

    private fun buildIndex(indexElems: Collection<IndexElemDto>): IndexDto {
        return IndexDto(
            elems = indexElems.asSequence().withIndex()
                .map { (i,dto) ->
                    dto.copy(id = i)
                }
                .toList()
        )
    }

    private fun createIndexElemDto(assertionDto: AssertionDto): IndexElemDto {
        return IndexElemDto(
            id = -1,
            type = assertionDto.type.substring(0, 1),
            label = assertionDto.name,
            hypotheses = assertionDto.params,
            expression = assertionDto.retVal,
            varTypes = assertionDto.varTypes,
        )
    }

    fun visualizeAssertion(assertion: Assertion): AssertionDto {
        var proof: StackNode? = null
        var proofDto: List<StackNodeDto>? = null
        var symbolsInfo: SymbolsInfo?
        if (assertion.statement.type == 'p') {
            proof = ProofVerifier.verifyProof(assertion)
            symbolsInfo = collectSymbolsInfo(assertion, proof)
            val globalScope = symbolsInfo
            val nodes: MutableList<StackNodeDto> = ArrayList<StackNodeDto>()
            iterateNodes(proof) { node: StackNode ->
                if (node is CalculatedStackNode) {
                    val localScope = collectSymbolsInfo(node.assertion, null)
                    val localVarToGlobalVar = node.assertion.visualizationData.localVarToGlobalVar
                    var nodeDto = StackNodeDto(
                        id = node.getId(),
                        args = node.args.map { it.getId() },
                        type = node.assertion.statement.type.uppercase(),
                        label = node.assertion.statement.label,
                        params = node.assertion.hypotheses.map {
                            toSymbols(it.content, globalScope.constants, localScope.variables, localVarToGlobalVar)
                        },
                        numOfTypes = node.assertion.numberOfVariables,
                        retVal = toSymbols(node.assertion.statement.content, globalScope.constants, localScope.variables, localVarToGlobalVar),
                        substitution = toSymbols(node.substitution, globalScope.constants, globalScope.variables, localScope.variables, localVarToGlobalVar),
                        expr = toSymbols(node.value, globalScope.constants, globalScope.variables, null)
                    )
                    nodes.add(nodeDto)
                } else {
                    nodes.add(StackNodeDto(
                        id = node.getId(),
                        args = null,
                        type = node.stmt!!.type.uppercase(),
                        label = node.stmt.label,
                        params = null,
                        numOfTypes = 0,
                        retVal = null,
                        substitution = null,
                        expr = toSymbols(node.value, globalScope.constants, globalScope.variables, null)
                    ))
                }
            }
            Collections.sort(nodes, Comparator.comparing { it.id })
            val uniqueSteps: List<StackNodeDto> = removeDuplicates(nodes)
            proofDto = uniqueSteps
        } else {
            symbolsInfo = collectSymbolsInfo(assertion, null)
        }
        val params: List<List<String>> = assertion.hypotheses.asSequence()
            .filter { it.type == 'e' }
            .map { toSymbols(it.content, symbolsInfo.constants, symbolsInfo.variables, assertion.visualizationData.localVarToGlobalVar) }
            .toList()
        val retVal: List<String> = toSymbols(assertion.statement.content, symbolsInfo.constants, symbolsInfo.variables, assertion.visualizationData.localVarToGlobalVar)
        val allSymbols: Set<String> = extractAllSymbols(params, retVal, proofDto)
        var assertionDto = AssertionDto(
            type = getTypeStr(assertion),
            name = assertion.statement.label,
            description = assertion.visualizationData.description,
            varTypes = symbolsInfo.varTypes.filter { (k,v) -> allSymbols.contains(k) },
            params = params,
            retVal = retVal,
            proof = proofDto
        )
        return assertionDto
    }

    private fun extractAllSymbols(params: List<List<String>>, retVal: List<String>, proofDto: List<StackNodeDto>?): Set<String> {
        val allSymbols = HashSet<String>()
        for (param in params) {
            allSymbols.addAll(param)
        }
        allSymbols.addAll(retVal)
        if (proofDto != null) {
            for (stackNodeDto in proofDto) {
                if (stackNodeDto.params != null) {
                    for (param in stackNodeDto.params) {
                        allSymbols.addAll(param)
                    }
                }
                if (stackNodeDto.retVal != null) {
                    allSymbols.addAll(stackNodeDto.retVal)
                }
                allSymbols.addAll(stackNodeDto.expr)
            }
        }
        return allSymbols
    }

    private fun toSymbols(statement: IntArray, constants:Map<Int,String>, vars:Map<Int,String>, remapVariables:IntArray?): List<String> {
        return statement.map {
            if (it < 0) constants[it]!!
            else if (remapVariables == null) vars[it]!!
            else vars[remapVariables[it]]!!
        }
    }

    private fun toSymbols(
        substitution: List<IntArray>,
        constants:Map<Int,String>,
        globalVars:Map<Int,String>,
        localVars:Map<Int,String>,
        remapVariables:IntArray
    ): Map<String, List<String>> {
        val result = HashMap<String, List<String>>()
        for (i in 0 until substitution.size) {
            result[localVars[remapVariables[i]]!!] = toSymbols(substitution[i], constants, globalVars, null)
        }
        return result
    }

    private fun removeDuplicates(nodes: List<StackNodeDto>): List<StackNodeDto> {
        val nodesToProcess: MutableList<StackNodeDto> = ArrayList(nodes)
        val result: MutableList<StackNodeDto> = ArrayList()
        val exprToNode: MutableMap<String, StackNodeDto> = HashMap()
        val nodeIdRemap: MutableMap<Int, Int> = HashMap()
        while (!nodesToProcess.isEmpty()) {
            val node: StackNodeDto = nodesToProcess.removeAt(0)
            if (nodeIdRemap.containsKey(node.id)) {
                throw MetamathParserException("nodeIdRemap.containsKey(node.id)")
            }
            val exprStr: String = node.expr.joinToString(separator = " ")
            val existingNode: StackNodeDto? = exprToNode[exprStr]
            if (existingNode == null) {
                exprToNode[exprStr] = node
                nodeIdRemap[node.id] = node.id
                if (node.args?.isNotEmpty()?:false) {
                    node.args = node.args?.map { nodeIdRemap[it]!! }
                }
                result.add(node)
            } else {
                nodeIdRemap[node.id] = existingNode.id
            }
        }
        return result
    }

    private fun iterateNodes(root: StackNode, nodeConsumer: (StackNode) -> Unit) {
        val processed = HashSet<StackNode>()
        val toProcess = Stack<StackNode>()
        toProcess.push(root)
        while (!toProcess.isEmpty()) {
            val curNode: StackNode = toProcess.pop()
            if (!processed.contains(curNode)) {
                nodeConsumer.invoke(curNode)
                processed.add(curNode)
                if (curNode is CalculatedStackNode) {
                    toProcess.addAll(curNode.args)
                }
            }
        }
    }

    private fun copyUiFileToDir(fileInClassPath: String, dir: File) {
        copyFromClasspathToDir(1, fileInClassPath, dir)
    }

    private fun copyUiBinFileToDir(fileInClassPath: String, dir: File) {
        copyBinFileFromClasspathToDir(1, fileInClassPath, dir)
    }

    private fun copyFromClasspathToDir(numOfDirsToSkip: Int, fileInClassPath: String, dir: File) {
        copyFromClasspathToDir(numOfDirsToSkip, fileInClassPath, dir, Function.identity())
    }

    private fun copyFromClasspathToDir(
        numOfDirsToSkip: Int,
        fileInClassPath: String,
        dir: File,
        modifier: Function<String, String>
    ) {
        val content: String = readStringFromClassPath(fileInClassPath)
        val destFile = File(
            dir,
            fileInClassPath.split(filePathSeparatorRegex).drop(numOfDirsToSkip + 1).joinToString(separator = "/")
        )
        destFile.parentFile.mkdirs()
        destFile.writeText(modifier.apply(content))
    }

    private fun copyBinFileFromClasspathToDir(numOfDirsToSkip: Int, fileInClassPath: String, dir: File) {
        val destFile = File(
            dir,
            fileInClassPath.split(filePathSeparatorRegex).takeLast(numOfDirsToSkip + 1).joinToString(separator = "/")
        )
        destFile.parentFile.mkdirs()
        inputStreamFromClassPath(fileInClassPath).use { inp ->
            FileOutputStream(destFile).use { os ->
                inp.copyTo(out = os)
            }
        }
    }

    private fun copyFromClasspath(fileInClassPath: String, modifier: Function<String, String>, destFile: File) {
        val content: String = readStringFromClassPath(fileInClassPath)
        destFile.parentFile.mkdirs()
        val text = modifier.apply(content)
        DebugTimer.run("destFile.writeText") {
            destFile.writeText(text)
        }
    }

    private fun createHtmlFile(
        version: String?, relPathToRoot: String, viewComponentName: String, viewProps: Any, file: File
    ) {
        val viewPropsStr: String = MAPPER.writeValueAsString(MAPPER.writeValueAsString(viewProps))
        val decompressionFunctionName = when(viewProps) {
            is CompressedAssertionDto2 -> "decompressAssertionDto"
            is CompressedIndexDto2 -> "decompressIndexDto"
            else -> throw MetamathParserException("decompressionFunctionName == null")
        }
        val finalVersion = version ?: "."
        copyFromClasspath(
            "/ui/index.html",
            { html: String ->
                html
                    .replace("\$version", finalVersion)
                    .replace("\$relPathToRoot", relPathToRoot)
                    .replace("\$componentName", viewComponentName)
                    .replace("\$decompressionFunction", decompressionFunctionName)
                    .replace("'\$viewProps'", viewPropsStr)
                    .replace("src=\"", "src=\"$relPathToRoot/$finalVersion/")
            },
            file
        )
    }

    private fun createAssertionHtmlFile(
        version: String, assertionDto: AssertionDto, dataDir: File, relPath: List<String>
    ) {
        val relPathToRoot = relPath.stream()
            .limit((relPath.size - 1).toLong())
            .map { p: String? -> ".." }
            .collect(Collectors.joining("/"))
        createHtmlFile(
            version,
            relPathToRoot,
            "MetamathAssertionView",
            CompressionUtils.compress(assertionDto),
            File(dataDir, relPath.joinToString(separator = "/"))
        )
    }

    private fun getTypeStr(type: Assertion): String {
        return when(type.statement.type) {
            'a' -> "Axiom"
            'p' -> "Theorem"
            else -> type.statement.type.toString()
        }
    }

    private fun createRelPathToSaveTo(label: String): List<String> {
        return listOf("asrt", "$label.html")
    }

    private fun splitConstantsAndVars(symbolsMap: Map<Int,String>): Pair<MutableMap<Int,String>,MutableMap<Int,String>> {
        val constants = HashMap<Int, String>()
        val variables = HashMap<Int, String>()
        symbolsMap.asSequence().forEach {(k,v) ->
            putIfNoConflicts(if (k < 0) constants else variables, k, v)
        }
        return Pair(constants, variables)
    }

    private fun collectSymbolsInfo(assertion: Assertion, proof: StackNode?): SymbolsInfo {
        val (constants, variables) = splitConstantsAndVars(assertion.visualizationData.symbolsMap)
        val varTypes = HashMap<String,String>(assertion.visualizationData.variablesTypes)

        if (proof != null) {
            iterateNodes(proof) { node->
                if (node is CalculatedStackNode) {
                    node.assertion.visualizationData.symbolsMap.forEach {(k,v) ->
                        if (k < 0) {
                            putIfNoConflicts(constants, k, v)
                        }
                    }
                    node.assertion.visualizationData.variablesTypes.forEach {(k,v) ->
                        putIfNoConflicts(varTypes, k, v)
                    }
                }
            }
        }
        return SymbolsInfo(constants, variables, varTypes)
    }

    private fun <K,V> putIfNoConflicts(map:MutableMap<K,V>, k:K, v:V) {
        val existingV = map[k]
        if (existingV == null) {
            map[k] = v
        } else if (existingV != v) {
            throw MetamathParserException("existingV != v")
        }
    }
}
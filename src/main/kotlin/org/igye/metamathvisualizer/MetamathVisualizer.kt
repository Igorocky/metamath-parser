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
import kotlin.collections.HashSet

fun main() {
    val assertions: Map<String, Assertion> =
        Parsers.traverseMetamathFile(
            text = File("C:\\igye\\projects\\kotlin\\metamath-parser\\src\\test\\resources\\set-reduced.mm").readText(),
            ExpressionProcessor
        ).getAssertions()
    MetamathVisualizer.generateProofExplorer(
        assertions = assertions.values,
        version = "v8",
        numOfThreads = 8,
        pathToDirToSaveTo = "C:\\igye\\temp\\metamath\\new"
    )
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
//        if (dirToSaveTo.exists()) {
//            throw MetamathParserException("The directory already exists: " + dirToSaveTo.absolutePath)
//        }
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
                        indexElems[assertion.assertion.beginIdx] = createIndexElemDto(dto)
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
//            compress(buildIndex(indexElems.values)),
            buildIndex(indexElems.values),
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
        val proofDto: List<StackNodeDto>? = if (assertion.assertion.sequence.seqType == 'p') {
            proof = ProofVerifier.verifyProof(assertion)
            val nodes: MutableList<StackNodeDto> = ArrayList<StackNodeDto>()
            DebugTimer.run("iterateNodes") {
                iterateNodes(proof) { node: StackNode ->
                    if (node is CalculatedStackNode) {
                        var nodeDto = StackNodeDto(
                            id = node.getId(),
                            args = node.args.map { it.getId() },
                            type = node.assertion.assertion.sequence.seqType.uppercase(),
                            label = node.assertion.assertion.label,
                            params = node.assertion.hypotheses.map { it.sequence.symbols },
                            numOfTypes = node.assertion.hypotheses.asSequence().filter { it.sequence.seqType == 'f' }
                                .count(),
                            retVal = node.assertion.assertion.sequence.symbols,
                            substitution = node.substitution,
                            expr = node.value
                        )
                        if (nodeDto.args?.isEmpty() == true) nodeDto = nodeDto.copy(args = null)
                        if (nodeDto.params?.isEmpty() == true) nodeDto = nodeDto.copy(params = null)
                        if (nodeDto.retVal?.isEmpty() == true) nodeDto = nodeDto.copy(retVal = null)
                        if (nodeDto.substitution?.isEmpty() == true) nodeDto = nodeDto.copy(substitution = null)
                        nodes.add(nodeDto)
                    } else {
                        nodes.add(StackNodeDto(
                            id = node.getId(),
                            args = null,
                            type = node.stmt!!.sequence.seqType.uppercase(),
                            label = node.stmt!!.label,
                            params = null,
                            numOfTypes = 0,
                            retVal = null,
                            substitution = null,
                            expr = node.stmt.sequence.symbols
                        ))
                    }
                }
            }
            Collections.sort(nodes, Comparator.comparing { it.id })
            val uniqueSteps: List<StackNodeDto> = DebugTimer.run("remove-duplicate-steps") {
                removeDuplicates(nodes)
            }
            uniqueSteps
        } else {
            null
        }
        var assertionDto = AssertionDto(
            type = getTypeStr(assertion),
            name = assertion.assertion.label,
            description = assertion.description,
            varTypes = extractVariableTypes(assertion, proof),
            params = assertion.hypotheses.asSequence()
                .filter { it.sequence.seqType == 'e' }
                .map { it.sequence.symbols }
                .toList(),
            retVal = assertion.assertion.sequence.symbols,
            proof = proofDto
        )
        return assertionDto
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
        destFile.writeText(modifier.apply(content))
    }

    private fun createHtmlFile(
        version: String?, relPathToRoot: String, viewComponentName: String, viewProps: Any, file: File
    ) {
        file.parentFile.mkdirs()
        file.writeText(MAPPER.writeValueAsString(viewProps))
        if (true) return
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
//            compress(assertionDto),
            assertionDto,
            File(dataDir, relPath.joinToString(separator = "/"))
        )
    }

    private fun getTypeStr(type: Assertion): String {
        return when(type.assertion.sequence.seqType) {
            'a' -> "Axiom"
            'p' -> "Theorem"
            else -> type.assertion.sequence.seqType.toString()
        }
    }

    private fun createRelPathToSaveTo(label: String): List<String> {
        return listOf("asrt", "$label.html")
    }

    private fun extractVariableTypes(assertion: Assertion, proof: StackNode?): Map<String, String> {
        return DebugTimer.run("extractVarTypes") {
            val varTypes = HashMap<String,String>()
            extractVariableTypes(assertion, varTypes)
            if (proof != null) {
                iterateNodes(proof) {
                    if (it is CalculatedStackNode) {
                        extractVariableTypes(it.assertion, varTypes)
                    }
                }
            }
            varTypes
        }
    }

    private fun extractVariableTypes(assertion: Assertion, varTypes: MutableMap<String,String>) {
        for ((varName, varType) in assertion.visualizationData!!.variablesTypes) {
            if (varTypes.containsKey(varName) && varTypes[varName] != varType) {
                throw MetamathParserException("varTypes.containsKey(varName) && varTypes[varName] != varType")
            }
            varTypes[varName] = varType
        }
    }
}
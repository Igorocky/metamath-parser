package org.igye.metamathvisualizer

import org.igye.common.DebugTimer
import org.igye.common.Utils
import org.igye.common.Utils.inputStreamFromClassPath
import org.igye.common.Utils.readStringFromClassPath
import org.igye.metamathparser.Assertion
import org.igye.metamathparser.MetamathParserException
import org.igye.metamathparser.StackNode
import org.igye.metamathvisualizer.dto.AssertionDto
import org.igye.metamathvisualizer.dto.IndexDto
import org.igye.metamathvisualizer.dto.IndexElemDto
import org.igye.metamathvisualizer.dto.StackNodeDto
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Predicate
import java.util.stream.Collectors
import java.util.stream.Stream

object MetamathVisualizer {
    private const val DOT_REPLACEMENT = "-dot-"
    private val filePathSeparatorRegex = "/|\\\\".toRegex()

    fun generateProofExplorer(
        assertions: List<Assertion>, version: String, numOfThreads: Int, pathToDirToSaveTo: String
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
                        val dto: AssertionDto =
                            DebugTimer.run("visualizeAssertion") { visualizeAssertion(assertion) }
                        DebugTimer.run("createAssertionHtmlFile") {
                            createAssertionHtmlFile(
                                version, dto, dirToSaveTo, createRelPathToSaveTo(dto.name)
                            )
                        }
                        indexElems[assertion.assertion.beginIdx] = createIndexElemDto(dto)
                    } catch (e: Exception) {
                        println(e.message)
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
            throw errorOccurred.get()!!
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
                    dto
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
        val assertionDto: AssertionDto.AssertionDtoBuilder = AssertionDto.builder()
        assertionDto.type(getTypeStr(assertion.getType()))
        assertionDto.name(assertion.getLabel())
        assertionDto.description(assertion.getDescription())
        assertionDto.params(
            assertion.getFrame().getHypotheses().stream()
                .map(ListStatement::getSymbols)
                .collect(Collectors.toList())
        )
        assertionDto.retVal(
            assertion.getFrame().getAssertion().getSymbols()
        )
        val varTypes = HashMap<String, String>()
        assertionDto.varTypes(varTypes)
        varTypes.putAll(DebugTimer.call("extractVarTypes-1") {
            extractVarTypes(
                assertion.getFrame().getContext(),
                Stream.concat(
                    assertion.getFrame().getHypotheses().stream().flatMap { h -> h.getSymbols().stream() },
                    assertion.getFrame().getAssertion().getSymbols().stream()
                ).collect<Set<String>, Any>(Collectors.toSet<Any>())
            )
        })
        if (assertion.getType() === ListStatementType.THEOREM) {
            val nodes: MutableList<StackNodeDto> = ArrayList<StackNodeDto>()
            val proof: StackNode = DebugTimer.call("verifyProof") { verifyProof(assertion) }
            DebugTimer.run("iterateNodes") {
                iterateNodes(proof,
                    Consumer<StackNode> { node: StackNode ->
                        if (node is RuleStackNode) {
                            val ruleNode: RuleStackNode = node as RuleStackNode
                            val frame: Frame = ruleNode.getAssertion().getFrame()
                            nodes.add(
                                StackNodeDto.builder()
                                    .id(node.getId())
                                    .args(
                                        ruleNode.getArgs().stream().map(StackNode::getId).collect(Collectors.toList())
                                    )
                                    .type(ruleNode.getAssertion().getType().getShortName().toUpperCase())
                                    .label(ruleNode.getAssertion().getLabel())
                                    .params(
                                        Stream.concat(
                                            frame.getTypes().stream(),
                                            frame.getHypotheses().stream()
                                        )
                                            .map<Any>(Function<T, Any> { stm: T -> stm.getSymbols() })
                                            .collect(Collectors.toList())
                                    )
                                    .numOfTypes(frame.getTypes().size())
                                    .retVal(ruleNode.getAssertion().getSymbols())
                                    .substitution(ruleNode.getSubstitution())
                                    .expr(ruleNode.getExpr())
                                    .build()
                            )
                        } else {
                            val constNode: ConstStackNode = node as ConstStackNode
                            nodes.add(
                                StackNodeDto.builder()
                                    .id(node.getId())
                                    .type(constNode.getStatement().getType().getShortName().toUpperCase())
                                    .label(constNode.getStatement().getLabel())
                                    .expr(constNode.getExpr())
                                    .build()
                            )
                        }
                    })
            }
            Collections.sort(nodes, Comparator.comparing<Any, Any>(StackNodeDto::getId))
            val uniqueSteps: List<StackNodeDto> = DebugTimer.call("remove-duplicate-steps") {
                removeDuplicates(
                    nodes
                )
            }
            assertionDto.proof(uniqueSteps)
            varTypes.putAll(DebugTimer.call("extractVarTypes-2") {
                extractVarTypes(
                    assertion.getFrame().getContext(),
                    nodes.stream()
                        .flatMap(Function<StackNodeDto, Stream<*>> { node: StackNodeDto ->
                            Stream.concat(
                                Stream.concat(
                                    if (node.getParams() == null) Stream.empty() else node.getParams()
                                        .stream()
                                        .flatMap { obj: List<*> -> obj.stream() },
                                    if (node.getRetVal() == null) Stream.empty() else node.getRetVal()
                                        .stream()
                                ),
                                node.getExpr().stream()
                            )
                        }).collect(Collectors.toSet<Any>())
                )
            })
        }
        return assertionDto.build()
    }


    private fun removeDuplicates(nodes: List<StackNodeDto>): List<StackNodeDto> {
        val nodesToProcess: List<StackNodeDto> = ArrayList<Any?>(nodes)
        val result: MutableList<StackNodeDto?> = ArrayList<StackNodeDto?>()
        val exprToNode: MutableMap<String, StackNodeDto> = HashMap<String, StackNodeDto>()
        val nodeIdRemap: MutableMap<Int, Int> = HashMap()
        while (!nodesToProcess.isEmpty()) {
            val node: StackNodeDto = nodesToProcess.removeAt(0)
            if (nodeIdRemap.containsKey(node.getId())) {
                throw MetamathException("nodeIdRemap.containsKey(node.getId())")
            }
            val exprStr: String = StringUtils.join(node.getExpr(), " ")
            val existingNode: StackNodeDto? = exprToNode[exprStr]
            if (existingNode == null) {
                exprToNode[exprStr] = node
                nodeIdRemap[node.getId()] = node.getId()
                result.add(node)
                if (node.getArgs() != null) {
                    node.setArgs(node.getArgs().stream().map { key: Any? -> nodeIdRemap[key] }
                        .collect(Collectors.toList()))
                    if (node.getArgs().stream().anyMatch { obj: Any? -> Objects.isNull(obj) }) {
                        throw MetamathException("node.getArgs().stream().anyMatch(Objects::isNull)")
                    }
                }
            } else {
                nodeIdRemap[node.getId()] = existingNode.getId()
            }
        }
        return result
    }









    private fun iterateNodes(root: StackNode, nodeConsumer: Consumer<StackNode>) {
        val processed: MutableSet<StackNode> = HashSet<StackNode>()
        val toProcess: Stack<StackNode> = Stack<StackNode>()
        toProcess.push(root)
        while (!toProcess.isEmpty()) {
            val curNode: StackNode = toProcess.pop()
            if (!processed.contains(curNode)) {
                nodeConsumer.accept(curNode)
                processed.add(curNode)
                if (curNode is RuleStackNode) {
                    toProcess.addAll((curNode as RuleStackNode).getArgs())
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
            fileInClassPath.split(filePathSeparatorRegex).takeLast(numOfDirsToSkip + 1).joinToString(separator = "/")
        )
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
        destFile.writeText(modifier.apply(content))
    }

    private fun createHtmlFile(
        version: String?, relPathToRoot: String, viewComponentName: String, viewProps: Any, file: File
    ) {
        val viewPropsStr: String = Utils.toJson(Utils.toJson(viewProps))
        val decompressionFunctionName =
            (if (viewProps is CompressedAssertionDto2) "decompressAssertionDto" else if (viewProps is CompressedIndexDto2) "decompressIndexDto" else null)
                ?: throw MetamathException("decompressionFunctionName == null")
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
            compress(assertionDto),
            File(dataDir, StringUtils.join(relPath, '/'))
        )
    }

    private fun getTypeStr(type: ListStatementType): String? {
        return if (type === ListStatementType.AXIOM) "Axiom" else if (type === ListStatementType.THEOREM) "Theorem" else type.name()
    }

    @JvmStatic
    fun main(args: Array<String>) {
        if (1 == 1) throw MetamathException("")
        if (1 != 1) {
            val files: MutableMap<String, MutableList<String>> = HashMap()
            val database: MetamathDatabase = MetamathParsers.load("D:\\Install\\metamath\\metamath\\set.mm")
            val allAssertions: List<ListStatement> = database.getAllAssertions()
            println("Len = 1: " + allAssertions.stream().filter(Predicate<ListStatement> { a: ListStatement ->
                a.getLabel().length() === 1
            }).count())
            allAssertions.stream().filter(Predicate<ListStatement> { a: ListStatement ->
                a.getLabel().length() === 1
            }).forEach(
                Consumer<ListStatement> { a: ListStatement -> System.out.println("- " + a.getLabel()) })
            for (assertion in allAssertions) {
                val relPath: String = Utils.toJson(getRelPath(assertion.getLabel()))
                files.computeIfAbsent(
                    relPath
                ) { p: String? -> ArrayList() }.add(assertion.getLabel().toString() + ".html")
            }
            files.entries.stream()
                .filter { (_, value): Map.Entry<String, List<String>> -> value.size > 50 }
                .sorted(
                    Comparator.comparing<Map.Entry<String, List<String>>, String>(
                        Function<Map.Entry<String, List<String>>, String> { (key, value) -> java.util.Map.Entry.key })
                )
                .forEach { (key, value): Map.Entry<String, List<String>> ->
                    println(
                        key + " = " + value.size
                    )
                }
        }
    }

    private fun getRelPath(label: String): List<String>? {
        var label = label
        label = StringUtils.trim(label)
        if (StringUtils.isBlank(label)) {
            throw MetamathException("StringUtils.isBlank(label)")
        }
        return if (label.length >= 6) {
            Arrays.asList(label.substring(0, 2), label.substring(2, 4), label.substring(4, 6))
        } else if (label.length >= 4) {
            Arrays.asList(label.substring(0, 2), label.substring(2, 4))
        } else if (label.length >= 2) {
            Arrays.asList(label.substring(0, 2))
        } else {
            listOf(label)
        }
    }

    protected fun createRelPathToSaveTo(label: String): List<String> {
        return Arrays.asList("asrt", "$label.html")
    }

    private fun replaceDots(str: String): String? {
        return str.replace(".", DOT_REPLACEMENT)
    }

    private fun extractVarTypes(context: MetamathContext, symbols: Set<String>): Map<String?, String?>? {
        return context.getSymbolsInfo().getVarTypes().entrySet().stream()
            .filter { entry -> symbols.contains(entry.getKey()) }
            .collect(Collectors.toMap(
                Function<T, K> { java.util.Map.Entry.key },
                Function<T, U> { entry: T -> entry.getValue().getSymbols().get(0) }
            ))
    }
}
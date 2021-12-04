package org.igye.common

import org.igye.metamathparser.MetamathParserException
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import kotlin.collections.HashSet

object DebugTimer {
    private val begins: MutableMap<String, MutableMap<String, Long>> = HashMap()
    private val accumulatedDurations: MutableMap<String, MutableMap<String, Long>> = HashMap()

    fun <T> run(label: String, callable: () -> T): T {
        return try {
            begin(label)
            callable.invoke()
        } finally {
            end(label)
        }
    }

    @Synchronized
    private fun begin(label: String) {
        val beginsForThisThread = begins.computeIfAbsent(Thread.currentThread().name) { HashMap() }
        beginsForThisThread[label] = System.nanoTime()
    }

    @Synchronized
    private fun end(label: String) {
        val now = System.nanoTime()
        val threadName = Thread.currentThread().name
        val durationMap = accumulatedDurations.computeIfAbsent(threadName) { HashMap() }
        durationMap[label] = durationMap.getOrDefault(label, 0) + (
            now - begins[threadName]!![label]!!
        )
    }

    fun getStats(): Map<String, Pair<Long, List<Long>>> {
        val accumulatedDurations = HashMap<String, Pair<Long, MutableList<Long>>>()
        for ((_, durationsForThread) in this.accumulatedDurations) {
            for ((label, durationForLabel) in durationsForThread.entries) {
                val accumulatedDurationForLabel = accumulatedDurations.computeIfAbsent(label) { Pair(0, ArrayList<Long>()) }
                accumulatedDurationForLabel.second.add(durationForLabel)
                accumulatedDurations[label] = Pair(
                    accumulatedDurationForLabel.first.plus(durationForLabel),
                    accumulatedDurationForLabel.second
                )
            }
        }
        return accumulatedDurations
    }

    fun getStatsStr(totalTimeLabel:String = "", grouping: Pair<String,List<Any>>? = null): String {
        val stats: Map<String, Pair<Long, List<Long>>> = getStats()
        if (grouping != null) {
            val totalNanos: Long? = stats[grouping.first]?.first
            val sb = StringBuilder()
            val stack = Stack<NodeInfo>()
            stack.push(NodeInfo(level = 0, label = grouping.first,children = grouping.second as List<Pair<String, List<Any>>>))
            val processedLabels = HashSet<String>()
            while (stack.isNotEmpty()) {
                val curr = stack.pop()
                sb.append("\n")
                if (curr.level > 1) {
                    sb.append("|   ".repeat((curr.level-1)))
                }
                if (curr.level > 0) {
                    if (!stack.isEmpty() && stack.peek().level == curr.level) {
                        sb.append("|")
                    } else {
                        sb.append("\\")
                    }
                    sb.append("---")
                }
                sb.append("${getPct(stats[curr.label]?.first, totalNanos)}${nanosToSeconds(stats[curr.label]?.first)}s ${curr.label}")
                processedLabels.add(curr.label)
                curr.children.asSequence()
                    .sortedBy { stats[it.first]?.first?:0 }
                    .forEach {
                        stack.push(NodeInfo(
                            level = curr.level+1,
                            label = it.first,
                            children = it.second as List<Pair<String, List<Any>>>
                        ))
                    }
            }
            val missingLabels = stats.keys.minus(processedLabels)
            if (missingLabels.isNotEmpty()) {
                throw MetamathParserException("Missing labels: " + missingLabels.joinToString(", ") + ".")
            }
            return sb.toString()
        } else {
            val totalNanos: Long? = stats[totalTimeLabel]?.first
            return stats.entries.asSequence()
                .sortedByDescending { it.key.first() }
                .map { (label, duration) ->
                    "$label - ${getPct(duration.first, totalNanos)}${nanosToSeconds(duration.first)}s" +
                            if (duration.second.size > 1) {
                                duration.second.asSequence()
                                    .map { nanosToSeconds(it) }
                                    .sortedDescending()
                                    .map(Objects::toString)
                                    .joinToString(separator = ", ", prefix = " [", postfix = "]")
                            } else {
                                ""
                            }
                }
                .joinToString("\n")
        }
    }

    private fun getPct(value:Long?, total:Long?): String {
        if (total == null) {
            return ""
        } else {
            return BigDecimal(value?:0).setScale(10)
                .divide(BigDecimal(total), RoundingMode.HALF_UP)
                .times(BigDecimal(100))
                .setScale(0, RoundingMode.HALF_UP).toString() + "% "
        }
    }

    private fun nanosToSeconds(nanos: Long?): BigDecimal {
        return BigDecimal(nanos?:0).setScale(10)
            .divide(BigDecimal(1000_000_000L), RoundingMode.HALF_UP)
            .setScale(3, RoundingMode.HALF_UP)
    }

    data class NodeInfo(
        val level: Int,
        val label: String,
        val children:List<Pair<String,List<Any>>>
    )
}

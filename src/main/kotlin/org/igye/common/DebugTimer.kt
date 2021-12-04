package org.igye.common


import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

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

    fun getStatsStr(totalTimeLabel:String = ""): String {
        val stats: Map<String, Pair<Long, List<Long>>> = getStats()
        val total: Long? = stats[totalTimeLabel]?.first
        return stats.entries.asSequence()
            .sortedByDescending { it.key.first() }
            .map { (label, duration) ->
                "$label - ${getPct(duration.first, total)}${nanosToSeconds(duration.first)}s" +
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

    private fun getPct(value:Long, total:Long?): String {
        if (total == null) {
            return ""
        } else {
            return BigDecimal(value).setScale(10)
                .divide(BigDecimal(total), RoundingMode.HALF_UP)
                .times(BigDecimal(100))
                .setScale(0, RoundingMode.HALF_UP).toString() + "% "
        }
    }

    private fun nanosToSeconds(nanos: Long): BigDecimal {
        return BigDecimal(nanos).setScale(10)
            .divide(BigDecimal(1000_000_000L), RoundingMode.HALF_UP)
            .setScale(3, RoundingMode.HALF_UP)
    }
}

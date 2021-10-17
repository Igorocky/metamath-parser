package org.igye.common


import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.stream.Collectors

object DebugTimer {
    private val begins: MutableMap<String, MutableMap<String, Instant>> = HashMap()
    private val accumulatedDurations: MutableMap<String, MutableMap<String, Duration>> = HashMap()

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
        begins.computeIfAbsent(Thread.currentThread().name) { HashMap() }[label] = Instant.now()
    }

    @Synchronized
    private fun end(label: String) {
        val now = Instant.now()
        val threadName = Thread.currentThread().name
        val durationMap = accumulatedDurations.computeIfAbsent(threadName) { HashMap() }
        durationMap[label] = durationMap.getOrDefault(label, Duration.ZERO).plus(
            Duration.between(
                begins[threadName]!![label],
                now
            )
        )
    }

    fun getStats(): String {
        val durationsMap = HashMap<String, Pair<Duration, List<Duration>>>()
        for ((_, value) in accumulatedDurations) {
            for ((label, value) in value.entries) {
                val durations = durationsMap.computeIfAbsent(
                    label,
                    { Pair(Duration.ZERO, ArrayList<Duration>()) }
                )
                durationsMap[label] = Pair(
                    durations.first.plus(value),
                    append(durations.second, value)
                )
            }
        }
        return durationsMap.entries.stream()
            .sorted(compareBy<MutableMap.MutableEntry<String, Pair<Duration, List<Duration>>>?>({ it!!.value.first }).reversed())
            .map { (key, value): Map.Entry<String, Pair<Duration, List<Duration>>> ->
                (key + " - " + millisToSeconds(value.first.toMillis())
                        + if (value.second.size > 1) value.second.asSequence()
                    .map { millisToSeconds(it.toMillis()) }
                    .sortedDescending()
                    .map(Objects::toString)
                    .joinToString(separator = ", ", prefix = " [", postfix = "]") else "")
            }
            .collect(Collectors.joining("\n"))
    }

    private fun millisToSeconds(millis: Long): BigDecimal = BigDecimal(millis).divide(BigDecimal(1000L), RoundingMode.HALF_UP).setScale(3)

    private fun <T> append(list: List<T>, elem: T): List<T> {
        val result = ArrayList(list)
        result.add(elem)
        return result
    }
}

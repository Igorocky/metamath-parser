package org.igye.common

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import java.util.concurrent.atomic.AtomicLong

class DebugTimer2(val name: String) {
    private val duration = AtomicLong()
    private val cnt = AtomicLong()

    fun <T> run(callable: () -> T): T {
        val begin = System.nanoTime()
        try {
            return callable.invoke()
        } finally {
            val end = System.nanoTime()
            duration.addAndGet(end-begin)
            cnt.incrementAndGet()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DebugTimer2
        return name == other.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    companion object {
        val total = DebugTimer2("total")
        val loadMetamathFile = DebugTimer2("loadMetamathFile")
        val prove = DebugTimer2("prove")
        val updateDist = DebugTimer2("updateDist")
        val findConstant = DebugTimer2("findConstant")
        val findMatchingAssertions = DebugTimer2("findMatchingAssertions")
        val iterateSubstitutions = DebugTimer2("iterateSubstitutions")
        val markDependantsAsProved = DebugTimer2("markDependantsAsProved")
        val createProvableAssertion = DebugTimer2("createProvableAssertion")

        val timers: Pair<DebugTimer2,List<Any>> = total to listOf(
            loadMetamathFile to emptyList<Any>(),
            prove to listOf(
                updateDist to emptyList<Any>(),
                findConstant to emptyList(),
                findMatchingAssertions to listOf(
                    iterateSubstitutions to emptyList<Any>(),
                ),
                markDependantsAsProved to emptyList(),
            ),
            createProvableAssertion to emptyList(),
        )

        fun getStatsStr(timers: Pair<DebugTimer2,List<Any>>): String {
            val totalNanos: Long = timers.first.duration.get()
            val sb = StringBuilder()
            val stack = Stack<NodeInfo>()
            stack.push(NodeInfo(level = 0, timer = timers.first, children = timers.second as List<Pair<DebugTimer2, List<Any>>>))
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
                sb.append("${getPct(curr.timer.duration.get(), totalNanos)}${nanosToSeconds(curr.timer.duration.get())}s ${curr.timer.name}[${curr.timer.cnt.get()}]")
                curr.children.asSequence()
                    .sortedBy { it.first.duration.get() }
                    .forEach {
                        stack.push(NodeInfo(
                            level = curr.level+1,
                            timer = it.first,
                            children = it.second as List<Pair<DebugTimer2, List<Any>>>
                        ))
                    }
            }
            return sb.toString()
        }

        private fun getPct(value:Long, total:Long): String {
            if (total == 0L) {
                return "0% "
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

    data class NodeInfo(
        val level: Int,
        val timer: DebugTimer2,
        val children:List<Pair<DebugTimer2,List<Any>>>
    )
}

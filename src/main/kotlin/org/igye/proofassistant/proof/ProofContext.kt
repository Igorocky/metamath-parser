package org.igye.proofassistant.proof

import org.igye.proofassistant.proof.ProofNodeState.*
import org.igye.proofassistant.proof.prooftree.*
import java.lang.Integer.min
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class ProofContext(val target: PendingProofNode) {
    private val newStatements: MutableMap<Stmt, PendingProofNode> = HashMap()
    private val waitingStatements: MutableMap<Stmt, PendingProofNode> = HashMap()
    private val provedStatements: MutableMap<Stmt, ProofNode> = HashMap()

    init {
        addNewStatement(target)
    }

    fun hasNewStatements(): Boolean = newStatements.isNotEmpty()

    fun getNextStatementToProve(): PendingProofNode {
        updateDist(target)
        val minByOrNull = newStatements.values.asSequence()
            .filter { it.dist >= 0 && it.dist < Int.MAX_VALUE }
            .minByOrNull { it.dist }
        if (minByOrNull == null) {
            throw AssumptionDoesntHoldException()
        }
        return minByOrNull!!
    }

    fun getProved(stmt: Stmt): ProofNode? = provedStatements[stmt]

    fun getWaiting(stmt: Stmt): ProofNode? = waitingStatements[stmt]

    fun getNew(stmt: Stmt): ProofNode? = newStatements[stmt]

    fun addNewStatement(node: PendingProofNode) {
        if (newStatements.put(node.stmt, node) != null) {
            throw AssumptionDoesntHoldException()
        }
        if (waitingStatements.contains(node.stmt)) {
            throw AssumptionDoesntHoldException()
        }
        if (provedStatements.contains(node.stmt)) {
            throw AssumptionDoesntHoldException()
        }
        node.state = NEW
    }

    fun markWaiting(node: PendingProofNode) {
        val new = newStatements.remove(node.stmt)
        if (new !== null && new !== node) {
            throw AssumptionDoesntHoldException()
        }
        val prevWaitingNode = waitingStatements.put(node.stmt, node)
        if (prevWaitingNode != null) {
            throw AssumptionDoesntHoldException()
        }
        if (provedStatements.contains(node.stmt)) {
            throw AssumptionDoesntHoldException()
        }
        node.state = WAITING
    }

    fun markProved(node: ProofNode) {
        if (!(node is ConstProofNode || node is CalcProofNode)) {
            throw AssumptionDoesntHoldException()
        }
        val new = newStatements.remove(node.stmt)
        if (new !== null && new !== node) {
            throw AssumptionDoesntHoldException()
        }
        val waiting = waitingStatements.remove(node.stmt)
        if (waiting !== null && waiting !== node) {
            throw AssumptionDoesntHoldException()
        }
        if (provedStatements.put(node.stmt, node) != null) {
            throw AssumptionDoesntHoldException()
        }
        node.state = PROVED
    }

    fun remove(node: ProofNode) {
        if (newStatements.remove(node.stmt) !== node && waitingStatements.remove(node.stmt) !== node) {
            throw AssumptionDoesntHoldException()
        }
        node.state = REMOVED
    }

    fun proofFoundForNodeToBeProved(nodeToBeProved: PendingProofNode, foundProof: ProofNode) {
        replacePendingNodeWithItsProof(pendingNode = nodeToBeProved, proofArg = foundProof)
        markDependantsAsProved(foundProof)
    }

    private fun replacePendingNodeWithItsProof(pendingNode: PendingProofNode, proofArg: ProofNode? = null): ProofNode {
        remove(pendingNode)
        var proof: ProofNode? = proofArg
        for (p in pendingNode.proofs) {
            if (p.state == PROVED || p === proofArg) {
                if (proof == null) {
                    proof = p
                } else if (p !== proofArg) {
                    throw AssumptionDoesntHoldException()
                }
            } else {
                p.removeDependantIf { true }
                for (a in p.args) {
                    a.removeDependantIf { it === p }
                }
            }
        }
        if (proof == null) {
            throw AssumptionDoesntHoldException()
        }
        if (!pendingNode.stmt.value.contentEquals(proof.stmt.value)) {
            throw AssumptionDoesntHoldException()
        }
        proof.removeDependantIf { it === pendingNode }
        proof.addDependants(pendingNode.getDependants())
        for (dependantOfPendingNode in pendingNode.getDependants()) {
            if (dependantOfPendingNode is CalcProofNode) {
                var replaceCnt = 0
                var argIdx = dependantOfPendingNode.args.indexOfFirst { it === pendingNode }
                while (argIdx >= 0) {
                    dependantOfPendingNode.args.removeAt(argIdx)
                    dependantOfPendingNode.args.add(argIdx, proof)
                    replaceCnt++
                    argIdx = dependantOfPendingNode.args.indexOfFirst { it === pendingNode }
                }
                if (replaceCnt < 1) {
                    throw AssumptionDoesntHoldException()
                }
            } else {
                throw AssumptionDoesntHoldException()
            }
        }
        pendingNode.removeDependantIf { true }
        if (proof.state != PROVED) {
            markProved(proof)
        }
        return proof
    }

    private fun markDependantsAsProved(provedNode: ProofNode) {
        if (!(provedNode is CalcProofNode || provedNode is ConstProofNode)) {
            throw AssumptionDoesntHoldException()
        }
        val toBeMarkedAsProved: Stack<ProofNode> = Stack()
        provedNode.getDependants().forEach {
            if (it.state == PROVED) {
                throw AssumptionDoesntHoldException()
            }
            toBeMarkedAsProved.push(it)
        }
        while (toBeMarkedAsProved.isNotEmpty()) {
            val currNode = toBeMarkedAsProved.pop()
            if (currNode.state == PROVED) {
                continue
            }
            val newNodesToBeMarkedAsProved: List<ProofNode> = when (currNode) {
                is PendingProofNode -> {
                    replacePendingNodeWithItsProof(pendingNode = currNode).getDependants()
                }
                is CalcProofNode -> {
                    if (currNode.args.all { it.state == PROVED }) {
                        val directDependantsOfCurrNode = ArrayList(currNode.getDependants())
                        for (directDependant in directDependantsOfCurrNode) {
                            if (directDependant is PendingProofNode) {
                                replacePendingNodeWithItsProof(pendingNode = directDependant, proofArg = currNode)
                            } else {
                                throw AssumptionDoesntHoldException()
                            }
                        }
                        currNode.getDependants()
                    } else {
                        emptyList()
                    }
                }
                else -> throw AssumptionDoesntHoldException()
            }
            newNodesToBeMarkedAsProved.forEach { toBeMarkedAsProved.push(it) }
        }
    }

    private fun updateDist(root: PendingProofNode) {
        newStatements.values.forEach { it.dist = -1 }
        waitingStatements.values.forEach {
            it.dist = -1
            it.proofs.forEach { it.dist = -1 }
        }
        provedStatements.values.forEach { it.dist = -1 }
        root.dist = 0
        val nodesToUpdateDist = LinkedList<ProofNode>()
        nodesToUpdateDist.addAll(root.proofs)
        while (nodesToUpdateDist.isNotEmpty()) {
            val currNode = nodesToUpdateDist.removeFirst()
            if (currNode.dist >= 0) {
                continue
            }
            var newDist = Int.MAX_VALUE
            for (dep in currNode.getDependants()) {
                newDist = min(newDist, if (dep.dist < 0) Int.MAX_VALUE else dep.dist)
            }
            currNode.dist = if (newDist == Int.MAX_VALUE) Int.MAX_VALUE else newDist + 1
            nodesToUpdateDist.addAll(
                when (currNode) {
                    is CalcProofNode -> currNode.args
                    is PendingProofNode -> currNode.proofs
                    is ConstProofNode -> emptyList()
                }
            )
        }
    }
}
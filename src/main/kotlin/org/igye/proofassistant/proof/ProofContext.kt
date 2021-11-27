package org.igye.proofassistant.proof

import org.igye.proofassistant.proof.ProofNodeState.*
import org.igye.proofassistant.proof.prooftree.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class ProofContext {
    private val statementsToProve: MutableMap<Stmt, PendingProofNode> = HashMap()
    private val waitingStatements: MutableMap<Stmt, ProofNode> = HashMap()
    private val provedStatements: MutableMap<Stmt, ProofNode> = HashMap()

    fun hasStatementsToProve(): Boolean = statementsToProve.isNotEmpty()

    fun getNextStatementToProve(): PendingProofNode = statementsToProve.iterator().next().value

    fun isProved(stmt: Stmt): ProofNode? = provedStatements[stmt]

    fun isWaiting(stmt: Stmt): ProofNode? = waitingStatements[stmt]

    fun isToBeProved(stmt: Stmt): ProofNode? = statementsToProve[stmt]

    fun addStatementToProve(node: PendingProofNode) {
        node.state = TO_BE_PROVED
        if (waitingStatements.contains(node.stmt)) {
            throw AssumptionDoesntHoldException()
        }
        if (provedStatements.contains(node.stmt)) {
            throw AssumptionDoesntHoldException()
        }
        if (statementsToProve.put(node.stmt, node) != null) {
            throw AssumptionDoesntHoldException()
        }
    }

    fun markWaiting(node: ProofNode) {
        if (!(node is CalcProofNode || node is PendingProofNode)) {
            throw AssumptionDoesntHoldException()
        }
        node.state = WAITING
        val toBeProved = statementsToProve.remove(node.stmt)
        if (toBeProved !== null && toBeProved !== node) {
            throw AssumptionDoesntHoldException()
        }
        val prevWaitingNode = waitingStatements.put(node.stmt, node)
        if (prevWaitingNode != null) {
            throw AssumptionDoesntHoldException()
        }
        if (provedStatements.contains(node.stmt)) {
            throw AssumptionDoesntHoldException()
        }
    }

    fun markProved(node: ProofNode) {
        if (!(node is ConstProofNode || node is CalcProofNode)) {
            throw AssumptionDoesntHoldException()
        }
        node.state = PROVED
        val toBeProved = statementsToProve.remove(node.stmt)
        if (toBeProved !== null && toBeProved !== node) {
            throw AssumptionDoesntHoldException()
        }
        val waiting = waitingStatements.remove(node.stmt)
        if (waiting !== null && waiting !== node && !(waiting is PendingProofNode && waiting.proofs.any { it === node })) {
            throw AssumptionDoesntHoldException()
        }
        if (provedStatements.put(node.stmt, node) != null) {
            throw AssumptionDoesntHoldException()
        }
    }

    fun cancel(node: ProofNode) {
        if (node is ConstProofNode || node.state == PROVED) {
            return
        }
        node.state = CANCELLED
        val toBeProved = statementsToProve.remove(node.stmt)
        if (toBeProved !== null && toBeProved !== node) {
            throw AssumptionDoesntHoldException()
        }
        val waiting = waitingStatements.remove(node.stmt)
        if (waiting !== null && waiting !== node) {
            throw AssumptionDoesntHoldException()
        }
    }

    fun replacePendingNodeWithConstNode(pendingNode: PendingProofNode, constProofNode: ConstProofNode) {
        cancel(pendingNode)
        constProofNode.parents.addAll(pendingNode.parents)
        markParentsAsProved(constProofNode)
    }

    fun markParentsAsProved(provedNode: ProofNode) {
        val toBeMarkedAsProved: Stack<ProofNode> = Stack()
        provedNode.parents.forEach { if (it.state != PROVED) toBeMarkedAsProved.push(it) }
        while (toBeMarkedAsProved.isNotEmpty()) {
            val currNode = toBeMarkedAsProved.pop()
            if (currNode.state == PROVED) {
                throw AssumptionDoesntHoldException()
            }
            val newNodesToBeMarkedAsProved: List<ProofNode> = when (currNode) {
                is CalcProofNode -> {
                    markProved(currNode)
                    currNode.parents
                }
                is PendingProofNode -> {
                    cancel(currNode)
                    currNode.proofs.forEach { if (it.state != PROVED) cancelWithAllChildren(it) }
                    var proof: ProofNode? = null
                    for (p in currNode.proofs) {
                        if (p.state == PROVED) {
                            if (proof == null) {
                                proof = p
                            } else {
                                throw AssumptionDoesntHoldException()
                            }
                        }
                    }
                    if (proof == null) {
                        throw AssumptionDoesntHoldException()
                    }
                    proof.parents.removeIf { it === currNode }
                    proof.parents.addAll(currNode.parents)
                    for (parent in currNode.parents) {
                        if (parent is CalcProofNode) {
                            parent.args.removeIf { it === currNode }
                            parent.args.add(proof)
                        } else {
                            throw AssumptionDoesntHoldException()
                        }
                    }
                    currNode.parents
                }
                is CalcProofNode -> {
                    if (currNode.args.all { it.state == PROVED }) {
                        markProved(currNode)
                        currNode.parents
                    } else {
                        emptyList()
                    }
                }
                else -> throw AssumptionDoesntHoldException()
            }
            newNodesToBeMarkedAsProved.forEach { toBeMarkedAsProved.push(it) }
        }
    }

    private fun cancelWithAllChildren(node: ProofNode) {
        val rootsToStartCancellingFrom = ArrayList<ProofNode>()
        rootsToStartCancellingFrom.add(node)
        while (rootsToStartCancellingFrom.isNotEmpty()) {
            val currNode = rootsToStartCancellingFrom.removeLast()
            cancel(currNode)
            rootsToStartCancellingFrom.addAll(when (currNode) {
                is ConstProofNode -> emptyList()
                is CalcProofNode -> currNode.args
                is PendingProofNode -> currNode.proofs
            })
        }
    }
}
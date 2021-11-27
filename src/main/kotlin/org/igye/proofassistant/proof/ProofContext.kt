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

    fun getProved(stmt: Stmt): ProofNode? = provedStatements[stmt]

    fun getWaiting(stmt: Stmt): ProofNode? = waitingStatements[stmt]

    fun getToBeProved(stmt: Stmt): ProofNode? = statementsToProve[stmt]

    fun addStatementToProve(node: PendingProofNode) {
        if (statementsToProve.put(node.stmt, node) != null) {
            throw AssumptionDoesntHoldException()
        }
        if (waitingStatements.contains(node.stmt)) {
            throw AssumptionDoesntHoldException()
        }
        if (provedStatements.contains(node.stmt)) {
            throw AssumptionDoesntHoldException()
        }
        node.state = TO_BE_PROVED
    }

    fun markWaiting(node: ProofNode) {
        if (!(node is CalcProofNode || node is PendingProofNode)) {
            throw AssumptionDoesntHoldException()
        }
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
        node.state = WAITING
    }

    fun markProved(node: ProofNode) {
        if (!(node is ConstProofNode || node is CalcProofNode)) {
            throw AssumptionDoesntHoldException()
        }
        val toBeProved = statementsToProve.remove(node.stmt)
        if (toBeProved !== null && toBeProved !== node) {
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

    private fun cancel(node: ProofNode) {
        if (node is ConstProofNode || node.state == PROVED) {
            return
        }
        val toBeProved = statementsToProve.remove(node.stmt)
        if (toBeProved !== null && toBeProved !== node) {
            throw AssumptionDoesntHoldException()
        }
        val waiting = waitingStatements.remove(node.stmt)
        if (waiting !== null && waiting !== node) {
            throw AssumptionDoesntHoldException()
        }
        node.state = CANCELLED
    }

    fun proofFoundForNodeToBeProved(nodeToBeProved: PendingProofNode, foundProof: ProofNode) {
        for (dependant in nodeToBeProved.dependants) {
            if (dependant is CalcProofNode) {
                dependant.args.removeIf { it == nodeToBeProved }
                dependant.args.add(foundProof)
            } else {
                throw AssumptionDoesntHoldException()
            }
        }
        cancel(nodeToBeProved)
        markProved(foundProof)
        foundProof.dependants.addAll(nodeToBeProved.dependants)
        markDependantsAsProved(foundProof)
    }

    private fun markDependantsAsProved(provedNode: ProofNode) {
        val toBeMarkedAsProved: Stack<ProofNode> = Stack()
        provedNode.dependants.forEach {
            if (it.state == PROVED) {
                throw AssumptionDoesntHoldException()
            }
            toBeMarkedAsProved.push(it)
        }
        while (toBeMarkedAsProved.isNotEmpty()) {
            val currNode = toBeMarkedAsProved.pop()
            if (currNode.state == PROVED) {
                throw AssumptionDoesntHoldException()
            }
            val newNodesToBeMarkedAsProved: List<ProofNode> = when (currNode) {
                is PendingProofNode -> {
                    cancel(currNode)
                    var proof: ProofNode? = null
                    for (p in currNode.proofs) {
                        if (p.state == PROVED) {
                            if (proof == null) {
                                proof = p
                            } else {
                                throw AssumptionDoesntHoldException()
                            }
                        } else {
                            cancelWithAllChildren(p)
                        }
                    }
                    if (proof == null) {
                        throw AssumptionDoesntHoldException()
                    }
                    proof.dependants.removeIf { it === currNode }
                    proof.dependants.addAll(currNode.dependants)
                    for (dependant in currNode.dependants) {
                        if (dependant is CalcProofNode) {
                            if (!dependant.args.removeIf { it === currNode }) {
                                throw AssumptionDoesntHoldException()
                            }
                            dependant.args.add(proof)
                        } else {
                            throw AssumptionDoesntHoldException()
                        }
                    }
                    currNode.dependants
                }
                is CalcProofNode -> {
                    if (currNode.args.all { it.state == PROVED }) {
                        for (dependant in currNode.dependants) {
                            if (dependant is PendingProofNode) {
                                //all the dependants will be processed on the next step
                                cancel(dependant)
                            } else {
                                throw AssumptionDoesntHoldException()
                            }
                        }
                        markProved(currNode)
                        currNode.dependants
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
        if (node is ConstProofNode) {
            return
        }
        val rootsToStartCancellingFrom = ArrayList<ProofNode>()
        rootsToStartCancellingFrom.add(node)
        while (rootsToStartCancellingFrom.isNotEmpty()) {
            val currNode = rootsToStartCancellingFrom.removeLast()
            cancel(currNode)
            rootsToStartCancellingFrom.addAll(when (currNode) {
                is CalcProofNode -> currNode.args
                is PendingProofNode -> currNode.proofs
                is ConstProofNode -> emptyList()
            })
        }
    }
}
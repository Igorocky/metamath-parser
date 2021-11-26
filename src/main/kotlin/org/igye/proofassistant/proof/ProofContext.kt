package org.igye.proofassistant.proof

import org.igye.proofassistant.ProofAssistant
import org.igye.proofassistant.proof.ProofNodeState.*

class ProofContext {
    private val statementsToProve: MutableMap<Stmt, ProofNode> = HashMap()
    private val waitingStatements: MutableMap<Stmt, ProofNode> = HashMap()
    private val provedStatements: MutableMap<Stmt, ProofNode> = HashMap()

    fun hasStatementsToProve(): Boolean = statementsToProve.isNotEmpty()

    fun getNextStatementToProve(): ProofNode = statementsToProve.iterator().next().value

    fun addStatementToProve(node: ProofNode) {
        if (!(node is PendingProofNode || node is ValProofNode)) {
            throw AssumptionDoesntHoldException()
        }
        if (node.state != TO_BE_PROVED) {
            throw AssumptionDoesntHoldException()
        }
        if (statementsToProve.put(node.stmt, node) != null) {
            throw AssumptionDoesntHoldException()
        }
        if (waitingStatements.contains(node.stmt)) {
            throw AssumptionDoesntHoldException()
        }
        if (provedStatements.contains(node.stmt)) {
            throw AssumptionDoesntHoldException()
        }
    }

    fun saveConstant(node: ConstProofNode) {
        if (provedStatements.put(node.stmt, node) != null) {
            throw AssumptionDoesntHoldException()
        }
    }

    fun markAsWaiting(node: ProofNode) {
        if (!(node is CalcProofNode || node is ValProofNode || node is PendingProofNode)) {
            throw AssumptionDoesntHoldException()
        }
        if (waitingStatements.put(node.stmt, node) != null) {
            throw AssumptionDoesntHoldException()
        }
        if (node.state == TO_BE_PROVED) {
            if (statementsToProve.remove(node.stmt) != node) {
                throw AssumptionDoesntHoldException()
            }
            node.state == WAITING
        } else {
            throw AssumptionDoesntHoldException()
        }
    }

    fun markAsProved(node: ProofNode) {
        if (!(node is CalcProofNode || node is ValProofNode)) {
            throw AssumptionDoesntHoldException()
        }
        if (provedStatements.put(node.stmt, node) != null) {
            throw AssumptionDoesntHoldException()
        }
        if (node.state == TO_BE_PROVED) {
            if (statementsToProve.remove(node.stmt) != node) {
                throw AssumptionDoesntHoldException()
            }
            node.state == PROVED
        } else if (node.state == WAITING) {
            if (waitingStatements.remove(node.stmt) != node) {
                throw AssumptionDoesntHoldException()
            }
            node.state == PROVED
        } else {
            throw AssumptionDoesntHoldException()
        }
    }

    fun cancel(node: ProofNode) {
        if (!(node is CalcProofNode || node is ValProofNode || node is PendingProofNode)) {
            throw AssumptionDoesntHoldException()
        }
        if (node.state == TO_BE_PROVED) {
            if (statementsToProve.remove(node.stmt) != node) {
                throw AssumptionDoesntHoldException()
            }
            node.state == CANCELLED
        } else if (node.state == WAITING) {
            if (waitingStatements.remove(node.stmt) != node) {
                throw AssumptionDoesntHoldException()
            }
            node.state == CANCELLED
        } else {
            throw AssumptionDoesntHoldException()
        }
    }

    fun constProofFound(nodeToProve: ProofNode, constProof: ConstProofNode) {
        if (nodeToProve.state != TO_BE_PROVED) {
            throw AssumptionDoesntHoldException()
        }
        if (nodeToProve is PendingProofNode) {
            val parent = nodeToProve.parent
            if (parent is CalcProofNode) {
                parent.args.removeIf { it === nodeToProve }
                parent.args.add(constProof)
            } else if (parent is ValProofNode) {
                parent.proof = constProof
            } else {
                throw AssumptionDoesntHoldException()
            }
        } else if (nodeToProve is ValProofNode) {

        } else {
            throw AssumptionDoesntHoldException()
        }
        ProofAssistant.markProved(constProof)
    }

    private fun markParentsAsProved(provedNode: ProofNode) {
        if (provedNode.state != PROVED) {
            throw AssumptionDoesntHoldException()
        }
        var toBeMarkedAsProved: ProofNode? = if (provedNode is ConstProofNode || (provedNode is CalcProofNode && provedNode.args.isEmpty())) {
            provedNode.proofLength = 0
            provedNode.parent
        } else {
            throw AssumptionDoesntHoldException()
        }
        var child = provedNode
        var proofLength = 1
        while (toBeMarkedAsProved != null) {
            if (toBeMarkedAsProved is PendingProofNode) {
                child.parent = toBeMarkedAsProved.parent
                toBeMarkedAsProved.proofs.removeIf { it !== child }
                cancelWithAllChildren(toBeMarkedAsProved)
                val parent = toBeMarkedAsProved.parent
                if (parent != null) {
                    if (parent is CalcProofNode) {

                    }
                }
                toBeMarkedAsProved = parent
            }
            toBeMarkedAsProved.proofLength = proofLength
            markAsProved(toBeMarkedAsProved)
            if (toBeMarkedAsProved.parent != null) {
                val parent = toBeMarkedAsProved.parent!!
                if (parent is CalcProofNode) {
                    if (parent.args.all { it.state == PROVED }) {
                        parent.proofLength = parent.args.asSequence().map { it.proofLength }.sum() + 1
                        markAsProved(parent)
                        toBeMarkedAsProved = parent.parent
                        proofLength = parent.proofLength+1
                    } else {
                        break
                    }
                } else if (parent is PendingProofNode) {
                    toBeMarkedAsProved.parent = parent.parent
                } else {
                    throw AssumptionDoesntHoldException()
                }
            } else {
                break
            }
        }
    }

    private fun cancelWithAllChildren(node: ProofNode) {
        TODO()
    }
}
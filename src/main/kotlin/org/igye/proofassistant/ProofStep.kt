package org.igye.proofassistant

import org.igye.metamathparser.MetamathContext
import org.igye.metamathparser.StackNode
import org.igye.metamathparser.Statement

data class ProofStep(
    val ctx: MetamathContext,
    val parent: ProofStep? = null,
    val provedStatements: List<StackNode>,
    val statementsToProve: List<Statement>,
)
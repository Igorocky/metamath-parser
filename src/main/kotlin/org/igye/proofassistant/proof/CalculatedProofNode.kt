package org.igye.proofassistant.proof

import org.igye.common.MetamathUtils
import org.igye.metamathparser.Assertion

class CalculatedProofNode(
    val args: List<VarProofNode>,
    val substitution: List<IntArray>,
    val assertion: Assertion,
    val result: InstVarProofNode
):ProofNode(stmt = Stmt(value = result.stmt.value, valueStr = MetamathUtils.toString(assertion)))
package org.igye.proofassistant

import org.igye.metamathparser.Statement
import org.igye.proofassistant.substitutions.ConstParts
import org.igye.proofassistant.substitutions.VarGroup

data class AsrtArg(
    val stmt: Statement,
    val numberOfUniqueVars: Int,
    val constParts: ConstParts,
    val matchingConstParts: ConstParts,
    val varGroups: MutableList<VarGroup>,
)

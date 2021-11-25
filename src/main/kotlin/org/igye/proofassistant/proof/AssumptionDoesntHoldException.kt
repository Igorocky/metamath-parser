package org.igye.proofassistant.proof

import org.igye.metamathparser.MetamathParserException

class AssumptionDoesntHoldException(msg:String):MetamathParserException(msg) {
    constructor():this("")
}
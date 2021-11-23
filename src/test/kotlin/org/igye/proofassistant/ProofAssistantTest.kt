package org.igye.proofassistant

import org.igye.metamathparser.Parsers
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.File

internal class ProofAssistantTest {
    @Test
    fun prove_proves_simple_wffs() {
        //given
        val ctx = Parsers.parseMetamathFile(File("C:\\igye\\books\\metamath/set.mm"))

        //then
        assertNotNull(ProofAssistant.prove("wff ( y e. NN -> y e. CC )", ctx))
    }
}
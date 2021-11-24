package org.igye.proofassistant

import org.igye.common.MetamathUtils
import org.igye.metamathparser.Parsers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.File

internal class ProofAssistantTest {
    @Test
    fun prove_proves_simple_wffs() {
        //given
        val ctx = Parsers.parseMetamathFile(File("C:\\igye\\books\\metamath/set.mm"))

        //when/then
        val prove = ProofAssistant.prove("wff ( x + y ) = ( x + y )", ctx)
        println(MetamathUtils.toJson(prove))
        assertNotNull(prove)
    }

    @Test
    fun createProvableAssertion_creates_provable_assertion_verifiable_by_metamathExe() {
        //given
        val ctx = Parsers.parseMetamathFile(File("C:\\igye\\books\\metamath/set.mm"))
        val proof = ProofAssistant.prove("wff ( y e. NN -> y e. CC )", ctx)

        //when
        val provableAssertion = ProofAssistant.createProvableAssertion(proof, ctx)

        //then
        assertEquals("\$p wff ( y e. NN -> y e. CC ) \$= ( wi wcel cc cv cn ) AEFCAEDCB \$.", provableAssertion)
    }
}
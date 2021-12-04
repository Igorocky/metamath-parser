package org.igye.proofassistant

import org.igye.common.DebugTimer2
import org.igye.common.MetamathUtils
import org.igye.metamathparser.MetamathContext
import org.igye.metamathparser.Parsers
import org.igye.proofassistant.proof.ProofNodeState.PROVED
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

internal class ProofAssistantTest {
    @Test
    fun intToStr_produces_correct_values() {
//        for (i in 1 .. 130) {
//            println("$i = ${ProofAssistant.intToStr(i)}")
//        }
        assertEquals("A", ProofAssistant.intToStr(1))
        assertEquals("B", ProofAssistant.intToStr(2))
        assertEquals("C", ProofAssistant.intToStr(3))
        assertEquals("T", ProofAssistant.intToStr(20))
        assertEquals("UA", ProofAssistant.intToStr(21))
        assertEquals("UB", ProofAssistant.intToStr(22))
        assertEquals("UT", ProofAssistant.intToStr(40))
        assertEquals("VA", ProofAssistant.intToStr(41))
        assertEquals("VB", ProofAssistant.intToStr(42))
        assertEquals("YT", ProofAssistant.intToStr(120))
        assertEquals("UUA", ProofAssistant.intToStr(121))
        assertEquals("YYT", ProofAssistant.intToStr(620))
        assertEquals("UUUA", ProofAssistant.intToStr(621))
    }

    @Test
    fun prove_proves_simple_wffs() {
        //given
        val ctx = Parsers.parseMetamathFile(File("C:\\igye\\books\\metamath/set.mm"))

        //when/then
        val proof = ProofAssistant.prove("wff ( y e. NN -> y e. CC )", ctx)
        println(MetamathUtils.toJson(proof))
        assertEquals(PROVED, proof.state)
    }

    @Test
    fun createProvableAssertion_creates_provable_assertion_verifiable_by_metamathExe() {
        DebugTimer2.total.run {
            val ctx = DebugTimer2.loadMetamathFile.run {
                val ctx = Parsers.parseMetamathFile(File("C:\\igye\\books\\metamath/set.mm"))
                ProofAssistant.initProofAssistantData(ctx)
                ctx
            }
            testCompressedProof(
                expr = "wff ( y e. NN -> y e. CC )",
                expectedProof = "\$p wff ( y e. NN -> y e. CC ) \$= ( cv cn wcel cc wi ) ABZCDGEDF \$.",
                ctx = ctx
            )
            testCompressedProof(
                expr = "wff ( ( y / 2 ) e. NN -> ( A. z e. NN ( z < y -> A. x e. ZZ ( sqrt ` 2 ) =/= ( x / z ) ) -> ( ( y / 2 ) < y -> A. x e. ZZ ( sqrt ` 2 ) =/= ( x / ( y / 2 ) ) ) ) )",
                expectedProof = "\$p wff ( ( y / 2 ) e. NN -> ( A. z e. NN ( z < y -> A. x e. ZZ ( sqrt ` 2 ) =/= ( x / z ) ) -> ( ( y / 2 ) < y -> A. x e. ZZ ( sqrt ` 2 ) =/= ( x / ( y / 2 ) ) ) ) ) \$= ( cv c2 cdiv co cn wcel clt wbr csqrt cfv wne cz wral wi ) BDZEZFZGZHZICDZRJZKSLMZADZUCTGNAOZPQCUBPUARUDKUEUFUATGNAUGPQQQ \$.",
                ctx = ctx
            )
            testCompressedProof(
                expr = "wff ( ( A = B /\\ C = D ) -> ( A X. C ) = ( B X. D ) )\n",
                expectedProof = "\$p wff ( ( A = B /\\ C = D ) -> ( A X. C ) = ( B X. D ) ) \$= ( wceq wa cxp wi ) ABECDEFACGBDGEH \$.",
                ctx = ctx
            )
            testCompressedProof(
                expr = "wff ( ( k e. ( 0 ... N ) |-> ( n e. ( 1 ... N ) |-> C ) ) : ( 0 ... N ) --> ( QQ ^m ( 1 ... N ) ) -> ( ( k e. ( 0 ... N ) |-> ( n e. ( 1 ... N ) |-> C ) ) LIndF ( ( CCfld |`s QQ ) freeLMod ( 1 ... N ) ) <-> A. w e. ( QQ ^m ( 0 ... N ) ) ( ( ( ( CCfld |`s QQ ) freeLMod ( 1 ... N ) ) gsum ( w oF ( .s ` ( ( CCfld |`s QQ ) freeLMod ( 1 ... N ) ) ) ( k e. ( 0 ... N ) |-> ( n e. ( 1 ... N ) |-> C ) ) ) ) = ( ( 1 ... N ) X. { 0 } ) -> w = ( ( 0 ... N ) X. { 0 } ) ) ) )",
                expectedProof = "\$p wff ( ( k e. ( 0 ... N ) |-> ( n e. ( 1 ... N ) |-> C ) ) : ( 0 ... N ) --> ( QQ ^m ( 1 ... N ) ) -> ( ( k e. ( 0 ... N ) |-> ( n e. ( 1 ... N ) |-> C ) ) LIndF ( ( CCfld |`s QQ ) freeLMod ( 1 ... N ) ) <-> A. w e. ( QQ ^m ( 0 ... N ) ) ( ( ( ( CCfld |`s QQ ) freeLMod ( 1 ... N ) ) gsum ( w oF ( .s ` ( ( CCfld |`s QQ ) freeLMod ( 1 ... N ) ) ) ( k e. ( 0 ... N ) |-> ( n e. ( 1 ... N ) |-> C ) ) ) ) = ( ( 1 ... N ) X. { 0 } ) -> w = ( ( 0 ... N ) X. { 0 } ) ) ) ) \$= ( cc0 cfz co cq c1 cmap cmpt wf ccnfld cress cfrlm clindf wbr cv cvsca cfv cof cgsu csn cxp wceq wi wral wb ) FZEGZHZIZJEUKHZKZHCULDUNBLLZMUPNUMOHUNPHZQRUQASZUPUQTUAUBHUCHUNUJUDZUEUFURULUSUEUFUGAUMULUOHUHUIUG \$.",
                ctx = ctx
            )
            testCompressedProof(
                expr = "wff ( A. z e. NN ( ( z < y -> A. x e. ZZ ( sqrt ` 2 ) =/= ( x / z ) ) /\\ ( z = y -> A. x e. ZZ ( sqrt ` 2 ) =/= ( x / z ) ) ) <-> ( A. z e. NN ( z < y -> A. x e. ZZ ( sqrt ` 2 ) =/= ( x / z ) ) /\\ A. z e. NN ( z = y -> A. x e. ZZ ( sqrt ` 2 ) =/= ( x / z ) ) ) )",
                expectedProof = "\$p wff ( A. z e. NN ( ( z < y -> A. x e. ZZ ( sqrt ` 2 ) =/= ( x / z ) ) /\\ ( z = y -> A. x e. ZZ ( sqrt ` 2 ) =/= ( x / z ) ) ) <-> ( A. z e. NN ( z < y -> A. x e. ZZ ( sqrt ` 2 ) =/= ( x / z ) ) /\\ A. z e. NN ( z = y -> A. x e. ZZ ( sqrt ` 2 ) =/= ( x / z ) ) ) ) \$= ( cv clt wbr c2 csqrt cfv cdiv co wne cz wral wi wceq wa cn wb ) CDZBDZEFGHIADTJKLAMNZOZTUAPUBOZQCRZNUCCUENUDCUENQS \$.",
                ctx = ctx
            )
        }
        println("------------------------------------------------------------------------------------------")
        println(DebugTimer2.getStatsStr(DebugTimer2.timers))
        println("------------------------------------------------------------------------------------------")
    }

    private fun testCompressedProof(expr: String, expectedProof: String, ctx: MetamathContext) {
        //given
        val proof = DebugTimer2.prove.run { ProofAssistant.prove(expr, ctx) }

        //when
        val actualProof = DebugTimer2.createProvableAssertion.run { ProofAssistant.createProvableAssertion(proof, ctx) }

        //then
        assertEquals(expectedProof, actualProof)
    }
}
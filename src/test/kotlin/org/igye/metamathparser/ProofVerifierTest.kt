package org.igye.metamathparser

import org.igye.common.Utils
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.util.*

internal class ProofVerifierTest {
    @Test
    fun successfully_verifies_all_noncompressed_valid_proofs() {
        //given
        val assertions: Map<String, Assertion> =
            Parsers.parseMetamathFile(text = Utils.readStringFromClassPath("/demo0.mm"), ExpressionProcessor).getAssertions()

        //when
        val node = ProofVerifier.verifyProof(assertions["th1"]!!)

        //then
        Assert.assertNotNull(node)
    }

    @Test
    fun fails_to_verify_invalid_noncompressed_proof() {
        //given
        val assertions: Map<String, Assertion> =
            Parsers.parseMetamathFile(text = Utils.readStringFromClassPath("/demo0.mm"), ExpressionProcessor).getAssertions()

        //when
        var errorMessage: String? = null
        try {
            ProofVerifier.verifyProof(assertions["th2"]!!)
        } catch (ex: MetamathParserException) {
            errorMessage = ex.message
        }

        //then
        Assert.assertEquals("stack.value != assertion.hypothesis", errorMessage)
    }

    @Test
    fun successfully_verifies_all_compressed_valid_proofs() {
        //given
        val assertions: Map<String, Assertion> =
            Parsers.parseMetamathFile(text = Utils.readStringFromClassPath("/set-reduced.mm"), ExpressionProcessor).getAssertions()
//            Parsers.parseMetamathFile(text = File("C:\\igye\\books\\metamath/set.mm").readText(), ExpressionProcessor).getAssertions()

        //when
        val verifiedTheorems = assertions.values.asSequence()
            .filter { it.statement.type == 'p' }
            .filter { ProofVerifier.verifyProof(it) != null }
            .map { it.statement.label }
            .toList()

        //then
        Assert.assertEquals(
            listOf("pm2.5","pm2.6","pm2.86i","pm2.61iii","syl8","com35","syl9","com34","syl6","syl7","4syl","pm2.521","jad","pm2.43","imim2","con4i","syl9r","imim1","pm2.18d","3syld","con4d","impbidd","syl5","impt","pm2.86d","peirce","com25","com24","a1iiOLD","com23","sylsyld","impi","mpii","con3d","imim12","con3i","pm2.61","com3r","imim12i","mt3d","mt3i","pm2.65","syli","com4r","a1dd","com4l","pm2.52","pm2.65i","pm2.51","com45","syld","impbid21d","mt2d","sylc","mt2i","imim12d","a1d","id1","mpsyl","a1i","pm2.65d","com3l","imim3i","pm2.86","pm2.83","mth8","bijust","a2d","syl5d","a2i","syl6com","mt4d","idd","idi","mt4i","syl6d","dfbi1","pm2.61d2","syl6c","pm2.61d1","con1","com15","com14","com13","com12","imim1d","syl","mpcom","bi1","bi3","impbid","con2d","impbii","con2","con2i","con3","pm2.61d","imim2i","imim2d","pm2.21ddOLD","pm2.61i","mp2","con1d","mp2OLD","con1i","imim1i","syl5com","pm2.21dd","syl6mpi","mp1i","id","mpd","pm3.2im","mpi","pm2.24ii","dummylink","simprim","ja","jc","a1ii","pm2.61nii","syl2im","syldd","dfbi1ALT","loowoz","mpdd","pm2.24d","mpdi","syl3c","loolin","pm2.24i","mp2b","mp2d","syl56","com52r","mt2","mt4","mt3","com52l","pm2.01d","embantd","jarl","jarr","nsyld","pm2.04","pm2.01","syl6ci","mtod","nsyli","notnotrd","notnotri","pm2.43i","notnoti","com4t","mto","3syl","nsyl","pm2.43a","com5r","simplim","pm2.43b","mpisyl","pm2.43d","looinv","com5l","expi","pm2.27","mpid","pm2.24","pm2.21","pm2.18","nsyl4","expt","nsyl2","nsyl3","pm2.86iALT","pm2.21i","sylcom","syl10","pm2.61ii","mtoi","con3rr3","pm2.21d","notnot1","notnot2"
            ),
            verifiedTheorems
        )
    }

    @Test
    fun strToInt_shouldParseNumbersFromCompressedProofs() {
        Assert.assertEquals(1, ProofVerifier.strToInt("A"))
        Assert.assertEquals(2, ProofVerifier.strToInt("B"))
        Assert.assertEquals(20, ProofVerifier.strToInt("T"))
        Assert.assertEquals(21, ProofVerifier.strToInt("UA"))
        Assert.assertEquals(22, ProofVerifier.strToInt("UB"))
        Assert.assertEquals(40, ProofVerifier.strToInt("UT"))
        Assert.assertEquals(41, ProofVerifier.strToInt("VA"))
        Assert.assertEquals(42, ProofVerifier.strToInt("VB"))
        Assert.assertEquals(120, ProofVerifier.strToInt("YT"))
        Assert.assertEquals(121, ProofVerifier.strToInt("UUA"))
        Assert.assertEquals(620, ProofVerifier.strToInt("YYT"))
        Assert.assertEquals(621, ProofVerifier.strToInt("UUUA"))
    }

    @Test
    fun splitEncodedProof_shouldSplitEncodedProofIntoParts() {
        //when
        val parts: List<String> = ProofVerifier.splitEncodedProof("ABCZUACZYYWA")

        //then
        Assert.assertEquals(Arrays.asList("A", "B", "C", "Z", "UA", "C", "Z", "YYWA"), parts)
    }
}
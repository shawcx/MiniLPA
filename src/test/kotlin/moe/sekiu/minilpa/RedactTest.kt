package moe.sekiu.minilpa

import kotlin.test.Test
import kotlin.test.assertEquals
import moe.sekiu.minilpa.model.ActivationCode

class RedactTest
{
    @Test
    fun masksEidLikeTheUi()
    {
        val line = """{"type":"lpa","payload":{"code":0,"message":"success","data":{"eidValue":"89044045930000000000001715303442","EuiccConfiguredAddresses":{"defaultDpAddress":"smdp.example.com"}}}}"""
        assertEquals(
            """{"type":"lpa","payload":{"code":0,"message":"success","data":{"eidValue":"890440459300********************","EuiccConfiguredAddresses":{"defaultDpAddress":"smdp.example.com"}}}}""",
            line.redact()
        )
    }

    @Test
    fun masksIccidsLikeTheUi()
    {
        val line = """{"data":[{"iccid":"8944500102198304826","profileNickname":"Work"},{"seqNumber":3,"iccid":"89445001021983048261"}]}"""
        assertEquals("""{"data":[{"iccid":"89445001***********","profileNickname":"Work"},{"seqNumber":3,"iccid":"89445001************"}]}""", line.redact())
    }

    @Test
    fun hidesActivationCodeMatchingId()
    {
        assertEquals("LPA:1\$rsp.example.com\$**************\$1.3.6.1\$1", "LPA:1\$rsp.example.com\$K2-ABCDE-12345\$1.3.6.1\$1".redact())
        assertEquals(
            "ActivationCode(SMDP=rsp.example.com, MatchingID=**************, OID=null, ReqConCode=false)",
            ActivationCode.of("LPA:1\$rsp.example.com\$K2-ABCDE-12345").toString()
        )
    }

    @Test
    fun leavesOtherTextAlone()
    {
        val line = """{"type":"progress","payload":{"code":0,"message":"es10b_get_euicc_challenge_and_info","data":null}}"""
        assertEquals(line, line.redact())
    }

    @Test
    fun redactsLpacArguments()
    {
        assertEquals(
            "profile download -s rsp.example.com -m ************** -c **** -i ***************",
            arrayOf("profile", "download", "-s", "rsp.example.com", "-m", "K2-ABCDE-12345", "-c", "5678", "-i", "356938035643809").redactLpacArguments()
        )
        assertEquals("profile enable 89445001***********", arrayOf("profile", "enable", "8944500102198304826").redactLpacArguments())
        assertEquals("profile nickname 89445001************ My phone", arrayOf("profile", "nickname", "89445001021983048261", "My phone").redactLpacArguments())
        assertEquals("chip info", arrayOf("chip", "info").redactLpacArguments())
    }
}

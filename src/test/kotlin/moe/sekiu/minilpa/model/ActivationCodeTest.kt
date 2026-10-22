package moe.sekiu.minilpa.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ActivationCodeTest
{
    @Test
    fun parsesAllFields()
    {
        assertEquals(
            ActivationCode("rsp.example.com", "K2-ABCDE-12345", "1.3.6.1.4.1.31746", true),
            ActivationCode.of("LPA:1\$rsp.example.com\$K2-ABCDE-12345\$1.3.6.1.4.1.31746\$1")
        )
    }

    @Test
    fun parsesWithoutOptionalFields()
    {
        assertEquals(ActivationCode("rsp.example.com", "K2-ABCDE-12345", null, false), ActivationCode.of("LPA:1\$rsp.example.com\$K2-ABCDE-12345"))
    }

    @Test
    fun rejectsOtherText()
    {
        assertNull(ActivationCode.of("LPA:2\$rsp.example.com\$K2-ABCDE-12345"))
        assertNull(ActivationCode.of("https://example.com"))
    }
}

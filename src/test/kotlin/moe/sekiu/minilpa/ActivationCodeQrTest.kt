package moe.sekiu.minilpa

import com.google.zxing.BarcodeFormat
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.QRCodeWriter
import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import moe.sekiu.minilpa.model.ActivationCode

class ActivationCodeQrTest
{
    @Test
    fun readsActivationCodeFromQrImage()
    {
        val text = "LPA:1\$rsp.example.com\$K2-ABCDE-12345"
        val image = MatrixToImageWriter.toBufferedImage(QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 300, 300))
        assertEquals(ActivationCode.of(text), image.parseActivationCode())
    }

    @Test
    fun ignoresImagesWithoutQrCode()
    {
        assertNull(BufferedImage(300, 300, BufferedImage.TYPE_INT_RGB).parseActivationCode())
    }
}

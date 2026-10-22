package moe.sekiu.minilpa

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProfileIconTest
{
    private fun encode(image : BufferedImage, format : String) : ByteArray
    {
        val bytes = ByteArrayOutputStream()
        ImageIO.write(image, format, bytes)
        return bytes.toByteArray()
    }

    @Test
    fun decodesPngAndJpeg()
    {
        assertEquals(16, encode(BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB), "png").decodeProfileIcon()?.width)
        assertEquals(16, encode(BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB), "jpeg").decodeProfileIcon()?.width)
    }

    @Test
    fun refusesOversizedImages()
    {
        assertNull(encode(BufferedImage(2048, 1, BufferedImage.TYPE_BYTE_GRAY), "png").decodeProfileIcon())
    }

    @Test
    fun refusesOtherFormats()
    {
        assertNull("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"16\" height=\"16\"/>".toByteArray().decodeProfileIcon())
        assertNull(encode(BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB), "bmp").decodeProfileIcon())
    }
}

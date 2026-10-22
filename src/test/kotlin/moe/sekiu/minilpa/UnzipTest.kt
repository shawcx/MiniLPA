package moe.sekiu.minilpa

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.apache.commons.lang3.SystemUtils

class UnzipTest
{
    private val temp = Files.createTempDirectory("unzip-test").toFile()

    @AfterTest
    fun cleanUp() = temp.deleteRecursively().drop()

    private fun zip(vararg entries : Pair<String, String?>) : ZipInputStream
    {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { out ->
            for ((name, content) in entries)
            {
                out.putNextEntry(ZipEntry(name))
                content?.let { out.write(it.toByteArray()) }
                out.closeEntry()
            }
        }
        return ZipInputStream(bytes.toByteArray().inputStream())
    }

    @Test
    fun extractsFilesAndFolders()
    {
        val folder = File(temp, "lpac")
        zip("lpac" to "binary", "LICENSE" to "text", "docs/" to null, "docs/README.md" to "readme").unzip(folder, setOf("lpac"))

        assertEquals("binary", File(folder, "lpac").readText())
        assertEquals("readme", File(folder, "docs/README.md").readText())
        if (!SystemUtils.IS_OS_WINDOWS)
        {
            assertTrue(File(folder, "lpac").canExecute())
            assertFalse(File(folder, "LICENSE").canExecute())
        }
    }

    @Test
    fun refusesEntriesOutsideTheFolder()
    {
        val folder = File(temp, "lpac")
        assertFailsWith<IOException> { zip("../evil" to "payload").unzip(folder) }
        assertFalse(File(temp, "evil").exists())
    }
}

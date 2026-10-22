package moe.sekiu.minilpa.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.Json

class EumManifestTest
{
    private val json = Json { ignoreUnknownKeys = true }

    // A trimmed copy of osmocom's accredited.json, including fields the app ignores
    private val accredited = """
        {
          "version": 1,
          "suppliers": [
            {"name": "Giesecke+Devrient GmbH", "abbr": "G+D", "url": "https://www.gi-de.com", "eum": ["89049032", "89049038"], "country": "DE", "locations": {"GD-MH": "DE"}},
            {"name": "IDEMIA", "url": "https://www.idemia.com", "eum": ["89033024"], "country": "FR"},
            {"name": "Gemalto", "url": "https://www.gemalto.com", "country": "FR", "deprecated": "Gemalto is now part of Thales Group"}
          ],
          "certificates": {"GD-MH-0101": "https://example.com/certificate.pdf"}
        }
    """.trimIndent()

    @Test
    fun findsSupplierByEidPrefix()
    {
        val manifest = json.decodeFromString<EumManifest.Accredited>(accredited)
        EumManifest.suppliers.clear()
        EumManifest.suppliers.addAll(manifest.suppliers)

        val gd = EumManifest.findSupplier("89049038000000000000000000000000")
        assertEquals("G+D", gd?.shortName)
        assertEquals("DE", gd?.country)
        assertEquals("IDEMIA", EumManifest.findSupplier("89033024000000000000000000000000")?.shortName)
        assertNull(EumManifest.findSupplier("12345678000000000000000000000000"))
    }
}

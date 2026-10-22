package moe.sekiu.minilpa.model

import java.io.File
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.decodeFromStream
import moe.sekiu.minilpa.BuildConfig
import moe.sekiu.minilpa.appDataFolder
import moe.sekiu.minilpa.bufferedResourceStream
import moe.sekiu.minilpa.json
import moe.sekiu.minilpa.language
import moe.sekiu.minilpa.openLink
import moe.sekiu.minilpa.setting

sealed class Manifest<T : Any>
{
    abstract val path : String
    val data : MutableList<T> = mutableListOf()

    internal inline fun <reified R> loadManifest0(entries : (R) -> List<T>) : Boolean
    {
        var update = false
        val file = File(appDataFolder, path)
        if (!file.exists() || file.length() == 0L || BuildConfig.EUICC_INFO_UPDATE_TIME > setting.`euicc-info-update-time`)
        {
            file.outputStream().buffered().use { out -> bufferedResourceStream(path).use { `in` -> `in`.copyTo(out) } }
            update = true
        }
        val manifest = file.inputStream().buffered().use { `in` -> json.decodeFromStream<R>(`in`) }
        data.clear()
        data.addAll(entries(manifest))
        return update
    }

    abstract fun loadManifest() : Boolean

    companion object
    {
        fun loadManifests()
        {
            if (EumManifest.loadManifest() or CIManifest.loadManifest())
            {
                setting.update { `euicc-info-update-time` = BuildConfig.EUICC_INFO_UPDATE_TIME }
            }
        }
    }
}

data object EumManifest : Manifest<EumManifest.Supplier>()
{
    override val path = "eum-accredited.json"
    val suppliers = data

    override fun loadManifest() = loadManifest0<Accredited> { it.suppliers }

    @Serializable
    data class Accredited(val suppliers : List<Supplier>)

    @Serializable
    data class Supplier(
        val name : String,
        val abbr : String? = null,
        val country : String,
        val eum : List<String> = emptyList()
    )
    {
        val shortName get() = abbr ?: name
    }

    fun findSupplier(eid : String) : Supplier? = suppliers.find { supplier -> supplier.eum.any { eid.startsWith(it) } }
}

data object CIManifest : Manifest<CIManifest.CertificateIssuer>()
{
    override val path = "ci-manifest.json"
    val cis = data

    override fun loadManifest() = loadManifest0<List<CertificateIssuer>> { it }

    @Serializable
    data class CertificateIssuer(
        @SerialName("key-id")
        val keyId : String,
        val country : String? = null,
        val name : String = language.unknown,
        val crls : List<String>? = null)
    {
        fun openExternalLink() = "https://euicc-manual.osmocom.org/docs/pki/ci/files/${keyId.substring(0..5)}.txt".openLink()
    }

    fun findCIs(euiccInfo2 : JsonObject) : List<CertificateIssuer>
    {
        val euiccInfo2Lite = json.decodeFromJsonElement<ChipInfo.EuiccInfo2Lite>(euiccInfo2)
        val intersection = euiccInfo2Lite.euiccCiPKIdListForSigning intersect euiccInfo2Lite.euiccCiPKIdListForVerification
        return intersection.map { cis.find { ci -> it.startsWith(ci.keyId) }?.copy(keyId = it) ?: CertificateIssuer(keyId = it) }
    }
}
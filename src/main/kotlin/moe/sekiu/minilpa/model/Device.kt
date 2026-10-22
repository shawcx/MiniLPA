package moe.sekiu.minilpa.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

sealed interface Device

@Serializable
data class Driver(val env : String, val name : String, @Transient val envName : String = "") : Device
{
    override fun toString() : String = name
}
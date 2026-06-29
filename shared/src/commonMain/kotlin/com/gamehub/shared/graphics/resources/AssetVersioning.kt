package com.gamehub.shared.graphics.resources

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Asset metadata with version info
 */
@Serializable
data class AssetInfo(
    val id: String,
    val version: Int,
    val checksum: String,
    val size: Long
)

/**
 * Asset manifest for version tracking
 */
@Serializable
data class AssetManifest(
    val version: Int,
    val assets: List<AssetInfo>
)

/**
 * Asset Patcher interface (no‑op default implementation)
 */
open class AssetPatcher {
    open suspend fun checkForUpdates(currentManifest: AssetManifest): AssetManifest = currentManifest

    open suspend fun downloadPatch(assetId: String, fromVersion: Int, toVersion: Int): ByteArray = byteArrayOf()

    open suspend fun applyPatch(assetBytes: ByteArray, patchBytes: ByteArray): ByteArray = assetBytes
}

package com.nuvio.app.features.addons

internal expect object AddonStorage {
    fun loadInstalledAddonUrls(profileId: Int): List<String>
    fun saveInstalledAddonUrls(profileId: Int, urls: List<String>)
    fun loadAddonEnabledStates(profileId: Int): Map<String, Boolean>
    fun saveAddonEnabledStates(profileId: Int, states: Map<String, Boolean>)
    fun hasSeededDefaultAddons(profileId: Int): Boolean
    fun markDefaultAddonsSeeded(profileId: Int)
}

/**
 * Manifest URLs installed automatically the first time a profile ever loads addons, so a
 * fresh install has working catalogs/metadata, subtitles, and streams out of the box.
 * Only public, unauthenticated addon instances belong here — never a URL carrying a
 * personal debrid/API token.
 */
internal val DefaultAddonManifestUrls: List<String> = listOf(
    "https://v3-cinemeta.strem.io/manifest.json",
    "https://opensubtitles-v3.strem.io/manifest.json",
    "https://torrentio.strem.fun/manifest.json",
    "https://comet.elfhosted.com/manifest.json",
)

/**
 * Manifest URLs that were seeded by default in an earlier build but turned out to be broken
 * (missing artwork, dead streams) and should be actively removed even from installs that
 * already seeded them, not just excluded from future seeding.
 */
internal val RetiredDefaultAddonManifestUrls: List<String> = listOf(
    "https://2ecbbd610840-stremio-ar.baby-beamup.club/manifest.json",
)

data class RawHttpResponse(
    val status: Int,
    val statusText: String,
    val url: String,
    val body: String,
    val headers: Map<String, String>,
)

/** Default safety limit for generic and plugin-provided HTTP responses. */
internal const val DefaultRawHttpResponseMaxBytes = 1024 * 1024

expect suspend fun httpGetText(url: String): String

expect suspend fun httpPostJson(url: String, body: String): String

expect suspend fun httpGetTextWithHeaders(
    url: String,
    headers: Map<String, String>,
): String

expect suspend fun httpPostJsonWithHeaders(
    url: String,
    body: String,
    headers: Map<String, String>,
): String

expect suspend fun httpRequestRaw(
    method: String,
    url: String,
    headers: Map<String, String>,
    body: String,
    followRedirects: Boolean = true,
    maxResponseBodyBytes: Int = DefaultRawHttpResponseMaxBytes,
): RawHttpResponse

package com.nuvio.app.features.collection

import com.nuvio.app.core.storage.ProfileScopedKey
import platform.Foundation.NSUserDefaults

actual object CollectionStorage {
    private const val payloadKey = "collections_payload"

    actual fun loadPayload(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(payloadKey))

    actual fun savePayload(payload: String) {
        NSUserDefaults.standardUserDefaults.setObject(payload, forKey = ProfileScopedKey.of(payloadKey))
    }

    actual fun hasSeededDefaultNetworks(): Boolean =
        NSUserDefaults.standardUserDefaults.boolForKey(ProfileScopedKey.of("default_networks_seeded"))

    actual fun markDefaultNetworksSeeded() {
        NSUserDefaults.standardUserDefaults.setBool(true, forKey = ProfileScopedKey.of("default_networks_seeded"))
    }
}

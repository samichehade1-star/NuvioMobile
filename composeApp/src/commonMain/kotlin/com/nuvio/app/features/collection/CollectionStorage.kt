package com.nuvio.app.features.collection

internal expect object CollectionStorage {
    fun loadPayload(): String?
    fun savePayload(payload: String)
    fun hasSeededDefaultNetworks(): Boolean
    fun markDefaultNetworksSeeded()
}

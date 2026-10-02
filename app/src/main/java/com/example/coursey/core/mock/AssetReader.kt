package com.example.coursey.core.mock

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface AssetReader {
    suspend fun readText(path: String): String
}

class AssetReaderImpl(context: Context) : AssetReader {

    private val assets = context.applicationContext.assets

    override suspend fun readText(path: String): String = withContext(Dispatchers.IO) {
        assets.open(path).bufferedReader().use { it.readText() }
    }
}

package com.cheatpot.shrinko.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cheatpot.shrinko.domain.model.HistoryItem
import com.cheatpot.shrinko.domain.model.OutputFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.historyDataStore: DataStore<Preferences> by preferencesDataStore(name = "pixel_shrink_history")

class HistoryDataStore(private val context: Context) {

    private val historyKey = stringPreferencesKey("history_items_json")

    val historyFlow: Flow<List<HistoryItem>> = context.historyDataStore.data.map { prefs ->
        val jsonString = prefs[historyKey] ?: "[]"
        deserializeHistory(jsonString)
    }

    suspend fun addHistoryItem(item: HistoryItem) {
        context.historyDataStore.edit { prefs ->
            val jsonString = prefs[historyKey] ?: "[]"
            val currentList = deserializeHistory(jsonString).toMutableList()

            // Remove duplicates if any
            currentList.removeAll { it.id == item.id }
            currentList.add(0, item)

            // Limit history to 50 items
            if (currentList.size > 50) {
                currentList.subList(50, currentList.size).clear()
            }

            prefs[historyKey] = serializeHistory(currentList)
        }
    }

    suspend fun removeHistoryItem(id: String) {
        context.historyDataStore.edit { prefs ->
            val jsonString = prefs[historyKey] ?: "[]"
            val currentList = deserializeHistory(jsonString).toMutableList()
            currentList.removeAll { it.id == id }
            prefs[historyKey] = serializeHistory(currentList)
        }
    }

    suspend fun clearHistory() {
        context.historyDataStore.edit { prefs ->
            prefs[historyKey] = "[]"
        }
    }

    private fun serializeHistory(items: List<HistoryItem>): String {
        val array = JSONArray()
        items.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("originalUriString", item.originalUriString)
                put("outputUriString", item.outputUriString)
                put("filename", item.filename)
                put("originalSizeBytes", item.originalSizeBytes)
                put("outputSizeBytes", item.outputSizeBytes)
                put("savedPercentage", item.savedPercentage.toDouble())
                put("outputWidth", item.outputWidth)
                put("outputHeight", item.outputHeight)
                put("outputFormat", item.outputFormat.name)
                put("timestamp", item.timestamp)
            }
            array.put(obj)
        }
        return array.toString()
    }

    private fun deserializeHistory(jsonString: String): List<HistoryItem> {
        return runCatching {
            val list = mutableListOf<HistoryItem>()
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val format = runCatching { OutputFormat.valueOf(obj.optString("outputFormat", "JPEG")) }.getOrDefault(OutputFormat.JPEG)
                list.add(
                    HistoryItem(
                        id = obj.getString("id"),
                        originalUriString = obj.getString("originalUriString"),
                        outputUriString = obj.getString("outputUriString"),
                        filename = obj.getString("filename"),
                        originalSizeBytes = obj.getLong("originalSizeBytes"),
                        outputSizeBytes = obj.getLong("outputSizeBytes"),
                        savedPercentage = obj.getDouble("savedPercentage").toFloat(),
                        outputWidth = obj.getInt("outputWidth"),
                        outputHeight = obj.getInt("outputHeight"),
                        outputFormat = format,
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            list
        }.getOrDefault(emptyList())
    }
}

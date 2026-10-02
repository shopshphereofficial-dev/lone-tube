package com.soulstream.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object History {

    data class Item(val name: String, val size: Long, val time: Long, val uri: String, val mime: String)

    private const val PREFS = "soulstream_history"
    private const val KEY = "items"

    @Synchronized
    fun add(context: Context, name: String, size: Long, uri: String, mime: String) {
        val list = list(context).toMutableList()
        list.add(0, Item(name, size, System.currentTimeMillis(), uri, mime))
        while (list.size > 300) list.removeAt(list.size - 1)
        save(context, list)
    }

    @Synchronized
    fun list(context: Context): List<Item> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val out = mutableListOf<Item>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(
                    Item(
                        o.getString("name"),
                        o.getLong("size"),
                        o.getLong("time"),
                        o.getString("uri"),
                        o.getString("mime")
                    )
                )
            }
            out
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun remove(context: Context, uri: String) {
        save(context, list(context).filterNot { it.uri == uri })
    }

    @Synchronized
    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply()
    }

    private fun save(context: Context, list: List<Item>) {
        val arr = JSONArray()
        for (item in list) {
            arr.put(
                JSONObject().apply {
                    put("name", item.name)
                    put("size", item.size)
                    put("time", item.time)
                    put("uri", item.uri)
                    put("mime", item.mime)
                }
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }
}

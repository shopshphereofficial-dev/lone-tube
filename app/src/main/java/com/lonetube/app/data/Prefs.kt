package com.lonetube.app.data

import android.content.Context

object Prefs {

    private const val FILE = "lonetube_settings"

    private fun sp(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun defaultQuality(ctx: Context): Int = sp(ctx).getInt("default_quality", 0)
    fun setDefaultQuality(ctx: Context, q: Int) = sp(ctx).edit().putInt("default_quality", q).apply()

    fun askQuality(ctx: Context): Boolean = sp(ctx).getBoolean("ask_quality", true)
    fun setAskQuality(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("ask_quality", v).apply()

    fun engineUpdatedAt(ctx: Context): Long = sp(ctx).getLong("last_engine_update", 0L)
    fun setEngineUpdatedAt(ctx: Context, t: Long) = sp(ctx).edit().putLong("last_engine_update", t).apply()
}

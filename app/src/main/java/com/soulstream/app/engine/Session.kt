package com.soulstream.app.engine

import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView

/**
 * Keeps the browser's logins (Google, Instagram, Facebook, X, ...) alive across
 * app restarts. Cookies are written to disk on every pause and are only ever
 * wiped by the explicit "Log out everywhere" action in Settings.
 */
object Session {

    fun prepare(webView: WebView) {
        try {
            val cm = CookieManager.getInstance()
            cm.setAcceptCookie(true)
            cm.setAcceptThirdPartyCookies(webView, true)
            webView.settings.domStorageEnabled = true
            webView.settings.databaseEnabled = true
        } catch (e: Exception) {
            // nothing we can do - browsing still works
        }
    }

    fun flush() {
        try {
            CookieManager.getInstance().flush()
        } catch (e: Exception) {
            // ignore
        }
    }

    /** Manual logout: clears cookies + web storage for every site. */
    fun logoutEverywhere() {
        try {
            val cm = CookieManager.getInstance()
            cm.removeAllCookies(null)
            cm.flush()
        } catch (e: Exception) {
            // ignore
        }
        try {
            WebStorage.getInstance().deleteAllData()
        } catch (e: Exception) {
            // ignore
        }
    }
}

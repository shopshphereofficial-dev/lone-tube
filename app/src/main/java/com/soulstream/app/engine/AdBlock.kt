package com.soulstream.app.engine

import android.net.Uri

/**
 * A light, honest ad blocker: it kills ad/tracker networks at the request
 * level and hides the usual ad slots with CSS.
 *
 * It cannot remove ads that are stitched into a video stream itself (for
 * example YouTube's in-video ads, which come from the same video server).
 * For those, the browser's "Ad-free YouTube" shortcut opens a Piped/Invidious
 * front-end instead, which serves YouTube without ads.
 */
object AdBlock {

    private val HOSTS = listOf(
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "adservice.google.com", "ads.youtube.com", "googletagservices.com",
        "googletagmanager.com", "amazon-adsystem.com", "adnxs.com", "adsrvr.org",
        "criteo.com", "criteo.net", "taboola.com", "outbrain.com", "moatads.com",
        "pubmatic.com", "rubiconproject.com", "openx.net", "smartadserver.com",
        "yieldmo.com", "sharethrough.com", "adform.net", "casalemedia.com",
        "bidswitch.net", "adsafeprotected.com", "innovid.com", "spotxchange.com",
        "springserve.com", "teads.tv", "3lift.com", "scorecardresearch.com",
        "quantserve.com", "serving-sys.com", "adzerk.net", "buysellads.com",
        "carbonads.com", "mgid.com", "revcontent.com", "propellerads.com",
        "popads.net", "onclickads.net", "exoclick.com", "juicyads.com",
        "trafficjunky.com", "zedo.com", "adroll.com", "smartclip.net",
        "adcolony.com", "applovin.com", "unityads.unity3d.com", "inmobi.com",
        "chartboost.com", "vungle.com", "tapjoy.com", "supersonicads.com"
    )

    fun isBlocked(url: String): Boolean {
        val host = try {
            Uri.parse(url).host?.lowercase()
        } catch (e: Exception) {
            null
        } ?: return false
        return HOSTS.any { host == it || host.endsWith(".$it") }
    }

    /** Injected after every page load - hides the leftover ad slots. */
    val CSS: String = """
        ins.adsbygoogle,
        .adsbygoogle,
        [id^="google_ads"],
        [id*="div-gpt-ad"],
        [id*="taboola"],
        [id*="outbrain"],
        [class*="ad-slot"],
        [class*="adsbox"],
        [class*="ad-banner"],
        [class*="adBanner"],
        [class*="advert"],
        [class*="Advert"],
        [class*="sponsored"],
        [class*="Sponsored"],
        [data-ad],
        [data-ad-slot],
        iframe[src*="doubleclick"],
        iframe[src*="googlesyndication"],
        .ytp-ad-module,
        .video-ads,
        #masthead-ad,
        ytd-promoted-sparkles-web-renderer,
        ytd-display-ad-renderer,
        ytd-ad-slot-renderer,
        ytd-in-feed-ad-layout-renderer,
        ytd-banner-promo-renderer,
        ytmusic-mealbar-promo-renderer {
            display: none !important;
            visibility: hidden !important;
            height: 0 !important;
            width: 0 !important;
        }
    """.trimIndent()

    fun cssInjection(): String = """
        (function(){
          var id='soulstream-adblock';
          if(document.getElementById(id)) return 'ok';
          var s=document.createElement('style');
          s.id=id; s.type='text/css';
          s.appendChild(document.createTextNode(${jsonString(CSS)}));
          (document.head||document.documentElement).appendChild(s);
          return 'ok';
        })()
    """.trimIndent()

    /** Ad-free YouTube front-ends (Piped / Invidious). */
    val AD_FREE_YOUTUBE = listOf(
        "https://piped.video",
        "https://inv.nadeko.net",
        "https://yewtu.be",
        "https://invidious.nerdvpn.de"
    )

    private fun jsonString(s: String): String {
        val sb = StringBuilder("\"")
        for (c in s) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("")
                '\t' -> sb.append("\\t")
                else -> sb.append(c)
            }
        }
        sb.append("\"")
        return sb.toString()
    }
}

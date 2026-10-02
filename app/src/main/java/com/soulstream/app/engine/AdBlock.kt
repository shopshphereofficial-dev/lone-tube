package com.soulstream.app.engine

import android.net.Uri

/**
 * Ad blocker for the in-app browser.
 *
 * Three layers, so it actually holds up on real sites:
 *  1. request level  - [isBlocked] kills ad/tracker networks before they load.
 *  2. DOM level      - [cssInjection] hides ad slots, [domGuardJs] removes them
 *                      as they appear (MutationObserver) and neutralises popups.
 *  3. YouTube        - [youtubeAdFreeJs] auto-skips in-video ads and strips the
 *                      YouTube ad renderers inside the app's own player.
 */
object AdBlock {

    private val HOSTS = listOf(
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "adservice.google.com", "ads.youtube.com", "googletagservices.com",
        "googletagmanager.com", "google-analytics.com", "analytics.google.com",
        "amazon-adsystem.com", "adnxs.com", "adsrvr.org", "adsafeprotected.com",
        "criteo.com", "criteo.net", "taboola.com", "outbrain.com", "moatads.com",
        "pubmatic.com", "rubiconproject.com", "openx.net", "smartadserver.com",
        "yieldmo.com", "sharethrough.com", "adform.net", "casalemedia.com",
        "bidswitch.net", "innovid.com", "spotxchange.com", "springserve.com",
        "teads.tv", "3lift.com", "scorecardresearch.com", "quantserve.com",
        "serving-sys.com", "adzerk.net", "buysellads.com", "carbonads.com",
        "mgid.com", "revcontent.com", "propellerads.com", "popads.net",
        "onclickads.net", "exoclick.com", "juicyads.com", "trafficjunky.com",
        "zedo.com", "adroll.com", "smartclip.net", "adcolony.com", "applovin.com",
        "unityads.unity3d.com", "inmobi.com", "chartboost.com", "vungle.com",
        "tapjoy.com", "supersonicads.com", "adkernel.com", "advertising.com",
        "adtechus.com", "advertising.yahoo.com", "bluekai.com", "demdex.net",
        "everesttech.net", "krxd.net", "mathtag.com", "media.net", "nativeads.com",
        "onetag-sys.com", "smartadserver.com", "smaato.net", "sonobi.com",
        "spotx.tv", "tremorhub.com", "triplelift.com", "trustx.org", "yieldlab.net",
        "zemanta.com", "zqtk.net", "adition.com", "adnium.com", "adpushup.com",
        "adtelligent.com", "bidtellect.com", "contextweb.com", "districtm.io",
        "emxdgt.com", "gumgum.com", "improvedigital.com", "indexexchange.com",
        "loopme.me", "nativo.com", "pixalate.com", "pulsepoint.com", "rhythmone.com",
        "richaudience.com", "rtbhouse.com", "sekindo.com", "sharethrough.com",
        "sovrn.com", "stickyadstv.com", "teads.com", "vidoomy.com", "vidazoo.com",
        "yieldbot.com", "zergnet.com", "zeropark.com", "adcash.com", "adspyglass.com",
        "adsterra.com", "clksite.com", "hilltopads.net", "popcash.net", "popmyads.com",
        "revenuehits.com", "exponential.com", "tribalfusion.com", "adbrite.com",
        "adsnative.com", "admedo.com", "adblade.com", "adroll.com", "audienceinsights.net",
        "conversantmedia.com", "dotomi.com", "flashtalking.com", "mediaplex.com",
        "ml314.com", "nexac.com", "simpli.fi", "turn.com", "specificmedia.com",
        "adnxs-simple.com", "adnxs.com", "agkn.com", "adsymptotic.com",
        "company-target.com", "crwdcntrl.net", "exelator.com", "narrative.io",
        "owneriq.net", "quantcast.com", "rlcdn.com", "tapestry.tapad.com",
        "tapad.com", "thetradedesk.com", "adsrvr.org", "mathtag.com",
        "ad-delivery.net", "ad-maven.com", "adf.ly", "shortzon.com",
        "mgid.com", "a-ads.com", "coinzilla.io", "cointraffic.io",
        "linkbucks.com", "adfoc.us", "bc.vc", "shorte.st", "ouo.io",
        "sponsored.ad", "taboola.com", "outbrain.com", "zemanta.com",
        "smartnews-ads.com", "popin.cc", "logly.co.jp", "adstir.com"
    )

    /** Host- or path-based block test for a request URL. */
    fun isBlocked(url: String): Boolean {
        val host = try {
            Uri.parse(url).host?.lowercase()
        } catch (e: Exception) {
            null
        }
        if (host != null && HOSTS.any { host == it || host.endsWith(".$it") }) return true

        // Common ad/tracker URL shapes (kept tight to avoid breaking real pages).
        val low = url.lowercase()
        return low.contains("/adserver") ||
            low.contains("/adservice") ||
            low.contains("/pagead/") ||
            low.contains("/ads?") ||
            low.contains("&ad_type=") ||
            low.contains("/advert/") ||
            low.contains("advertisement") ||
            low.contains("/popunder") ||
            low.contains("/tracker?") ||
            low.contains("/pixel?") ||
            low.contains("/beacon?")
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

    /**
     * Removes ad nodes as the page mutates them in, and neutralises popups /
     * pop-unders. Runs once per page load; sets up its own observer.
     */
    fun domGuardJs(): String = """
        (function(){
          if(window.__ssGuard) return 'ok';
          window.__ssGuard = true;
          var SEL = 'ins.adsbygoogle,.adsbygoogle,[id*="div-gpt-ad"],[id*="taboola"],[id*="outbrain"],[class*="ad-slot"],[class*="adsbox"],[class*="ad-banner"],[class*="advert"],[class*="Advert"],[class*="sponsored"],[class*="Sponsored"],[data-ad],[data-ad-slot]';
          function sweep(){
            try{
              var n=document.querySelectorAll(SEL);
              for(var i=0;i<n.length;i++){ n[i].style.setProperty('display','none','important'); }
            }catch(e){}
          }
          sweep();
          try{
            var mo=new MutationObserver(function(){ sweep(); });
            mo.observe(document.documentElement,{childList:true,subtree:true});
          }catch(e){}
          try{
            // stop pop-unders / forced new tabs
            var realOpen=window.open;
            window.open=function(){ return null; };
          }catch(e){}
          return 'ok';
        })()
    """.trimIndent()

    /**
     * Aggressive in-app YouTube de-adder: strips the ad renderers, auto-clicks
     * Skip, and fast-forwards any ad that is mid-roll in the app's own player.
     */
    fun youtubeAdFreeJs(): String = """
        (function(){
          if(window.__ssYt) return 'ok';
          window.__ssYt = true;
          var SEL='.video-ads,.ytp-ad-module,.ytp-ad-overlay-slot,.ytp-ad-overlay-container,.ytp-ad-text-overlay,#masthead-ad,ytd-ad-slot-renderer,ytd-promoted-sparkles-web-renderer,ytd-display-ad-renderer,ytd-in-feed-ad-layout-renderer,.ytp-ad-progress-list,.ytp-ad-persistent-progress-bar-container';
          function kill(){
            try{
              var n=document.querySelectorAll(SEL);
              for(var i=0;i<n.length;i++){ n[i].remove(); }
              var p=document.getElementById('movie_player');
              var v=document.querySelector('video');
              if(p && p.classList && p.classList.contains('ad-showing') && v && isFinite(v.duration) && v.duration>0){
                v.currentTime = v.duration;
              }
              var sk=document.querySelector('.ytp-ad-skip-button,.ytp-ad-skip-button-modern,.ytp-skip-ad-button,.ytp-ad-skip-button-container button');
              if(sk){ sk.click(); }
              var cb=document.querySelector('.ytp-ad-overlay-close-button');
              if(cb){ cb.click(); }
            }catch(e){}
          }
          kill();
          try{ setInterval(kill, 450); }catch(e){}
          return 'ok';
        })()
    """.trimIndent()

    /** Ad-free YouTube front-ends (Piped / Invidious), tried in order. */
    val AD_FREE_YOUTUBE = listOf(
        "https://piped.video",
        "https://inv.nadeko.net",
        "https://yewtu.be",
        "https://invidious.nerdvpn.de",
        "https://piped.moomoo.me"
    )

    fun isYoutube(url: String): Boolean {
        val h = try {
            Uri.parse(url).host?.lowercase()
        } catch (e: Exception) {
            null
        } ?: return false
        return h == "youtube.com" || h.endsWith(".youtube.com") ||
            h == "youtu.be" || h.endsWith(".youtu.be") ||
            h == "youtube-nocookie.com" || h.endsWith(".youtube-nocookie.com")
    }

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

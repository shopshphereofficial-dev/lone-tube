# SoulStream

A gamer-grade media downloader for Android, powered by
[yt-dlp](https://github.com/yt-dlp/yt-dlp) (via
[youtubedl-android](https://github.com/JunkFood02/youtubedl-android)) with
ffmpeg merging and aria2c turbo downloads.

Grab video, audio **and images** from YouTube, Instagram, Facebook, TikTok and
1000+ other sites in the original quality - no watermark, no ads.

## What's inside

**Download**
- Quality picker: Best / 1080p / 720p / 480p / 360p / MP3 320 kbps / MP3 128 kbps
- Turbo downloads (aria2c, 16 connections), live progress cards + notification
- Clipboard link auto-detected the moment you open the app
- Share a link from any app straight into SoulStream

**Browser (SnapTube + Aloha style)**
- Google search bar, back / forward / home / reload
- **Ad blocking**: ad networks killed at the request level, ad slots hidden
- **Detection on any website**: videos *and* images are found and offered in one
  tap - videos go to yt-dlp, images save straight to `Pictures/SoulStream`
- **Logins persist** until you press *Log out everywhere* (Google, Instagram,
  Facebook, X ...)

**Vault**
- WhatsApp status saver (WhatsApp + WhatsApp Business): grid, preview, save to
  gallery, share, or *save all* in one tap

**Play**
- Built-in player for everything you downloaded

**Look**
- Neon gamer theme with four accent skins, drifting grid + scanline backdrop,
  glow buttons, animated progress rings, staggered pop-in lists and springy
  press feedback everywhere

## Usage

1. Copy a link (or share it into SoulStream)
2. Tap **DOWNLOAD NOW**, pick a quality
3. Find it in your **Downloads** folder, or play it in the **Play** tab

## Important

For **personal use of your own content** only. Downloading other people's
copyrighted content and re-sharing it is a copyright violation, and this app
must not be redistributed (Play Store policy prohibits such apps).

Honest note on ads: the browser blocks ad networks and hides ad slots, but ads
stitched *into* a video stream (YouTube in-video ads) cannot be removed without
breaking playback - for those, use the **Ad-free YouTube** shortcut, which opens
a Piped / Invidious mirror.

## Signing

`keystore.b64` holds the app's signing key (base64) so future updates install
cleanly over each other. It is a self-signed personal-use key; the repo is
public, so treat it as a public key.

## Build

APK is built automatically by GitHub Actions - see `.github/workflows/build.yml`.

Note: the yt-dlp engine is licensed GPL-3.0.

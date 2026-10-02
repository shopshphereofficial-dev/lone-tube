# LoneTube

A premium media downloader for Android, powered by [yt-dlp](https://github.com/yt-dlp/yt-dlp)
(via [youtubedl-android](https://github.com/JunkFood02/youtubedl-android)), with
ffmpeg merging and aria2c turbo downloads.

Download videos and audio from YouTube, Instagram, Facebook, TikTok and 1000+
other sites in the original available quality - no watermark, no ads.

## What's new in v1.1

- **Much smaller download** - the release build is R8-minified and resource-shrunk
  (the v1.0 debug build carried ~56 MB of dead code)
- **WhatsApp status saver** - a Statuses tab that lists every image/video status
  (WhatsApp and WhatsApp Business) with one-tap save to gallery and share
- **SnapTube-style browser** - Google search bar, back / forward / home / reload
- **Aloha-style video detection** - while you browse, LoneTube watches for videos
  (both in the page and on the network) and shows a "N videos found" pill so any
  video on any website can be downloaded in one tap

## Highlights

- **Premium animated UI** - drifting gradient backdrop, glass cards, springy
  press feedback, animated progress rings and smooth screen transitions
- **Turbo downloads** - aria2c engine with 16 parallel connections
- **Quality picker** - Best / 1080p / 720p / 480p / 360p / MP3 320 kbps / MP3 128 kbps
- **In-app browser** - log in to your accounts once and yt-dlp reuses the cookies,
  so private / account-only videos download too
- **Live download cards** - watch progress inside the app, plus a progress
  notification you can leave in the shade
- **Library** - every finished download listed with open / delete
- **Share sheet** - share a link from any app straight into LoneTube
- **Self-updating engine** - yt-dlp refreshes itself daily so sites keep working

## Usage

1. Copy a video link (Share -> Copy link), or use Share -> LoneTube
2. Open LoneTube, tap **Paste**, then **Download**
3. Pick a quality - the file lands in your phone's **Downloads** folder

For statuses: open the **Statuses** tab, grant "All files access" once, then
save or share anything WhatsApp is showing.

For any website: open the built-in browser, play the video, and tap the
"N videos found" pill.

## Important

This app is for **personal use of your own content** only. Downloading other
people's copyrighted content and re-sharing it is a copyright violation, and
this app must not be redistributed (Play Store policy prohibits such apps).

## Signing

`keystore.b64` holds the app's signing key (base64) so that every future update
installs cleanly over the previous one. It is a self-signed personal-use key;
because the repo is public, treat it as a public key - do not reuse it for
anything that needs real security.

## Build

APK is built automatically by GitHub Actions - see `.github/workflows/build.yml`.

Note: the yt-dlp engine is licensed GPL-3.0.

[app]
title = THRESHOLD Global Crisis
package.name = thresholdglobalcrisis
package.domain = org.threshold

source.dir = .
source.include_exts = py,json,png,jpg,ttf,wav,ogg

version = 1.0.0

requirements = python3,pygame==2.1.2

android.archs = arm64-v8a

# Android permissions
android.permissions = INTERNET,WRITE_EXTERNAL_STORAGE,READ_EXTERNAL_STORAGE,VIBRATE

android.api = 33
android.minapi = 26
android.ndk = 25b
android.sdk = 33
android.accept_sdk_license = True

# Keep screen on during gameplay
android.wakelock = True

# Fullscreen
fullscreen = 1

# Icons (place a 512x512 icon.png in the assets/ folder)
# icon.filename = %(source.dir)s/assets/icon.png

# Presplash (place a presplash.png in assets/)
# presplash.filename = %(source.dir)s/assets/presplash.png

log_level = 2

[buildozer]
log_level = 2
warn_on_root = 1

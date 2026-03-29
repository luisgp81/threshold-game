[app]
title = THRESHOLD Global Crisis
package.name = thresholdglobalcrisis
package.domain = org.threshold

source.dir = .
source.include_exts = py,json,png,jpg,ttf,wav,ogg

version = 1.0.0

requirements = python3,pygame-ce

orientation = landscape

android.archs = arm64-v8a,armeabi-v7a

# Android permissions
android.permissions = INTERNET,WRITE_EXTERNAL_STORAGE,READ_EXTERNAL_STORAGE,VIBRATE

android.api = 33
android.minapi = 26
android.sdk = 33
android.accept_sdk_license = True

# Keep screen on during gameplay
android.wakelock = True

# Fullscreen
fullscreen = 1

# Use develop branch of p4a for better pygame-ce support
p4a.fork = kivy
p4a.branch = develop

log_level = 2

[buildozer]
log_level = 2
warn_on_root = 1

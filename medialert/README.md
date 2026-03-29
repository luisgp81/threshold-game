# MediAlert

**MediAlert** is a native Android application (Kotlin + Jetpack Compose) for intelligent medication management, designed for both caretakers and elderly users. It features Gemini Vision AI for prescription scanning, AlarmManager-driven reminders that survive device reboots, two user modes (Caretaker / Elder), and Google Drive backup.

---

## Architecture

```
MVVM + Repository Pattern + Clean Architecture
├── domain/          Pure Kotlin — models, repository interfaces, use cases
├── data/            Android — Room entities/DAOs, repository implementations, Gemini API service
├── presentation/    Jetpack Compose screens, ViewModels, navigation graph
├── di/              Hilt dependency injection modules
├── workers/         WorkManager workers (alarms, daily generator, backup)
├── notifications/   NotificationHelper, channel setup
├── receivers/       BootReceiver, AlarmReceiver, NotificationActionReceiver
└── utils/           DateUtils, AppConstants, GeminiPrompts
```

**Key libraries:** Room 2.6, WorkManager 2.9, CameraX 1.3, Hilt 2.52, Paging 3, DataStore, Moshi, Coil, Firebase Auth, Google Sign-In, Accompanist Permissions, AdMob (disabled in v1).

---

## Prerequisites

1. **Android Studio Ladybug** (2024.2.1) or newer
2. **Java 17** (bundled with Android Studio)
3. **Android SDK 35** (targetSdk), min SDK 26 (Android 8.0)

---

## Setup

### 1. Firebase Project

```
1. Visit https://console.firebase.google.com
2. Create a project (or use an existing one)
3. Add an Android app → package name: com.medialert
4. Download google-services.json → place in medialert/app/
5. Enable Google Sign-In:
   Firebase Console → Authentication → Sign-in method → Google → Enable
6. Add your SHA-1 debug fingerprint:
   ./gradlew :app:signingReport   (copy SHA-1 from the "debug" section)
   Firebase Console → Project settings → Your apps → Add fingerprint
```

### 2. Gemini API Key

```
1. Visit https://aistudio.google.com/app/apikey
2. Create an API key (free tier available)
3. Add to medialert/local.properties:
   GEMINI_API_KEY=your_actual_key_here
```

### 3. local.properties

Copy the template and fill in your values:

```bash
cp local.properties.template local.properties
# then edit local.properties with your SDK path, API keys
```

```properties
sdk.dir=/path/to/your/Android/Sdk
GEMINI_API_KEY=your_gemini_key
ADMOB_APP_ID=ca-app-pub-3940256099942544~3347511713   # test ID for dev
```

### 4. Update Web Client ID in strings.xml

After setting up Firebase, copy the **Web Client ID** from:
`Firebase Console → Project Settings → Your apps → OAuth 2.0 Web client`

Replace `YOUR_WEB_CLIENT_ID_HERE` in:
```
app/src/main/res/values/strings.xml  → default_web_client_id
```

---

## Running the App

### Emulator

```bash
# Open Android Studio, create an AVD (Pixel 6, API 34+)
./gradlew :app:installDebug
# Or press ▶ Run in Android Studio
```

### Physical Device

```bash
# Enable Developer Options → USB Debugging on the device
adb devices   # verify device is listed
./gradlew :app:installDebug
```

---

## Project Structure

```
medialert/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/medialert/
│       │   ├── MediAlertApp.kt              Application class (Hilt)
│       │   ├── MainActivity.kt              Entry point
│       │   ├── data/
│       │   │   ├── db/                      Room database, entities, DAOs
│       │   │   ├── remote/                  GeminiApiService
│       │   │   └── repository/              Implementations + UserPreferencesRepository
│       │   ├── domain/
│       │   │   ├── model/                   Pure domain models
│       │   │   ├── repository/              Interfaces
│       │   │   └── usecase/                 Business logic
│       │   ├── presentation/
│       │   │   ├── navigation/              NavGraph + Routes
│       │   │   ├── auth/                    Login with Google
│       │   │   ├── today/                   Today's doses screen
│       │   │   ├── inventory/               Medication list + detail + add
│       │   │   ├── scan/                    CameraX + Gemini Vision
│       │   │   ├── elder/                   Simplified elder UI
│       │   │   ├── history/                 Paged dose log
│       │   │   ├── settings/                Settings + PIN lock
│       │   │   └── components/              BannerAdView (disabled)
│       │   ├── di/                          Hilt modules
│       │   ├── workers/                     WorkManager workers
│       │   ├── notifications/               NotificationHelper
│       │   ├── receivers/                   Boot, Alarm, NotificationAction
│       │   └── utils/                       DateUtils, AppConstants, GeminiPrompts
│       └── res/
│           ├── values/strings.xml           Spanish strings (default)
│           └── values-en/strings.xml        English strings
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/libs.versions.toml
├── local.properties.template
├── google-services.json.template
└── README.md
```

---

## Features

| Feature | Status |
|---|---|
| Google Sign-In (Firebase Auth) | ✅ |
| Caretaker Mode (full access) | ✅ |
| Elder Mode (simplified UI, large text 20sp, 64dp buttons) | ✅ |
| PIN protection (4-digit, configurable) | ✅ |
| Today's dose schedule with progress bar | ✅ |
| Medication inventory (CRUD, filters) | ✅ |
| Prescription scan via Gemini Vision | ✅ |
| Medication box scan via Gemini Vision | ✅ |
| Dose photo verification via Gemini Vision | ✅ |
| Exact alarms (AlarmManager, survives reboot) | ✅ |
| Rich notifications with 3 action buttons | ✅ |
| Follow-up notifications (5 min, 15 min) | ✅ |
| WorkManager for reliability | ✅ |
| Room Database (local, offline-first) | ✅ |
| Paging 3 for history | ✅ |
| DataStore for preferences | ✅ |
| Material Design 3 + Dynamic Color | ✅ |
| Dark / Light / System theme | ✅ |
| i18n: Spanish + English | ✅ |
| AdMob ready (disabled in v1) | ✅ |
| Google Drive backup (structure ready) | 🔜 v1.1 |
| PDF / CSV history export | 🔜 v1.1 |

---

## Gemini Vision Prompts

All prompts are centralized in:
```
app/src/main/java/com/medialert/utils/GeminiPrompts.kt
```

Adjust wording there without touching business logic.

---

## AdMob Activation (when ready to monetize)

1. Get a real AdMob App ID from https://admob.google.com
2. Update `ADMOB_APP_ID` in `local.properties`
3. In `app/build.gradle.kts`, change:
   ```kotlin
   buildConfigField("Boolean", "ADS_ENABLED", "false")
   // to:
   buildConfigField("Boolean", "ADS_ENABLED", "true")
   ```
4. Add `BannerAdView()` composable where desired in screens

---

## Security Notes

- The Gemini API key is stored in `local.properties` (git-ignored) and injected via `BuildConfig`
- Firebase credentials are in `google-services.json` (git-ignored)
- Never commit either file to version control
- The `.gitignore` excludes `local.properties` and `google-services.json`

---

## License

MediAlert is private software. All rights reserved.

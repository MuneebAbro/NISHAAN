# MANUAL_SETUP.md — Things You Must Do Manually

> These are setup steps that Antigravity cannot do for you. Complete these before running the app.

---

## 1. Firebase Project Setup

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project named **NISHAAN** (or use existing)
3. Add an Android app with package name: `com.maximus.nishaan`
4. Download `google-services.json`
5. Place it in `d:\Nishaan\app\google-services.json`

### Enable Firebase Services
In Firebase Console, enable:
- **Authentication** → Sign-in methods → Email/Password + Anonymous
- **Cloud Firestore** → Create database (start in test mode for hackathon)
- **Cloud Storage** → Enable (for missing person photos)
- **Cloud Messaging** → Enabled by default

### Firestore Security Rules (paste in Console)
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{uid} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == uid;
    }
    match /crises/{crisisId} {
      allow read: if true;
      allow write: if false;
    }
    match /agent_traces/{traceId} {
      allow read: if true;
      allow write: if false;
    }
    match /signals/{signalId} {
      allow read: if false;
      allow write: if false;
    }
    match /missing_persons/{reportId} {
      allow read: if true;
      allow write: if request.auth != null && !request.auth.token.firebase.sign_in_provider.matches('anonymous');
    }
    match /eyewitness_reports/{reportId} {
      allow read: if false;
      allow write: if request.auth != null;
    }
  }
}
```

---

## 2. Google Maps API Key

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Enable **Maps SDK for Android**
3. Create an API key (restrict to Android apps + your SHA-1)
4. Open `local.properties` in the root of your project
5. Add your key at the bottom like this:
   `MAPS_API_KEY=YOUR_MAPS_API_KEY_HERE`

*(The app is now configured to securely read this file and inject it during the build process, keeping your key safe from Git)*

---

## 3. Gemini API Key (for Agent Scripts)

1. Go to [Google AI Studio](https://aistudio.google.com/)
2. Get a Gemini API key
3. Add to `agent-scripts/shared/` config (created later)


---

## 4. Add SHA-1 Fingerprint (Fixes DEVELOPER_ERROR)

The `GoogleApiManager DEVELOPER_ERROR` in logcat means your debug SHA-1 isn't registered:

1. Get your debug SHA-1:
```bash
cd d:\Nishaan
.\gradlew.bat signingReport
```
2. Copy the `SHA1:` value from the `debug` variant
3. Go to Firebase Console → Project Settings → Your Android app → **Add fingerprint**
4. Paste the SHA-1 and save
5. **Re-download `google-services.json`** and replace the one in `app/`

---

## 5. Seed Firestore with Test Data

A seeding script is ready at `d:\Nishaan\seed_firestore.js`. To use it:

**Option A — Node.js script (recommended):**
```bash
cd d:\Nishaan
npm install firebase-admin
# Download service account key from Firebase Console → Project Settings → Service Accounts → Generate New Private Key
# Save as d:\Nishaan\serviceAccountKey.json
node seed_firestore.js
```

**Option B — Manual via Firebase Console:**
1. Go to Firestore → Create collection `crises`
2. Add a document with fields: `crisis_type` (string: "FLOOD"), `severity` (string: "CRITICAL"), `confidence` (number: 91), `status` (string: "CONFIRMED"), `title_en` (string: "Flash Flood — Gulshan"), `description_en` (string), `created_at` (timestamp), `updated_at` (timestamp)
3. Create collection `agent_traces` with fields: `agent_name`, `crisis_id` (use the crisis doc ID), `action`, `reasoning_summary`, `timestamp`

---

## 5. Build & Run

```bash
# Sync Gradle
./gradlew.bat :app:dependencies

# Build debug APK
./gradlew.bat :app:assembleDebug

# Install on device/emulator
./gradlew.bat :app:installDebug
```

---

## Checklist

- [yes] `google-services.json` placed in `app/`
- [yes] Google Maps API key added to manifest
- [yes] Firebase Auth methods enabled
- [yes] Firestore database created
- [yes] Firebase Storage enabled
- [yes] Test data seeded in Firestore
- [yes] App builds successfully
- [yes] App runs on device/emulator

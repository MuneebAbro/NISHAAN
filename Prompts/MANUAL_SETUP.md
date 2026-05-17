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
4. Add the key to `AndroidManifest.xml`:

```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="YOUR_MAPS_API_KEY_HERE" />
```

The placeholder `YOUR_MAPS_API_KEY_HERE` is already in the manifest — just replace the value.

---

## 3. Gemini API Key (for Agent Scripts)

1. Go to [Google AI Studio](https://aistudio.google.com/)
2. Get a Gemini API key
3. Add to `agent-scripts/shared/` config (created later)

---

## 4. Seed Firestore with Test Data

For the hackathon demo, you need sample data in Firestore. Antigravity can generate a seeding script, or you can manually add documents to:
- `crises` — 3-5 sample crisis documents
- `agent_traces` — Sample agent decision logs
- `signals` — Sample signal documents

Ask Antigravity: "Generate a Firestore seeding script"

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

- [ ] `google-services.json` placed in `app/`
- [ ] Google Maps API key added to manifest
- [ ] Firebase Auth methods enabled
- [ ] Firestore database created
- [ ] Firebase Storage enabled
- [ ] Test data seeded in Firestore
- [ ] App builds successfully
- [ ] App runs on device/emulator

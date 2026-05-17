# 🚀 NISHAAN Project Requirements & Setup Guide

Welcome to the NISHAAN codebase! To get this project building and running locally, you need to provide a few secret keys and configuration files. **These files are intentionally excluded from the Git repository to keep them secure.**

Please follow the checklist below to set up your local environment.

---

## 📂 1. Firebase Configuration (`google-services.json`)

You need the Firebase configuration file to connect the app to the backend (Authentication, Firestore, Storage).

- **What you need:** `google-services.json`
- **Where to get it:** 
  1. Ask the project owner for the file, OR
  2. Go to the [Firebase Console](https://console.firebase.google.com/) for the NISHAAN project.
  3. Go to Project Settings > General > Your Apps (Android).
  4. Download the `google-services.json` file.
- **Where to put it:** Place the file exactly at:
  ```
  [Project Root]/app/google-services.json
  ```

---

## 🔑 2. Google Maps API Key (`local.properties`)

The app uses Google Maps to render crisis geofences and missing person locations. The API key must be added to your local environment file.

- **What you need:** A valid Google Maps Android API Key.
- **Where to get it:**
  1. Ask the project owner for the key, OR
  2. Generate one in the [Google Cloud Console](https://console.cloud.google.com/) (Ensure "Maps SDK for Android" is enabled).
- **Where to put it:** 
  1. Open (or create) the `local.properties` file in the **root** of the project:
     ```
     [Project Root]/local.properties
     ```
  2. Add the following line at the bottom of the file:
     ```properties
     MAPS_API_KEY=your_actual_api_key_here
     ```
  *(The build system will automatically inject this into the app's manifest securely.)*

---

## 🗄️ 3. Firebase Admin Service Account (`serviceAccountKey.json`) - *Optional*

If you need to run the Node.js script (`seed_firestore.js`) to populate the database with sample crises and agent traces, you will need the Admin SDK private key.

- **What you need:** `serviceAccountKey.json`
- **Where to get it:**
  1. Ask the project owner for the file, OR
  2. Go to [Firebase Console](https://console.firebase.google.com/) > Project Settings > Service Accounts.
  3. Click "Generate New Private Key" and download the JSON file.
- **Where to put it:** Place the file in the **root** of the project:
  ```
  [Project Root]/serviceAccountKey.json
  ```
- **How to use it:**
  Run the seeder from the terminal:
  ```bash
  npm install firebase-admin
  node seed_firestore.js
  ```

---

## ⚠️ Important Notes for Developers
- **Do not commit secrets:** The `.gitignore` is already configured to ignore `google-services.json`, `serviceAccountKey.json`, and `local.properties`. Please ensure you **do not force add** these files to version control.
- **DEVELOPER_ERROR in Logcat:** If you see a `DEVELOPER_ERROR` from Google Play Services in your Logcat, it means your local debug `SHA-1` fingerprint is not registered in Firebase. To fix this:
  1. Run `.\gradlew.bat signingReport` to get your debug `SHA-1` key.
  2. Add it to the Firebase Console under Project Settings.
  3. Re-download and replace your `google-services.json`.

You're all set! Sync Gradle and build the app. 🎉

# STACK.md — NISHAAN Technology Stack

> This file defines every technology choice for the NISHAAN project. AI agents must not introduce libraries or tools outside this spec without explicit approval.

---

## Platform

| Layer | Choice |
|---|---|
| **Platform** | Android (native only) |
| **Language** | Kotlin |
| **Min SDK** | API 26 (Android 8.0) |
| **Target SDK** | API 34 (Android 14) |
| **UI System** | XML Layouts (View system — NOT Jetpack Compose) |
| **Architecture Pattern** | MVVM (Model-View-ViewModel) |

---

## Android Stack

### Core
- **Kotlin** — All code is Kotlin. No Java files.
- **ViewBinding** — Enabled for all modules. No `findViewById` calls anywhere.
- **ViewModel + LiveData** — All UI state lives in ViewModels. Fragments/Activities observe LiveData only.
- **Coroutines + Flow** — All async operations use Kotlin Coroutines. No RxJava. No AsyncTask.
- **Navigation Component** — Jetpack Navigation for all screen transitions. Single Activity architecture.

### Networking
- **Retrofit 2** — All HTTP calls go through Retrofit interfaces.
- **OkHttp 4** — HTTP client with logging interceptor (debug builds only).
- **Gson** — JSON serialization/deserialization.
- **No raw HttpURLConnection** — ever.

### UI & Maps
- **Google Maps SDK for Android** — Crisis map, geofencing, location overlays.
- **Google Location Services (FusedLocationProvider)** — User location.
- **Glide** — Image loading and caching (profile photos, missing person photos).
- **Material Design Components (MDC)** — Buttons, cards, text fields, bottom sheets.
- **RecyclerView + DiffUtil** — All lists. No ListView.

### Local Storage
- **Room Database** — Local cache for crisis data and missing person reports.
- **SharedPreferences (via DataStore)** — User preferences, language selection, onboarding state.

### Push Notifications
- **Firebase Cloud Messaging (FCM)** — All push notifications routed through FCM.

---

## Backend Stack

### Firebase
- **Firebase Firestore** — Primary database. All collections defined in DATABASE_SCHEMA.md.
- **Firebase Authentication** — Email/password + anonymous auth for citizens.
- **Firebase Cloud Functions** — Lightweight triggers (e.g., on new missing person report, notify MATCHER agent).
- **Firebase Storage** — Missing person photo uploads.

### Mock Agency API
- **Node.js (Express)** — Simple stub server simulating Rescue 1122, NDMA, and PMD endpoints.
- Runs locally or on a free-tier cloud VM for demo purposes.
- All responses are hardcoded JSON matching the real API contract shape.

---

## AI & Agent Stack

| Component | Technology |
|---|---|
| **Agent Orchestration** | Google Antigravity |
| **NLP / Classification** | Gemini API (gemini-1.5-pro) |
| **Semantic Matching (MATCHER)** | Google Embedding API (text-embedding-004) |
| **Agent runtime** | Python (serverless / Cloud Functions) |

### Agent Communication
- Agents write decision logs to Firestore collection `agent_traces`.
- The Android app observes `agent_traces` in real-time via Firestore snapshots.
- No direct agent ↔ app socket connection. Firestore is the message bus.

---

## Rules

1. **No class components or legacy patterns** — MVVM only. No MVC, no God Activities.
2. **No inline lambdas for click listeners in XML** — Use ViewBinding + ViewModel methods.
3. **No hardcoded strings** — All user-facing strings in `res/strings.xml` with Urdu translations in `res/values-ur/strings.xml`.
4. **No hardcoded colors** — All colors defined in `res/colors.xml` and referenced via theme attributes.
5. **No new libraries without team review** — Agent must flag library additions as a proposal, not auto-add them.
6. **Dependency injection via manual DI** — A simple `AppContainer` singleton. No Hilt/Dagger for hackathon scope (can be added post-hackathon).
7. **All network calls wrapped in Result<T>** — Never let raw exceptions propagate to the UI layer.
8. **Room entities are not domain models** — Always map to/from domain models at the repository layer.

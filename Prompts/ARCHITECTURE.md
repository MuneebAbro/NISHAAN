# ARCHITECTURE.md — NISHAAN Project Structure & Design

> Defines the folder system, layer responsibilities, and design rules. All generated code must conform to this structure.

---

## Top-Level Project Structure

```
nishaan-android/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/nishaan/app/
│   │   │   │   ├── NishaanApplication.kt      # App entry point, AppContainer init
│   │   │   │   ├── MainActivity.kt            # Single activity host
│   │   │   │   │
│   │   │   │   ├── core/                      # Shared infrastructure (no feature logic)
│   │   │   │   │   ├── di/                    # AppContainer (manual DI)
│   │   │   │   │   ├── network/               # RetrofitClient, OkHttpClient, interceptors
│   │   │   │   │   ├── firebase/              # FirebaseManager, FCMService
│   │   │   │   │   ├── location/              # LocationService (FusedLocationProvider wrapper)
│   │   │   │   │   ├── util/                  # Extensions, formatters, constants
│   │   │   │   │   └── ui/                    # Base classes, ViewBindingFragment, custom views
│   │   │   │   │
│   │   │   │   ├── data/                      # Data layer (repositories + sources)
│   │   │   │   │   ├── local/
│   │   │   │   │   │   ├── db/                # Room database, DAOs
│   │   │   │   │   │   └── datastore/         # DataStore preferences
│   │   │   │   │   ├── remote/
│   │   │   │   │   │   ├── firestore/         # Firestore data sources
│   │   │   │   │   │   ├── api/               # Retrofit service interfaces (mock agency APIs)
│   │   │   │   │   │   └── fcm/               # FCM token management
│   │   │   │   │   └── repository/            # Repository implementations
│   │   │   │   │
│   │   │   │   ├── domain/                    # Business logic layer
│   │   │   │   │   ├── model/                 # Domain models (pure Kotlin data classes)
│   │   │   │   │   ├── repository/            # Repository interfaces
│   │   │   │   │   └── usecase/               # Use cases (one action per class)
│   │   │   │   │
│   │   │   │   └── feature/                   # Feature modules (UI layer)
│   │   │   │       ├── onboarding/
│   │   │   │       ├── auth/
│   │   │   │       ├── home/
│   │   │   │       ├── crisis/
│   │   │   │       ├── missing/
│   │   │   │       ├── alerts/
│   │   │   │       ├── agenttrace/
│   │   │   │       └── profile/
│   │   │   │
│   │   │   ├── res/
│   │   │   │   ├── layout/                    # XML layouts (feature_screen_name.xml convention)
│   │   │   │   ├── navigation/                # nav_graph.xml
│   │   │   │   ├── values/                    # colors.xml, strings.xml, themes.xml, dimens.xml
│   │   │   │   ├── values-ur/                 # Urdu string translations
│   │   │   │   ├── values-night/              # Dark theme overrides
│   │   │   │   ├── font/                      # IBM Plex Sans, IBM Plex Mono, Noto Nastaliq Urdu
│   │   │   │   ├── drawable/                  # Vector icons, backgrounds
│   │   │   │   ├── drawable-night/            # Dark theme drawables
│   │   │   │   └── raw/                       # map_style_dark.json, map_style_light.json
│   │   │   │
│   │   │   └── AndroidManifest.xml
│   │   │
│   │   └── test/                              # Unit tests
│   │       └── androidTest/                   # Instrumented tests
│   │
│   └── build.gradle.kts
│
├── backend/                                   # Node.js mock agency API server
│   ├── src/
│   │   ├── routes/                            # /rescue1122, /ndma, /police
│   │   └── data/                              # Mock response JSON files
│   ├── index.js
│   └── package.json
│
└── agent-scripts/                             # Python AI agent scripts (deployed to Cloud Functions)
    ├── sentinel.py
    ├── analyst.py
    ├── commander.py
    ├── matcher.py
    └── shared/
        ├── firestore_client.py
        ├── gemini_client.py
        └── models.py
```

---

## Layer Responsibilities

### Feature Layer (`feature/`)
- Contains: Fragments, ViewModels, XML layouts, adapters
- **Only** imports from `domain/` layer and `core/ui/`
- Never imports from `data/` directly
- One package per feature: `feature/crisis/` contains `CrisisDetailFragment.kt`, `CrisisViewModel.kt`, `CrisisAdapter.kt`

### Domain Layer (`domain/`)
- Pure Kotlin — no Android imports
- `model/`: Data classes with no Room annotations, no Retrofit annotations. These are the app's source of truth.
- `repository/`: Interfaces only. Implementations live in `data/`
- `usecase/`: Single-purpose classes. Example: `GetActiveCrisesUseCase`, `SubmitMissingPersonReportUseCase`. Each use case has one `invoke()` / `execute()` function.

### Data Layer (`data/`)
- Implements domain repository interfaces
- Maps between remote/local entities and domain models at this layer
- Room entities have suffix `Entity`: `CrisisEntity`, `MissingPersonEntity`
- Retrofit response models have suffix `Response`: `CrisisResponse`, `AgencyResponse`
- Firestore document models have suffix `Document`: `CrisisDocument`, `SignalDocument`
- **Never expose entity/response/document types to the domain or feature layer**

### Core Layer (`core/`)
- No business logic
- Shared infrastructure only: DI container, network client config, utility extensions
- `AppContainer`: a singleton that holds all repository instances and use cases, instantiated in `NishaanApplication`

---

## Dependency Rule

```
feature/ → domain/ → (interfaces only)
data/    → domain/ → (implements interfaces)
core/    ← (injected into both data/ and feature/ via AppContainer)
```

Feature layer never imports Data layer directly. All data flows through domain interfaces.

---

## State Management

- **ViewModels** own all UI state via `LiveData<UiState<T>>` or `StateFlow<UiState<T>>`
- `UiState<T>` is a sealed class: `Loading`, `Success(data: T)`, `Error(message: String)`
- Fragments observe state and render accordingly — no logic in Fragment beyond rendering and delegating to ViewModel
- Navigation events are handled via `SingleLiveEvent` (not regular LiveData) to prevent re-emission on configuration change

---

## Real-Time Data

- Firestore real-time listeners are started in ViewModel's `init {}` block and cancelled in `onCleared()`
- Use `callbackFlow` to wrap Firestore snapshot listeners as Kotlin Flow
- No polling from the Android app — all real-time data comes via Firestore listeners

---

## Error Handling

- All repository functions return `Result<T>` (Kotlin stdlib)
- ViewModels map `Result.failure` to `UiState.Error` with user-friendly message
- Network errors: mapped to a typed `NetworkException` in the data layer before surfacing
- Firestore errors: logged to Crashlytics (or Logcat in debug), surfaced as `UiState.Error`
- Never `throw` from a repository — always return `Result.failure()`

---

## Agent Backend Structure

Each agent is a standalone Python module deployed as a Firebase Cloud Function (or triggered by Pub/Sub):

```
sentinel.py     → polls sources, writes to signals collection
analyst.py      → reads signals, writes to crises collection
commander.py    → reads crises, calls agency APIs, sends FCM
matcher.py      → reads missing_persons + crises, writes links + sends FCM
```

Shared utilities in `shared/`:
- `firestore_client.py` — authenticated Firestore reads/writes
- `gemini_client.py` — Gemini API wrapper with retry logic
- `models.py` — dataclass definitions matching Firestore schema

All agents write their trace to `agent_traces` collection after every significant action.

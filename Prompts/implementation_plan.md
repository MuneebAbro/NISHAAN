# NISHAAN — Full Project Build Implementation Plan

Build the complete NISHAAN Android application from the current bare Android Studio scaffold into a fully functional crisis detection and missing persons system, following all specifications in the Prompts folder.

## Current State

The project is a **bare Android Studio scaffold** with:
- A single `MainActivity.kt` (Hello World)
- Default `activity_main.xml`, `colors.xml`, `strings.xml`, `themes.xml`
- No Kotlin plugin in Gradle (`build.gradle.kts` is Java-only)
- Package: `com.maximus.nishaan` (spec says `com.nishaan.app` — see Open Questions)
- No Firebase, Navigation, Room, Retrofit, Maps, or any stack dependencies
- No architecture layers, no domain models, no features

## Open Questions

> [!IMPORTANT]
> **Package name mismatch:** The existing project uses `com.maximus.nishaan` but `ARCHITECTURE.md` references `com/nishaan/app/`. Should I:
> - **(A)** Keep `com.maximus.nishaan` (already configured in Gradle + Manifest) — **recommended for hackathon speed**
> - **(B)** Refactor to `com.nishaan.app` to match the spec exactly
>
> I'll proceed with **(A)** unless you say otherwise.

> [!IMPORTANT]
> **Google Maps API Key & Firebase config:** I will create placeholder slots for these. You'll need to provide:
> - `google-services.json` (from Firebase Console)
> - Google Maps API key (for `AndroidManifest.xml` meta-data)
>
> I can set up the code to compile and run without them (mocked/stubbed), but the real features need these files.

> [!IMPORTANT]
> **compileSdk syntax issue:** The current `build.gradle.kts` uses an unusual `compileSdk { version = release(36) { minorApiLevel = 1 } }` syntax which is AGP 9.x specific. I'll keep this as-is since it matches your Gradle plugin version.

---

## Proposed Changes — Phased Build

I'll build this in **6 phases**, each leaving the project in a compilable state.

---

### Phase 1: Foundation — Gradle, Dependencies, Design System, Architecture Skeleton

Set up the entire project foundation: Kotlin plugin, all dependencies, color/typography/theme system, dimension tokens, folder structure, and base classes.

#### [MODIFY] [build.gradle.kts](file:///d:/Nishaan/build.gradle.kts) (root)
- Add Kotlin plugin, Google Services plugin, Navigation SafeArgs plugin

#### [MODIFY] [libs.versions.toml](file:///d:/Nishaan/gradle/libs.versions.toml)
- Add all dependencies: Kotlin, Navigation, Room, Retrofit, OkHttp, Gson, Firebase (Firestore, Auth, Messaging, Storage), Google Maps, Glide, Coroutines, Lifecycle, DataStore

#### [MODIFY] [build.gradle.kts](file:///d:/Nishaan/app/build.gradle.kts) (app)
- Apply Kotlin, KSP, Google Services, Navigation SafeArgs plugins
- Enable ViewBinding
- Add all dependency references

#### [MODIFY] [settings.gradle.kts](file:///d:/Nishaan/settings.gradle.kts)
- Add google() to plugin management if needed

#### [MODIFY] [colors.xml](file:///d:/Nishaan/app/src/main/res/values/colors.xml)
- Full NISHAAN color system from `UI_GUIDE.md` (dark + light tokens)

#### [MODIFY] [themes.xml](file:///d:/Nishaan/app/src/main/res/values/themes.xml) (day)
- Light theme with all color token mappings, NishaanCard style, button styles

#### [MODIFY] [themes.xml](file:///d:/Nishaan/app/src/main/res/values-night/themes.xml) (night)
- Dark theme (default) — full command-center color mapping

#### [NEW] [dimens.xml](file:///d:/Nishaan/app/src/main/res/values/dimens.xml)
- All spacing tokens: `spacing_xs` through `spacing_xxl`, `dimen_card_padding`, `dimen_screen_margin`, card corners, button heights

#### [MODIFY] [strings.xml](file:///d:/Nishaan/app/src/main/res/values/strings.xml)
- All English UI strings for all 14 screens

#### [NEW] [strings.xml](file:///d:/Nishaan/app/src/main/res/values-ur/strings.xml)
- Urdu translations for all strings

#### [NEW] [styles.xml](file:///d:/Nishaan/app/src/main/res/values/styles.xml)
- Typography styles (`TextDisplay`, `TextHeadline`, `TextBodyLarge`, `TextBodyMedium`, `TextCaption`, `TextMono`, `TextBadge`)
- Component styles: `NishaanCard`, `NishaanCardAlert`, `PrimaryButton`, `SecondaryButton`, `TextButton`, `NishaanInputLayout`

#### Architecture Skeleton (Kotlin files under `com.maximus.nishaan`)
- [NEW] `core/di/AppContainer.kt` — Manual DI container
- [NEW] `core/ui/UiState.kt` — `UiState<T>` sealed class
- [NEW] `core/ui/SingleLiveEvent.kt` — Single-emission LiveData for navigation
- [NEW] `core/ui/BaseFragment.kt` — Base ViewBinding fragment pattern (optional helper)
- [NEW] `core/util/Extensions.kt` — Common extensions (formatTimeAgo, etc.)
- [NEW] `core/util/Constants.kt` — App-wide constants
- [NEW] `NishaanApplication.kt` — Application class, AppContainer init

---

### Phase 2: Navigation, MainActivity, Splash, Onboarding, Language Select

Single-Activity navigation setup with the initial user flow.

#### [NEW] [nav_graph.xml](file:///d:/Nishaan/app/src/main/res/navigation/nav_graph.xml)
- All 14 destinations, actions, and deep links

#### [MODIFY] [activity_main.xml](file:///d:/Nishaan/app/src/main/res/layout/activity_main.xml)
- Replace Hello World with `NavHostFragment`

#### [MODIFY] [MainActivity.kt](file:///d:/Nishaan/app/src/main/java/com/maximus/nishaan/MainActivity.kt)
- NavController setup, edge-to-edge, status bar color

#### [MODIFY] [AndroidManifest.xml](file:///d:/Nishaan/app/src/main/AndroidManifest.xml)
- Add permissions (INTERNET, ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, POST_NOTIFICATIONS, CAMERA)
- Add `NishaanApplication` as `android:name`
- Add Google Maps API key meta-data placeholder

#### Splash Screen
- [NEW] `feature/splash/SplashFragment.kt`
- [NEW] `fragment_splash.xml`

#### Onboarding
- [NEW] `feature/onboarding/OnboardingFragment.kt`
- [NEW] `feature/onboarding/OnboardingPagerAdapter.kt`
- [NEW] `fragment_onboarding.xml`
- [NEW] `item_onboarding_slide.xml`

#### Language Selection
- [NEW] `feature/onboarding/LanguageSelectFragment.kt`
- [NEW] `fragment_language_select.xml`

---

### Phase 3: Auth, Permissions, Home Dashboard with Map

#### Permissions Screen
- [NEW] `feature/onboarding/PermissionsFragment.kt`
- [NEW] `fragment_permissions.xml`

#### Auth Screen
- [NEW] `feature/auth/AuthFragment.kt`
- [NEW] `feature/auth/AuthViewModel.kt`
- [NEW] `fragment_auth.xml`

#### Domain Models
- [NEW] `domain/model/Crisis.kt`
- [NEW] `domain/model/MissingPerson.kt`
- [NEW] `domain/model/AgentTrace.kt`
- [NEW] `domain/model/Signal.kt`
- [NEW] `domain/model/User.kt`

#### Domain Repository Interfaces
- [NEW] `domain/repository/CrisisRepository.kt`
- [NEW] `domain/repository/MissingPersonRepository.kt`
- [NEW] `domain/repository/AgentTraceRepository.kt`
- [NEW] `domain/repository/UserRepository.kt`
- [NEW] `domain/repository/AuthRepository.kt`

#### Use Cases
- [NEW] `domain/usecase/GetActiveCrisesUseCase.kt`
- [NEW] `domain/usecase/SignInUseCase.kt`
- [NEW] `domain/usecase/SignInAnonymouslyUseCase.kt`

#### Data Layer (Firebase)
- [NEW] `data/remote/firestore/CrisisFirestoreSource.kt`
- [NEW] `data/remote/firestore/document/CrisisDocument.kt`
- [NEW] `data/repository/CrisisRepositoryImpl.kt`
- [NEW] `data/repository/AuthRepositoryImpl.kt`

#### Home Dashboard
- [NEW] `feature/home/HomeDashboardFragment.kt`
- [NEW] `feature/home/HomeDashboardViewModel.kt`
- [NEW] `feature/home/CrisisCardAdapter.kt`
- [NEW] `fragment_home_dashboard.xml`
- [NEW] `item_crisis_card.xml`

---

### Phase 4: Crisis Detail, Agent Trace View, Alerts List

#### Crisis Detail
- [NEW] `feature/crisis/CrisisDetailFragment.kt`
- [NEW] `feature/crisis/CrisisDetailViewModel.kt`
- [NEW] `fragment_crisis_detail.xml`

#### Agent Trace View
- [NEW] `feature/agenttrace/AgentTraceFragment.kt`
- [NEW] `feature/agenttrace/AgentTraceViewModel.kt`
- [NEW] `feature/agenttrace/AgentTraceAdapter.kt`
- [NEW] `fragment_agent_trace.xml`
- [NEW] `item_agent_trace.xml`

#### Alerts List
- [NEW] `feature/alerts/AlertsFragment.kt`
- [NEW] `feature/alerts/AlertsViewModel.kt`
- [NEW] `feature/alerts/AlertsAdapter.kt`
- [NEW] `fragment_alerts_list.xml`
- [NEW] `item_alert.xml`

#### Supporting Data Layer
- [NEW] `data/remote/firestore/AgentTraceFirestoreSource.kt`
- [NEW] `data/remote/firestore/document/AgentTraceDocument.kt`
- [NEW] `data/repository/AgentTraceRepositoryImpl.kt`
- [NEW] `domain/usecase/GetAgentTracesUseCase.kt`
- [NEW] `domain/usecase/GetCrisisDetailUseCase.kt`

---

### Phase 5: Missing Persons (Report, Hub, Detail, Confirmation)

#### Report Missing Person
- [NEW] `feature/missing/ReportMissingFragment.kt`
- [NEW] `feature/missing/ReportMissingViewModel.kt`
- [NEW] `fragment_report_missing.xml`

#### Confirmation
- [NEW] `feature/missing/ConfirmationFragment.kt`
- [NEW] `fragment_confirmation.xml`

#### Missing Persons Hub
- [NEW] `feature/missing/MissingHubFragment.kt`
- [NEW] `feature/missing/MissingHubViewModel.kt`
- [NEW] `feature/missing/MissingPersonAdapter.kt`
- [NEW] `fragment_missing_hub.xml`
- [NEW] `item_missing_person.xml`

#### Missing Person Detail
- [NEW] `feature/missing/MissingDetailFragment.kt`
- [NEW] `feature/missing/MissingDetailViewModel.kt`
- [NEW] `fragment_missing_detail.xml`

#### Supporting Data Layer
- [NEW] `data/remote/firestore/MissingPersonFirestoreSource.kt`
- [NEW] `data/remote/firestore/document/MissingPersonDocument.kt`
- [NEW] `data/repository/MissingPersonRepositoryImpl.kt`
- [NEW] `domain/usecase/SubmitMissingPersonReportUseCase.kt`
- [NEW] `domain/usecase/GetMissingPersonsUseCase.kt`
- [NEW] `domain/usecase/GetMissingPersonDetailUseCase.kt`
- [NEW] `domain/usecase/MarkPersonFoundUseCase.kt`

---

### Phase 6: Profile, Room Cache, FCM, Polish

#### Profile
- [NEW] `feature/profile/ProfileFragment.kt`
- [NEW] `feature/profile/ProfileViewModel.kt`
- [NEW] `fragment_profile.xml`

#### Room Database
- [NEW] `data/local/db/NishaanDatabase.kt`
- [NEW] `data/local/db/CrisisDao.kt`
- [NEW] `data/local/db/CrisisEntity.kt`
- [NEW] `data/local/db/MissingPersonDao.kt`
- [NEW] `data/local/db/MissingPersonEntity.kt`

#### FCM Service
- [NEW] `core/firebase/NishaanFcmService.kt`
- [NEW] `core/firebase/FirebaseManager.kt`

#### DataStore
- [NEW] `data/local/datastore/UserPreferences.kt`

#### Reusable Custom Views
- [NEW] `core/ui/SeverityBadgeView.kt` + layout
- [NEW] `core/ui/LoadingStateView.kt` + layout
- [NEW] `core/ui/EmptyStateView.kt` + layout

#### Vector Drawables
- [NEW] `ic_flood.xml`, `ic_earthquake.xml`, `ic_heatwave.xml`, `ic_unrest.xml`, `ic_accident.xml`, `ic_infrastructure.xml`
- [NEW] `ic_nishaan_logo.xml` (app wordmark)

---

## Verification Plan

### Automated Tests
- Gradle sync: `.\gradlew.bat :app:dependencies` — confirms all dependencies resolve
- Build: `.\gradlew.bat :app:assembleDebug` — confirms compilation
- Lint: `.\gradlew.bat :app:lint` — no critical issues

### Manual Verification
- Navigation flow: Splash → Onboarding → Language → Permissions → Auth → Home
- Dark theme renders correctly
- Bottom navigation switches tabs
- Map loads (requires API key)
- Missing person form submits (requires Firebase)

---

## File Count Estimate

| Category | Count |
|---|---|
| Kotlin files | ~55 |
| XML layouts | ~20 |
| Resource files (colors, strings, dimens, styles) | ~8 |
| Navigation graph | 1 |
| Drawable vectors | ~8 |
| Gradle config | ~4 |
| **Total new/modified files** | **~96** |

---

> [!NOTE]
> This is a large build. I'll proceed phase by phase, ensuring each phase compiles before moving to the next. Each phase will be logged in `ANTIGRAVITY_DEV_LOG.md`.

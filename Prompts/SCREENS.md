# SCREENS.md — NISHAAN Screen-by-Screen Specification

> Every screen in the app is fully described here. UI elements, layout, buttons, inputs, actions, and navigation targets.

---

## Screen 1: SplashScreen

**File:** `fragment_splash.xml` / `SplashFragment.kt`
**Duration:** 1.5 seconds, then auto-navigate

### UI Elements
- Full-screen dark background (`color_background`)
- NISHAAN logo Foreground (`logo.png` scaled with dynamic background color `#1C7556`) — centered, animated: fade in over 500ms
- Arabic/Urdu wordmark "نشان" below the logo — `text_headline`, `color_primary`
- Tagline: "The Signal" — `text_caption`, `color_on_surface_muted`
- No buttons, no inputs

### Logic
- On attach: check `DataStore` for `onboarding_complete` flag
  - If false → navigate to `OnboardingFragment`
  - If true → check Firebase Auth `currentUser`
    - If null → navigate to `AuthFragment`
    - If exists → navigate to `HomeDashboardFragment`
- Navigation happens after 1.5s delay (not on logo tap)

---

## Screen 2: OnboardingScreen

**File:** `fragment_onboarding.xml` / `OnboardingFragment.kt`
**Type:** ViewPager2 with 3 slides

### Slide Layout (each slide)
- Full-screen background: `color_background`
- Illustration area (top 50%): custom vector drawable per slide
  - Slide 1: autonomous radar/signal icon animation
  - Slide 2: missing person search icon
  - Slide 3: notification/alert bell icon
- Slide title: `text_display`, `color_on_background`, centered
- Slide body: `text_body_large`, `color_on_surface`, centered, max 2 lines

### Slide Content
| Slide | Title (EN) | Body (EN) |
|---|---|---|
| 1 | "Always Watching" | "NISHAAN monitors crisis signals 24/7 — floods, heatwaves, unrest — automatically." |
| 2 | "Find the Missing" | "Submit a missing person report. Our AI links it to the nearest crisis and notifies rescue teams." |
| 3 | "Stay Ahead" | "Receive geofenced alerts before danger reaches your location." |

### Navigation Controls
- Dot indicator (bottom center): 3 dots, active dot `color_primary`, inactive `color_divider`
- "Skip" text button (top right): navigates directly to `LanguageSelectFragment`
- "Next" primary button (bottom right): advances slide
- On last slide: "Next" becomes "Get Started" — navigates to `LanguageSelectFragment`
- Swipe gesture also advances slides

---

## Screen 3: LanguageSelectScreen

**File:** `fragment_language_select.xml` / `LanguageSelectFragment.kt`

### UI Elements
- Title: "Choose Your Language / زبان منتخب کریں" (bilingual)
- Two large selection cards (full width, stacked vertically):
  - Card 1: "English" — subtitle: "English"
  - Card 2: "اردو" — subtitle: "Urdu"
- **Note:** Roman Urdu selection has been completely removed to prioritize a clean English/Urdu dual layout.
- Each card: `NishaanCard` style, language name in `text_headline`, a checkmark icon on the right (hidden by default, shown when selected)
- "Continue" primary button (bottom) — disabled until selection made

### Actions
- Tap a card: mark it selected (checkmark visible, border highlights in `color_primary`), deselect others
- Tap "Continue": save language to DataStore
  - If opened from Settings: refresh locale via `LocaleHelper.wrap`, pop back stack, and recreate MainActivity.
  - If opened from Onboarding: navigate to `PermissionsFragment`.

---

## Screen 4: PermissionsScreen

**File:** `fragment_permissions.xml` / `PermissionsFragment.kt`

### UI Elements
- Title: "Permissions Required"
- Subtitle: "NISHAAN needs these to protect you"
- Permission list (3 items, each a row):
  - Location (icon: `ic_location`) — label: "Location Access" — description: "To alert you when a crisis is nearby" — status chip: "Required"
  - Notifications (icon: `ic_notification`) — label: "Push Notifications" — description: "To receive crisis and missing person alerts" — status chip: "Required"
  - Camera (icon: `ic_camera`) — label: "Camera & Photos" — description: "To attach photos to missing person reports" — status chip: "Optional"
- Each row shows a ✅ / ❌ icon once the system dialog is answered
- "Grant Permissions" primary button (bottom)
- "Skip Camera" text button (bottom, below primary) — only shown if camera is the only ungranted permission

### Actions
- Tap "Grant Permissions": requests Location, then Notifications, then Camera in sequence using `ActivityResultContracts.RequestPermission`
- If required permissions denied:
  - Show rationale bottom sheet: "NISHAAN cannot alert you without location access. Please enable it in Settings."
  - Show "Open Settings" button in bottom sheet
- If all required permissions granted: "Grant Permissions" becomes "Continue" → navigate to `AuthFragment`

---

## Screen 5: AuthScreen

**File:** `fragment_auth.xml` / `AuthFragment.kt`

### UI Elements
- Logo (small, top center)
- Title: "Sign In"
- Email input: `TextInputLayout` outlined, label "Email Address"
- Password input: `TextInputLayout` outlined, label "Password", password toggle visible
- "Sign In" primary button (full width)
- Divider: "— or —"
- "Continue as Guest" secondary outline button (full width)
- "Create Account" text button (bottom center) — navigates to `SignupFragment`
- Error message: inline below password field, `color_primary` (red), hidden by default

### Actions
- Tap "Sign In": validate inputs (non-empty, valid email format) → call Firebase Auth `signInWithEmailAndPassword` → on success navigate to `HomeDashboardFragment` → on failure show inline error
- Tap "Continue as Guest": call Firebase Auth `signInAnonymously` → navigate to `HomeDashboardFragment`
- Loading state: disable all inputs/buttons, show spinner inside "Sign In" button

---

## Screen 6: HomeDashboardScreen

**File:** `fragment_home_dashboard.xml` / `HomeDashboardFragment.kt`
**Root destination of nav graph**

### UI Elements

**Top Bar (custom, not Toolbar)**
- Left: NISHAAN wordmark (small)
- Right: Notification bell icon (badge count if unread alerts > 0)

**Alert Banner (below top bar)**
- Horizontal `RecyclerView` of `CrisisCard` items sorted by severity, then creation date.
- Shows top 5 most recent active crises.
- Each card: severity badge, crisis type icon, title, distance from user, time ago.
- If no active crises: banner hidden (View.GONE).

**Geofence Safety Status Card (top center, below Alert Banner)**
- Premium dynamic card layout (`cardSafetyStatus`) displayed at the top to provide instant safety feedback and permissions shortcuts.
- **Instant Load**: Card is visible immediately when `onViewCreated` starts, initializing in a disabled fallback state so users have instant visual feedback and clickability.
- Displays three dynamic visual styles:
  1. **Warning State (Red Danger Style)**: User is inside an active crisis geofence (`impactRadiusKm`). Shows red background, alert icon, and warning text: `"Warning: Inside Crisis Area! (Crisis Title)"`.
  2. **Safe State (Green Safe Style)**: User is far from any active incidents. Shows green background, info icon, and reassuring text: `"You're Safe: No active crises nearby"`.
  3. **Location Disabled Fallback State (Neutral Style)**: Location services disabled, permissions denied, or lastLocation is empty. Shows theme-safe surface colors, info icon, and text: `"Location disabled: Showing active crises across Pakistan"`.
- **Subview-forwarded Clicks**: Card container, icon, and text all bind to a shared click listener:
  * **Location Disabled**: Tap instantly launches the standard Android location permission popup prompt.
  * **Location Enabled**: Tap animates the Google Map camera back to center on the user's live coordinates at a premium zoom level of `14f`.

**Map (center, full remaining height)**
- Google Maps SDK with custom sleek Dark theme.
- **Dynamic Auto-Zooming Viewport**: Map builder dynamically bounds both the user location (if available) and all active crises into a single perfectly-fit camera frame on launch.
- **Fallback Centering**: If location services are disabled, camera centers directly on the whole of Pakistan (`LatLng(30.3753, 69.3451)`) at wide zoom `5.5f` framing the entire country.
- Crisis markers and impact geofence circles render on coordinates resolved by the backend agent (see backend coordinate lookup and dispersion).
- Tapping a marker shows info window (crisis title + severity badge) → tap info window → navigates to `CrisisDetailFragment`.

**FAB**
- Extended: "Report Missing" label + `ic_person_add` icon.
- Tap → navigates to `ReportMissingFragment`.

**Bottom Navigation**
- Tabs: Map (home_filled) | Alerts (notification) | Missing (search_person) | Profile (account_circle)

---

## Screen 7: CrisisDetailScreen

**File:** `fragment_crisis_detail.xml` / `CrisisDetailFragment.kt`
**Opened from:** map marker, alert card, alerts list

### UI Elements

**Header**
- Crisis type icon (large, 40dp) + type label
- Severity badge (pill, colored by severity)
- Confidence score: `text_mono`, e.g. "Confidence: 87%"
- Timestamp: "Detected 14 minutes ago"
- Location: area name (reverse-geocoded from centroid)

**Section: Map Thumbnail**
- Small Google Maps `MapView` (non-interactive) showing crisis centroid and impact radius circle
- "View Full Map" text button → returns to `HomeDashboard` with map centered on this crisis

**Section: Agencies Assigned**
- Horizontal chip list of assigned agency names (e.g., "Rescue 1122", "NDMA")
- Each chip: agency icon + name, non-interactive

**Section: Evacuation Routes**
- List of route names (if any) — each row has a `ic_directions` icon + route name
- Tap: opens Google Maps app with the route

**Section: Missing Persons**
- Label: "[N] missing persons linked to this crisis"
- Tap row → `MissingHubFragment` filtered to this crisis

**Section: Agent Trace (Preview)**
- Shows last 3 trace entries from `agent_traces` (most recent first)
- Each entry: agent color dot + agent name + action + timestamp
- "View Full Agent Trace →" text button → `AgentTraceFragment`

**Back navigation:** system back / toolbar back arrow → pop back stack

---

## Screen 8: ReportMissingScreen

**File:** `fragment_report_missing.xml` / `ReportMissingFragment.kt`
**Multi-step form, 3 steps shown via Step-by-step layout transition**

### Step 1: Personal Details
- Input: Full Name (required) — `TextInputLayout`, label "Full Name / پورا نام"
- Input: Age (required) — numeric keyboard, label "Age"
- Input: Gender (required) — `MaterialButtonToggleGroup` with 3 options: Male / Female / Other
- Input: Physical Description (optional) — multiline `TextInputLayout`, label "Description (clothing, marks, etc.)", max 500 characters, character counter shown
- "Next →" button

### Step 2: Location & Photo
- Label: "Last Seen Location"
- Map (small, interactive) — user places a pin on the map
- "OR type an address" text button → shows address `TextInputLayout` (geocoded on submit)
- Photo section: "Add Photo (optional)"
  - Large dashed-border tap zone: "Tap to add photo"
  - Tap → bottom sheet: "Camera" or "Gallery"
  - After selection: photo thumbnail shown with "Remove" X button
  - File size validated client-side: > 5MB → error "Photo too large. Max 5MB."
- "Next →" / "← Back" buttons

### Step 3: Your Contact Info
- Input: Phone Number (required) — phone keyboard, label "Your Phone Number"
- Input: Your Relationship (optional) — dropdown: Father / Mother / Sibling / Spouse / Friend / Other
- Checkbox: "I confirm this report is accurate to the best of my knowledge"
- "Submit Report" primary button — disabled until checkbox checked

### Actions on Submit
- Show loading overlay (semi-transparent over form, spinner centered)
- Upload photo → write to Firestore → on success → navigate to `ConfirmationFragment`
- On error → dismiss loading → show Snackbar "Submission failed. Please try again."

---

## Screen 9: ConfirmationScreen

**File:** `fragment_confirmation.xml` / `ConfirmationFragment.kt`

### UI Elements
- Large checkmark icon (animated, draw-on effect, `color_accent_low`)
- Title: "Report Submitted"
- Body: "Your report for [Name] has been submitted. Rescue teams have been notified. You will receive a notification if a match is found."
- Reference ID: "Report ID: [first 8 chars of report_id]" — `text_mono`, `color_on_surface_muted`
- "Back to Home" primary button

### Actions
- "Back to Home" → `popBackStack` to `HomeDashboardFragment`
- Back press → same as "Back to Home" (custom `OnBackPressedCallback`)
- **Do not allow navigating back to the form** — report already submitted

---

## Screen 10: MissingPersonsHubScreen

**File:** `fragment_missing_hub.xml` / `MissingHubFragment.kt`

### UI Elements
- Tab row: "All" | "By Crisis" | "Unlinked"
- Each tab drives a filtered `RecyclerView` of `MissingPersonCard` items
- Sort: newest first by default

**MissingPersonCard (item_missing_person.xml)**
- Photo thumbnail (circular, 48dp) — placeholder if no photo
- Name + age + gender
- Status chip: "Searching" (blue) | "Linked to [Crisis Type]" (severity color) | "Found" (green)
- Last seen: location name + "X hours ago"
- Rescue notified indicator: small icon if agencies have been notified

### Filter Behavior
- "By Crisis" tab: groups cards under crisis type headers (e.g., "FLOOD — Gulshan")
- "Unlinked" tab: shows reports with no `linked_crisis_id`
- Search bar (top, always visible): filters by name in real-time (client-side on loaded list)

### Actions
- Tap card → `MissingDetailFragment`
- Pull to refresh → re-fetches from Firestore

---

## Screen 11: MissingPersonDetailScreen

**File:** `fragment_missing_detail.xml` / `MissingDetailFragment.kt`

### UI Elements
- Photo (full-width header image or avatar if no photo)
- Name (large), Age, Gender
- Description text (full)
- Last seen location (address + small map thumbnail, non-interactive)
- Linked crisis section (if linked): crisis type badge + crisis title + link chip → `CrisisDetailFragment`
- Status section: "MATCHER Status: [status]" — last MATCHER trace entry for this report
- Reporter contact: phone number (formatted, tap to copy)
- "Mark as Found" secondary button (only visible to the submitting user)
  - Tap → confirmation dialog "Mark [Name] as found?" → Yes → updates Firestore status to `FOUND` → navigates back

---

## Screen 12: AgentTraceViewScreen

**File:** `fragment_agent_trace.xml` / `AgentTraceFragment.kt`
**Opened from:** CrisisDetailScreen "View Full Agent Trace" button

### UI Elements
- Title: "Agent Trace — [Crisis Title]"
- Subtitle: "Real-time AI decision log"
- `RecyclerView` of `AgentTraceItem` (item_agent_trace.xml):
  - Vertical timeline layout — connecting line between items
  - Agent color dot (left, colored by agent)
  - Agent name badge: `SENTINEL` | `ANALYST` | `COMMANDER` | `MATCHER`
  - Timestamp: `text_mono`, e.g. "14:32:07"
  - Action label: `text_body_medium`, bold
  - Reasoning summary: `text_body_medium`, `color_on_surface`
  - Confidence: `text_mono` if present, e.g. "87%"
- Real-time Firestore listener — new items appear at top with slide-down animation
- "NEW ACTIVITY" floating chip appears if user has scrolled down and a new item arrives — tap chip to scroll to top

### Actions
- No user input — read-only
- Back → `CrisisDetailFragment`

---

## Screen 13: AlertsListScreen

**File:** `fragment_alerts_list.xml` / `AlertsFragment.kt`

### UI Elements
- Title: "Alerts"
- `RecyclerView` of active crises real-time listener (sorted by severity then date).
- Each row: crisis type icon (colored by severity) | title | location | "X km away" | time ago
- Empty state (no alerts yet): `EmptyStateView` with signal icon + "No alerts yet. Stay safe."

### Actions
- Tap row → `CrisisDetailFragment` for that crisis_id

---

## Screen 14: SettingsScreen

**File:** `fragment_settings.xml` / `SettingsFragment.kt`
**Opened from:** ProfileScreen Settings navigation click

### UI Elements
- Title: "Settings"
- Subtitle: "Application preferences and configuration"
- Section: App Settings (language preference change button)
- **Sign Out Button**: Premium, destructive-styled primary button (`btnSignOut`) positioned at the bottom of the Settings interface.

### Actions
- Tap Language Settings → opens `LanguageSelectFragment` to update app-wide language locale dynamically.
- Tap **Sign Out**: Shows confirmation dialog `"Sign out of NISHAAN?"` → Yes → calls Firebase `signOut()` → clears MainActivity back stack and redirects user directly back to `AuthFragment`.

---

## Screen 15: ProfileScreen

**File:** `fragment_profile.xml` / `ProfileFragment.kt`

### UI Elements
- Avatar (initials-based generated avatar — no profile photo feature)
- Display name (editable via inline tap → text field)
- Email (non-editable, grayed out for anonymous: "Guest Account")
- Language preference: current language chip, tap → opens settings
- Section: Notification Preferences
  - Toggle rows: Critical Alerts | Medium Alerts | Low Alerts | Missing Person Matches
  - Each toggle writes to Firestore `users/{uid}.notification_prefs`
- Section: My Reports
  - List of user's submitted missing person reports (up to 5 shown, "See all" link → full list)
- "Create Account" banner (if guest, top of screen, dismissible)
- **Settings Icon**: Positioned at the top right of the profile interface. Tap → navigates to `SettingsFragment`.
- **Note**: The "Sign Out" button has been completely removed from this screen and moved inside Screen 14 (SettingsScreen).

### Actions
- Tap Settings → navigates to `SettingsFragment`.
- Guest "Create Account" tap → redirects back to `AuthFragment` (registration mode) or displays a helpful Snackbar.

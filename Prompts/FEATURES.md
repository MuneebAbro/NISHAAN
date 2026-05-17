# FEATURES.md — NISHAAN Feature Behavior Specification

> Defines what every feature does, how it behaves, edge cases, constraints, and logic rules. AI agents must implement features exactly as described here.

---

## Feature 1: Autonomous Agent Pipeline

### Overview
A 4-agent system orchestrated by Google Antigravity that runs a full crisis detection cycle every 60 seconds. No human action required at any point.

### SENTINEL Agent
- **Poll interval:** Every 60 seconds, on a scheduled Cloud Function trigger
- **Data sources polled:**
  - Twitter/X mock feed (pre-seeded JSON, filtered by Urdu, Roman Urdu, English crisis keywords)
  - PMD weather API mock (returns alert objects with severity and region)
  - Google Maps Traffic API (real — detects anomalous traffic patterns as crisis signal)
  - NDMA official feed mock (returns structured alert JSON)
  - Firestore `eyewitness_reports` collection (citizen-submitted reports)
- **Keyword extraction:** Gemini API call — system prompt instructs classification of incoming text into crisis signals. Handles Urdu script, Roman Urdu, and English in a single prompt.
- **Output:** Writes raw signal objects to Firestore `signals` collection with `source`, `raw_text`, `extracted_keywords[]`, `location`, `timestamp`, `agent: "SENTINEL"`
- **Edge cases:**
  - If a source times out: skip it, log the failure to `agent_traces`, continue with remaining sources
  - If all sources fail: write a `status: "DEGRADED"` trace, do not write to `signals`, retry in 60s
  - Duplicate signals within 5 minutes from the same source and location: deduplicated by hash

### ANALYST Agent
- **Trigger:** Fires after SENTINEL writes new signals, or on a 60s cycle over unanalyzed signals
- **Logic:**
  - Reads the latest N signals (N=20 max) from the last 10 minutes
  - Cross-references: requires **3 or more** independent signals before escalating (avoids false positives from a single noisy source)
  - Calls Gemini to classify: crisis type, severity level, geographic centroid, confidence score
  - Crisis types: `FLOOD`, `EARTHQUAKE`, `HEATWAVE`, `CIVIL_UNREST`, `TRAFFIC_ACCIDENT`, `INFRASTRUCTURE_FAILURE`, `UNKNOWN`
  - Severity levels: `CRITICAL`, `HIGH`, `MEDIUM`, `LOW`, `MONITORING`
  - Confidence score: 0–100 integer, derived from Gemini's reasoning
- **Output:** Writes to Firestore `crises` collection. If crisis already exists: updates `severity`, `confidence`, `updated_at`. If new: creates new document.
- **Thresholds:**
  - Confidence < 40: write as `status: MONITORING` — no alerts sent yet
  - Confidence 40–69: write as `status: ACTIVE` — alerts sent to agencies only
  - Confidence ≥ 70: write as `status: CONFIRMED` — citizens alerted via FCM
- **Edge cases:**
  - If cross-referencing produces conflicting crisis types from same signals: Gemini picks majority + logs uncertainty to trace
  - If signals are geographically scattered (>50km apart): treat as separate crises, not one

### COMMANDER Agent
- **Trigger:** Fires when ANALYST confirms a crisis with confidence ≥ 40
- **Logic:**
  - Reads crisis type and severity from `crises` document
  - Agency assignment rules (hard-coded logic, not AI-decided):
    - FLOOD → Rescue 1122, NDMA, Pakistan Army (if CRITICAL)
    - EARTHQUAKE → Rescue 1122, NDMA, Pakistan Army, Civil Defence
    - HEATWAVE → Health Department, NDMA
    - CIVIL_UNREST → Police, Rangers (if CRITICAL)
    - TRAFFIC_ACCIDENT → Rescue 1122, Police, Edhi Foundation
    - INFRASTRUCTURE_FAILURE → WASA, NEPRA, relevant utility authority
  - Calls mock agency APIs (Node.js stubs) with crisis_id and dispatch instructions
  - Writes agency assignment to `crises.assigned_agencies[]`
  - Sends FCM to all users whose last known location is within **3km** of crisis centroid (only if confidence ≥ 70)
  - FCM payload: `{ crisis_type, severity, title_en, title_ur, body_en, body_ur, crisis_id, deep_link }`
  - Deep link opens CrisisDetailScreen for that crisis_id
- **Edge cases:**
  - If FCM send fails for a user: log failure, retry once after 30s, then mark as `notification_failed`
  - If agency mock API returns error: log to `agent_traces`, continue (non-blocking)
  - Do not re-alert the same user for the same crisis within 30 minutes unless severity escalates

### MATCHER Agent
- **Trigger:** Fires when a new missing person report is submitted OR when ANALYST creates/updates a crisis
- **Logic:**
  - For each open missing person report (status: `SEARCHING`), computes geographic proximity to all active crises
  - If last seen location of missing person is within **5km** of any active crisis: links the report to that crisis (`linked_crisis_id`)
  - For linked reports, runs semantic similarity matching:
    - Generates text embedding of missing person description using Google Embedding API
    - Compares against descriptions of all other missing persons linked to the same crisis (cluster)
    - Similarity score ≥ 0.85: flag as potential duplicate / same person reported by multiple families
    - Currently: only groups reports, does not auto-resolve (human verification step assumed)
  - On match or link: updates Firestore `missing_persons` document, sends FCM to family's registered device
  - FCM to family: "Your report for [Name] has been linked to a [FLOOD] crisis in [Area]. Rescue teams have been notified."
- **Edge cases:**
  - Missing person with no photo: semantic matching on text description only
  - Multiple reports for same person (duplicate names + locations): MATCHER clusters them, marks them `potential_duplicate`
  - If crisis resolves while missing person still `SEARCHING`: keep link, change crisis status display to "Resolved" — person still needs to be found

---

## Feature 2: Live Crisis Map

- Displays all crises with `status: ACTIVE` or `status: CONFIRMED` from Firestore `crises` collection
- Real-time listener — map updates within ~2 seconds of a Firestore write
- Each crisis = a custom marker colored by severity
- Tap marker → CrisisDetailScreen
- Geofence circles shown around each crisis centroid (radius corresponds to estimated impact zone, stored in crisis document)
- Map style: dark JSON style (see UI_GUIDE.md)
- User location shown as blinking dot
- Clusters markers when zoomed out (Google Maps clustering library)
- Map is always full-screen behind the overlay cards — never shrunk to a partial view

---

## Feature 3: Missing Person Report Submission

### Rules
- **Required fields:** Full name, age, gender, last seen location (either map pin OR text address), reporter's phone number
- **Optional fields:** Physical description, photo, relationship to missing person
- **Photo:** Max 1 photo per report. Max file size: 5MB. Accepted formats: JPG, PNG. Compressed to 800px longest side before upload.
- **Location:** Map pin is preferred. If user types an address, geocode it via Google Geocoding API before storing.
- **Phone number:** Validated as Pakistani format (+92 or 0 prefix, 10–11 digits)
- **Language of submission:** Any (Urdu, Roman Urdu, English all accepted — stored as-is for Gemini to process)
- A single user (by auth UID) can submit **max 10** active missing person reports. Beyond that, show "Please contact authorities directly for additional reports."
- Reports cannot be deleted by the user once submitted (to prevent abuse). They can be marked "Person Found" which closes the report.

### Submission Flow
1. Client validates all required fields before network call
2. Upload photo to Firebase Storage (if provided)
3. Write report to Firestore `missing_persons` with status: `SEARCHING`
4. Cloud Function triggers MATCHER agent
5. Show confirmation screen

---

## Feature 4: Geofenced FCM Alerts

- Alert is sent when COMMANDER agent confirms a crisis with confidence ≥ 70
- Target radius: 3km from crisis centroid
- Targeting: users whose last stored location in Firestore `users.last_location` falls within radius
- Location stored: app updates `users.last_location` every time the app comes to foreground (not continuous background tracking — battery consideration)
- Notification tapping opens the app directly to CrisisDetailScreen via deep link
- Notification channels (Android):
  - `channel_critical` — HIGH/CRITICAL severity (default sound, heads-up display)
  - `channel_informational` — LOW/MEDIUM severity (silent, no heads-up)
  - `channel_missing_persons` — MATCHER match notifications (default sound)
- Users can disable individual channels in OS settings (respected)

---

## Feature 5: Agent Trace View

- Real-time Firestore listener on `agent_traces` collection, filtered by `crisis_id`
- Each trace entry contains: `agent_name`, `timestamp`, `action`, `confidence` (if applicable), `reasoning_summary` (1–2 sentence Gemini-generated summary of why the action was taken)
- Displayed as a chronological timeline, newest at top
- Auto-scrolls to top when new entry arrives (with a "New activity" toast if user is scrolled down)
- Monospace font for all trace text
- Entries are color-coded by agent:
  - SENTINEL: `color_accent_info` (blue)
  - ANALYST: `color_secondary` (amber)
  - COMMANDER: `color_primary` (red)
  - MATCHER: `color_accent_low` (teal)
- Trace data is read-only. No user interaction except scroll.
- Limitation: trace is per-crisis only. There is no global trace view of all agent activity in the app.

---

## Feature 6: Multilingual Support

- Supported languages: English, اردو (Urdu script), Roman Urdu
- Language selected during onboarding, changeable in Profile
- All static UI strings translated for English and Urdu in `res/values/strings.xml` and `res/values-ur/strings.xml`
- Roman Urdu: treated as English locale for the purpose of Android resource resolution (no separate locale — just rendered in Latin script with Urdu phonetics)
- Dynamic content from Firestore (crisis descriptions, agent traces): stored in three fields `text_en`, `text_ur`, `text_roman_ur`. App displays based on user language setting.
- Gemini prompts for NLP include explicit instruction to handle all three languages in a single call.

---

## Feature 7: Guest vs Authenticated Users

| Capability | Guest | Authenticated |
|---|---|---|
| View crisis map | ✅ | ✅ |
| Receive FCM alerts | ✅ | ✅ |
| View missing persons list | ✅ | ✅ |
| View agent traces | ✅ | ✅ |
| Submit missing person report | ❌ | ✅ |
| View own submitted reports | ❌ | ✅ |
| Receive MATCHER match notifications | ❌ | ✅ |

Guest users see a non-intrusive banner on the Report Missing screen: "Sign in to submit a report" with a Sign In button. No forced gate on any read-only screen.

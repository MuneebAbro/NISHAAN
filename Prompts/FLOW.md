# FLOW.md — NISHAAN User Journey & Navigation Structure

> This file defines every user path through the app, how screens connect, and what triggers each transition.

---

## App Entry Points

```
Cold Launch
│
├── First time user → Onboarding Flow → Language Selection → Permissions → Auth
│
└── Returning user → Splash (1.5s logo) → Home Dashboard
```

---

## Flow 1: Onboarding & Auth

```
SplashScreen (1.5s)
│
└── [First time?]
    ├── YES → OnboardingScreen (3 slides)
    │           Slide 1: "نشان monitors crises 24/7 automatically"
    │           Slide 2: "Report missing persons instantly"
    │           Slide 3: "Get alerts before danger reaches you"
    │           → LanguageSelectScreen
    │               Options: اردو | Roman Urdu | English
    │               → PermissionsScreen
    │                   Requests: Location (required), Notifications (required), Camera (optional)
    │                   [All required granted?]
    │                   ├── YES → AuthScreen
    │                   └── NO  → Show "Required for alerts" rationale → Re-request
    │
    └── NO → HomeDashboard
```

### Auth Screen
- Options: **Sign In** (email + password) | **Continue as Guest** (anonymous auth)
- Guest users can view the map and receive alerts but cannot submit missing person reports without upgrading to a real account
- On successful auth → Home Dashboard
- On failure → Inline error message (no separate error screen)

---

## Flow 2: Home Dashboard (Hub)

The Home Dashboard is the **root destination**. All major features radiate from here.

```
HomeDashboard
│
├── Crisis Map (center of screen, always visible)
│   └── [Tap crisis marker] → CrisisDetailScreen
│
├── Alert Cards (horizontal scroll, top)
│   └── [Tap card] → CrisisDetailScreen
│
├── Bottom Navigation Bar
│   ├── 🗺 Map → HomeDashboard (current)
│   ├── 🚨 Alerts → AlertsListScreen
│   ├── 🔍 Missing → MissingPersonsHubScreen
│   └── 👤 Profile → ProfileScreen
│
└── FAB (Floating Action Button) → ReportMissingScreen
```

---

## Flow 3: Crisis Detail

```
CrisisDetailScreen (entered from map marker or alert card)
│
├── Crisis Info Section: type, severity badge, confidence score, timestamp
├── Assigned Agencies List: Rescue 1122, NDMA, Police, etc.
├── Evacuation Routes: Maps overlay with suggested routes
├── Related Missing Persons count → [Tap] → MissingPersonsHubScreen (filtered to this crisis)
└── Agent Trace Section → [Tap "View Full Trace"] → AgentTraceViewScreen
```

---

## Flow 4: Report Missing Person

```
FAB on HomeDashboard → ReportMissingScreen
│
├── Step 1 — Personal Details
│   Name (Urdu/English), Age, Gender, Physical description (text area)
│
├── Step 2 — Location & Photo
│   Last seen location (map pin or address text)
│   Photo upload (camera or gallery, max 1 photo, max 5MB)
│
├── Step 3 — Contact Info
│   Reporter's phone number, Relationship to missing person
│
└── [Submit]
    ├── Validates all required fields
    ├── Uploads photo to Firebase Storage
    ├── Writes report to Firestore `missing_persons` collection
    ├── Triggers MATCHER agent via Cloud Function
    └── Success → ConfirmationScreen
                  "Your report has been submitted. You will be notified if a match is found."
                  [Back to Home] button
```

---

## Flow 5: Missing Persons Hub

```
MissingPersonsHubScreen
│
├── Filter tabs: All | By Crisis | Unlinked
│
├── Each case card shows: photo, name, age, last seen location, linked crisis (if any), time since report
│
└── [Tap case card] → MissingPersonDetailScreen
    ├── Full profile info
    ├── Linked crisis (with link to CrisisDetailScreen)
    ├── Match status: "Searching..." | "Match found — family notified"
    └── MATCHER agent trace for this case (latest action)
```

---

## Flow 6: Agent Trace View (Judges Feature)

```
AgentTraceViewScreen (accessible from CrisisDetailScreen)
│
└── Real-time Firestore listener on `agent_traces` collection (filtered by crisis_id)
    │
    ├── Timeline of agent decisions, newest first
    │   Each entry shows:
    │   [AGENT_NAME] [timestamp] [action taken] [confidence: XX%]
    │   Example: [ANALYST] 14:32:07 → "Classified as FLOOD | Severity: HIGH | Confidence: 91%"
    │
    └── Auto-scrolls as new trace entries appear
        No user action required — purely observational
```

---

## Flow 7: Alerts List

```
AlertsListScreen
│
├── Full list of FCM alerts received by this user
├── Each alert: crisis type icon, title, location, time, distance from user
└── [Tap alert] → CrisisDetailScreen
```

---

## Flow 8: Profile

```
ProfileScreen
│
├── Name, email (if not guest), language preference
├── Notification preferences (toggle by crisis type)
├── My Reports (list of submitted missing person reports)
│   └── [Tap] → MissingPersonDetailScreen
└── Sign Out → AuthScreen
    Guest accounts shown "Create Account" CTA instead
```

---

## Autonomous Background Flow (No User Interaction)

This runs in the backend continuously. The app is a **viewer**, not a trigger.

```
Every 60 seconds:
SENTINEL polls → Twitter mock, PMD mock, NDMA mock, Google Maps Traffic, Firestore eyewitness reports
    ↓
Extracts crisis keywords (NLP, Urdu + Roman Urdu + English)
    ↓
ANALYST cross-references 3+ signals → classifies crisis type + severity + confidence score
    ↓
If confidence ≥ threshold:
COMMANDER → assigns agencies → writes to Firestore `crises` collection → triggers FCM to users within 3km
    ↓
MATCHER → links open missing person reports to new/updated crisis → semantic similarity matching → notifies families on match
    ↓
All decisions written to Firestore `agent_traces` collection → app observes in real-time
```

---

## Navigation Graph Summary

| From | To | Trigger |
|---|---|---|
| Splash | Onboarding | First launch |
| Splash | HomeDashboard | Returning user |
| Onboarding | LanguageSelect | Swipe through 3 slides |
| LanguageSelect | PermissionsScreen | Language chosen |
| PermissionsScreen | AuthScreen | Permissions granted |
| AuthScreen | HomeDashboard | Auth success |
| HomeDashboard | CrisisDetailScreen | Tap map marker or alert card |
| HomeDashboard | ReportMissingScreen | Tap FAB |
| HomeDashboard | MissingPersonsHubScreen | Bottom nav |
| HomeDashboard | AlertsListScreen | Bottom nav |
| HomeDashboard | ProfileScreen | Bottom nav |
| CrisisDetailScreen | AgentTraceViewScreen | "View Full Trace" tap |
| CrisisDetailScreen | MissingPersonsHubScreen | "X missing persons" tap |
| MissingPersonsHubScreen | MissingPersonDetailScreen | Tap case card |
| ReportMissingScreen | ConfirmationScreen | Successful submit |
| ConfirmationScreen | HomeDashboard | "Back to Home" |

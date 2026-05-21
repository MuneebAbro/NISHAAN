# NISHAAN — نشان — The Signal

> **Pakistan's Autonomous Crisis Detection & Missing Persons System**  
> Hackathon Challenge 3 — CIRO (Crisis Intelligence & Response Operations)

---

## Table of Contents

1. [Overview](#overview)
2. [Problem Statement](#problem-statement)
3. [Solution Design](#solution-design)
4. [System Architecture](#system-architecture)
5. [The 4-Agent AI Pipeline](#the-4-agent-ai-pipeline)
6. [Key Features](#key-features)
7. [Technology Stack](#technology-stack)
8. [APIs & Integrations](#apis--integrations)
9. [Database Design](#database-design)
10. [App Screens & Navigation](#app-screens--navigation)
11. [Design System](#design-system)
12. [Setup & Installation](#setup--installation)
13. [Real vs Mocked Components](#real-vs-mocked-components)

---

## Overview

**NISHAAN** (نشان — *"The Signal"*) is an Android-native application that operates two parallel intelligence layers 24/7:

1. **Autonomous Layer** — A 4-agent AI pipeline (SENTINEL → ANALYST → COMMANDER → MATCHER) that monitors signal sources every 60 seconds, detects emerging crises, classifies severity, dispatches relevant agencies, and pushes geofenced alerts to citizens — all without any human trigger.

2. **Citizen Layer** — A public-facing mobile interface where citizens can report missing persons, monitor a live crisis map, submit witness sightings, and receive real-time alerts relevant to their geographic location.

The name نشان means *"The Signal"* — a literal reference to the signals the system monitors and a conceptual statement about giving voice to crises that are often invisible until it's too late.

**NISHAAN is not a chatbot. It does not wait for instructions. It acts.**

---

## Problem Statement

Pakistan faces recurring crises — floods, heatwaves, earthquakes, urban unrest, traffic accidents — where the gap between an **event occurring** and **people knowing** costs lives. Key challenges include:

- **Delayed Detection:** Crises are detected only after significant damage, because no system continuously monitors multiple signal sources.
- **Fragmented Response:** Agencies (Rescue 1122, NDMA, Police) operate in silos with no automated dispatch coordination.
- **Missing Persons Gap:** During crises, missing persons go untracked because there is no system connecting family reports to active emergencies.
- **Information Asymmetry:** Citizens near a crisis zone have no way to receive geographically targeted warnings in real time.

NISHAAN closes all four gaps: **it detects the crisis, dispatches agencies, alerts citizens, and tracks the people lost inside it.**

---

## Solution Design

### Core Pipeline

```
Signal Sources ──► SENTINEL ──► ANALYST ──► COMMANDER ──► Citizens Alerted
                                                │
                     Missing Person Reports ──► MATCHER ──► Families Notified
```

Four AI agents form a closed loop from raw signal noise to citizen action:

1. **SENTINEL** continuously polls 5 signal sources (social media, weather APIs, traffic data, official feeds, citizen eyewitness reports).
2. **ANALYST** cross-references 3+ independent signals, classifies the crisis type and severity, and assigns a confidence score using Gemini.
3. **COMMANDER** dispatches relevant agencies based on crisis type and sends geofenced FCM push notifications to all citizens within 3km.
4. **MATCHER** links open missing person reports to active crises by geographic proximity and detects potential duplicates via semantic embedding similarity.

Every agent decision is logged to a transparent **Agent Trace** that is visible in the app in real time — giving judges (and citizens) full visibility into AI reasoning.

### Design Philosophy

- **Command-center aesthetic** — dark, high-contrast, data-dense UI inspired by emergency operations dashboards
- **Zero-trigger autonomy** — the backend runs continuously; the app is a viewer, not a trigger
- **Citizen trust through transparency** — every AI decision is logged with agent name, timestamp, confidence score, and reasoning summary

---

## System Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    ANDROID CLIENT                        │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌────────┐  │
│  │ Feature  │  │  Domain  │  │   Data   │  │  Core  │  │
│  │  Layer   │──│  Layer   │◄─│  Layer   │  │ Layer  │  │
│  │(Fragments│  │(Models,  │  │(Repos,   │  │(DI,    │  │
│  │ViewModels│  │Interfaces│  │Firestore)│  │Utils)  │  │
│  │Adapters) │  │UseCases) │  │          │  │        │  │
│  └──────────┘  └──────────┘  └──────────┘  └────────┘  │
│                        │                                 │
│              Firestore Real-Time Listeners               │
└────────────────────────┬────────────────────────────────┘
                         │
                    Firebase Services
                         │
        ┌────────────────┼────────────────┐
        │                │                │
   Firestore        Cloud Storage    Cloud Messaging
   (Database)       (Photos)         (Push Alerts)
        │
        │  ◄── Reads/Writes ──►
        │
┌───────┴─────────────────────────────────────────────────┐
│                   AI AGENT BACKEND                        │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐ │
│  │ SENTINEL │─►│ ANALYST  │─►│COMMANDER │  │ MATCHER  │ │
│  │(Poller)  │  │(Classif.)│  │(Dispatch)│  │(Linker)  │ │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘ │
│       │              │              │             │       │
│   Gemini API    Gemini API    Mock Agency   Embedding    │
│   (NLP)         (Classify)    APIs (Node)  API (Google)  │
└──────────────────────────────────────────────────────────┘
```

### Android Architecture (MVVM)

The app follows a **clean MVVM architecture** with strict layer separation:

| Layer | Responsibility | Imports From |
|---|---|---|
| **Feature** | Fragments, ViewModels, Adapters, Layouts | Domain + Core |
| **Domain** | Models, Repository Interfaces, Use Cases (pure Kotlin) | Nothing |
| **Data** | Repository Implementations, Firestore Sources | Domain |
| **Core** | DI Container, Network Config, Utilities, Map Helpers | Nothing |

**Key architectural decisions:**
- **Single Activity** architecture with Jetpack Navigation Component
- **ViewBinding** for all view references (no `findViewById`)
- **ViewModels** own all UI state via `LiveData<UiState<T>>` sealed class
- **Coroutines + Flow** for all async operations
- **Manual DI** via `AppContainer` singleton (no Hilt/Dagger for hackathon scope)
- **Firestore real-time listeners** for live data (no polling from app)
- All repository functions return `Result<T>` — errors never thrown, always wrapped

---

## The 4-Agent AI Pipeline

### SENTINEL — Signal Collector

| Property | Value |
|---|---|
| **Poll Interval** | Every 60 seconds (Cloud Function trigger) |
| **Data Sources** | Twitter/X mock, PMD weather mock, Google Maps Traffic (real), NDMA mock, citizen eyewitness reports |
| **NLP Engine** | Gemini API — handles Urdu, Roman Urdu, and English in a single prompt |
| **Output** | Raw signal objects → Firestore `signals` collection |
| **Deduplication** | SHA-256 hash; duplicates within 5 minutes are ignored |

### ANALYST — Crisis Classifier

| Property | Value |
|---|---|
| **Trigger** | After SENTINEL writes new signals, or 60s cycle |
| **Cross-Reference** | Requires **3+ independent signals** before escalating |
| **Classification** | Crisis type, severity, geographic centroid, confidence score (via Gemini) |
| **Crisis Types** | `FLOOD`, `EARTHQUAKE`, `HEATWAVE`, `CIVIL_UNREST`, `TRAFFIC_ACCIDENT`, `INFRASTRUCTURE_FAILURE` |
| **Severity Levels** | `CRITICAL`, `HIGH`, `MEDIUM`, `LOW`, `MONITORING` |

**Confidence Thresholds:**
| Confidence | Status | Action |
|---|---|---|
| < 40 | `MONITORING` | No alerts sent |
| 40 – 69 | `ACTIVE` | Agencies notified only |
| ≥ 70 | `CONFIRMED` | Citizens alerted via FCM |

### COMMANDER — Dispatch & Alert

| Property | Value |
|---|---|
| **Trigger** | When ANALYST confirms crisis with confidence ≥ 40 |
| **Agency Rules** | Hardcoded by crisis type (e.g., FLOOD → Rescue 1122, NDMA, Army) |
| **FCM Targeting** | Users within **3km** of crisis centroid (confidence ≥ 70) |
| **Rate Limiting** | No re-alert within 30 minutes unless severity escalates |

**Agency Assignment Matrix:**
| Crisis Type | Agencies Dispatched |
|---|---|
| FLOOD | Rescue 1122, NDMA, Pakistan Army (if CRITICAL) |
| EARTHQUAKE | Rescue 1122, NDMA, Pakistan Army, Civil Defence |
| HEATWAVE | Health Department, NDMA |
| CIVIL_UNREST | Police, Rangers (if CRITICAL) |
| TRAFFIC_ACCIDENT | Rescue 1122, Police, Edhi Foundation |
| INFRASTRUCTURE_FAILURE | WASA, NEPRA, relevant utility authority |

### MATCHER — Missing Person Linker

| Property | Value |
|---|---|
| **Trigger** | New missing person report OR crisis created/updated |
| **Geo-Matching** | Links report to crisis if last seen location is within **5km** |
| **Semantic Matching** | Google Embedding API; similarity ≥ 0.85 → `POTENTIAL_DUPLICATE` |
| **Family Notification** | FCM: "Your report for [Name] has been linked to a [FLOOD] crisis in [Area]" |

---

## Key Features

### 1. Live Crisis Map
- Google Maps SDK with custom dark theme
- Real-time Firestore listener — updates within ~2 seconds
- Severity-coded markers and geofence circles
- Dynamic auto-zooming viewport fitting user + all crises
- Fallback to Pakistan-wide view if location disabled

### 2. Geofence Safety Status Card
- **Warning (Red):** User inside active crisis zone
- **Safe (Green):** No nearby crises
- **Disabled (Neutral):** Location services off — tap to enable

### 3. Missing Person Reporting (3-Step Form)
- **Step 1:** Name, age, gender, physical description
- **Step 2:** Last seen location (map pin or address) + photo with WhatsApp-style 1:1 cropping (uCrop)
- **Step 3:** Reporter's phone + relationship + accuracy confirmation
- Photo compressed to 800px, max 5MB, uploaded to Firebase Storage
- Max 10 active reports per user; reports cannot be deleted (anti-abuse)

### 4. Geofenced FCM Push Notifications
- 3km radius targeting from crisis centroid
- Three notification channels: Critical (heads-up), Informational (silent), Missing Persons (default sound)
- Deep link opens crisis detail screen directly

### 5. Agent Trace View (AI Transparency)
- Real-time timeline of every AI decision
- Color-coded by agent: SENTINEL (blue), ANALYST (amber), COMMANDER (red), MATCHER (green)
- Shows: agent name, timestamp, action, reasoning summary, confidence score

### 6. Citizen Verification Voting
- Citizens vote YES / NO / UNSURE on active crises
- Aggregated votes adjust confidence modifier

### 7. Crisis Timeline & Spread Prediction
- Chronological audit trail of crisis evolution
- AI-predicted spread direction and radius

### 8. Witness Sighting Reports
- Citizens submit sightings for missing persons
- Bottom sheet interface with sighting time, neighborhood, visual details

### 9. Safe Evacuation Routes
- Dynamically calculated Bezier curve routing away from crisis centers
- Rendered as dashed green polyline on map

### 10. Multilingual Support
- English + اردو (Urdu script)
- All UI strings translated; dynamic content stored in `text_en` / `text_ur` fields
- Language selectable during onboarding and in settings

### 11. Dark/Light Theme System
- Dark theme (default): command-center aesthetic
- Light theme: inverted backgrounds, same brand colors
- User preference: Light / Dark / System Default

### 12. Full-Screen Image Viewer & WhatsApp-Style Cropping
- Tap any missing person photo for full-screen view
- All photo uploads enforced to 1:1 aspect ratio via uCrop

---

## Technology Stack

### Android Client

| Category | Technology |
|---|---|
| Language | Kotlin |
| UI | XML Layouts + ViewBinding |
| Architecture | MVVM + Single Activity + Navigation Component |
| Async | Coroutines + Flow |
| Maps | Google Maps SDK + FusedLocationProvider |
| Images | Glide (loading) + uCrop (cropping) |
| Networking | Retrofit 2 + OkHttp 4 + Gson |
| Local Storage | Room (cache) + DataStore (preferences) |
| UI Components | Material Design Components 3 |
| Push | Firebase Cloud Messaging |

### Backend

| Category | Technology |
|---|---|
| Database | Firebase Firestore |
| Auth | Firebase Authentication (Email/Password + Anonymous) |
| File Storage | Firebase Cloud Storage |
| Triggers | Firebase Cloud Functions |
| Mock APIs | Node.js (Express) |

### AI / Agent

| Category | Technology |
|---|---|
| Orchestration | Google Antigravity |
| NLP & Classification | Gemini API (gemini-1.5-pro) |
| Semantic Matching | Google Embedding API (text-embedding-004) |
| Agent Runtime | Python (Cloud Functions) |

---

## APIs & Integrations

### Real Integrations (Production)

| API / Service | Usage |
|---|---|
| **Gemini API (gemini-1.5-pro)** | NLP classification of crisis signals across Urdu, Roman Urdu, and English. Used by SENTINEL for keyword extraction and ANALYST for crisis type/severity classification. |
| **Google Embedding API (text-embedding-004)** | Generates text embeddings of missing person descriptions for semantic similarity matching by the MATCHER agent. |
| **Google Maps SDK for Android** | Live crisis map rendering with custom dark theme, severity-coded markers, geofence circles, evacuation route polylines, and user location tracking. |
| **Google Maps Traffic API** | Real-time traffic anomaly detection as a crisis signal source for SENTINEL. |
| **Google Geocoding API** | Converts typed addresses to geographic coordinates for missing person last-seen locations. |
| **Firebase Firestore** | Primary NoSQL database. Real-time listeners power all live data in the app. Collections: `users`, `crises`, `signals`, `agent_traces`, `missing_persons`, `eyewitness_reports`. |
| **Firebase Authentication** | Email/password sign-in and anonymous (guest) authentication. |
| **Firebase Cloud Storage** | Missing person photo uploads (compressed to 800px, max 5MB). |
| **Firebase Cloud Messaging (FCM)** | Geofenced push notifications to citizens within 3km of confirmed crises. Three channels: critical, informational, missing persons. |
| **Firebase Cloud Functions** | Trigger hooks for agent pipeline (e.g., on new missing person report → trigger MATCHER). |

### Mocked APIs (Hackathon Scope)

| API | Mock Implementation | Purpose |
|---|---|---|
| **Twitter/X Social Feed** | Pre-seeded JSON data in Firestore | Simulates social media crisis signals (Urdu + English tweets) |
| **PMD Weather API** | Node.js Express stub | Returns weather alert objects with severity and region |
| **NDMA Official Feed** | Pre-seeded JSON data | Simulates National Disaster Management Authority alerts |
| **Rescue 1122 API** | Node.js Express stub (`/rescue1122`) | Simulates dispatch acknowledgment responses |
| **Police / Rangers API** | Node.js Express stub (`/police`) | Simulates law enforcement dispatch |
| **NDMA Dispatch API** | Node.js Express stub (`/ndma`) | Simulates NDMA coordination responses |

> **Note:** All mock APIs return hardcoded JSON matching the real API contract shape, enabling seamless swap to production endpoints without app changes.

---

## Database Design

### Firestore Collections

```
Firestore
├── users/{uid}                          — User profiles, location, FCM tokens
├── signals/{signal_id}                  — Raw signals from SENTINEL
├── crises/{crisis_id}                   — Active/historical crisis records
│   ├── verifications/{uid}              — Citizen verification votes
│   └── timeline/{event_id}             — Crisis lifecycle events
├── missing_persons/{report_id}          — Missing person reports
├── agent_traces/{trace_id}              — AI agent decision logs
└── eyewitness_reports/{report_id}       — Citizen eyewitness reports
```

### Key Collections

**`crises`** — Written by ANALYST, updated by COMMANDER
- Crisis type, severity, confidence (0–100), status (MONITORING → ACTIVE → CONFIRMED → RESOLVED)
- Geographic centroid (GeoPoint), impact radius (km)
- Bilingual titles/descriptions (English + Urdu)
- Assigned agencies, evacuation routes
- ANALYST reasoning summary, spread prediction

**`missing_persons`** — Written by citizens via app
- Person details (name, age, gender, description)
- Last seen location (GeoPoint), photo URL
- Status: `SEARCHING` → `LINKED` → `POTENTIAL_DUPLICATE` → `FOUND`
- Linked crisis ID (set by MATCHER), match score, witness report count

**`agent_traces`** — Written by all 4 agents
- Agent name, action label, reasoning summary, confidence score
- Linked to crisis and/or missing person
- Powers the real-time Agent Trace View screen

### Local Storage

- **Room Database:** Offline cache for recently viewed crises and user's own reports
- **DataStore:** Preferences (theme, language, onboarding state, notification toggles)

---

## App Screens & Navigation

### Navigation Flow

```
Splash (1.5s) ──► [First time?]
                   ├── YES ──► Onboarding (3 slides) ──► Language Select
                   │           ──► Permissions ──► Auth ──► Home Dashboard
                   └── NO  ──► Home Dashboard

Home Dashboard (root)
├── Map markers / Alert cards ──► Crisis Detail
│                                  ├── Agent Trace View
│                                  └── Missing Persons Hub (filtered)
├── FAB ──► Report Missing (3 steps) ──► Confirmation ──► Home
├── Bottom Nav: Alerts ──► Alerts List ──► Crisis Detail
├── Bottom Nav: Missing ──► Missing Persons Hub ──► Missing Person Detail
└── Bottom Nav: Profile ──► Profile ──► Settings ──► Language / Sign Out
```

### Screen Summary (16 Screens)

| # | Screen | Purpose |
|---|---|---|
| 1 | Splash | Logo animation, auth check, auto-navigate |
| 2 | Onboarding | 3-slide feature walkthrough |
| 3 | Language Select | English / Urdu selection |
| 4 | Permissions | Location, notifications, camera requests |
| 5 | Auth | Email sign-in, guest access, create account |
| 6 | Signup | Name, email, CNIC, profile photo (1:1 crop) |
| 7 | Home Dashboard | Crisis map, safety card, alert banner, FAB |
| 8 | Crisis Detail | Full crisis info, agencies, routes, timeline, agent trace preview |
| 9 | Report Missing | 3-step form: details → location/photo → contact |
| 10 | Confirmation | Success screen with report ID |
| 11 | Missing Persons Hub | Filterable list (All / By Crisis / Unlinked) with search |
| 12 | Missing Person Detail | Full profile, photo viewer, witness reports, mark as found |
| 13 | Agent Trace View | Real-time AI decision timeline |
| 14 | Alerts List | All active crises with severity counters |
| 15 | Settings | Theme, notifications, language, sign out |
| 16 | Profile | Instagram-style layout, stats, own reports |

---

## Design System

### Brand Colors (Same in Both Themes)

| Color | Hex | Role |
|---|---|---|
| Signal Red | `#C8001A` | Primary actions, CRITICAL/HIGH severity |
| Ember Amber | `#D4720A` | Secondary, MEDIUM severity, active nav |
| Ops Blue | `#1B52E8` | Info, links, SENTINEL agent |
| Field Green | `#1A7C3A` | LOW severity, success, MATCHER agent |

### Themes
- **Dark (default):** Near-black backgrounds (`#09090F`), light text, command-center feel
- **Light:** White/grey backgrounds (`#F6F7FA`), dark text, same brand accents
- Adaptive color system: `values/colors.xml` (light) + `values-night/colors.xml` (dark)

### Typography
- **Poppins** (all weights) — primary font
- **Noto Nastaliq Urdu** — Urdu script text

---

## Setup & Installation

### Prerequisites
- Android Studio (latest stable)
- Android device or emulator (API 26+)
- Firebase project with Firestore, Auth, Storage, and Cloud Messaging enabled

### Steps

1. **Clone the repository**
   ```bash
   git clone <repo-url>
   cd Nishaan
   ```

2. **Firebase Configuration**
   - Download `google-services.json` from Firebase Console
   - Place in `app/google-services.json`

3. **Google Maps API Key**
   - Enable Maps SDK for Android in Google Cloud Console
   - Add to `local.properties`:
     ```properties
     MAPS_API_KEY=your_api_key_here
     ```

4. **Register Debug SHA-1**
   ```bash
   ./gradlew.bat signingReport
   ```
   Add the SHA-1 to Firebase Console → Project Settings → Android app

5. **Seed Test Data** (optional)
   ```bash
   npm install firebase-admin
   node seed_firestore.js
   ```

6. **Build & Run**
   ```bash
   ./gradlew.bat assembleDebug
   ./gradlew.bat installDebug
   ```

---

## Real vs Mocked Components

| Component | Status | Details |
|---|---|---|
| Gemini NLP Classification | ✅ **Real** | Live Gemini API calls for multilingual crisis signal analysis |
| Google Embedding API | ✅ **Real** | Live semantic similarity for missing person duplicate detection |
| Google Maps SDK | ✅ **Real** | Live map with custom theming, markers, geofences |
| Google Maps Traffic API | ✅ **Real** | Live traffic anomaly detection as crisis signal |
| Firebase Firestore + Auth | ✅ **Real** | Production database and authentication |
| Firebase Cloud Storage | ✅ **Real** | Real photo upload and retrieval |
| FCM Push Notifications | ✅ **Real** | Live geofenced push delivery |
| Agent Trace Logging | ✅ **Real** | Every agent decision logged with full transparency |
| Missing Person Submission | ✅ **Real** | Full form with photo crop and Firestore write |
| Multilingual Support | ✅ **Real** | English + Urdu with full string translations |
| Twitter/X Social Feed | 🟡 **Mocked** | Pre-seeded JSON simulating crisis tweets |
| PMD Weather API | 🟡 **Mocked** | Node.js stub returning weather alerts |
| NDMA Official Alerts | 🟡 **Mocked** | Pre-seeded structured alert data |
| Rescue 1122 Dispatch | 🟡 **Mocked** | Node.js stub simulating dispatch ACK |
| Police/Rangers Dispatch | 🟡 **Mocked** | Node.js stub simulating dispatch ACK |

---

<p align="center">
  <strong>NISHAAN — نشان</strong><br>
  <em>It does not wait for instructions. It acts.</em>
</p>

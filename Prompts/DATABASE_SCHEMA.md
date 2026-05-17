# DATABASE_SCHEMA.md — NISHAAN Backend Data Structure

> Defines all Firestore collections, document fields, indexes, and data flow rules. All agents and the Android app must read/write exactly these structures.

---

## Overview

Primary database: **Firebase Firestore** (NoSQL document store).
Secondary: **Room (SQLite)** on the Android device for local caching.
File storage: **Firebase Storage** for missing person photos.

---

## Firestore Collections

---

### Collection: `users`

**Path:** `/users/{uid}`

Stores registered user profiles. Created on first sign-in.

| Field | Type | Required | Notes |
|---|---|---|---|
| `uid` | string | ✅ | Firebase Auth UID, same as document ID |
| `email` | string | ❌ | Null for anonymous users |
| `display_name` | string | ❌ | Optional display name |
| `language` | string | ✅ | `"en"`, `"ur"`, `"roman_ur"` |
| `last_location` | GeoPoint | ❌ | Updated on app foreground. Used for FCM targeting. |
| `last_location_updated_at` | Timestamp | ❌ | When `last_location` was last written |
| `fcm_token` | string | ❌ | Current FCM registration token |
| `is_anonymous` | boolean | ✅ | True for guest users |
| `notification_prefs` | map | ✅ | `{ critical: true, medium: true, low: false, missing: true }` |
| `active_report_count` | number | ✅ | Count of open missing person reports (max 10) |
| `created_at` | Timestamp | ✅ | Account creation time |
| `updated_at` | Timestamp | ✅ | Last profile update |

---

### Collection: `signals`

**Path:** `/signals/{signal_id}`

Raw data written by SENTINEL agent from all polled sources.

| Field | Type | Required | Notes |
|---|---|---|---|
| `signal_id` | string | ✅ | Auto-generated Firestore ID |
| `source` | string | ✅ | `"twitter_mock"`, `"pmd_mock"`, `"ndma_mock"`, `"google_traffic"`, `"eyewitness"` |
| `raw_text` | string | ✅ | Original text or summary from source |
| `extracted_keywords` | array[string] | ✅ | Crisis keywords extracted by SENTINEL/Gemini |
| `location` | GeoPoint | ❌ | Geographic location of signal (if available) |
| `language_detected` | string | ❌ | `"en"`, `"ur"`, `"roman_ur"` |
| `processed_by_analyst` | boolean | ✅ | Flag to avoid double-processing |
| `agent` | string | ✅ | Always `"SENTINEL"` |
| `timestamp` | Timestamp | ✅ | When signal was ingested |
| `hash` | string | ✅ | SHA-256 of source + raw_text for deduplication |

**Indexes:**
- `timestamp` DESC (for ANALYST to read latest signals)
- `processed_by_analyst` + `timestamp` (compound — for fetching unprocessed signals)

---

### Collection: `crises`

**Path:** `/crises/{crisis_id}`

Active and historical crisis records. Written by ANALYST, updated by COMMANDER.

| Field | Type | Required | Notes |
|---|---|---|---|
| `crisis_id` | string | ✅ | Auto-generated Firestore ID |
| `crisis_type` | string | ✅ | `"FLOOD"`, `"EARTHQUAKE"`, `"HEATWAVE"`, `"CIVIL_UNREST"`, `"TRAFFIC_ACCIDENT"`, `"INFRASTRUCTURE_FAILURE"`, `"UNKNOWN"` |
| `severity` | string | ✅ | `"CRITICAL"`, `"HIGH"`, `"MEDIUM"`, `"LOW"`, `"MONITORING"` |
| `confidence` | number | ✅ | 0–100 integer, ANALYST's confidence score |
| `status` | string | ✅ | `"MONITORING"`, `"ACTIVE"`, `"CONFIRMED"`, `"RESOLVED"` |
| `centroid` | GeoPoint | ✅ | Geographic center of crisis |
| `impact_radius_km` | number | ✅ | Estimated radius of impact in km |
| `title_en` | string | ✅ | Short English title e.g. "Flash Flood — Gulshan" |
| `title_ur` | string | ✅ | Urdu script title |
| `description_en` | string | ❌ | Longer English description |
| `description_ur` | string | ❌ | Urdu description |
| `signal_ids` | array[string] | ✅ | References to `signals` documents that contributed |
| `assigned_agencies` | array[string] | ❌ | e.g. `["rescue_1122", "ndma", "police"]` |
| `agency_notified_at` | Timestamp | ❌ | When COMMANDER dispatched |
| `citizens_alerted_count` | number | ❌ | How many FCM notifications sent |
| `evacuation_routes` | array[map] | ❌ | `[{ route_name: string, polyline_encoded: string }]` |
| `analyst_reasoning` | string | ❌ | Gemini summary of classification reasoning |
| `missing_persons_count` | number | ✅ | Count of linked missing person reports |
| `created_at` | Timestamp | ✅ | When crisis was first detected |
| `updated_at` | Timestamp | ✅ | Last update (severity change, agency update, etc.) |
| `resolved_at` | Timestamp | ❌ | When status changed to RESOLVED |

**Indexes:**
- `status` + `created_at` DESC (for map display — active crises)
- `centroid` (GeoPoint — for geofence queries via Geofire or manual bounding box)

---

### Collection: `missing_persons`

**Path:** `/missing_persons/{report_id}`

Missing person reports submitted by citizens.

| Field | Type | Required | Notes |
|---|---|---|---|
| `report_id` | string | ✅ | Auto-generated Firestore ID |
| `submitted_by_uid` | string | ✅ | Auth UID of reporting user |
| `reporter_phone` | string | ✅ | Validated Pakistani phone number |
| `reporter_relationship` | string | ❌ | e.g. "Father", "Sister" |
| `person_name` | string | ✅ | Full name of missing person |
| `person_age` | number | ✅ | Age in years |
| `person_gender` | string | ✅ | `"male"`, `"female"`, `"other"` |
| `description` | string | ❌ | Physical description, clothing, distinguishing marks |
| `description_embedding` | array[number] | ❌ | Embedding vector generated by MATCHER for semantic matching |
| `last_seen_location` | GeoPoint | ✅ | Geocoded from address or map pin |
| `last_seen_address` | string | ❌ | Human-readable address |
| `photo_url` | string | ❌ | Firebase Storage download URL |
| `photo_storage_path` | string | ❌ | Storage path for deletion reference |
| `linked_crisis_id` | string | ❌ | Set by MATCHER when report is linked to a crisis |
| `status` | string | ✅ | `"SEARCHING"`, `"LINKED"`, `"POTENTIAL_DUPLICATE"`, `"FOUND"` |
| `match_score` | number | ❌ | Highest similarity score found by MATCHER (0.0–1.0) |
| `matched_with_report_ids` | array[string] | ❌ | Report IDs that are potential duplicates |
| `family_notified_at` | Timestamp | ❌ | When FCM was sent to family about a match or link |
| `submitted_at` | Timestamp | ✅ | Report submission time |
| `updated_at` | Timestamp | ✅ | Last status update |
| `closed_at` | Timestamp | ❌ | When marked FOUND |

**Indexes:**
- `submitted_by_uid` + `submitted_at` DESC (user's own reports)
- `linked_crisis_id` + `status` (missing persons hub, filtered by crisis)
- `status` + `last_seen_location` (geospatial queries by MATCHER)

---

### Collection: `agent_traces`

**Path:** `/agent_traces/{trace_id}`

Decision log entries written by all agents. Read by the Android app in real-time.

| Field | Type | Required | Notes |
|---|---|---|---|
| `trace_id` | string | ✅ | Auto-generated |
| `agent_name` | string | ✅ | `"SENTINEL"`, `"ANALYST"`, `"COMMANDER"`, `"MATCHER"` |
| `crisis_id` | string | ❌ | Associated crisis (null for SENTINEL polls that found nothing) |
| `missing_person_id` | string | ❌ | Set for MATCHER traces |
| `action` | string | ✅ | Short action label e.g. `"CRISIS_CLASSIFIED"`, `"FCM_SENT"`, `"MATCH_FOUND"` |
| `reasoning_summary` | string | ✅ | 1–2 sentence Gemini-generated reasoning in English |
| `confidence` | number | ❌ | Confidence score at time of action (0–100) |
| `metadata` | map | ❌ | Extra context: `{ signals_processed: 12, agencies_notified: ["rescue_1122"] }` |
| `timestamp` | Timestamp | ✅ | Exact time of action |

**Indexes:**
- `crisis_id` + `timestamp` DESC (for AgentTraceViewScreen — per-crisis trace)
- `timestamp` DESC (for global admin view, if built)

---

### Collection: `eyewitness_reports`

**Path:** `/eyewitness_reports/{report_id}`

Citizen eyewitness reports (not missing person reports — these are "I see something happening" reports).

| Field | Type | Required | Notes |
|---|---|---|---|
| `report_id` | string | ✅ | Auto-generated |
| `submitted_by_uid` | string | ✅ | Auth UID |
| `report_text` | string | ✅ | Description of what the user is witnessing |
| `location` | GeoPoint | ✅ | User's current location at time of report |
| `photo_url` | string | ❌ | Optional photo |
| `processed_by_sentinel` | boolean | ✅ | Flag so SENTINEL doesn't re-process |
| `submitted_at` | Timestamp | ✅ | Submission time |

---

## Firebase Storage Structure

```
/missing_persons_photos/
  /{report_id}/
    /photo.jpg          ← compressed to 800px longest side, max 5MB
```

Access rules: authenticated write (uploader only), public read (for rescue teams).

---

## Room Database (Android Local Cache)

Used for offline access to recently viewed crises and the user's own submitted reports.

### Table: `crisis_entities`

| Column | Type | Notes |
|---|---|---|
| `crisis_id` | TEXT (PK) | Firestore document ID |
| `crisis_type` | TEXT | |
| `severity` | TEXT | |
| `confidence` | INTEGER | |
| `status` | TEXT | |
| `centroid_lat` | REAL | |
| `centroid_lng` | REAL | |
| `title_en` | TEXT | |
| `title_ur` | TEXT | |
| `updated_at` | INTEGER | Unix millis |
| `cached_at` | INTEGER | When this row was last fetched from Firestore |

### Table: `missing_person_entities`

| Column | Type | Notes |
|---|---|---|
| `report_id` | TEXT (PK) | |
| `person_name` | TEXT | |
| `status` | TEXT | |
| `linked_crisis_id` | TEXT | Nullable |
| `submitted_at` | INTEGER | Unix millis |
| `cached_at` | INTEGER | |

---

## Data Flow Rules

1. **Firestore is the single source of truth** — Room is only a cache. Never write to Room directly from a use case — only the repository syncs Room from Firestore.
2. **All Firestore writes from agents include `updated_at: FieldValue.serverTimestamp()`** — never use client-side timestamps from agents.
3. **Android app never writes to `signals` or `agent_traces`** — these are agent-only collections.
4. **Android app writes to:** `users`, `missing_persons`, `eyewitness_reports`
5. **Firestore Security Rules must enforce:** authenticated write to `users/{uid}` (own doc only), authenticated write to `missing_persons` (any authenticated non-anonymous user), public read on `crises` and `agent_traces`, no client write to `signals` or `agent_traces`.

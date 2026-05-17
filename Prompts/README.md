# NISHAAN — نشان — The Signal

> **Pakistan's autonomous crisis detection and missing persons system.**
> Built for Hackathon Challenge 3: CIRO (Crisis Intelligence & Response Operations).

---

## What It Is

NISHAAN is an Android-native application that runs two parallel intelligence layers 24 hours a day:

1. **Autonomous Layer** — A 4-agent AI backend that monitors signal sources every 60 seconds, detects emerging crises, classifies severity, dispatches relevant agencies, and pushes geofenced alerts to citizens — all without any human trigger.
2. **Citizen Layer** — A public-facing interface where users can report missing persons, monitor a live crisis map, and receive real-time alerts relevant to their location.

The name نشان (Nishaan) means *The Signal* — both a literal reference to the signals the system monitors and a conceptual statement about giving voice to crises that are often invisible until it's too late.

---

## Why It Exists

Pakistan faces recurring crises — floods, heatwaves, urban unrest, accidents — where the gap between *event occurring* and *people knowing* costs lives. Simultaneously, missing persons during crises go untracked because there is no system connecting reports to active emergencies.

NISHAAN closes both gaps: it detects the crisis and tracks the people lost inside it.

---

## Core Idea

```
Signal Sources → SENTINEL → ANALYST → COMMANDER → Citizens Alerted
                                              ↓
                             Missing Reports → MATCHER → Families Notified
```

Four AI agents, orchestrated by Google Antigravity, form a closed loop from raw signal noise to citizen action — with full reasoning traces visible in the app.

---

## Target Users

| User | What they see |
|---|---|
| **General citizen** | Crisis map, geofenced alerts, missing person submission |
| **Rescue/agency responder** | Dispatch notifications (mocked), active case feeds |
| **Hackathon judges** | Agent Trace View — real-time AI decision logs with timestamps and confidence scores |
| **Families of missing persons** | Report submission + match notifications |

---

## Main Features

- **Live Crisis Map** — Google Maps overlay with active crisis markers, severity color-coding, and evacuation routes
- **Autonomous Agent Pipeline** — SENTINEL → ANALYST → COMMANDER → MATCHER running every 60 seconds
- **Missing Persons Hub** — Submit reports (name, description, photo, last known location), clustered by nearest active crisis
- **Geofenced FCM Alerts** — Citizens within 3km of a confirmed crisis receive push notifications automatically
- **Multilingual Input** — Supports Urdu script, Roman Urdu, and English across all forms
- **Agent Trace View** — Transparent AI reasoning: every decision logged with agent name, timestamp, confidence score, and action taken
- **Crisis Detail Screen** — Crisis type, severity level, confidence score, assigned agencies, and recommended evacuation routes

---

## Overall Vibe

NISHAAN is a **command-center app for citizens**. The aesthetic is dark, high-contrast, and data-dense — inspired by emergency operations dashboards. It feels serious, fast, and trustworthy. No decorative fluff. Every pixel serves the crisis response mission.

It is **not a chatbot**. It does not wait for instructions. It acts.

---

## Real vs Simulated (Hackathon Scope)

| Component | Status |
|---|---|
| NLP classification (Gemini) | ✅ Real |
| Multilingual input (Urdu/Roman Urdu/English) | ✅ Real |
| Google Maps SDK | ✅ Real |
| Firebase Firestore + Auth | ✅ Real |
| FCM push notifications | ✅ Real |
| Agent trace logs | ✅ Real |
| Missing person form submission | ✅ Real |
| Twitter/X social feed | 🟡 Mocked (pre-seeded data) |
| PMD weather API | 🟡 Mocked |
| Rescue 1122 / agency dispatch | 🟡 Mocked (Node.js stub) |
| NDMA alerts | 🟡 Mocked |
| Photo matching | 🟡 Mocked (embedding similarity) |

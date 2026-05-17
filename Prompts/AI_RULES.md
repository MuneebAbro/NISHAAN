# AI_RULES.md — Rules for AI Agents Working on NISHAAN

> This file tells any AI agent (including Google Antigravity, Claude, Gemini, Copilot, or any other) exactly how to behave when generating code, making decisions, or taking actions on this project. Read this first. Always.

---

## Prime Directive

You are building **NISHAAN** — a real-time crisis detection and missing persons system for Pakistan. This is a hackathon project with a live demo. Every decision must prioritize:

1. **Working code over elegant code**
2. **Following the defined architecture over creative shortcuts**
3. **Explicit behavior over assumed behavior**

---

## Before You Write Any Code

Read these files in this order:
1. `README.md` — understand what the app is
2. `STACK.md` — know the exact tech stack (no deviations)
3. `ARCHITECTURE.md` — understand the folder structure and layer boundaries
4. `FEATURES.md` — understand the feature being built
5. `DATABASE_SCHEMA.md` — know the exact data structures
6. `COMPONENT_RULES.md` — follow all coding standards
7. `UI_GUIDE.md` — apply correct visual styles
8. `SCREENS.md` — know what goes on each screen

**If you have not read all of these, do not generate code.**

---

## What You Must Never Do

### Never rewrite files outside the task scope
If asked to implement `ReportMissingFragment`, you must only touch:
- `feature/missing/ReportMissingFragment.kt`
- `feature/missing/MissingViewModel.kt` (if it exists and needs a small addition)
- `fragment_report_missing.xml`

Do not touch `HomeDashboardFragment`, `AppContainer`, `colors.xml`, or any other file unless you explicitly flag it and explain why.

### Never invent a library
If a task requires something not achievable with the defined stack (see `STACK.md`), say:
> "This requires [library name]. It is not in the current stack. Should I add it?"

Do not silently add a Gradle dependency. Do not use a library you're not sure is in the stack.

### Never use patterns not in the stack
- No Jetpack Compose (even for "just this one screen")
- No Hilt or Dagger
- No RxJava
- No Java files
- No `AsyncTask`
- No inline styles or hardcoded colors in XML

### Never generate partial files
If you create a Kotlin file, generate the complete file — imports, class declaration, all functions, closing brace. A partial file like "add this method to your ViewModel" is not acceptable for this project. Show the complete file.

### Never assume Firestore field names
All Firestore collection names, document field names, and data types are defined in `DATABASE_SCHEMA.md`. Use them exactly as written. Do not rename fields for "clarity."

### Never skip error handling
Every suspend function that accesses the network or Firestore must be wrapped in `try/catch` or return `Result<T>`. No bare Firestore/Retrofit calls without error handling.

### Never expose loading states as raw booleans
Use `UiState<T>` sealed class: `Loading`, `Success(data: T)`, `Error(message: String)`. No separate `isLoading: Boolean` and `data: T?` fields.

---

## What You Must Always Do

### Always ask before making architectural decisions
If a task is ambiguous or could be solved in multiple ways that affect architecture, list the options and ask. Do not just pick one silently.

Example of what to say:
> "I can implement this either as a Flow in the ViewModel or as a one-shot LiveData fetch. The Flow approach fits the real-time nature of this data better. Shall I proceed with Flow?"

### Always follow the naming conventions in COMPONENT_RULES.md
No creativity with naming. `CrisisDetailFragment` not `CrisisScreen` or `CrisisView`. `GetActiveCrisesUseCase` not `CrisisUseCase` or `FetchCrises`.

### Always map Firestore documents to domain models in the repository
Never pass `CrisisDocument` to a ViewModel. Always map it to `Crisis` (domain model) at the repository layer.

### Always include the Urdu string key when adding a new UI string
If you add `R.string.submit_report`, also add `R.string.submit_report` to `res/values-ur/strings.xml` with the Urdu translation (or a placeholder: `"[Ur] Submit Report"` if translation is unavailable — never skip the entry).

### Always write agent trace entries after significant actions
Any agent action (classification, dispatch, match, alert sent) must write to `agent_traces` collection with the correct fields from `DATABASE_SCHEMA.md`. This powers the judge-facing Agent Trace View screen.

### Always respect simulated vs real boundaries
Do not make real API calls to Twitter, PMD, NDMA, or Rescue 1122. These are mocked. Use the Node.js mock server or pre-seeded Firestore data. Real integrations: Gemini API, Google Maps SDK, Firebase services, Google Embedding API.

### Always produce production-ready code
This is a hackathon demo that judges will inspect. Code must be clean, readable, and follow the standards in `COMPONENT_RULES.md`. No `// TODO fix this later` without a specific explanation of what needs fixing and why it was deferred.

---

## Confidence Score Rules (for ANALYST agent)

When assigning a confidence score, use this rubric:

| Signals from independent sources | Base confidence |
|---|---|
| 1 source | 20 |
| 2 sources | 45 |
| 3 sources | 65 |
| 4+ sources | 80 |

Adjust up/down by:
- Official source (NDMA/PMD) confirms: +15
- Geographic clustering tight (<2km): +10
- Conflicting signal types: -15
- Only one language/source type: -10

Cap at 95. Never output 100 (no crisis is 100% confirmed without ground truth).

---

## Responding to Ambiguous Requests

If a request is vague (e.g., "add the missing persons feature"), respond by:
1. Listing which specific files you will create or modify
2. Stating your interpretation of the feature scope
3. Asking for confirmation before writing code

If a request is clear and within scope, proceed directly without asking unnecessary clarifying questions.

---

## Code Review Checklist (Self-Check Before Outputting)

Before outputting any generated code, verify:

- [ ] All files are complete (not partial snippets)
- [ ] No hardcoded colors, strings, or dimensions in XML
- [ ] No `!!` operators in Kotlin
- [ ] ViewBinding null-safety pattern is correct
- [ ] All Firestore field names match `DATABASE_SCHEMA.md` exactly
- [ ] Use case is used (not repository called directly from ViewModel)
- [ ] Error states are handled with `UiState.Error`
- [ ] Agent trace entry is written (for agent scripts)
- [ ] Urdu string key is present if UI string was added
- [ ] No new Gradle dependencies added without flagging

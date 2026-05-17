# COMPONENT_RULES.md — NISHAAN Coding Standards & Discipline

> These rules are non-negotiable. AI agents generating code for NISHAAN must follow every rule here without exception.

---

## File Size Limits

| File type | Max lines |
|---|---|
| Fragment | 150 lines |
| ViewModel | 200 lines |
| Repository implementation | 150 lines |
| Use case | 60 lines |
| Adapter | 100 lines |
| Utility / Extension file | 100 lines |
| XML layout | 200 lines |

If a file approaches its limit, it must be split. No exceptions for "just this once."

---

## Kotlin Rules

### Must Do
- All code in Kotlin. No Java files in the Android project.
- Use `data class` for all models. No `class` for data containers.
- Use `sealed class` for all state representations (`UiState`, navigation events).
- All coroutines launched from ViewModel use `viewModelScope`. Never `GlobalScope`.
- All suspend functions are called from within a coroutine scope — never from a regular function.
- Use `object` for singleton utilities that have no state.
- Use `companion object` only for constants and factory methods — not for state.
- `when` expressions must be exhaustive — always include `else` for non-sealed types.
- Extension functions go in a dedicated `*Extensions.kt` file in `core/util/`, never inline in a feature file.

### Must Not Do
- No `!!` (non-null assertion operator). Use `?: return`, `?: throw`, or safe-call `?.`.
- No `lateinit var` for ViewBinding — use `private var _binding` pattern with nullable backing field.
- No `Thread.sleep()` or blocking calls on the main thread.
- No `runBlocking` in production code (test code only).
- No mutable state exposed from ViewModel — `_stateLiveData` (private MutableLiveData) + `stateLiveData` (public immutable).
- No business logic in Fragment or Activity classes.
- No direct Firestore or Retrofit calls from ViewModel — always through use cases.

---

## ViewBinding Pattern (Required)

```kotlin
// ALL fragments must use this exact pattern
class CrisisDetailFragment : Fragment(R.layout.fragment_crisis_detail) {

    private var _binding: FragmentCrisisDetailBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCrisisDetailBinding.bind(view)
        // setup UI here
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null  // ALWAYS null the binding to avoid memory leaks
    }
}
```

---

## Naming Conventions

### Files
| Type | Convention | Example |
|---|---|---|
| Fragment | `[Feature]Fragment.kt` | `CrisisDetailFragment.kt` |
| ViewModel | `[Feature]ViewModel.kt` | `CrisisDetailViewModel.kt` |
| Repository interface | `[Feature]Repository.kt` | `CrisisRepository.kt` |
| Repository impl | `[Feature]RepositoryImpl.kt` | `CrisisRepositoryImpl.kt` |
| Use case | `[Verb][Noun]UseCase.kt` | `GetActiveCrisesUseCase.kt` |
| Room entity | `[Name]Entity.kt` | `CrisisEntity.kt` |
| Adapter | `[Name]Adapter.kt` | `CrisisCardAdapter.kt` |
| Layout (fragment) | `fragment_[name].xml` | `fragment_crisis_detail.xml` |
| Layout (item) | `item_[name].xml` | `item_crisis_card.xml` |
| Drawable | `ic_[name].xml` or `bg_[name].xml` | `ic_flood.xml`, `bg_card.xml` |

### Variables & Functions
- Private backing fields: `_camelCase` (e.g., `_binding`, `_crisisState`)
- Public exposed fields: `camelCase` (no underscore)
- Constants: `SCREAMING_SNAKE_CASE` in `companion object` or top-level `const val`
- Functions: `camelCase`, verb-first (e.g., `loadCrisisDetails()`, `submitReport()`)

### Strings
- String resource IDs: `snake_case` (e.g., `crisis_detail_title`, `btn_report_missing`)
- Color resource IDs: `color_[semantic_name]` (e.g., `color_primary`, `color_surface`)
- Dimension resource IDs: `dimen_[context]` (e.g., `dimen_card_padding`, `dimen_screen_margin`)

---

## XML Layout Rules

- **No inline styles** — use `style="@style/..."` references for every view that has more than 2 style attributes
- **No hardcoded colors** — use `?attr/colorSurface`, `@color/color_primary`, etc.
- **No hardcoded strings** — use `@string/key_name`. Even placeholder text.
- **No hardcoded dimensions** — use `@dimen/spacing_md` etc. Only exception: `0dp` for constraint chains.
- Every `ImageView` must have `android:contentDescription` (use `@null` only if it's purely decorative)
- Every clickable view must have `android:background="?attr/selectableItemBackground"` or a ripple drawable
- IDs follow `camelCase` convention: `crisisTypeLabel`, `submitButton`, `mapView`

---

## TypeScript Enforcement (Backend / Agent scripts context)

For the Node.js mock backend:
- TypeScript strict mode enabled (`"strict": true` in tsconfig)
- No `any` types — define interfaces for all request/response shapes
- All async functions explicitly typed with return type
- No `require()` — use ES module `import/export`

---

## Reusable Component Rules

Any UI pattern that appears on **2 or more screens** must be extracted into a reusable custom view or an included layout.

Examples of mandatory extractions:
- `SeverityBadgeView` — used on crisis cards, crisis detail, missing person hub
- `AgentTraceItemView` — used in trace timeline
- `CrisisCardView` — used on home dashboard and alerts list
- `LoadingStateView` — full-screen loading overlay, used on every data-loading screen
- `EmptyStateView` — empty list states, used on alerts, missing persons, trace view

Each reusable view lives in `core/ui/` with its own XML layout and a Kotlin custom view class (or a binding extension function for simple cases).

---

## Code Comments Policy

- **Required:** All public functions in repository and use case classes get a single-line KDoc comment
- **Required:** All non-obvious logic blocks get an inline comment explaining *why*, not *what*
- **Forbidden:** Commented-out code checked into the repo. Use git stash instead.
- **Forbidden:** TODO comments without an associated ticket/issue reference

---

## Import Rules

- Wildcard imports forbidden: `import com.nishaan.app.domain.model.*` — not allowed
- Imports must be organized: Android/Java → Third-party → Internal (enforced by ktlint)
- Do not import a class just to use it once in a string interpolation or reflection — spell out the full path

---

## Pull Request / Code Generation Rules (for AI agent)

- Never modify a file outside the scope of the current task
- If a task requires changing a shared file (e.g., `colors.xml`, `AppContainer`), flag it explicitly before making the change
- Always generate the full file, not a "partial snippet with the rest left as before" — partial outputs cause integration bugs
- If a file already exists, diff it mentally before overwriting — do not remove existing functionality silently
- Never add a library (new Gradle dependency) without stating it in the response and waiting for confirmation

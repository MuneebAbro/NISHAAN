# UI_GUIDE.md — NISHAAN Visual Design System

> Every visual decision in NISHAAN follows this guide. AI agents must not introduce colors, fonts, spacing values, or component styles that aren't defined here.

---

## Aesthetic Direction

**Dark Command Center.** NISHAAN looks like an emergency operations dashboard that a citizen can hold in their hand. Think dark backgrounds, sharp accent colors, monospaced data readouts, and zero decorative noise. Every element must earn its presence.

No gradients that are purely decorative. No rounded corners so large they look playful. No illustrations or emojis in the core UI. Crisis is the context — the design must match the gravity.

---

## Color System

All colors defined in `res/colors.xml`. Referenced via theme attributes, never hardcoded.

### Dark Theme (Primary — default)

| Token | Hex | Usage |
|---|---|---|
| `color_background` | `#0A0D12` | Screen backgrounds |
| `color_surface` | `#12171F` | Cards, sheets, dialogs |
| `color_surface_variant` | `#1C2330` | Secondary cards, input backgrounds |
| `color_primary` | `#E63946` | Primary actions, crisis HIGH severity, FAB |
| `color_primary_variant` | `#C1121F` | Pressed states for primary |
| `color_secondary` | `#F4A261` | MEDIUM severity, warnings |
| `color_accent_low` | `#2EC4B6` | LOW severity, success states |
| `color_accent_info` | `#4895EF` | Informational, links, agent trace header |
| `color_on_background` | `#E8EDF5` | Primary text on dark background |
| `color_on_surface` | `#B0BAC9` | Secondary text, labels |
| `color_on_surface_muted` | `#5A6478` | Placeholder text, disabled states |
| `color_divider` | `#1E2A3A` | Dividers, separators |
| `color_map_overlay` | `#CC0A0D12` | Semi-transparent map overlay panels |

### Light Theme (Optional — user preference)

| Token | Hex | Usage |
|---|---|---|
| `color_background` | `#F0F4F8` | Screen backgrounds |
| `color_surface` | `#FFFFFF` | Cards |
| `color_primary` | `#C1121F` | Primary actions |
| `color_on_background` | `#0D1117` | Primary text |
| `color_on_surface` | `#3D4A5C` | Secondary text |

### Severity Color Mapping

| Severity | Color Token | Hex |
|---|---|---|
| CRITICAL | `color_primary` | `#E63946` |
| HIGH | `color_primary` | `#E63946` |
| MEDIUM | `color_secondary` | `#F4A261` |
| LOW | `color_accent_low` | `#2EC4B6` |
| MONITORING | `color_accent_info` | `#4895EF` |

---

## Typography

Font: **IBM Plex Sans** (primary) + **IBM Plex Mono** (data/trace readouts).

Both available via Google Fonts / bundled in `res/font/`.

| Style | Font | Weight | Size | Usage |
|---|---|---|---|---|
| `text_display` | IBM Plex Sans | Bold (700) | 28sp | Screen titles |
| `text_headline` | IBM Plex Sans | SemiBold (600) | 20sp | Section headers, card titles |
| `text_body_large` | IBM Plex Sans | Regular (400) | 16sp | Body text, descriptions |
| `text_body_medium` | IBM Plex Sans | Regular (400) | 14sp | Secondary info, labels |
| `text_caption` | IBM Plex Sans | Regular (400) | 12sp | Timestamps, metadata |
| `text_mono` | IBM Plex Mono | Regular (400) | 13sp | Agent trace logs, confidence scores, coords |
| `text_badge` | IBM Plex Sans | Bold (700) | 11sp | Severity badges, status chips |

### Urdu / Arabic Script
- Font for Urdu: **Noto Nastaliq Urdu** (bundled)
- Applied via `android:fontFamily="@font/noto_nastaliq_urdu"` on Urdu-specific TextViews
- Text direction: `android:layoutDirection="rtl"` for Urdu text containers
- Never mix scripts in the same TextView

---

## Spacing System

Base unit: **8dp**. All spacing values are multiples of 8.

| Token | Value | Usage |
|---|---|---|
| `spacing_xs` | 4dp | Icon padding, tight gaps |
| `spacing_sm` | 8dp | Inner card padding (compact) |
| `spacing_md` | 16dp | Standard screen horizontal margin, card padding |
| `spacing_lg` | 24dp | Section spacing |
| `spacing_xl` | 32dp | Major section breaks |
| `spacing_xxl` | 48dp | Top margin for first content block |

Screen horizontal margin: always `spacing_md` (16dp) unless inside a map view.

---

## Component Styles

### Cards
```xml
style="@style/NishaanCard"
```
- Background: `color_surface`
- Corner radius: `8dp`
- Elevation: `2dp` (not `4dp` — keeps it flat and serious)
- Border: `1dp` stroke in `color_divider`
- Padding: `16dp` all sides
- No shadows on dark theme (elevation shows as border instead)

### Crisis Alert Cards (High-prominence)
- Left border stripe: `4dp` in severity color
- Background: `color_surface_variant`
- Icon: crisis type icon, 24dp, colored by severity

### Severity Badges
- Pill shape, `4dp` corner radius
- Background: 20% opacity of severity color
- Text: severity color, `text_badge` style, all caps
- No outline/border — background fill only

### Buttons

**Primary Button**
- Background: `color_primary`
- Text: White, `text_body_large`, Bold
- Corner radius: `6dp`
- Height: `52dp`
- No elevation
- Pressed: `color_primary_variant`

**Secondary Button (outline)**
- Background: Transparent
- Border: `1.5dp` `color_primary`
- Text: `color_primary`
- Same size and radius as primary

**Destructive Button**
- Same as primary but `color_primary` (already red — same token)
- Label must make destructive intent clear ("Delete Report", not just "Delete")

**Text Button**
- No background, no border
- Text: `color_accent_info`
- Used for low-emphasis actions only

### FAB (Floating Action Button)
- Color: `color_primary`
- Icon: `+` or `person_add`
- Position: Bottom-right, `16dp` from bottom nav, `16dp` from right edge
- Extended on Home screen: shows "Report Missing" label

### Inputs (Text Fields)
- Style: Outlined (MDC `TextInputLayout` outline box style)
- Background: `color_surface_variant`
- Stroke: `color_divider` at rest, `color_primary` on focus
- Label: `color_on_surface_muted`
- Input text: `color_on_background`
- Corner radius: `6dp`

### Bottom Navigation
- Background: `color_surface`
- Active icon + label: `color_primary`
- Inactive: `color_on_surface_muted`
- No colored indicator pill — active state via color only
- Top border: `1dp` `color_divider`

---

## Animation Style

**Philosophy:** Fast and functional. No animations for decoration. Animations communicate state changes.

| Interaction | Animation |
|---|---|
| Screen transitions (Navigation Component) | Slide left/right, 200ms, linear interpolator |
| Crisis marker appears on map | Scale from 0 to 1, 150ms, overshoot interpolator |
| Alert card enters list | Fade + slide up, 180ms, staggered by 30ms per item |
| Severity badge state change | Cross-fade, 120ms |
| Agent trace new entry | Slide down from top of list, 160ms |
| FAB extended → collapsed | MDC built-in shrink animation |
| Loading states | Circular indeterminate progress, `color_primary` |
| Map geofence pulse | Ripple expand + fade loop, 2s duration |

All durations must be under 300ms. No bounce physics. No spring animations.

---

## Dark / Light Theme Rules

1. Default is **dark theme**. Light theme is a user preference in Profile settings.
2. Use `?attr/colorSurface`, `?attr/colorOnSurface` etc. — never hardcoded hex in layout XML.
3. All drawables that need theme variants go in `drawable-night/` and `drawable/`.
4. Map style switches between `map_style_dark.json` and `map_style_light.json`.
5. Status bar: always matches background (`color_background`). No white status bar in dark mode.

---

## Icons

- Icon set: **Material Symbols** (outlined variant), 24dp default
- Crisis type icons (custom): defined in `res/drawable/` as vector drawables
  - `ic_flood.xml`, `ic_earthquake.xml`, `ic_heatwave.xml`, `ic_unrest.xml`, `ic_accident.xml`
- Agent icons: abstract circuit/signal motifs, monochrome, in `color_accent_info`

---

## Map Styling

- Base map: dark style JSON applied to Google Maps (`map_style_dark.json`)
- Crisis markers: custom `BitmapDescriptor` colored by severity
- Geofence circles: stroke in severity color at 40% opacity, fill at 10% opacity
- Evacuation routes: Polyline in `#2EC4B6` (accent_low), 4dp stroke, dashed
- User location dot: white with `color_primary` border pulse

# Family Guardian — UI v3 Design Direction

**Status:** Proposal • Draft 1
**Audience:** Iddo's father — elderly Hebrew-native speaker, mild tremor, limited patience for modern UI.
**Goal:** Keep the warm, loving personality of v2.1; strip the visual noise; raise it to "Apple Simple View / Samsung Easy Mode / Jitterbug" accessibility grade.

---

## 1. Mood & Metaphor

**One sentence:** *"A warm, lit kitchen table with labeled wooden boxes — everything in its place, nothing shouting."*

Current v2.1 = a children's birthday card: cream background, rainbow tiles, many emoji, decorative banner. Beloved by Dad but visually loud — when every tile is saturated, nothing is emphasized and the SOS stops feeling special.

**v3 direction:**
- Calm, dignified, grown-up warmth. Think "fresh bread on linen" not "birthday balloons."
- Keep exactly one playful moment per screen (the heart in the greeting, a single emoji per tile).
- Quiet background, loud content. Hierarchy through scale and whitespace, not through color competition.
- Hebrew-first typography; every numeral is Hebrew-local where it reads naturally (dates in Hebrew, clock in Arabic numerals for legibility).

---

## 2. Color Palette

### 2.1 Foundation — the "linen" layer

| Role              | Hex        | Notes                                                |
|-------------------|------------|------------------------------------------------------|
| Surface / bg      | `#FBF6EE`  | Warm off-white linen, 0 saturation shift from v2.1.  |
| Surface raised    | `#FFFFFF`  | Cards sit on the linen with a soft shadow.           |
| Hairline          | `#EADFCB`  | 1dp borders on cards; replaces heavy strokes.        |
| Ink primary       | `#2A1C0E`  | Near-black warm brown. 14.2:1 on linen. WCAG AAA.    |
| Ink secondary     | `#6B5B4F`  | Muted brown for time-of-day, date, captions. 5.9:1.  |
| Ink disabled      | `#B2A698`  | Only for admin "hidden" affordance.                  |

### 2.2 Accent — the "warmth" layer

Single primary accent replaces the rainbow-as-chrome. Rainbow survives only inside tiles (see TILE_SPEC.md).

| Role              | Hex        | Notes                                                |
|-------------------|------------|------------------------------------------------------|
| Warm amber        | `#E88C00`  | Brand accent, same as v2.1. Used for focus ring, active states, admin gear. |
| Warm amber soft   | `#FFE7C2`  | Chip backgrounds (battery, date).                    |
| Honey             | `#FFC24A`  | Flashlight tile gradient top.                        |

### 2.3 Semantic — the "signal" layer

| Role              | Hex        | Use                                                  |
|-------------------|------------|------------------------------------------------------|
| SOS red           | `#D13B2E`  | SOS only. Never used elsewhere. Protect its meaning. |
| SOS red deep      | `#9E1F14`  | SOS gradient base, SOS confirm screen.               |
| Safe green        | `#3E8E5A`  | "Call succeeded" toast, battery > 50%.               |
| Caution           | `#C97A2A`  | Battery 10-30%. Avoid yellow — low contrast.         |
| Danger            | `#B23A2E`  | Battery < 10%. Different tone than SOS red.          |

### 2.4 Tile category palette (muted, 6 families)

Replaces the 8-color rainbow palette. Each tile gets a **muted pastel surface** with a **saturated emoji** — inversion of v2.1 where the surface was saturated.

| Category     | Surface    | Ink        | Example tiles                      |
|--------------|------------|------------|------------------------------------|
| People       | `#F7DDE3`  | `#7A2D3E`  | Contacts, Family Call              |
| Communication| `#D9E9F5`  | `#1F4A6B`  | WhatsApp, Messages, Dialer         |
| Capture      | `#E4E0F2`  | `#3E2C6B`  | Camera, Gallery                    |
| Assist       | `#E2EEDB`  | `#2E5A2A`  | Claude, Reminders                  |
| Utility      | `#F3E7CE`  | `#6B4A15`  | Flashlight, Settings               |
| Emergency    | `#F6CEC9`  | `#8A1C12`  | SOS confirm screen only            |

Rationale: elderly users lose ability to distinguish high-saturation hues (especially in the yellow-green band). Muted pastels with a strong dark-ink label give high label contrast **and** category color-coding.

### 2.5 High-contrast mode (accessibility switch in admin)

- Surface: `#FFFFFF`
- Ink: `#000000`
- Accent: `#B85E00` (darker amber for 7:1)
- Tiles: pure white with 3dp amber border, no fills, ink-only label. Emoji retained.

---

## 3. Typography

### 3.1 Font stack

**Primary:** **Rubik** (Google Fonts) — Hebrew + Latin, weights 400/500/700.
- Rationale: purpose-built for Hebrew readability at large sizes, rounded terminals feel warm, excellent on low-density screens, free.
- Fallback cascade: `Rubik → Heebo → Assistant → Open Sans Hebrew → sans-serif`.

**Numerals / clock:** **Rubik** tabular figures (`font-feature-settings: "tnum"`) — replaces v2.1's `Typeface.MONOSPACE` clock, which has poor Hebrew co-location.

**Reject:** Frank Ruehl, Narkis Tam (too literary/serifed for at-a-glance reading); Alef (strokes too thin at the sizes Dad needs).

### 3.2 Type scale (Hebrew-tuned)

Hebrew has no ascenders/descenders and reads ~10% smaller than Latin at the same pt. All sizes below include a +2sp Hebrew uplift vs. a typical Material scale.

| Token             | Size   | Weight | Line-h | Usage                             |
|-------------------|--------|--------|--------|-----------------------------------|
| Display-clock     | 68sp   | 700    | 1.0    | Home clock (`21:47`)              |
| Display-greeting  | 32sp   | 700    | 1.2    | "בוקר טוב אבא"                    |
| Title-tile        | 24sp   | 700    | 1.2    | Tile label                        |
| Title-screen      | 30sp   | 700    | 1.2    | SOS / Contacts / Admin headers    |
| Body-lg           | 22sp   | 500    | 1.4    | Contact names, list items         |
| Body              | 20sp   | 400    | 1.4    | Date, descriptions                |
| Body-sm           | 18sp   | 500    | 1.3    | Battery chip, admin captions      |
| Button-cta        | 28sp   | 700    | 1.0    | SOS "כן, להתקשר"                  |

**Dynamic type:** the admin screen exposes a 3-position slider (רגיל / גדול / ענק = normal / large / huge) which multiplies the scale by `1.0 / 1.15 / 1.30`. Stored in prefs, applied app-wide on resume.

### 3.3 Hebrew typography rules (apply everywhere)

1. **RTL layout direction** on every container. No mirrored icons (phone icon is not mirrored; but a back-arrow IS mirrored in RTL).
2. **Text-align: end** (which resolves to right in RTL) for all Hebrew labels longer than one word.
3. **No all-caps.** Hebrew has no case; forcing tracking is meaningless. Never expand-letter-spacing Hebrew.
4. **Punctuation:** the greeting "בוקר טוב, אבא" uses a Hebrew comma (U+002C is fine in Hebrew), but avoid Latin `!` in headers — use emoji or nothing.
5. **Numbers:** clock and battery use Western Arabic numerals (0–9). Date uses full Hebrew ("יום שני, 14 באפריל 2026"). This matches how Israeli elders actually read.
6. **Maximum line length:** 24 Hebrew characters at Body-lg. Past that, readability drops sharply. Tile labels must fit in 2 lines at this width.

---

## 4. Spacing Scale

Base unit = **8dp**. Generous — cramped layouts intimidate elderly users.

| Token | dp   | Use                                          |
|-------|------|----------------------------------------------|
| s-1   | 8    | Inside a chip                                |
| s-2   | 16   | Between sibling text lines                   |
| s-3   | 24   | Inside a card (padding)                      |
| s-4   | 32   | Between cards                                |
| s-5   | 48   | Section breaks                               |
| s-6   | 64   | Screen-edge safe margin on bottom (thumb zone clearance) |

Grid gutters: **s-3 (24dp)** between tiles. v2.1 uses 16dp which caused accidental neighbor-taps during tremor.

---

## 5. Shape & Elevation

### 5.1 Corner radius

- Tiles & cards: **20dp** (down from v2.1's 24dp — feels slightly more grounded, less "gummy")
- Chips: **14dp**
- SOS button: **24dp** (softer than tile, bigger-feeling)
- Modal dialogs: **28dp**

### 5.2 Elevation philosophy

v2.1 mixes `elevation` (4-8dp) with `setShadowLayer` on text — double shadows compete. v3: **one shadow source per element**, and shadows are **warm, not black**.

| Layer    | Elevation | Shadow                                    |
|----------|-----------|-------------------------------------------|
| Linen bg | 0         | none                                      |
| Tile     | 2         | `0 2 8 rgba(42,28,14,0.10)` (warm brown)  |
| Tile pressed | 1     | shadow halves; tile scales 0.97            |
| Top card | 3         | `0 3 12 rgba(42,28,14,0.08)`              |
| SOS btn  | 6         | `0 4 16 rgba(158,31,20,0.35)` (red-tinted)|
| Modal    | 12        | `0 8 32 rgba(0,0,0,0.25)` + scrim         |

**No `setShadowLayer` on text** — drops the embossed-text look that made v2.1 feel like a toy.

---

## 6. Touch Targets

Minimum **72dp × 72dp** hit area for every interactive element (exceeds WCAG 2.5.5 AAA of 44dp). Rationale: mild tremor + imprecise finger placement. Jitterbug phones use ~70dp; we match.

| Element         | Size            |
|-----------------|-----------------|
| Home tile       | 160dp × 160dp   |
| SOS button      | 88dp height, full row |
| Flashlight btn  | 110dp × 88dp    |
| Admin gear      | 72dp × 72dp     |
| Contact row     | 88dp height     |
| Dialog button   | 72dp height, min 160dp wide |

Spacing between targets: **≥ 16dp** of empty gutter.

---

## 7. Motion

Elderly users benefit from motion (shows cause → effect) but are disturbed by fast, overlapping animations.

- Tile tap: scale 1.0 → 0.97, 120ms ease-out, haptic `CLICK` (25ms).
- Tile release: scale back, 180ms ease-out-back.
- Screen transition: 250ms cross-fade. **No slide/shared-element** (disorients).
- SOS pulse: retained from v2.1 (1.00 → 1.03 → 1.00 over 1800ms). Only element that breathes.
- Long-press admin: scale 1.0 → 1.05 over 3000ms (visual feedback that hold is registering), then haptic `LONG_PRESS` and nav.

**Reduced motion:** if system setting is on, kill all animations except SOS pulse (safety).

---

## 8. Accessibility Contract

1. **Contrast:** every text/bg pair ≥ 4.5:1 (WCAG AA), primary body text ≥ 7:1 (AAA).
2. **Touch:** 72dp minimum, 16dp gutters.
3. **Type:** 3-step dynamic scale user-controlled.
4. **High-contrast mode:** toggle in admin, persists.
5. **Screen reader:** every tile has `contentDescription` in Hebrew — e.g. "כפתור שיחת משפחה. לחץ להתקשר לגלעד." Emoji stripped from content description (TalkBack reads them literally otherwise).
6. **No essential info via color alone:** battery icon + percent text + shape change, not just color.
7. **Keep-screen-on** (already in v2.1) — retained.
8. **Blue-light filter overlay** (already in v2.1) — retained but default OFF; opt-in from admin.
9. **No timeouts, no auto-dismiss** on dialogs. Dad decides when it closes.
10. **Single back path:** hardware back returns to home. No nested screens deeper than 2.

---

## 9. What Carries Over From v2.1 (deliberately)

- Warm cream background family (linen hue, slightly desaturated).
- The amber accent — Dad associates it with the app; changing would break recognition.
- Greeting card with time-of-day personalization ("בוקר טוב אבא").
- SOS red button with subtle pulse.
- Flashlight as a permanent, bottom-row tile (Dad uses it nightly).
- 2-column tile grid.
- Hebrew day and month arrays.

## 10. What Changes

- Rainbow tile palette → muted 6-category palette.
- Decorative rainbow "banner strip" → removed. Replaced with whitespace.
- Emoji-as-chrome reduced: one emoji per tile, not inside every chip and label.
- Monospace clock → Rubik tabular.
- Multiple shadow styles → one warm-brown shadow token.
- Ad-hoc dp() inline styling → token system (colors, spacing, type) that design + code both reference.

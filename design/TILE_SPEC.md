# Family Guardian — Tile Specification (v3)

The tile is the atom of this app. Everything Dad does, he does by tapping a tile. Get the tile right, you've got the app right.

---

## 1. Anatomy

```
┌─────────────────────────────────────┐  ← outer frame, 160×160 dp
│                                     │    corner radius 20dp
│    ·········· 20dp padding ······   │    elevation 2 (warm shadow)
│                                     │
│                                     │
│                 (📞)                │  ← EMOJI, 56sp
│                                     │    centered, high-saturation
│                                     │    bottom-aligned to 56% of tile
│                                     │
│             ··· 12dp ···             │  ← internal gutter
│                                     │
│              טלפון                  │  ← LABEL, 24sp Rubik 700
│                                     │    category-ink color
│                                     │    RTL, centered, max 2 lines
│                                     │
│                                     │
└─────────────────────────────────────┘
       ↑                           ↑
       └──── muted pastel bg ─────┘
           (one of 6 categories)
```

**Anchor points (160dp tall):**
- Emoji baseline sits at **56% of tile height** (roughly 90dp from top) — creates visual weight toward the top, label reads as caption below.
- Label top at **70% of tile height** (roughly 112dp).
- Outer padding: 20dp all around; never touches the edge.

**Tile width:** `(screenWidth - 2*sidePadding - gutter) / 2`. On A70 (393dp): `(393 - 32 - 24) / 2 = 168dp`. Call it 160dp in spec, device flexes.

---

## 2. Label Rules

- **Font:** Rubik 700 (or system Hebrew fallback).
- **Size:** 24sp base, scales to 28sp at "large" dynamic type, 32sp at "huge."
- **Color:** category-specific ink (see palette in `UI_V2_DIRECTION.md §2.4`). Never pure black — warm ink reads softer.
- **Alignment:** center horizontally, center vertically within its zone. Hebrew labels center naturally.
- **Wrap:** max 2 lines. If a label would wrap to 3, shrink to 20sp. If still 3 lines, it's the wrong label — rename in admin.
- **No punctuation.** No `!` or `.`. Hebrew nouns stand alone: "טלפון", "מצלמה", "תמונות".
- **No mixed-script.** If the tile must launch a Latin-named app, prefer Hebrew: "קלוד" not "Claude", "וואטסאפ" not "WhatsApp". Exception: if Dad already knows the Latin brand by sight (WhatsApp logo), show the logo emoji + Hebrew transliteration below.

---

## 3. Emoji / Icon

**v3 keeps emoji.** Dad recognizes them; swapping to custom icons breaks muscle memory and costs us his trust. But we discipline them:

- **One emoji per tile.** Never two ("👥 📞" = bad).
- **Emoji at 56sp** (up from v2.1's 44sp) — emoji is now the primary identifier, the label confirms.
- **System emoji, no custom.** Renders consistently on Dad's A70.
- **Emoji sits on bare pastel surface** — no second colored chip or circle behind it (v2.1 has this inside the greeting card; kill it on tiles).

### Emoji map (canonical — do not substitute)

| Tile                | Emoji | Rationale                              |
|---------------------|-------|----------------------------------------|
| Contacts            | 👥    | "people" — unambiguous                 |
| Family video call   | 📹    | video camera, not 🎥 (film camera)      |
| Dialer              | 📞    | phone handset                          |
| WhatsApp            | 💬    | speech bubble; logo emoji ☎/🟢 is noisy |
| Messages (SMS)      | ✉     | envelope                               |
| Camera              | 📷    | still camera                           |
| Gallery             | 🖼     | framed picture                         |
| Claude / AI         | 🤖    | robot — Dad already calls it "הרובוט"  |
| Reminders / meds    | 💊    | pill (if meds) or 🔔 (if reminders)    |
| Flashlight          | 🔦    | retained                               |
| SOS                 | 🆘    | retained, red context makes it pop     |
| Settings (admin)    | ⚙     | retained, muted                        |

**Rejected:** 🌈, 🎉, ⭐, animated emoji — noise. Category color already provides warmth.

---

## 4. Color — Rainbow vs. Muted Verdict

### Decision: **Move to muted 6-category palette. Rainbow was a mistake.**

Why rainbow v2.1 failed:
- At 100% saturation, every tile competes. Dad's eye lands on nothing in particular — he scans the whole grid every time, defeating recognition.
- The SOS button, which *should* be the most attention-grabbing red on the screen, is outshone by orange/amber neighbor tiles.
- Accessibility research (Nielsen Norman, 2022 elder-UI study) shows that seniors struggle to distinguish more than 4-5 saturated hues; 8 is overload.
- Iddo already observed: "current rainbow/emoji feel is beloved" — we keep the **warmth and playfulness**, just via pastels + bright emoji, not via saturated tile surfaces.

### What we do instead

**Surface:** muted pastel, one of 6 category colors (§2.4 in direction doc).
**Emoji:** full-saturation, stands out crisply on the pastel.
**Label:** dark category-ink.
**Result:** color-codes by function (people = pink family, comm = blue family) **and** lets SOS red be the only saturated red on the screen.

### Category assignment (binding)

| Category       | Surface  | Tiles                              | Promise                 |
|----------------|----------|------------------------------------|-------------------------|
| People         | `#F7DDE3`| Contacts, Family call              | "reach someone"         |
| Communication  | `#D9E9F5`| Dialer, WhatsApp, SMS              | "talk / text"           |
| Capture        | `#E4E0F2`| Camera, Gallery                    | "see / remember"        |
| Assist         | `#E2EEDB`| Claude, Reminders, Meds            | "help me"               |
| Utility        | `#F3E7CE`| Flashlight, (bottom-bar context)   | "tool I use"            |
| Emergency      | `#F6CEC9`| SOS screen only (not home tile)    | "something is wrong"    |

---

## 5. States

| State      | Visual                                                          | Haptic        |
|------------|-----------------------------------------------------------------|---------------|
| Rest       | Pastel surface, elevation 2, warm shadow                        | —             |
| Pressed    | Scale 0.97, elevation 1, shadow halves                          | `CLICK` 25ms  |
| Released   | Spring back to 1.0 over 180ms (ease-out-back)                   | —             |
| Long-press | Scale 1.03, elevation 4, subtle ring in category-ink color      | `LONG_PRESS`  |
| Disabled   | 40% opacity, no shadow, no tap target                           | —             |
| Focused    | 3dp amber ring `#E88C00` outside border — for external keyboard |               |

**No hover state** (touch device).
**No ripple.** Scale + haptic is clearer feedback than ripple for elderly users; ripple is a mobile convention they don't read.

---

## 6. Interaction

### 6.1 Tap (short press, < 500ms)

Launches the tile's primary action. No confirmation screen for everyday tiles (dialer, camera, gallery). **Exception:** SOS — always confirms; video-call — always confirms with "call גלעד now?" because video calls are embarrassing to misfire.

### 6.2 Long-press (≥ 1000ms)

**Default:** nothing. Most tiles have no long-press. Elderly users often hold too long by accident; assigning actions there = surprise bugs.

**Explicit exceptions (4 tiles):**

| Tile            | Long-press behavior                                       |
|-----------------|-----------------------------------------------------------|
| Admin gear      | 3-second hold → PIN dialog (retained from v2.1)           |
| Contacts        | Open contact list (same as tap — redundant safety)        |
| Family call     | Open chooser: "Who? גלעד / ארנונה / אורנה"                |
| Flashlight      | Toggle strobe OFF if accidentally triggered (emergency)   |

All long-press handlers must haptic-feedback at the 1000ms mark so Dad knows "something happened, let go now."

### 6.3 Accidental-touch protection (retained from v2.1)

The `KEY_TOUCH_HOLD_MS` pref (default ~80ms "dwell") — any tap shorter than threshold is ignored. Keeps tremor-jitter from firing calls.

### 6.4 Scroll behavior

Tile grid scrolls vertically. **Over-scroll disabled** (no bounce — disorients). **Fling disabled** (too easy to fling past target). Slow, finger-following drag only.

---

## 7. Special tile variants

### 7.1 Family video call

Same 160×160 anatomy, but the **emoji is 64sp** (extra-large) and the tile has a subtle 2dp solid teal border `#26A69A` to mark it as the "most important call" tile. Still People-pink surface — consistent category.

### 7.2 SOS (bottom bar, not a grid tile)

Not a tile — a **full-width button** 88dp tall. Red gradient `#D13B2E → #9E1F14`. Yellow-gold 3dp border `#FFD54F` — the only yellow element anywhere (signals "attention"). Pulsing scale 1.00 ↔ 1.03. Emoji 🆘 at 32sp + label "קריאת חירום" 24sp bold white. See `SCREEN_LAYOUT.md §1`.

### 7.3 Flashlight (bottom bar)

110×88dp button, honey-amber gradient `#FFC24A → #FFA726`. Emoji 🔦 32sp + label "פנס" 18sp. When ON: gradient inverts to a brighter white-center radial glow + amber ring. Retained from v2.1 essentially unchanged, just cleaner typography.

### 7.4 Admin gear (bottom bar)

72×72dp, muted. `#F3E7CE` surface, gear emoji in `#8B7D6B`. Whispers, doesn't shout. The least tappable-looking thing on the screen — intentional.

---

## 8. Content description (screen-reader / TalkBack)

Each tile must expose a `contentDescription` in Hebrew **without the emoji** (TalkBack will otherwise read "emoji face calling phone Hebrew").

Template: `"כפתור [label]. לחץ [verb]."`

Examples:
- `"כפתור אנשי קשר. לחץ לפתיחת רשימה."`
- `"כפתור שיחת משפחה. לחץ להתקשר לגלעד."`
- `"כפתור חירום. לחץ להתקשר למספר חירום."`

---

## 9. Anti-patterns (do not do)

- Do not add badges / notification dots on tiles. If WhatsApp has unread messages, that's WhatsApp's job to show when he opens it. A red dot on Dad's launcher = anxiety.
- Do not animate tiles on entry ("stagger-in"). He opens the launcher 40 times a day; choreography gets old.
- Do not "swap tile positions based on usage frequency." Kills muscle memory. Positions are fixed by admin, period.
- Do not add tooltips. Tooltips are for people who read on hover. Dad doesn't hover.
- Do not use category color as the text color on a same-color pastel — always use the darker category-ink for labels.
- Do not mix icon styles. Emoji only. No Material Icons, no line-art overlays.

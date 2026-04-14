# Family Guardian — Screen Layouts (v3)

All wireframes are **RTL**. Read right-to-left. Hebrew text sits against the right edge; primary action on the right. Back-arrow (when shown) sits in the top-**left** corner (RTL-mirrored).

Reference device: **Samsung Galaxy A70**, 1080 × 2400 px, 6.7", 393 × 873 dp. All dp measurements below target that canvas; layouts flex.

Legend:
- `█` = filled color block
- `░` = muted pastel tile surface
- `·` = whitespace
- `[...]` = interactive element
- `(🔔)` = emoji (actual emoji rendered full-color)
- Dimensions shown as `W×H dp`

---

## 1. HOME / LAUNCHER SCREEN (primary)

Replaces `LauncherActivity.buildLayout()`. This is the screen Dad sees 95% of the time.

```
┌────────────────────────────────────────────────────┐  ← screen top
│                                                    │   (status bar hidden / translucent)
│   16dp safe margin                                 │
│                                                    │
│   ┌──────────────────────────────────────────┐    │
│   │                                          │    │
│   │                       (❤)  בוקר טוב אבא  │    │  ← GREETING CARD
│   │                                          │    │    361×170 dp
│   │                                  21:47   │    │    bg #FFFFFF, r=20dp
│   │   [🔋 87%]                               │    │    elevation 3
│   │                                          │    │
│   │             יום שני, 14 באפריל 2026       │    │
│   │                                          │    │
│   └──────────────────────────────────────────┘    │
│                                                    │   24dp gap
│   ┌────────────────┐    ┌────────────────┐        │
│   │       ░        │    │       ░        │        │  ← TILE ROW 1
│   │      👥        │    │      📹        │        │    160×160 dp each
│   │                │    │                │        │    People category (pink)
│   │   אנשי קשר     │    │   שיחת משפחה   │        │    & People category (pink)
│   │       ░        │    │       ░        │        │
│   └────────────────┘    └────────────────┘        │
│                                                    │   24dp gutter
│   ┌────────────────┐    ┌────────────────┐        │
│   │       ░        │    │       ░        │        │  ← TILE ROW 2
│   │      📞        │    │      💬        │        │    Communication (blue)
│   │                │    │                │        │
│   │    טלפון       │    │   וואטסאפ      │        │
│   │                │    │                │        │
│   └────────────────┘    └────────────────┘        │
│                                                    │
│   ┌────────────────┐    ┌────────────────┐        │
│   │       ░        │    │       ░        │        │  ← TILE ROW 3
│   │      📷        │    │      🖼         │        │    Capture (lavender)
│   │                │    │                │        │
│   │    מצלמה       │    │   תמונות       │        │
│   │                │    │                │        │
│   └────────────────┘    └────────────────┘        │
│                                                    │
│   ┌────────────────┐    ┌────────────────┐        │
│   │       ░        │    │       ░        │        │  ← TILE ROW 4
│   │      🤖        │    │      💊        │        │    Assist (sage)
│   │                │    │                │        │    (if configured)
│   │    קלוד        │    │   תרופות       │        │
│   │                │    │                │        │
│   └────────────────┘    └────────────────┘        │
│                                                    │   24dp gap
│   ┌──────────────────────────────────────────┐    │
│   │  ┌─────┐  ┌───────────────────┐  ┌─────┐ │    │  ← BOTTOM BAR CARD
│   │  │ ⚙  │  │                   │  │ 🔦  │ │    │    361×120 dp
│   │  │hold │  │  🆘  קריאת חירום  │  │פנס  │ │    │    bg #FFFFFF, r=20dp
│   │  │ 3s  │  │                   │  │     │ │    │    elevation 3
│   │  └─────┘  └───────────────────┘  └─────┘ │    │
│   │   72×72    full-width, 88 high    88×88 │    │
│   └──────────────────────────────────────────┘    │
│                                                    │   24dp bottom margin
│                                                    │   (thumb-zone)
└────────────────────────────────────────────────────┘  ← screen bottom
```

**Measurements:**
- Greeting card: `361 × 170 dp`, 24dp internal padding, 16dp from screen edges, 24dp from top.
- Tile grid: 2 columns, tiles `160 × 160 dp`, **24dp gutter** (was 16dp in v2.1), scrolls if > 4 rows.
- Bottom bar: fixed, **does not scroll**. 88dp above screen bottom. SOS is full-width between admin and flashlight.
- Total visible at once on A70: greeting + 3 tile rows + bottom bar. Row 4 requires a short scroll — Dad's usual tiles (WhatsApp, camera) live in rows 1-3.

**Tile order (fixed, mirrors current v2.1 muscle memory):**
1. Contacts (אנשי קשר) — top-right
2. Family video call (שיחת משפחה) — top-left
3. Dialer (טלפון)
4. WhatsApp (וואטסאפ)
5. Camera (מצלמה)
6. Gallery (תמונות)
7. Claude (קלוד) — if enabled
8. Open slot / reminders / messages (per whitelist order from admin)

**Bottom bar hierarchy:**
- **Admin gear** far-right (RTL far-start), muted, 3-second long-press — **unchanged from v2.1**.
- **SOS** center, full-width, red gradient, pulsing — **unchanged position from v2.1**.
- **Flashlight** far-left, honey-amber — **unchanged from v2.1**.

---

## 2. SOS CONFIRMATION SCREEN

Replaces `SosActivity`. Triggered by SOS button on home. Dad must confirm — prevents accidental dial.

```
┌────────────────────────────────────────────────────┐
│                                                    │
│                                                    │
│                                                    │
│                                         ✕  [ביטול] │  ← CANCEL, top-left
│                                                    │    72dp tap target
│                                                    │
│                                                    │
│                                 🆘                 │  ← SOS glyph, 120sp
│                                                    │     (#D13B2E on linen)
│                                                    │
│                                                    │
│                          קריאת חירום               │  ← TITLE, 36sp, bold
│                                                    │
│                                                    │
│                                                    │
│                   האם להתקשר עכשיו                 │  ← BODY, 24sp
│                        לגלעד?                      │    (family name from prefs)
│                                                    │
│                     050-123-4567                   │  ← 22sp, muted
│                                                    │
│                                                    │
│     ┌──────────────────────────────────────┐      │
│     │                                      │      │
│     │    📞     כן, להתקשר                 │      │  ← PRIMARY CTA
│     │                                      │      │    full-width minus 32dp
│     │                                      │      │    96dp high
│     │                                      │      │    bg: SOS red gradient
│     └──────────────────────────────────────┘      │    text: white 28sp bold
│                                                    │
│                  24dp                              │
│                                                    │
│     ┌──────────────────────────────────────┐      │
│     │                                      │      │
│     │              לא, לא עכשיו             │      │  ← SECONDARY
│     │                                      │      │    88dp high
│     │                                      │      │    bg: linen + 2dp border #EADFCB
│     └──────────────────────────────────────┘      │    text: ink 24sp
│                                                    │
│                                                    │
│                  (3-second countdown auto-dials    │
│                   if no cancel — shown as ring     │
│                   around the 🆘 glyph)              │
└────────────────────────────────────────────────────┘
```

**Behavior:**
- Entire screen is SOS-red pastel surface `#F6CEC9` (subtle wash — tells Dad this is the emergency screen without shouting).
- Countdown ring draws around the 🆘 glyph; 3000ms, 0° → 360°, haptic pulse every 1s.
- If Dad doesn't press anything, call auto-places. This is a deliberate accessibility decision: if he's fallen and hit SOS, passive inaction must still trigger help.
- Cancel top-left is a full 72dp target — tremor-safe.
- The primary CTA is **enormous** (96dp tall, full-width) — far easier than finding a small button in a panic.

---

## 3. IMPORTANT CONTACTS SCREEN

Replaces `ContactsActivity`. Flat, scannable list. No search, no tabs, no sorting controls.

```
┌────────────────────────────────────────────────────┐
│                                                    │
│   ┌─────┐                              ┌────────┐  │
│   │  ←  │                              │ אנשי   │  │  ← HEADER
│   │ 72  │                              │ קשר    │  │    title right, back LEFT
│   │     │                              │        │  │    (RTL back arrow)
│   └─────┘                              └────────┘  │
│                                                    │   24dp gap
│   ┌──────────────────────────────────────────┐    │
│   │  ┌──┐                                    │    │
│   │  │📞│              ┌────────┐             │    │  ← CONTACT ROW
│   │  │  │              │ גלעד    │ [avatar ] │    │    361×110 dp
│   │  └──┘              │         │           │    │    bg #FFFFFF, r=20dp
│   │   72×72            │   בן    │ ┌───────┐ │    │    avatar 72dp circle,
│   │   pastel           └────────┘ │       │ │    │    initials or photo
│   │   people-pink                 │   ג   │ │    │
│   │   (tap = call)                │       │ │    │    name: 26sp bold
│   │                               └───────┘ │    │    relation: 20sp muted
│   │                                                │    end: call icon tile
│   └──────────────────────────────────────────┘    │
│                                                    │   16dp between rows
│   ┌──────────────────────────────────────────┐    │
│   │  ┌──┐              ┌────────┐  ┌───────┐ │    │
│   │  │📞│              │ אורנה  │  │   א   │ │    │  ← Orna (sister)
│   │  └──┘              │  אחות  │  │       │ │    │
│   │                    └────────┘  └───────┘ │    │
│   └──────────────────────────────────────────┘    │
│                                                    │
│   ┌──────────────────────────────────────────┐    │
│   │  ┌──┐              ┌────────┐  ┌───────┐ │    │
│   │  │📞│              │  רפי    │  │   ר   │ │    │  ← Rafi (brother)
│   │  └──┘              │   אח   │  │       │ │    │
│   │                    └────────┘  └───────┘ │    │
│   └──────────────────────────────────────────┘    │
│                                                    │
│   ┌──────────────────────────────────────────┐    │
│   │  ┌──┐              ┌────────┐  ┌───────┐ │    │
│   │  │📞│              │ ארנונה  │  │   א   │ │    │  ← Arnona (wife)
│   │  └──┘              │  אישה  │  │       │ │    │
│   │                    └────────┘  └───────┘ │    │
│   └──────────────────────────────────────────┘    │
│                                                    │
│     ... scrollable, 4-5 visible at once ...       │
│                                                    │
│   ┌──────────────────────────────────────────┐    │
│   │                                          │    │
│   │            🆘    חירום                   │    │  ← Sticky bottom pinned
│   │                                          │    │    shortcut back to SOS
│   │                                          │    │    (so he's never
│   │                                          │    │    trapped from help)
│   └──────────────────────────────────────────┘    │
│                                                    │
└────────────────────────────────────────────────────┘
```

**Per-row interaction:**
- Tap anywhere on the row → places a voice call immediately (no intermediate screen — that's what the big green phone tile on the left signals).
- Long-press (1s) → opens a 2-button modal: **[📹 וידאו] [💬 הודעה]**. This is the only place video-call-per-contact lives; Dad's default is voice.
- Haptic on tap, haptic-long on long-press.

**Contact order:** configured in admin, not alphabetical. Most-called contact is always top.

**Row anatomy (right to left in RTL):**
1. Avatar circle (72dp): family photo if available, else big Hebrew initial on category color.
2. Name (26sp bold) + relation label (20sp muted).
3. `📞` green call affordance (72×72, pastel Safe-green).

---

## 4. ADMIN SCREEN (PIN-gated)

Entered by **3-second long-press on gear**. Before the screen loads, a PIN dialog.

### 4.1 PIN dialog

```
┌────────────────────────────────────────────────────┐
│                                                    │
│                                                    │
│                                                    │
│                    הזן קוד גישה                    │  ← 28sp bold, centered
│                                                    │
│                                                    │
│              ┌───┐ ┌───┐ ┌───┐ ┌───┐              │
│              │ • │ │ • │ │ _ │ │ _ │              │  ← 4-digit PIN dots
│              └───┘ └───┘ └───┘ └───┘              │    each 56×72 dp
│                                                    │
│                                                    │
│           ┌───┐ ┌───┐ ┌───┐                       │
│           │ 3 │ │ 2 │ │ 1 │                       │  ← NUMPAD (RTL)
│           └───┘ └───┘ └───┘                       │    keys 88×88 dp
│           ┌───┐ ┌───┐ ┌───┐                       │    24dp gutter
│           │ 6 │ │ 5 │ │ 4 │                       │
│           └───┘ └───┘ └───┘                       │
│           ┌───┐ ┌───┐ ┌───┐                       │
│           │ 9 │ │ 8 │ │ 7 │                       │
│           └───┘ └───┘ └───┘                       │
│           ┌───┐ ┌───┐ ┌───┐                       │
│           │ ⌫ │ │ 0 │ │   │                       │
│           └───┘ └───┘ └───┘                       │
│                                                    │
│                          [ביטול]                  │
│                                                    │
└────────────────────────────────────────────────────┘
```

Numbers read **right-to-left** per digit pair (1 is on the right, matching how Hebrew speakers write Arabic-numeral phone pads). Default PIN set in first-run; changed inside admin itself.

### 4.2 Admin screen (after PIN)

```
┌────────────────────────────────────────────────────┐
│  [←]                                  הגדרות       │  ← header
│                                                    │
│   ┌──────────────────────────────────────────┐    │
│   │  אנשי קשר חשובים               ╲        │    │  ← section (nav to submenu)
│   │  4 מוגדרים                                │    │
│   └──────────────────────────────────────────┘    │
│                                                    │
│   ┌──────────────────────────────────────────┐    │
│   │  אפליקציות מותרות                ╲        │    │
│   │  WhatsApp, טלפון, מצלמה...                │    │
│   └──────────────────────────────────────────┘    │
│                                                    │
│   ┌──────────────────────────────────────────┐    │
│   │  SOS — מספר חירום                ╲        │    │
│   │  050-123-4567 (גלעד)                      │    │
│   └──────────────────────────────────────────┘    │
│                                                    │
│   ┌──────────────────────────────────────────┐    │
│   │  גודל טקסט           [רגיל] [גדול] [ענק]  │    │  ← inline segmented control
│   └──────────────────────────────────────────┘    │
│                                                    │
│   ┌──────────────────────────────────────────┐    │
│   │  ניגודיות גבוהה                   [OFF]   │    │  ← toggle switch
│   └──────────────────────────────────────────┘    │
│                                                    │
│   ┌──────────────────────────────────────────┐    │
│   │  מסנן אור כחול                    [ON]    │    │
│   └──────────────────────────────────────────┘    │
│                                                    │
│   ┌──────────────────────────────────────────┐    │
│   │  שינוי קוד גישה                   ╲        │    │
│   └──────────────────────────────────────────┘    │
│                                                    │
│   ┌──────────────────────────────────────────┐    │
│   │  אודות + גרסה 3.0                ╲        │    │
│   └──────────────────────────────────────────┘    │
│                                                    │
└────────────────────────────────────────────────────┘
```

**Admin uses a different visual register** — flat list rows, 20dp radius cards, muted chevrons (RTL mirror, so they point left). Zero emoji except toggle indicators. Signals: "this isn't your app, Dad, this is the technician screen."

---

## Cross-screen rules

1. **Back arrow always top-left** (RTL mirror of top-right). Hardware-back also works.
2. **Screen title always top-right.**
3. **SOS always accessible within 1 tap** — every screen except the PIN dialog has either the home SOS button or the sticky bottom SOS shortcut.
4. **No tabs, no bottom nav.** One screen does one thing.
5. **Portrait only.** Rotation disabled in manifest.

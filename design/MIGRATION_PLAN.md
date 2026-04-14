# Family Guardian — v2.1 → v3 Migration Plan

**Core constraint:** Dad has 6+ months of muscle memory on v2.1. Every tile he uses, every position, every color-in-a-position is a cached motor pattern. Breaking those patterns even for "better" UI will produce frustrated calls to Iddo.

**Strategy:** two phases. Phase 1 ships a **visual-only refresh** that keeps every tile in its current grid cell and the bottom-bar layout identical. Phase 2, after Dad has had ~2 weeks to settle, tweaks layout and adds the new contacts/admin screens.

---

## 1. Muscle-Memory Audit (what absolutely cannot move)

From `LauncherActivity.kt:200-550` + observed usage:

| Position / Element         | v2.1 state                         | v3 rule        |
|----------------------------|------------------------------------|----------------|
| Greeting card location     | Top, full-width                    | FROZEN         |
| Time-of-day greeting text  | "בוקר טוב אבא" etc.                | FROZEN         |
| Clock size dominance       | 58sp monospace, right-aligned       | FROZEN (but font changes) |
| Battery chip position      | Left of clock, inline               | FROZEN         |
| Contacts tile              | Grid position 1 (top-right, RTL)   | FROZEN         |
| Family video tile          | Grid position 3                    | FROZEN         |
| SOS button                 | Bottom bar, center, full-width, red| FROZEN         |
| Flashlight                 | Bottom bar, left (RTL-start: right of SOS)  | FROZEN |
| Admin gear                 | Bottom bar, far right (RTL-start)  | FROZEN         |
| Admin = 3-second hold      | 3000ms long-press                  | FROZEN         |
| 2-column grid              | 2 cols                             | FROZEN         |
| Warm cream background      | #FFF8F0                            | SHIFT to #FBF6EE (imperceptible) |

Everything above must look/feel the same to a glance. Changing the exact pixel of the gear icon is fine. Moving the gear to the left side is NOT fine.

---

## 2. Phase 1 — Visual Refresh (v3.0)

**Ship target:** 1 APK, installed the usual way on A70. No new screens, no new behavior.

**What changes:**

| Area                    | v2.1                                 | v3.0                                              |
|-------------------------|--------------------------------------|---------------------------------------------------|
| Background              | `#FFF8F0` cream + decorative rainbow banner | `#FBF6EE` linen, **no banner** — quiet top    |
| Tile surface            | Saturated rainbow (8 colors)         | Muted pastel (6 categories, see TILE_SPEC §4)     |
| Tile label color        | White on saturated                   | Category-ink on pastel (7:1+ contrast)            |
| Tile emoji size         | 44sp                                 | 56sp (emoji becomes primary identifier)           |
| Tile label size         | 20sp                                 | 24sp                                              |
| Tile corner radius      | 24dp                                 | 20dp                                              |
| Tile shadow             | Black 6dp elevation                  | Warm-brown shadow, 2dp elevation                  |
| Tile gutter             | 16dp                                 | 24dp (tremor-safe)                                |
| Greeting card shadow    | Mixed (elevation + text shadow)      | Single elevation-3 warm shadow                    |
| Clock font              | `Typeface.MONOSPACE`                 | Rubik tabular figures                             |
| Clock size              | 58sp                                 | 68sp (fills new breathing room)                   |
| Greeting text-shadow    | Present                              | Removed                                           |
| Heart emoji             | Present                              | Retained — the one playful moment                 |
| SOS gradient            | `#E53935 → #B71C1C`                  | `#D13B2E → #9E1F14` (slightly less aggressive red)|
| SOS border              | 3dp yellow `#FFD54F`                 | Retained                                          |
| SOS pulse               | 1.00 ↔ 1.03, 900ms                   | Retained                                          |
| Flashlight              | Amber gradient                       | Retained, typography cleanup only                 |
| Admin gear              | `#F6EEDD` chip, `#8B7D6B` glyph      | Retained                                          |
| Emoji-on-chip inside card  | Present (heart in greeting etc.)  | Heart retained; decorative-only emoji removed     |
| Decorative banner strip | Above greeting                       | **DELETED**                                       |

**What does NOT change in Phase 1:**
- Zero new screens.
- Contact screen visual = unchanged (still the v2.1 `ContactsActivity`).
- Admin screen visual = unchanged.
- SOS screen visual = unchanged.
- No new tile types, no new emoji, no category re-assignment for existing tiles (Contacts stays on the People-category pink surface but same *tile position*).
- Whitelist order and contents = unchanged.
- PIN, prefs, URLs = unchanged.
- App-install, launcher-role intent-filter = unchanged.

**Rollout:**
1. Build v3.0 APK.
2. Take a screenshot of Dad's current home with `adb shell screencap` — archive as "before."
3. Install over v2.1 (signing key preserved so no re-grant).
4. Show Dad: "אבא, עדכנתי את הצבעים שיהיה יותר נעים לעיניים. הכל במקום." Point to every familiar tile in its familiar spot.
5. Sit with him for one video call so he experiences the unchanged muscle-memory path end-to-end.
6. Leave, observe for 7 days. If he reports "I can't find X" — **roll back immediately**, do not defend the change.

**Acceptance criteria for Phase 1:**
- [ ] Dad successfully places a video call to Iddo within 5 seconds of launcher open.
- [ ] Dad recognizes SOS at a glance, unprompted.
- [ ] No support call in the first 48h about "where did X go."
- [ ] Battery/clock/date still legible at arm's length.
- [ ] Dark room: flashlight reachable in < 3 seconds.

---

## 3. Phase 2 — Layout & New Screens (v3.1)

**Ship target:** 2-3 weeks after Phase 1, contingent on Phase 1 acceptance.

**What's added:**

1. **New Important Contacts screen** (`SCREEN_LAYOUT.md §3`)
   - Replaces current `ContactsActivity`.
   - Big row cards, photo avatars, tap-to-call, long-press for video.
   - Populated from admin; default seed = גלעד, ארנונה, אורנה, רפי.
   - Sticky bottom SOS shortcut.

2. **New SOS confirmation screen** (`SCREEN_LAYOUT.md §2`)
   - Full-screen SOS glyph, 3-second countdown ring, confirm / cancel.
   - Pastel-red wash surface, large cancel target top-left.

3. **New Admin screen** (`SCREEN_LAYOUT.md §4`)
   - PIN dialog with Hebrew-oriented numpad.
   - Flat settings list with inline toggles and segmented controls.
   - Adds: dynamic-type slider, high-contrast toggle.

4. **Dynamic type support**
   - Admin exposes 3-step size (רגיל / גדול / ענק).
   - Persisted; applied in `onResume` across all screens.

5. **High-contrast mode**
   - Admin toggle.
   - When on: pure white surfaces, black ink, amber 3dp borders on tiles.

**What does NOT change in Phase 2:**
- Home tile layout (still 2-col, still same tile positions).
- Bottom-bar positions.
- Greeting card anatomy.
- Admin entry gesture (still 3-second hold on gear).

**Introduction strategy:**
- Iddo walks Dad through the new Contacts screen in person or over video. Says: "אבא, אני רוצה להראות לך מסך חדש שעשיתי בשבילך — לחיצה על אנשי קשר, ותראה."
- First time SOS is tapped post-update, Iddo is on-call.
- Admin changes are done *by Iddo for Dad*, not *by Dad*. The dynamic-type slider is a gift from son to father, not a feature Dad must discover.

**Acceptance criteria for Phase 2:**
- [ ] Dad calls someone from the new Contacts screen on the first try.
- [ ] Dad does not trigger SOS accidentally in the first 7 days (confirmation screen works).
- [ ] Text-size slider set once, stays set.
- [ ] No regression from Phase 1 metrics.

---

## 4. Phase 3 (deferred — not scoped yet)

Ideas parked for later, after Phase 2 settles:
- Voice-command tile: "תגיד לקלוד ..." — dictate → Claude response read aloud.
- Medication reminder tile tied to scheduled-tasks MCP.
- "Today" card above the greeting: weather + appointment + birthday reminders.
- Pairing with the Bluetooth-fob panic button Iddo may build.
- Remote admin (Iddo pushes contact changes from his PC via an MCP server without touching Dad's phone).

None of these land until Phase 2 is stable for a month.

---

## 5. Rollback Protocol

Per the **Last-Known-Good Rule** (`feedback_last_known_good.md`):

1. Before building v3.0, tag current HEAD as `dadapp-v2.1-stable`.
2. Archive the signed v2.1 APK to `E:/KOF_VAULT/dadapp/v2.1-stable.apk` + SHA-256 manifest.
3. If Phase 1 fails acceptance: `adb install -r v2.1-stable.apk` → Dad's launcher is exactly as it was.
4. If Phase 2 fails: keep v3.0 installed, only roll back the new screens (they're feature-flagged in prefs — flip `KEY_V3_SCREENS=false`).

A bad UI update on Dad's only phone is a family incident, not a bug. Safety over speed.

---

## 6. File/Code Touch-points (for the implementer — not this design spec)

For the future coding pass, these are the Kotlin files that will need edits:

- `app/src/main/java/com/family/guardian/ui/LauncherActivity.kt` — colors, typography, shadow, tile builder (Phase 1).
- `app/src/main/java/com/family/guardian/ui/ContactsActivity.kt` — new screen (Phase 2).
- `app/src/main/java/com/family/guardian/ui/SosActivity.kt` — confirmation layout (Phase 2).
- `app/src/main/java/com/family/guardian/ui/AdminActivity.kt` — PIN + settings (Phase 2).
- New: `ui/theme/Tokens.kt` — centralized color + type + spacing constants.
- New: `ui/theme/TileStyle.kt` — category → surface/ink mapping.
- `app/build.gradle.kts` — add Rubik font dependency (Google Fonts downloadable fonts or bundled).

Design tokens should be single-source-of-truth shared with this design folder. When a hex changes here, one constant changes in code. No hard-coded `0xFFxxxxxx.toInt()` scattered across the activity.

---

## 7. Communication Plan

Per **Contact Warmth** (`feedback_contact_warmth.md`) — this is Dad, brother-tier warmth minimum. The rollout message to Dad is never "I updated the app." It's: "אבא, עשיתי לך משהו קטן שיהיה יותר נוח — בוא אני אראה לך."

If Iddo can be physically present for the first launch post-Phase-1, that's the win. Otherwise a video call where Iddo watches Dad tap each tile and confirms it still works.

---

**Principle above all:** *Do no harm to the muscle memory. The best update is the one Dad doesn't notice until he notices it's nicer.*

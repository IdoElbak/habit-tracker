# Handoff — Habit Tracker Android app

> Read this and you can pick up exactly where the work stopped. Written at the end of the second
> session, which took the project from "engine only" to "everything but the phone".
>
> **The app is code-complete. What is left is one keystore and one afternoon with the phone** —
> jump to [§6 → Until it works on your phone](#until-it-works-on-your-phone).
>
> The approved plan lives at `C:\Users\Ido\.claude\plans\hey-this-folder-is-sharded-hanrahan.md`.
> Where this file and the plan disagree, **this file is right** — several decisions changed after the
> plan was approved, and every one of them is recorded in §4.

---

## 1. Goal

Ido wants to replace a Google Sheets habit tracker with a real **Android app for his Samsung phone
(Android 16)**. Two inspiration images sit in the project root:

- `csv_made.png` — the original spreadsheet. Habits with a **weekly goal** (Workout 3×/wk, Walk
  >10,000 steps 7×/wk, Avoid junk food 6×/wk), 7 daily checkboxes per habit, an "Actual" count, a
  "%" against goal, a progress bar, weekly totals, a per-habit trend chart across weeks.
- `mind_form.png` — a second, richer tracker. Adds a **full month grid** (habits × days), daily and
  weekly progress bar charts, an overall Goal/Completed/Left donut, a per-habit
  Goal/Actual/Left/% analysis table, a **Mood + Motivation (1–10) daily rating with a trend chart**,
  a "Top 10 habits" ranking, and emoji per habit.

The second inspiration in spirit is **Duolingo**: repeated nudges through the day, real urgency near
the deadline, and a streak worth protecting.

**The app's job is not record-keeping. It is to push Ido to finish a set of tasks he defines for
himself**, and to make the record of that worth looking at.

Hard requirements from the original request, and where each one landed:

| # | Requirement | Where it lives now |
|---|---|---|
| 1 | Daily checklist that resets visually but keeps every day for ever | Today screen · `day_records` are written once and never recomputed |
| 2 | Analytics — per-task success, weekly and monthly | Stats screen · `data/Stats.kt` |
| 3 | Easy to add tasks over time | FAB on Today and Habits, one tap from either |
| 4 | 1×1 home screen widget | `widget/TrackerWidget.kt`, stretches to 2×1 |
| 5 | Several reminders a day, urgent near the end | `notify/Reminders.kt` — six slots, live countdown from 22:00 |
| 6 | Duolingo-style day streak | `core/engine/StreakEngine.kt` |
| 7 | Fully offline, no server, no account | **No INTERNET permission in the manifest**, and never will be |
| 8 | 2–3 colours, one a blue, no red or yellow in the default | Indigo & Sage default, 7 palettes in Settings |

---

## 2. Current state

**Everything is built, tested, committed and pushed. Nothing is half-finished.**

- Repo: **https://github.com/IdoElbak/habit-tracker** (public, branch `main`, 22 commits)
- CI: **green** on a clean runner — tests, lint and a debug build in about five minutes
- `./gradlew assembleDebug` and `assembleRelease` both build; the release APK is ~3 MB through R8
- **95 unit tests, 0 failures**

### Phases

| Phase | Status |
|---|---|
| 0 · Toolchain | **DONE** — Android SDK, Gradle, JDK, Node all working |
| 1 · Design canvas | **DONE** — published; Ido's visual edits are welcome and block nothing |
| 2 · Engine + data layer | **DONE** |
| 3 · Today / Week / Habits + add and edit | **DONE** |
| 4 · Analytics | **DONE** — per-habit detail screen deliberately skipped, see §6 |
| 5 · Settings | **DONE** — palettes, appearance, week start, reminders, Samsung battery card |
| 6 · Notifications | **DONE** — six slots, live countdown, boot rescheduling |
| 7 · Widget | **DONE** — one responsive widget, 1×1 and 2×1 |
| 8 · Hebrew + bidi | **DONE** — full `values-iw`, language picker, bidi corpus on a debug screen |
| 9 · Export / import | **DONE** — versioned JSON, CSV, restore behind a confirmation |
| 10 · GitHub + CI + APK | **DONE except the keystore**, which only Ido can make |
| 11 · On-device verification | **NOT STARTED — needs the phone** |

### Test status

```
DayBoundaryTest      9      DueCalculatorTest   11
HabitStrengthTest    9      MoodInsightsTest     9
StatsWindowTest      9      StreakEngineTest    20
DayCloserTest       11      TodayUiTest          7
StatsTest            6      BackupTest           4
                    ───────────────────────────────
                    95 tests, 0 failures, 0 errors
```

```bash
export JAVA_HOME="C:/Program Files/Java/jdk-21.0.10"
cd C:/claude_apps/tracker
./gradlew testDebugUnitTest        # 95 tests, ~5s warm
./gradlew lintDebug                # clean; lint errors fail the build on purpose
./gradlew assembleDebug            # app/build/outputs/apk/debug/
```

`JAVA_HOME` is **not set globally on this machine** — export it in every shell or Gradle fails.

The tests deliberately cover what is expensive to get wrong: streak arithmetic, what lands on Today,
the stats maths, the day closer's refusal to rewrite history, and the backup format. There are no UI
tests; the screens are verified by eye, which is exactly what Phase 11 is.

### Published artifacts (both belong to Ido, both private by default)

| What | URL |
|---|---|
| Palette comparison (5 options, phone mockups, light/dark toggle) | https://claude.ai/code/artifact/a382caea-8587-4230-b241-abe49f8a453f |
| **Design canvas** — 9 editable artboards | https://claude.ai/code/artifact/5171f4d0-eae6-4c1e-b880-6f6586361df7 |

To update the design canvas: edit the `.dc.html` working files in `design/`, re-run the seeder, then
republish the **same file path** (`design/habit-tracker-screens.html`) with `contract: "0.1.31"`,
favicon `📐`, and **no** `capabilities` (omitting keeps the stored declaration). Never hand-edit the
seeded output. If Ido has edited it in the browser since, read the artifact back and `--extract` it
into a fresh directory first, or you will discard his work.

```bash
cd "C:/claude_apps/tracker/design"
NODE="C:/claude_apps/.tools/node-v24.19.0-win-x64/node.exe"
SK="C:/Users/Ido/AppData/Local/Temp/claude/bundled-skills/2.1.240/fd75c8d3646f1bc49d2d4606e1cfd3f2/design"
"$NODE" "$SK/seed-canvas.mjs" --template "$SK/payload.template.html" \
  --out habit-tracker-screens.html --title "Habit Tracker Screens" \
  --artboard Main.dc.html --artboard TodayDark.dc.html --artboard Analytics.dc.html \
  --artboard HabitDetail.dc.html --artboard AddHabit.dc.html --artboard Settings.dc.html \
  --artboard Widgets.dc.html --artboard Notifications.dc.html --artboard Tokens.dc.html \
  --canvas canvas.json
"$NODE" "$SK/seed-canvas.mjs" --check habit-tracker-screens.html
```

(The skill base directory is a temp path and may not survive a session — re-run `/design` to
re-extract it if it is gone. The `.dc.html` files in `design/` are permanent and are what matter.)

### Environment / installed tooling

| Tool | State |
|---|---|
| JDK | **21.0.10** at `C:/Program Files/Java/jdk-21.0.10`. `JAVA_HOME` NOT set globally. |
| Android SDK | `C:/Android/sdk` — platforms `android-36` and `android-37.1`, build-tools `36.0.0` and `37.0.0`, platform-tools (adb 1.0.41), licences accepted |
| Gradle | **9.7.1** — wrapper in the project, plus a standalone copy at `C:/claude_apps/.tools/gradle-9.7.1` |
| Node | **v24.19.0 portable** at `C:/claude_apps/.tools/node-v24.19.0-win-x64/node.exe`. Deliberately NOT system-wide — it exists only for the design-canvas tooling. Deleting `.tools/` breaks nothing else. |
| gh (GitHub CLI) | **2.98.0, logged in as `IdoElbak`.** Not always on `PATH` in the Bash tool: `export PATH="$PATH:/c/Program Files/GitHub CLI"` |
| git | 2.53.0 — `main`, 22 commits, pushed. Identity `Ido Elbak <ido.elbak@gmail.com>` |

### Claude Code permissions

Creating the repo needed two allow rules, because the auto-mode classifier refuses outward-facing
actions from a tool call. They live in **`.claude/settings.local.json`** — project-local, personal,
and gitignored:

```json
{ "permissions": { "allow": ["Bash(gh repo create:*)", "Bash(git push:*)"] } }
```

The user-level `~/.claude/settings.json` allow-list is deliberately empty, so nothing leaked into
other projects. Without these rules a session cannot push; with them it can.

---

## 3. Active files

### Project root — `C:\claude_apps\tracker`

```
README.md                     what it is, the rules, how to build, how to release
handoff.md                    this file
csv_made.png / mind_form.png  the two trackers this app replaces
.gitignore                    build/, local.properties, *.jks, .tools/, .claude/settings.local.json
settings.gradle.kts · build.gradle.kts · gradle.properties
local.properties              sdk.dir=C:/Android/sdk — GITIGNORED, forward slashes only
gradlew / gradlew.bat         gradlew is committed WITH the exec bit set (CI needs it)
gradle/libs.versions.toml     ALL dependency versions live here
.github/workflows/check.yml   tests + lint + debug build on every push
.github/workflows/release.yml signed APK attached to a Release on a v* tag
```

### Engine — pure Kotlin, no Android imports, fully unit-tested

```
core/model/Schedule.kt          ScheduleType (DAILY | TIMES_PER_WEEK | SPECIFIC_DAYS), DueState
core/engine/DayBoundary.kt      03:00 rollover, week start, days remaining in the week
core/engine/DueCalculator.kt    DUE / OPEN / NOT_DUE, and justEscalated() for the catch-up nudge
core/engine/StreakEngine.kt     verdicts, the allowance, freezes, closeDay(), streakAtRisk()
core/engine/HabitStrength.kt    Loop-style EMA, 13-day half-life
core/engine/StatsWindow.kt      the 4-week grid and the rolling windows
core/engine/MoodInsights.kt     low-vs-high-day comparison, or silence
```

### Data layer

```
data/db/Entities.kt        habits, completions, day_records, day_ratings, streak_state, categories
data/db/Daos.kt            queries, plus whole-table reads and clears for backup/restore
data/db/Converters.kt      LocalDate <-> epochDay, enums <-> String
data/db/TrackerDatabase.kt version 1, exportSchema = true, singleton, no destructive fallback
data/DayCloser.kt          settles finished days; never closes today, never rewrites a settled day
data/TrackerRepository.kt  the only place DAOs meet the engine. buildToday() and buildWeek() are
                           top-level internal functions precisely so the screen rules are
                           unit-tested without Room. Also `Backfill`, the 7-day correction window.
data/Stats.kt              StatsPeriod, StatsUi, buildStats() — pure and tested
data/Prefs.kt              DataStore: palette, theme mode, week start, reminders on/off
data/Backup.kt             versioned JSON + CSV export, and a restore that replaces
```

### UI — Compose, built against the design canvas

```
MainActivity.kt         AppCompatActivity (for per-app locales). Asks for POST_NOTIFICATIONS once,
                        and settles finished days on every resume.
ui/TrackerApp.kt        NavHost, bottom bar, file pickers, restore dialog, snackbar
ui/TrackerViewModel.kt  ONE view model for the whole app
ui/TodayScreen.kt       streak pill, ring, Due today, Optional, mood + motivation
ui/WeekScreen.kt        seven tappable boxes per habit — the spreadsheet grid, and where a
                        forgotten tick gets fixed
ui/HabitsScreen.kt      the dry definitions: name, how often, archive
ui/EditHabitScreen.kt   name, optional emoji, the three frequency modes
ui/StatsScreen.kt       period selector, headline numbers, strength, heat grid, weekday bars,
                        trend, mood findings
ui/SettingsScreen.kt    palettes, appearance, language, reminders, battery card, backups, and the
                        bidi corpus (debug builds only)
ui/Components.kt        isolated() bidi helper, currentLocale(), Glyph, ring, week dots, tick box
ui/theme/Palette.kt     7 palettes as DATA, light + dark token sets including the heat ramp
ui/theme/Theme.kt       fonts, type ramp, TextDirection.Content on every style, LocalTokens
notify/Reminders.kt     the six slots, the notifications, the alarm booking
notify/ReminderReceiver.kt  where alarms land, and where reminders survive a reboot
widget/TrackerWidget.kt one responsive Glance widget; the ring is a Bitmap
res/values/strings.xml · res/values-iw/strings.xml   every user-facing string, and its Hebrew
res/xml/locales_config.xml · res/xml/tracker_widget_info.xml
res/font/*.ttf          Figtree and Familjen Grotesk, variable, OFL
res/drawable, res/mipmap-anydpi-v26   status-bar icon and the adaptive launcher icon
```

### Tests

```
core/engine/DayBoundaryTest · DueCalculatorTest · StreakEngineTest · HabitStrengthTest
core/engine/StatsWindowTest · MoodInsightsTest
data/DayCloserTest      settling, and the refusal to rewrite a settled day
data/TodayUiTest        what lands on Today, what lands on Week, the back-fill window
data/StatsTest          the stats maths
data/BackupTest         the file format, including old and corrupt files
```

### Design working files — `C:\claude_apps\tracker\design`

```
Main.dc.html · TodayDark.dc.html · Analytics.dc.html · HabitDetail.dc.html · AddHabit.dc.html
Settings.dc.html · Widgets.dc.html · Notifications.dc.html · Tokens.dc.html
canvas.json                   artboard layout + sticky notes
habit-tracker-screens.html    SEEDED OUTPUT — never hand-edit, always regenerate
```

`Tokens.dc.html` is the build sheet: exact hexes, the type ramp, and the Hebrew acceptance strings.
`HabitDetail.dc.html` is drawn but not built — see §6.

---

## 4. Changes made

### The rules, and why they are what they are

**Platform & build**

- **Native Kotlin + Compose + Glance + Room.** Chosen over Flutter and React Native: strongest for
  custom visuals (rings, heatmaps, charts drawn with `Canvas`), the best 1×1 widget and notification
  story, and the fewest new installs.
- `minSdk 26`, `targetSdk 36` (the Android 16 behaviour the phone actually runs), `compileSdk 37` +
  `compileSdkMinor 1` (current AndroidX demands it; compileSdk does not restrict who can install).
- App id `com.idoelbak.tracker`; debug builds get `.debug`, so both can sit on the phone at once.

**Design**

- **Design canvas first**, then Compose built to match. Ido asked for Claude Design to be involved.
- **Indigo & Sage is the default, all 7 palettes are switchable.** Palettes are **data** — a list the
  Settings screen iterates over, so an eighth is one entry.
- **Fonts: Familjen Grotesk** (numbers, titles) + **Figtree** (UI), shipped as variable fonts in
  `res/font`. My choice, flagged, not objected to.
- **A missed habit is a quiet grey outline, never a red X.** Red reads as failure and drives
  uninstalls. Red appears in exactly one place: the end-of-day countdown notification.

**Design tokens — Indigo & Sage**

```
LIGHT                          DARK
background   #F4F0E6           #16181D
surface      #FFFFFF           #1F242D
surfaceAlt   #FAF7EF           #262C36
primary      #2E4A7D           #7B9BD6
success      #7A8B5A           #9DB37A
ink          #1D2430           #E8EAEE
ink2         #4E5765           #B4BBC7
muted        #767E8B           #8A93A3
track        #D9D4C6           #333944
rule         #E6E0D2           #2A303A

heat ramp (single hue, light→dark, never a rainbow):
#F0ECE0  #D4DCBF  #B2C094  #8FA16C  #6B7C47

shape: checkbox 22dp r7 · habit row 48dp r12 · card r16 · widget r20 · pill r999
screen padding 20dp
```

The other six palettes are **derived** from three colours each (primary, success, background) by a
`palette()` factory: neutrals are mixed from the background, so a cream palette gets warm rules and a
cool one cool rules, and dark mode lightens primary and success rather than leaving ink on ink.

**The four pages** — settled in the second session. Ido asked for three habit pages plus analytics
and invited a better idea:

| Tab | What it holds | Why |
|---|---|---|
| **Today** | what is due, then optional weekly-quota habits under their own heading, then mood/motivation | the day, and only the day |
| **Week** | every habit against **its own** full week, as seven tappable boxes | the spreadsheet view this app replaces; also where a forgotten tick is fixed |
| **Habits** | the dry definitions: name + how often | Ido: *"only the dry habits definitions and their configurations"* |
| **Stats** | strength, heat grid, weekday bars, trend, mood | the record, worth looking at |

**Settings is a gear in the Today header, not a fifth tab** — my suggestion, accepted: it is visited
once a month, and four labels stay readable in Hebrew. Changing that is one entry in `Tabs`.

**Habit model**

- **All three scheduling modes**: every day / N times per week / specific weekdays.
- **Tick-only in v1.** No numeric targets; the model can take them later.
- **`TIMES_PER_WEEK` escalation** is the quiet win: a quota habit is `OPEN` while there is slack and
  flips to `DUE` the moment `sessionsOwed == daysLeftInWeek`. Only `DUE` habits count toward the
  day's verdict, so a gym habit never reads as "missed" on a rest day and cannot poison a
  percentage. That same moment triggers the end-of-week catch-up line in the morning nudge.
- **Week starts Sunday** by default (Israel); changeable in Settings between Sat / Sun / Mon.
- **The day rolls over at 03:00** — a tick at 01:30 counts for the day before.
- **Back-fill is capped at seven days** (`Backfill.WINDOW_DAYS`) and lives on the Week grid. It fixes
  the record and the weekly quota; it does **not** change a settled day's verdict, so it cannot buy
  back a streak that was actually lost.
- **Archive, never delete** — history and past percentages survive.
- **Optional emoji per habit**, blank by default.
- **NO SEEDED HABITS.** Ido was explicit: the spreadsheet's habits *"aren't necessarily the ones I
  will keep"*. The app starts empty.

**Today's visibility rule** — Ido's, from the second session:

> *"tasks that are not due to today do not appear in the page of today's tasks but rather only at the
> page of all the tasks i defined. and then on the day they are due they will appear back."*

A `NOT_DUE` habit is absent from Today entirely — not greyed, not collapsed at the bottom. Two
deliberate exceptions, both confirmed: a weekly-quota habit with slack shows under **Optional**
(*"even under optional so they won't count towards the perfect/qualifying day"*), and a habit ticked
out of turn stays visible for the rest of that day rather than vanishing under the hand that ticked
it. `TodayUiTest` pins all of it.

**Streaks — read carefully, this changed twice**

- **One global day-streak** as the headline, per-habit strength alongside it.
- **The 1-off allowance is permanent and has no setting.** Ido: *"remove the option in the settings
  to make one off - just make the one off default."*
- **The allowance scales with the day**: it applies only when **4 or more** habits are due. A flat
  "miss one" is 9% slack on an 11-habit day but 50% on a 2-habit day.
- **Perfect and Complete are counted separately**, because with a permanent allowance you could run
  100 days without ever finishing everything.
- **Freezes: 2 max, start with 2, spent automatically, and 7 days that counted earn one back.**
  This is the rule that changed twice. The final version is Ido's, verbatim:
  > *"a qualifying day should also increment the days. and a frozen day doesn't reset the counter it
  > leaves it as is. only a broken day with no available freezes will reset the days."*

  So: **PERFECT and COMPLETE both increment `cleanDays`. FROZEN leaves it untouched. Only BROKEN
  resets it.** A bad day costs the freeze, not the fortnight of work behind the next one. Three tests
  pin it. (An intermediate version required *perfect* days; it was wrong and was reverted.)
- **A FROZEN day HOLDS the streak at its number and does not increment it** — verified against
  Duolingo rather than guessed. Do not change this back without checking.
- **A settled day is NEVER recomputed.** `DayRecordDao.close()` uses `OnConflictStrategy.IGNORE`,
  not REPLACE. That is the mechanism behind "changing a rule must not damage an existing streak",
  and there is a test called `changing the rules cannot rewrite a settled day` that proves it.
- Gamification is milestone celebrations only. No XP, no coins, no economy. (Not built yet.)

**Mood & motivation**

- Both tracked 1–10, optional, on Today. Stored in `day_ratings`, both fields nullable — a skipped
  rating is not a zero.
- `MoodInsights` compares mean completion on **low days (≤4)** against **high days (≥7)**.
  Deliberately a difference of two averages, not a correlation: *"you finish 30% less on
  low-motivation days"* is actionable; *"r = −0.42"* is not.
- **Returns null rather than manufacturing an insight** below 4 rated days a side or an 8-point gap.

**Analytics**

- **Four-week grid, NOT a calendar month.** Ido: *"make sure it is just the last 4 weeks and not
  month."* Weeks keep the weekday columns aligned, which is the entire point — you read down a
  column and see Saturdays failing. A calendar month starts on an arbitrary weekday and destroys it.
- **Only settled days count.** Today is excluded: counting it would drag every percentage down all
  morning and back up all evening.
- **A day with nothing due is left out of the percentage entirely**, not counted as a zero, so a rest
  day cannot make a good week look bad.
- The weakest-weekday sentence appears only when the gap is at least 5 points across at least 4 known
  weekdays. Below that it is noise, not a finding.

**Notifications** — full Duolingo aggression, with one deliberate exception

```
08:00              the day's plan, and whether a weekly habit is running out of days
12:30/15:30/18:30  progress and what is left, only if something is
22:00              ONGOING colorised RED notification with a LIVE counting-down chronometer
22:45              streak-save alert, only when the streak is genuinely at risk
```

- **It goes silent the moment the day is done.** The one place I held back from Duolingo: firing at
  someone who has already finished has no behavioural upside and gets apps uninstalled.
- **The countdown ticks itself.** `setChronometerCountDown(true)` + `setColorized(true)` +
  `setOngoing(true)` — one notification, not an alarm a minute. It refreshes on every tick and is
  cancelled the moment the day completes.
- **Alarms are inexact on purpose** (`setWindow`, ±10 min) and there is **no `SCHEDULE_EXACT_ALARM`**.
  Habit reminders do not need second accuracy, and Samsung One UI has a well-known bug where denying
  that permission makes the Alarms & Reminders setting vanish entirely.
- **Each alarm books the next when it fires**, so there is one pending intent and it lives in the
  system rather than in the process.
- **`RECEIVE_BOOT_COMPLETED` reschedules everything.** Without it every reminder stops silently the
  first time the phone restarts — the most common way habit apps quietly die.
- **Samsung battery optimisation is a first-class feature**, not an afterthought: a card in Settings
  fires `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` and walks through Settings → Battery →
  Background usage limits → remove from Sleeping apps.

**Widget**

- **One responsive widget, not two.** It declares `targetCellWidth/Height = 1` so the launcher puts
  it in a single cell, and a wide layout adds the streak when it is stretched to two — the swap Ido
  wanted is a drag, not a rebuild, and there is only one thing to keep in step.
- **Glance has no drawing API**, so the ring is painted into a Bitmap with the ordinary Canvas and
  shown as an Image.
- Refreshed on every tick and every 30 minutes via `updatePeriodMillis` — which turned out to be the
  whole periodic story, so the planned WorkManager pass was never written. Each refresh also settles
  any day that finished while the app was shut.

**Hebrew / bidi — Ido cares about this a lot**

His complaint: mixed-language text must not break *"like WhatsApp or Outlook where it ruins the
sentence structure."* The cause is rendering user strings without isolation, so an embedded English
word or a trailing `?` leaks its direction into the surrounding Hebrew. Both mechanisms are in:

1. `BidiFormatter.unicodeWrap(...)` on every user-entered string — `String.isolated()` in
   `ui/Components.kt`, used on Today, Week, Habits, Stats **and in notifications**.
2. `textDirection = TextDirection.Content` on **every** style in the typography, so paragraph
   direction comes from the string's first strong character rather than the app locale.

Plus `android:supportsRtl="true"`, `start`/`end` padding everywhere, `locales_config.xml`, and
`AppCompatDelegate.setApplicationLocales` behind the Settings language picker. Weekday names read
`LocalConfiguration`, not `Locale.getDefault()`, so they re-render on a language switch.

**The acceptance corpus ships as a debug-only card at the bottom of Settings**:

```
האם קראתי היום?          Hebrew + trailing question mark
לקרוא 10 pages ביום      Hebrew + English word + digits
ללכת 10,000 צעדים        Hebrew + grouped digits
מדיטציה (5 min) בבוקר    Hebrew + parenthesised English
ללמוד Russian            Hebrew ending in English
Workout אימון 3x         English + Hebrew + suffix
Read ספר daily!          English + Hebrew + trailing exclamation
```

**Backup**

- The whole database as **one versioned JSON file**. Dates travel as epoch days — the shape SQLite
  already holds — so a backup cannot drift a day across a timezone.
- Unknown fields are ignored and every field has a default, so an old backup loads into a newer app;
  a backup from a *newer* app is refused rather than half-read.
- **CSV** is one row per tick, for the spreadsheet this replaced.
- **A restore replaces, it does not merge.** Two half-merged histories would produce streak numbers
  that never happened. It runs in one transaction behind a dialog that names the counts, and every
  outcome including failure is reported — silence after a restore is how people lose a year of data.
- Files go through the Storage Access Framework, so there is still **no storage permission**.

**Distribution**

1. Public GitHub repo under Ido Elbak — **live**.
2. **One keystore, generated once, NEVER committed.** Android refuses an update signed by a different
   key; that is exactly what makes in-place updates possible for ever.
3. `release.yml` builds a signed APK on a `v*` tag and attaches it as **`tracker.apk`** — the
   README's permanent link points at that exact name. Do not rename it.
4. **Obtainium** on the phone watches the repo's releases and offers one-tap updates. This is the
   real answer to "comfortable to update in the future".
5. Local dev: `adb pair` over Wi-Fi (Android 11+) so `installDebug` needs no cable.

### Pinned versions — `gradle/libs.versions.toml`

```
agp              9.3.2          kotlin           2.2.10      ksp        2.2.10-2.0.2
composeBom       2026.08.00     room             2.8.4       glance     1.1.1
work             2.11.2         datastore        1.2.1       navigation 2.9.8
appcompat        1.8.0          activityCompose  1.13.0      lifecycle  2.11.0
coreKtx          1.19.0         serialization    1.11.0      gradle     9.7.1
```

**Do not casually bump Kotlin** — see §5. `kotlinx-datetime` was deliberately dropped; `java.time` is
native at minSdk 26. WorkManager is declared and currently unused.

---

## 5. Failed attempts

Everything below cost real time. Do not repeat any of it.

### Bash heredocs silently eat backslashes

Writing files with `cat > file <<'EOF'` collapsed `\\` to `\` twice, despite the quoted delimiter:

- `settings.gradle.kts` — `includeGroupByRegex("com\\.android.*")` became `com\.android.*` →
  *"Unsupported escape sequence"*. Fixed by deleting the `content { }` filter block entirely.
- `local.properties` — `sdk.dir=C\:\\Android\\sdk` became `C:Androidsdk` → *"The filename, directory
  name, or volume label syntax is incorrect"*, an error naming neither the file nor the cause.
  **Fixed by using forward slashes: `sdk.dir=C:/Android/sdk`.**

**Rule: use the Write tool for any file containing backslashes.** For long, quote-heavy edits, write
a Python script into the scratchpad and run that — a large heredoc through the Bash tool also failed
outright once with `unexpected EOF while looking for matching quote`.

### AGP 9 toolchain archaeology — five separate failures

1. **`org.jetbrains.kotlin.android` is rejected.** AGP 9 ships Kotlin built in. Removed from both
   build files.
2. `kotlin { compilerOptions { jvmTarget.set(...) } }` inside `android { }` → unresolved.
3. `kotlinOptions { jvmTarget = "17" }` → also unresolved. **The official AGP 9 release notes still
   document this form; the docs are stale.** Removing the block entirely is correct — AGP aligns
   Kotlin's jvmTarget with `compileOptions` itself.
4. **Kotlin 2.4.10 was wrong.** AGP 9.3.2 depends on **KGP 2.2.10** (confirmed by reading
   `gradle-9.3.2.pom`). Check the AGP POM rather than taking the newest Kotlin release.
5. **KSP 2.3.11 was wrong** (that is the new decoupled scheme). The one that pairs with Kotlin
   2.2.10 is **`2.2.10-2.0.2`**.

### KSP vs AGP 9 built-in Kotlin

*"Using kotlin.sourceSets DSL to add Kotlin sources is not allowed with built-in Kotlin."* KSP still
registers generated sources through that DSL. Google's escape hatch, in `gradle.properties`:

```properties
android.disallowKotlinSourceSets=false
```

Verified working. It prints an "experimental" warning on every build; that is expected.

### compileSdk 36 was not enough

Current AndroidX *"requires libraries and applications that depend on it to compile against version
37 or later."* Then the second trap: **`platforms;android-37` does not exist.** Platforms are
minor-versioned now — only `android-37.0` and `android-37.1` — and the DSL needs both parts:

```kotlin
compileSdk = 37
compileSdkMinor = 1
```

The CI workflows install `platforms;android-37.1` explicitly for the same reason.

### CI: two failures on the first green-field run

- **exit 127** — `sdkmanager` is not on the runner's PATH. It lives at
  `$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager`.
- **exit 126** — `gradlew` was committed from Windows without its exec bit. Fix:
  `git update-index --chmod=+x gradlew`. Any new script committed from this machine has the same
  trap waiting.

### Lint is set to fail the build, and caught two real things

- **`Locale.getDefault()` inside a composable** is not observable, so weekday names would keep the
  old language after a switch to Hebrew until the activity was recreated. Fixed with
  `currentLocale()` reading `LocalConfiguration`.
- **`context.getString()` inside `LaunchedEffect`** has the same problem; the snackbar text is now
  resolved in composition.
- One rule is switched off deliberately: `PropertyEscape`, which wants `local.properties` escaped in
  the way that already caused a real bug above.

### kotlinx.serialization and R8

Backups write fine in debug and would have failed in the shrunk release build — the generated
`$$serializer` classes are looked up reflectively. Keep rules are in `app/proguard-rules.pro`, and
the release build has been verified to survive R8.

### Three bugs caught in review rather than on the phone

- The morning nudge asked `justEscalated()` with **zero** completions banked, so it claimed every
  weekly habit was running out of days no matter what had already been done.
- Notifications rendered habit names **without** bidi isolation, so a Hebrew name with an English
  word in it would reorder in the shade — exactly the bug the whole bidi effort exists to prevent.
- `refresh()` settled finished days using the settings **StateFlow's seed value**, which on the first
  resume after launch can still be the default week start. Settled days are never recomputed, so a
  wrong one is wrong for ever — the one class of bug the immutability rule cannot protect against.

### Design canvas — four bugs caught in a self-review pass after publishing

1. **Settings, Notifications and Widgets artboards clipped their content.** Reframed to 1480 / 1120 /
   540. **Always over-provision artboard height** — surplus paints the background; clipping is the
   only failure mode.
2. **Heatmap row labels contradicted the Sunday week start** — Mon/Wed/Fri sat on rows 0/2/4.
3. **Habit detail's weekend dip was on the wrong days.** **In Israel the weekend is Friday–Saturday.**
4. Dead absolutely-positioned SVG in the first widget; `עברית` in Settings needed `dir="auto"`.

### Small environment gotchas

- **`bc` is not available** in this Git Bash. Don't pipe to it.
- **XML comments cannot contain `--`.** A manifest comment with a double hyphen produced
  *"Error parsing AndroidManifest.xml"* and no line number.
- **`BuildConfig` needs `buildFeatures { buildConfig = true }`** on AGP 8+; it is off by default.
- The first Gradle run takes ~60s; subsequent runs ~10s. A release build with R8 takes ~90s.

---

## 6. Next steps

<a name="until-it-works-on-your-phone"></a>

### Until it works on your phone

Five steps. Two of them only Ido can do; the rest is one command each.

**1 · Put the debug build on the phone — 10 minutes, no keystore needed**

The fastest way to see it working. Pair over Wi-Fi, or use a cable.

```bash
# On the phone: Settings → Developer options → Wireless debugging → Pair device with pairing code
export JAVA_HOME="C:/Program Files/Java/jdk-21.0.10"
C:/Android/sdk/platform-tools/adb.exe pair <phone-ip>:<pair-port>     # type the 6-digit code
C:/Android/sdk/platform-tools/adb.exe connect <phone-ip>:<debug-port>
cd C:/claude_apps/tracker && ./gradlew installDebug
```

It installs as its own app (`.debug` suffix), so a release build can live beside it later.

**2 · Walk the verification checklist below.** This is the part no one else can do. Anything wrong
here is a bug to fix before releasing, not after.

**3 · Make the keystore and set four secrets — 15 minutes, once, for ever**

The only thing standing between the repo and an installable APK, and it must be Ido: this key is
what proves every future update comes from him.

```bash
export PATH="$PATH:/c/Program Files/GitHub CLI"
cd C:/claude_apps/tracker

keytool -genkeypair -v -keystore tracker.jks -alias tracker \
  -keyalg RSA -keysize 4096 -validity 10000        # asks for a password; write it down

base64 -w 0 tracker.jks > tracker.jks.b64

gh secret set KEYSTORE_BASE64 < tracker.jks.b64
gh secret set KEYSTORE_PASSWORD      # the password just chosen
gh secret set KEY_ALIAS              # tracker
gh secret set KEY_PASSWORD           # the same, unless a separate key password was set

rm tracker.jks.b64                   # keep tracker.jks itself, safe, for ever
```

`tracker.jks` is gitignored and must stay that way. **Losing it means every install has to be
uninstalled and reinstalled by hand**, because Android refuses an update signed by a different key.
Keep a copy somewhere that will still exist in five years.

**4 · Cut the first release — one command**

```bash
git tag v0.1.0 && git push origin v0.1.0
gh run watch $(gh run list --limit 1 --json databaseId -q '.[0].databaseId') --exit-status
```

The workflow builds the signed APK and attaches it as `tracker.apk`.

**5 · Install it, and never touch a laptop again**

Open https://github.com/IdoElbak/habit-tracker/releases/latest/download/tracker.apk on the phone and
install it. Then install [Obtainium](https://github.com/ImranR98/Obtainium) once and point it at the
repo: every future `git tag v0.x.y` arrives as a one-tap update on the phone.

### On-device verification checklist (Phase 11 — cannot be automated)

1. Add, complete and archive a habit. Check the rollover: tick something after midnight but before
   03:00 and confirm it counts for the day before.
2. Add the widget. Confirm it lands in a **single cell**, that the count drops when you tick
   something in the app, and that stretching it to two cells shows the streak.
3. Leave something undone until 22:00. Confirm the red countdown appears, **ticks down live**, and
   disappears the moment the day is finished.
4. **Reboot the phone. Confirm reminders still fire** — this is the one that breaks silently.
5. Settings → Keep reminders working → **Fix it now**, then Battery → Background usage limits and
   remove Tracker from Sleeping apps.
6. Switch to Hebrew. Walk the bidi card at the bottom of Settings and confirm **every** line reads
   correctly — no word jumping sides, no punctuation drifting. Then check the habit rows, the
   notifications and the Week grid in Hebrew too.
7. Switch all 7 palettes in light and dark. Check the Stats heat grid in each.
8. Miss a day deliberately; confirm a freeze is spent and the streak holds at its number.
9. Forget a tick, then fix it on the Week grid. Confirm the weekly count updates and the settled
   day's verdict does **not**.
10. Export a backup, add some junk, restore it. Confirm the dialog reports the right counts and
    everything comes back.
11. Install the release APK over the debug build and confirm data survives.

### Deliberately left out — each waiting for a reason to exist

- **Category chips on the add screen.** `CategoryEntity` and `CategoryDao` exist and are unused by
  the UI. Nothing groups by category, so the picker would be decoration.
- **Per-habit reminder times.** The `reminderMinuteOfDay` column exists and the scheduler now does
  too, so this is a real option: it needs a time picker on the edit screen and one more alarm slot.
- **The per-habit detail screen** (`design/HabitDetail.dc.html` is drawn). The strength bars already
  answer "which habit is failing"; the natural entry point when it is wanted is tapping one.
- **Spreadsheet name suggestions** as ignorable chips on the Add screen.
- **Milestone celebrations.** Gamification was scoped to these alone and they are not built.
- **A second widget size** and **a WorkManager refresh pass** — both made unnecessary by the
  responsive widget and `updatePeriodMillis`.
- **Room migrations.** Schema is at version 1 and `fallbackToDestructiveMigration` is deliberately
  NOT set. The moment an entity changes, a migration must be written; a backup/restore round trip is
  the escape hatch in the meantime.

### Open questions for Ido

- **Does "Optional" belong on Today at all?** Weekly-quota habits with slack sit in their own section
  there, which he confirmed. If he changes his mind it is one `when` branch in `buildToday()`.
- **Design canvas feedback** — nothing built depends on it, but the artboards are still editable.
- **Version numbering.** `versionName` comes from the tag and `versionCode` from the CI run number,
  so the tag is the only thing to decide.

---

## The second session in one paragraph

Phases 3 through 10 all shipped: the three habit pages and the add/edit screen, Stats, Settings,
reminders, the widget, Hebrew, backup/restore, and CI. The streak rule was corrected twice and now
reads exactly as Ido stated it. Today was narrowed to what is actually due, and the Week grid became
the place a forgotten tick gets fixed. Three real bugs were caught in review rather than on the
phone: the morning nudge claimed every weekly habit was running out of days, notifications skipped
bidi isolation, and `refresh()` could settle a day under the wrong week start — which, because
settled days are never recomputed, would have been wrong for ever. What is left is a keystore and an
afternoon with the phone.

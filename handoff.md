# Handoff — Habit Tracker Android app

> Written at the end of the session that set this project up. Read this and you should be able to
> pick up exactly where it left off. The approved plan lives at
> `C:\Users\Ido\.claude\plans\hey-this-folder-is-sharded-hanrahan.md` (with an "Amendments since
> approval" section at the bottom — read that too).

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

Hard requirements from the original request:

1. Daily checklist that visually resets each day but keeps that day's results forever.
2. Analytics page — per-task average daily success, plus weekly and monthly views.
3. Easy to add new tasks over time.
4. **1×1 home screen widget** showing how the day is going.
5. Several reminders per day, with urgency near end of day.
6. A Duolingo-style day streak.
7. Fully offline — no server, no account. (The manifest deliberately has **no INTERNET permission**.)
8. 2–3 colour theme, one colour a blue variation, **no red, no yellow** in the default.

---

## 2. Current state

### Phases

| Phase | Status |
|---|---|
| 0 · Toolchain | **DONE** — Android SDK, Gradle, JDK, Node all working |
| 1 · Design canvas | **DONE** — published, awaiting Ido's visual edits (not blocking) |
| 2 · Engine + data layer | **DONE** — 94 unit tests, all passing |
| 3 · Today screen + add/edit habit | **DONE** — plus Week and Habits pages (see "Page structure") |
| 4 · Analytics screens | **DONE** — Stats screen; per-habit detail view deliberately deferred |
| 5 · Settings | **DONE** — palette, appearance, week start, reminders, Samsung battery card |
| 6 · Notifications | **DONE** — six slots, live countdown, boot rescheduling, Samsung battery card |
| 7 · Widgets (1×1, 2×1) | **DONE** — one responsive widget: 1×1 ring, stretch to 2×1 for the streak |
| 8 · Hebrew + bidi | **DONE** — full he translation, per-app language picker, bidi corpus on a debug screen |
| 9 · Export/import | **DONE** — JSON backup, CSV export, restore behind a confirmation |
| 10 · GitHub repo + CI + APK distribution | **DONE** except the keystore — repo live at https://github.com/IdoElbak/habit-tracker, CI and the release workflow are in. Ido generates the keystore and sets four secrets (README → Releasing) |
| 11 · On-device verification | **NEXT — only Ido can do this** |

### Test status

```
DayBoundaryTest      9      DueCalculatorTest   11
HabitStrengthTest    9      MoodInsightsTest     9
StatsWindowTest      9      StreakEngineTest    20
DayCloserTest       11      TodayUiTest          6
StatsTest            6      BackupTest           4
                    ───────────────────────────────
                    94 tests, 0 failures, 0 errors
```

Run them with:

```bash
export JAVA_HOME="C:/Program Files/Java/jdk-21.0.10"
cd C:/claude_apps/tracker
./gradlew testDebugUnitTest
```

`JAVA_HOME` is **not set globally on this machine** — export it in every shell or Gradle fails.
`./gradlew assembleDebug` builds green too.

### Published artifacts (both belong to Ido, both private by default)

| What | URL |
|---|---|
| Palette comparison (5 options, phone mockups, light/dark toggle) | https://claude.ai/code/artifact/a382caea-8587-4230-b241-abe49f8a453f |
| **Design canvas** — 9 editable artboards | https://claude.ai/code/artifact/5171f4d0-eae6-4c1e-b880-6f6586361df7 |

To update the design canvas: edit the `.dc.html` working files in `design/`, re-run the seeder, then
republish the **same file path** (`design/habit-tracker-screens.html`) with
`contract: "0.1.31"`, favicon `📐`, and **no** `capabilities` (omitting keeps the stored
declaration). Never hand-edit the seeded output file. If Ido has edited it in the browser since,
read the artifact back and `--extract` it into a fresh directory first, or you will discard his work.

Seeder command:

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
re-extract it if it is gone.)

### Environment / installed tooling

| Tool | State |
|---|---|
| JDK | **21.0.10** at `C:/Program Files/Java/jdk-21.0.10`. `JAVA_HOME` NOT set globally. |
| Android SDK | `C:/Android/sdk` — platforms `android-36` and `android-37.1`, build-tools `36.0.0` and `37.0.0`, platform-tools (adb 1.0.41), licences accepted |
| Gradle | **9.7.1** — wrapper in the project, plus a standalone copy at `C:/claude_apps/.tools/gradle-9.7.1` |
| Node | **v24.19.0 portable** at `C:/claude_apps/.tools/node-v24.19.0-win-x64/node.exe`. Deliberately NOT installed system-wide — it exists only for the design-canvas tooling. Deleting `.tools/` breaks nothing else. |
| **gh (GitHub CLI)** | **2.98.0, logged in as `IdoElbak`.** Ido installed it himself with `winget install --id GitHub.cli`. |
| git | 2.53.0 — **repo initialised**, branch `main`, ten commits, no remote yet |

### The repo, CI, and what is left of Phase 10

`gh auth status` now reports **logged in to github.com as IdoElbak** (scopes: gist, read:org, repo,
workflow). `gh` is still not always on `PATH` in the Bash tool — prefix with:

```bash
export PATH="$PATH:/c/Program Files/GitHub CLI"
```

The local repo exists: branch `main`, ten commits, git identity set locally to

```
user.name  = Ido Elbak
user.email = ido.elbak@gmail.com
```

**The repo is live and pushed: https://github.com/IdoElbak/habit-tracker** (public, branch `main`).
Creating it needed a permission rule — the auto-mode classifier refuses `gh repo create --public
--push` from a tool call. Ido added `Bash(gh repo create:*)` and `Bash(git push:*)` to
`.claude/settings.local.json`, which is project-local and gitignored.

`.github/workflows/check.yml` runs tests, lint and a debug build on every push.
`.github/workflows/release.yml` builds a **signed** APK on a `v*` tag and attaches it to a Release
as `tracker.apk`, which is what the README's permanent download link points at. Do not rename that
asset.

**The one thing left in Phase 10 is the keystore**, and it has to be Ido: generate it once, never
commit it, and set `KEYSTORE_BASE64` / `KEYSTORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD` as repo
secrets. The exact commands are in the README under "Releasing". Losing that file later means every
install has to be uninstalled and reinstalled by hand, because Android refuses an update signed by a
different key.

---

## 3. Active files

### Project root — `C:\claude_apps\tracker`

```
csv_made.png                  inspiration 1 (spreadsheet)
mind_form.png                 inspiration 2 (richer tracker, mood/motivation)
handoff.md                    this file
.gitignore                    excludes build/, local.properties, *.jks, *.keystore, .tools/
settings.gradle.kts
build.gradle.kts
gradle.properties             contains the KSP escape-hatch flag (see Failed attempts)
local.properties              sdk.dir=C:/Android/sdk  — GITIGNORED, forward slashes only
gradlew / gradlew.bat
gradle/libs.versions.toml     ALL dependency versions live here
gradle/wrapper/
```

### App module

```
app/build.gradle.kts
app/proguard-rules.pro
app/schemas/com.idoelbak.tracker.data.db.TrackerDatabase/1.json    (generated by Room, commit it)
app/src/main/AndroidManifest.xml                                   activity, receivers, locale service
app/src/main/res/values/strings.xml                                every user-facing string (en)
app/src/main/res/values-iw/strings.xml                             the Hebrew translation
app/src/main/res/xml/locales_config.xml                            en + iw, for the system picker
app/src/main/res/values/themes.xml                                 AppCompat DayNight shell
app/src/main/res/font/figtree.ttf                                  variable font, OFL
app/src/main/res/font/familjen_grotesk.ttf                         variable font, OFL
README.md
```

### Engine — pure Kotlin, no Android dependencies, fully unit-tested

```
app/src/main/java/com/idoelbak/tracker/core/model/Schedule.kt
    ScheduleType (DAILY | TIMES_PER_WEEK | SPECIFIC_DAYS), Schedule, DueState, DayOfWeek.bit()

app/src/main/java/com/idoelbak/tracker/core/engine/
    DayBoundary.kt      03:00 rollover, week start, days remaining in week
    DueCalculator.kt    DUE / OPEN / NOT_DUE, plus justEscalated() for the weekly catch-up nudge
    StreakEngine.kt     DayVerdict, StreakState, DayOutcome, closeDay(), streakAtRisk()
    HabitStrength.kt    Loop-style EMA, 13-day half-life
    StatsWindow.kt      the 4-week grid + rolling day windows
    MoodInsights.kt     RatedDay, MoodFinding, low-vs-high-day comparison
```

### Data layer

```
app/src/main/java/com/idoelbak/tracker/data/DayCloser.kt
    Settles finished days. Never closes today. Never rewrites a settled day.
    Constructor takes function parameters, not DAOs — that is what makes it unit-testable
    without Room. Wire the real DAOs in at the repository layer.

app/src/main/java/com/idoelbak/tracker/data/db/
    Entities.kt        CategoryEntity, HabitEntity, CompletionEntity, DayRecordEntity,
                       DayRatingEntity, StreakStateEntity
    Converters.kt      LocalDate <-> epochDay Long, enums <-> String
    Daos.kt            HabitDao, CompletionDao, DayRecordDao, DayRatingDao,
                       StreakStateDao, CategoryDao
    TrackerDatabase.kt version 1, exportSchema = true, singleton via get(context)
```

### UI — Compose, built against the design canvas

```
app/src/main/java/com/idoelbak/tracker/MainActivity.kt
    AppCompatActivity (so setApplicationLocales works later). Calls vm.refresh() in onResume,
    which settles finished days and re-reads which day it now is.

app/src/main/java/com/idoelbak/tracker/ui/
    TrackerApp.kt        NavHost + the custom bottom bar. Tabs: Today, Week, Habits, Stats.
                         Settings is a gear in the Today header. Stats and Settings are
                         placeholders until phases 4 and 5.
    TrackerViewModel.kt  ONE view model for the whole app -- today/habits StateFlows, toggle,
                         rate, save, archive. Three screens over one database.
    TodayScreen.kt       header, ring summary, Due today, Optional, mood + motivation sliders
    WeekScreen.kt        every habit against its own full week: bar, 7 dots, "3 of 7"
    StatsScreen.kt       period selector, three headline numbers, strength bars, 4-week heat
                         grid, weekday bars + the weakest-day sentence, 8-week trend, mood
    SettingsScreen.kt    7 palettes, System/Light/Dark, week start, the streak deal in words
    HabitsScreen.kt      the dry definitions only -- name, schedule, edit, archived
    EditHabitScreen.kt   name, optional emoji, the three frequency modes, archive
    Components.kt        isolated() bidi helper, Glyph (SVG path renderer), ring, week dots,
                         tick box, pill
    theme/Palette.kt     7 palettes as DATA, light + dark token sets
    theme/Theme.kt       fonts, type ramp, TextDirection.Content on every style, LocalTokens

app/src/main/java/com/idoelbak/tracker/widget/TrackerWidget.kt
    ONE responsive widget, not two: a 1x1 target that adds the streak when stretched to two
    cells. Glance has no drawing API, so the ring is painted into a Bitmap and shown as an
    Image. Refreshed on every tick (TrackerWidget().updateAll) and every 30 minutes by
    updatePeriodMillis -- no WorkManager pass was needed after all.

app/src/main/java/com/idoelbak/tracker/notify/
    Reminders.kt          the six slots, the notifications, and the alarm booking. Alarms are
                          INEXACT on purpose (setWindow, 10 min): no SCHEDULE_EXACT_ALARM, which
                          Samsung One UI has a nasty bug around. Each alarm books the next one,
                          so there is one pending intent, and it survives the app being killed.
    ReminderReceiver.kt   where alarms land, and where reminders come back after a reboot.

app/src/main/java/com/idoelbak/tracker/data/Backup.kt
    The whole database as one versioned JSON file, plus a CSV export. Dates travel as epoch
    days, the same shape SQLite holds, so a backup cannot drift a day across timezones.
    A restore REPLACES rather than merges, in one transaction, behind a confirmation dialog.

app/src/main/java/com/idoelbak/tracker/data/Prefs.kt
    DataStore-backed Settings: paletteId, themeMode, weekStart. The week start is passed INTO
    the repository rather than held on it -- a mutable field there is shared state that goes
    wrong quietly.

app/src/main/java/com/idoelbak/tracker/data/Stats.kt
    StatsPeriod, StatsUi and buildStats() -- another pure top-level function, so every
    percentage on the stats screen is unit-tested without Room.

app/src/main/java/com/idoelbak/tracker/data/TrackerRepository.kt
    The only place DAOs meet the engine. buildToday() is a top-level internal function
    precisely so the Today rules can be unit-tested without Room.
```

### Tests

```
app/src/test/java/com/idoelbak/tracker/core/engine/
    DayBoundaryTest.kt  DueCalculatorTest.kt  StreakEngineTest.kt
    HabitStrengthTest.kt  StatsWindowTest.kt  MoodInsightsTest.kt
app/src/test/java/com/idoelbak/tracker/data/DayCloserTest.kt
app/src/test/java/com/idoelbak/tracker/data/TodayUiTest.kt    what lands on Today and on Week
app/src/test/java/com/idoelbak/tracker/data/StatsTest.kt      the stats maths
```

### Design working files — `C:\claude_apps\tracker\design`

```
Main.dc.html            Today (light) — INTERACTIVE, checkboxes toggle the ring and count
TodayDark.dc.html       Today (dark), late-evening state, 6 of 7 done
Analytics.dc.html       Stats — strength bars, heatmap, weekday bars, weekly trend
HabitDetail.dc.html     "Read ~10 pages" (deliberately the failing habit)
AddHabit.dc.html        weekday mode expanded, 4 gym days selected
Settings.dc.html        7 palettes, language, reminders, Samsung battery card
Widgets.dc.html         1×1 states + 2×1, at true size on a wallpaper
Notifications.dc.html   the full day of nudges incl. the red countdown
Tokens.dc.html          build sheet: exact hexes, type ramp, HEBREW ACCEPTANCE TEST
canvas.json             artboard layout + sticky notes
habit-tracker-screens.html    SEEDED OUTPUT — never hand-edit, always regenerate
```

---

## 4. Changes made

### Every decision Ido made, and why

**Platform & build**

- **Native Kotlin + Compose + Glance + Room.** Chosen over Flutter and React Native. Ido asked
  whether the stack would be "easily customizable to look very good" — Compose is the *strongest* of
  the three for custom visuals (rings, heatmaps, charts drawn directly with `Canvas`/`Path`), has
  the best 1×1 widget and notification story, and needed the fewest new installs.
- **Phone runs Android 16.** `minSdk 26`, `targetSdk 36`, `compileSdk 37` + `compileSdkMinor 1`.
- **App id / namespace:** `com.idoelbak.tracker`. Debug builds get `.debug` suffix.

**Design**

- **Design canvas FIRST**, then build Compose to match. Ido explicitly asked to involve Claude Design.
- **Palette: Indigo & Sage is the default**, but **all 7 are switchable in Settings** — Ido asked for
  a theme picker, including palettes that DO use red and amber. Palettes are **data, not hard-coded
  colours**: a `Palette` list the Settings screen iterates over.
  1. **Indigo & Sage** (default) — `#2E4A7D` / `#7A8B5A` / `#F4F0E6`
  2. Navy & Teal — `#1B2A4A` / `#1F9C92` / `#F7F7F4`
  3. Blue & Plum — `#3A5BA0` / `#7E5A78` / `#EFEDEA`
  4. Midnight & Mint — `#4C7DF0` / `#4FD1A5` / `#0E1520`
  5. Steel & Olive — `#40566E` / `#6E7A3F` / `#EFE7DA`
  6. Blue & Amber — `#2E4A7D` / `#D99A2B` / `#F6F2E9`
  7. Blue & Crimson — `#2E4A7D` / `#B0453F` / `#F3EFEA`
- **Fonts: Familjen Grotesk** (numbers, titles) + **Figtree** (UI/body). Ship as `res/font`
  resources. This was my choice, not Ido's — flagged to him, not objected to.
- **A missed habit is a quiet grey outline, never a red X.** Red reads as failure and drives
  uninstalls. Red appears ONLY in the end-of-day urgency notification.
- Dark mode follows system with a manual override (System / Light / Dark).

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

heatmap ramp (single hue, light→dark, never a rainbow):
#F0ECE0  #D4DCBF  #B2C094  #8FA16C  #6B7C47

shape: checkbox 22dp r7 · habit row 48dp r12 · card r16 · widget r20 · pill r999
screen padding 20dp · min hit target 48dp
```

**Habit model**

- **All three scheduling modes**: every day / N times per week / specific weekdays. Ido wanted to
  assign gym sessions to 4 specific days and be reminded on those mornings.
- **Tick-only in v1.** No numeric targets. The model is built so numeric can be added later.
- **`TIMES_PER_WEEK` escalation** is the quiet win: a weekly-quota habit is `OPEN` while there is
  slack and flips to `DUE` the moment `sessionsOwed == daysLeftInWeek`. Only `DUE` habits count
  toward the day's verdict, so a gym habit never reads as "missed" on a rest day (which would poison
  every completion percentage). The escalation is also the exact trigger for the end-of-week
  catch-up notification Ido asked for.
- **Week starts Sunday** (Israel), configurable.
- **Day rolls over at 03:00** — a tick at 01:30 counts for the day before.
- **Back-fill capped at 7 days.**
- **Archive, never delete** — history and past percentages survive.
- **Optional emoji per habit, blank by default.** Small grid of common habit icons plus a keyboard
  fallback for anything else.
- **Today shows ONLY what is due today.** Ido's rule from the second session: *"tasks that are not
  due to today do not appear in the page of today's tasks but rather only at the page of all the
  tasks i defined. and then on the day they are due they will appear back."* A `NOT_DUE` habit is
  absent from Today entirely — not greyed, not collapsed at the bottom. Two deliberate exceptions,
  both confirmed by Ido: a weekly-quota habit with slack shows under an **"Optional"** heading
  (*"in the today habits add the optional - even under optional so they won't count towards the
  perfect/qualifying day"*) — ticking it there banks a session and takes it off a later day — and a
  habit ticked out of turn stays visible for the rest of that day rather than vanishing under the
  hand that ticked it. Pinned by `TodayUiTest`.

**Page structure — four tabs, settled in the second session**

Ido asked for three habit pages plus analytics, and invited a better idea. What shipped:

| Tab | What it holds | Why |
|---|---|---|
| **Today** | due habits, then optional ones under their own heading, then mood/motivation | the day, and only the day |
| **Week** | every active habit against **its own** full week — 7 for daily, the quota for weekly, the chosen days for weekday habits — with a bar, the 7-day dots and a weekly total | this is the spreadsheet view the app replaces; measuring each habit against its own goal is what stops rest days looking like failures |
| **Habits** | the dry definitions: name + how often, tap to edit, archived below | Ido: *"only the dry habits definitions and their configurations"* |
| **Stats** | Phase 4 | |

**Settings is a gear in the Today header, not a fifth tab** — that was my suggestion in place of five
tabs, since it is visited once a month, not once an hour. Four tabs also keeps the labels readable
in Hebrew. If Ido would rather have five, it is one entry in the `Tabs` list in `TrackerApp.kt`.
- **NO SEEDED HABITS.** Ido was explicit: *"those in my csv habit_tracker aren't necessarily the
  ones I will keep — don't make the app come with them by default."* The app starts empty. The
  spreadsheet names may appear only as **ignorable tappable suggestions** on the Add screen.

**Streaks — read this carefully, several rules changed during the session**

- **Both**: one global day-streak as the headline (Duolingo pull) + per-habit streaks in detail view.
- **The 1-off allowance is the permanent default, with NO setting to turn it off.** Ido asked for
  this explicitly: *"remove the option in the settings to make one off - just make the one off
  default."*
- **BUT the allowance scales with the day** (my refinement, accepted): allowance is 1 only when
  **4 or more** habits are due. On a day with 3 or fewer due, everything is required. A flat "miss
  one" is 9% slack on an 11-habit day but 50% on a 2-habit day.
- **Perfect vs Complete are tracked separately.** With a permanent allowance you could run 100 days
  without ever finishing everything, so `perfectDays` is counted independently.
- **Freezes: max 2, start with 2, auto-spent, earn 1 back per 7 days that counted.** Settled in the
  second session, after one wrong turn — I briefly made the earn-back require *perfect* days and Ido
  corrected it: *"a qualifying day should also increment the days. and a frozen day doesn't reset the
  counter it leaves it as is. only a broken day with no available freezes will reset the days."* So:
  **PERFECT and COMPLETE both increment `cleanDays`; FROZEN leaves it untouched; only BROKEN resets
  it.** A bad day costs the freeze, not the fortnight of work behind the next one. Three tests pin
  it: `a day saved by the allowance still counts toward the next freeze`,
  `spending a freeze keeps the progress toward the next one`,
  `breaking the streak is the only thing that wipes the progress`. Ido asked for
  "two, or three, max" — 2 was recommended and accepted, because 3 plus the allowance makes breaking
  a streak nearly impossible.
- **A FROZEN day HOLDS the streak at its current number and does NOT increment it.** This was
  changed from the originally approved plan (which said `streak + 1`). Ido said *"make the way it is
  in Duolingo, I think it counts but I don't remember"* — **verified by research: Duolingo preserves
  the number without incrementing.** Do not change this back without checking with him.
- **A settled day is NEVER recomputed.** `DayRecordDao.close()` uses `OnConflictStrategy.IGNORE`,
  not REPLACE. That is the mechanism behind Ido's requirement that a rule change must not damage an
  existing streak. There is a test named
  `changing the rules cannot rewrite a settled day` that proves it.
- **Habit strength** (Loop-style EMA, 13-day half-life) sits alongside streaks. 13 days of practice
  from zero lands on exactly 0.5 — there is a test asserting that.
- Gamification: milestone celebrations only. No XP, coins, or economy.

**Mood & motivation** (from `mind_form.png`, added after plan approval)

- **Both tracked**, 1–10, optional per day, on the Today screen below the checklist.
- Stored in `day_ratings`. Both fields nullable — a skipped rating is not a zero.
- `MoodInsights` compares mean completion on **low days (≤4)** against **high days (≥7)**.
  Deliberately a difference of two averages, not a correlation coefficient: *"you finish 30% less on
  low-motivation days"* is actionable; *"r = −0.42"* is not.
- **Returns null rather than manufacturing an insight** when there are fewer than 4 rated days per
  side or the gap is under 8 percentage points.

**Analytics**

- **Four-week grid, NOT a calendar month.** Ido: *"make sure it is just the last 4 weeks and not
  month."* Weeks aligned to the week start keep the weekday columns aligned, which is the whole
  point — you read down a column and see Saturdays failing. A calendar month starts on an arbitrary
  weekday and destroys that. `StatsWindow.gridWeeks()` implements it; there is a test asserting
  every column is a single weekday.
- Period selector Week / Month / Year / All, default **Month**.
- Metrics: habit strength per habit, calendar heatmap (overall + per habit), completion rate,
  current + best streak, **day-of-week bars** (the most actionable chart), weekly trend line,
  totals (completions, days tracked, perfect days). Insight cards deferred to v2.

**Notifications** — Ido asked for full Duolingo aggression

```
08:00              day plan, names weekday habits — "Today is a gym day"
12:30/15:30/18:30  progress + what's left, only if incomplete
22:00 → midnight   ONGOING colorized RED notification with a LIVE counting-down chronometer
                   setChronometerCountDown(true) + setColorized(true) + setOngoing(true)
                   updates as tasks are ticked
22:45              streak-save alert, only when genuinely at risk
on escalation      "2 gym sessions, 2 days left"
per habit          optional individual times, off by default
```

- **Goes silent once the day is done** (one congratulation, then nothing). This is the ONE place I
  held back from Duolingo — firing after completion has no behavioural upside. Flagged to Ido, not
  objected to.
- **No `SCHEDULE_EXACT_ALARM`.** Uses inexact doze-tolerant alarms (`setWindow`, ±10 min). An
  optional "precise reminders" toggle can request it later. Note the Samsung One UI 6.1 bug: if the
  user denies it, the Alarms & Reminders setting disappears entirely.
- **Samsung reliability is a first-class feature, not an afterthought.** One UI "deep sleeps" apps
  and silently kills reminders — the single most common reason habit-reminder apps fail on Samsung.
  Needs a first-run screen requesting `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` and walking
  through *Settings → Battery → Background usage limits → remove from Sleeping apps*.
  `RECEIVE_BOOT_COMPLETED` reschedules everything after a reboot.

**Widgets**

- **1×1**: progress ring with the remaining count in the middle. Filled ring + tick when done.
  Needs `targetCellWidth = 1`, `targetCellHeight = 1`, `minResizeWidth ≤ 56dp`.
- **2×1**: tasks left AND streak side by side. Ido asked for this so he can swap later without a
  rebuild.
- Refresh on every completion toggle, at day rollover, and via a periodic WorkManager pass.

**Hebrew / bidi — Ido cares about this a lot**

His exact complaint: mixed-language text must not break *"like WhatsApp or Outlook where it ruins
the sentence structure."* The cause is rendering user strings without isolation, so an embedded
English word or a trailing `?` leaks its direction into surrounding Hebrew and jumps to the wrong
side. Two mechanisms, **both required**:

1. `BidiFormatter.getInstance().unicodeWrap(...)` on every user-entered string before display —
   inserts Unicode FSI/PDI isolate marks.
2. `textDirection = TextDirection.Content` on every `Text` and text field — paragraph direction
   comes from the string's first strong character, not the app locale.

Plus `android:supportsRtl="true"` (already in the manifest), `start`/`end` padding everywhere
(never `left`/`right`), `AppCompatDelegate.setApplicationLocales()` with `locales_config.xml`,
strings in `values/` (en) and `values-iw/` (he).

**The acceptance test is already drawn** in `design/Tokens.dc.html` — seven deliberately nasty
strings shown twice, unisolated vs isolated. Ship them as a debug screen and verify visually in both
app languages:

```
האם קראתי היום?          Hebrew + trailing question mark
לקרוא 10 pages ביום      Hebrew + English word + digits
ללכת 10,000 צעדים        Hebrew + grouped digits
מדיטציה (5 min) בבוקר    Hebrew + parenthesised English
ללמוד Russian            Hebrew ending in English
Workout אימון 3x         English + Hebrew + suffix
Read ספר daily!          English + Hebrew + trailing exclamation
```

**Distribution — Ido wants to update from the phone, no laptop**

1. Public GitHub repo under **Ido Elbak**.
2. **One keystore, generated once, NEVER committed.** Android refuses an update signed by a
   different key — this is what makes in-place updates possible forever. Base64 it into repo
   secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
3. GitHub Actions: on a `v*` tag, build the signed release APK and attach it to a Release.
4. README carries a permanent link to `releases/latest/download/tracker.apk`.
5. **Obtainium** — Ido installs it once on the phone, points it at the repo, and it watches releases
   and offers one-tap updates. This is the real answer to "comfortable to update in the future".
6. Local dev: **`adb pair` over Wi-Fi** (Android 11+, fine on 16) so `gradlew installDebug` needs no
   cable. USB as fallback.

### Pinned versions — `gradle/libs.versions.toml`

```
agp              9.3.2          kotlin           2.2.10      ksp        2.2.10-2.0.2
composeBom       2026.08.00     room             2.8.4       glance     1.1.1
work             2.11.2         datastore        1.2.1       navigation 2.9.8
appcompat        1.8.0          activityCompose  1.13.0      lifecycle  2.11.0
coreKtx          1.19.0         serialization    1.11.0      gradle     9.7.1
```

**Do not casually bump Kotlin.** See Failed attempts — it must match what AGP bundles.

`kotlinx-datetime` was deliberately dropped; `java.time` is native at minSdk 26.

---

## 5. Failed attempts

Everything below cost real time. Do not repeat any of it.

### Bash heredocs silently eat backslashes

Writing files with `cat > file <<'EOF'` **collapsed `\\` to `\`** twice, despite the quoted
delimiter:

- `settings.gradle.kts` — `includeGroupByRegex("com\\.android.*")` became `com\.android.*`
  → *"Unsupported escape sequence"*. Fixed by deleting the `content { }` filter block entirely
  (it was only an optimisation).
- `local.properties` — `sdk.dir=C\:\\Android\\sdk` became `C\:\Android\sdk`, which Java properties
  parsing turned into `C:Androidsdk` → *"The filename, directory name, or volume label syntax is
  incorrect"*, a completely unhelpful error. **Fixed by using forward slashes: `sdk.dir=C:/Android/sdk`.**

**Rule: use the Write tool for any file containing backslashes.** Do not use heredocs for them.

### AGP 9 toolchain archaeology — five separate failures

1. **`org.jetbrains.kotlin.android` is rejected.** AGP 9 ships Kotlin built in. Error:
   *"Remove the 'org.jetbrains.kotlin.android' plugin from this project's build file."* Removed from
   BOTH `build.gradle.kts` and `app/build.gradle.kts`.
2. **`kotlin { compilerOptions { jvmTarget.set(...) } }` inside `android { }`** → *"Unresolved
   reference 'jvmTarget'"*.
3. **`kotlinOptions { jvmTarget = "17" }`** → also unresolved. The official AGP 9 release notes
   still document this form; **the docs are stale.** Removed the block entirely — AGP aligns Kotlin's
   jvmTarget with `compileOptions` on its own, and the build is green.
4. **Kotlin 2.4.10 was wrong.** AGP 9.3.2 has a runtime dependency on **KGP 2.2.10** (confirmed by
   reading `gradle-9.3.2.pom`). The compose and serialization compiler plugins must be **2.2.10**.
   Always check the AGP POM rather than taking the newest Kotlin release.
5. **KSP 2.3.11 was wrong** (that is the new decoupled scheme). The version that pairs with Kotlin
   2.2.10 is **`2.2.10-2.0.2`**.

### KSP vs AGP 9 built-in Kotlin

*"Using kotlin.sourceSets DSL to add Kotlin sources is not allowed with built-in Kotlin."*
KSP still registers its generated sources through the Kotlin source-set DSL. Google's documented
escape hatch is in `gradle.properties`:

```properties
android.disallowKotlinSourceSets=false
```

**Verified working** — Room generated all 7 DAO implementations and exported its schema. It prints an
"experimental" warning on every build; that is expected and harmless.

### compileSdk 36 was not enough

Current AndroidX (core-ktx 1.19.0 etc.) *"requires libraries and applications that depend on it to
compile against version 37 or later."* Then a second trap: **`platforms;android-37` does not exist.**
Platforms are minor-versioned now — only `android-37.0` and `android-37.1`. The DSL needs both parts:

```kotlin
compileSdk = 37
compileSdkMinor = 1
```

`targetSdk` stays **36 deliberately** — Android 16 is what Ido's phone runs and what can actually be
tested. compileSdk is compile-time API surface only and does not restrict which devices can install.

### Design canvas — four bugs caught in a self-review pass after publishing

1. **Settings, Notifications and Widgets artboards were too short and clipped their content**
   (Settings by ~200px, Notifications by ~150px). Reframed to 1480 / 1120 / 540. Both Today screens
   were within 16px of clipping and had their padding tightened. **Always over-provision artboard
   height — surplus paints the background, clipping is the only failure mode.**
2. **Heatmap row labels contradicted the Sunday week start** — Mon/Wed/Fri sat on rows 0/2/4, which
   is a Monday-start grid. Shifted to rows 1/3/5.
3. **Habit detail's weekend dip was on the wrong days** — it penalised Sunday and Saturday. **In
   Israel the weekend is Friday–Saturday.** Now rows 5 and 6.
4. Dead absolutely-positioned SVG in the first widget; `עברית` in Settings needed `dir="auto"` +
   `unicode-bidi: isolate`.

### Small environment gotchas

- **`bc` is not available** in this Git Bash. Don't pipe to it.
- **`gh` may not be on `PATH`** in the Bash tool — `export PATH="$PATH:/c/Program Files/GitHub CLI"`.
- The first Gradle run takes ~60s (JIT warmup + dependency resolution). Subsequent runs ~10s.
- `mind_form.png` **appeared in the folder mid-session**, after the plan was already approved. The
  original request said "two inspiration sources" but only one image was present at first. If
  something seems to be missing, check the folder again.

---

## 6. Next steps

### Deliberately left out of Phase 3 — add when there is a consumer

- **Category chips on the add screen.** `CategoryEntity` and `CategoryDao` exist and are unused by
  the UI. Nothing groups by category yet, so the picker would be decoration. Add it when Analytics
  groups by category.
- **Per-habit reminder time.** The `reminderMinuteOfDay` column exists; there is no scheduler yet, so
  a control that sets it would do nothing. Add it with Phase 6.
- **DataStore for the selected palette and light/dark override.** Palettes are already data and the
  theme takes a `Palette` parameter — the picker and its persistence belong to Phase 5, where they
  have a screen to live on.
- **Spreadsheet name suggestions** as ignorable chips on the Add screen.
- **Launcher icon.** The app currently ships the system default. Needed before the release APK.

### Phase 4 as built — and what it left out

The Stats screen has the period selector (Week / Month / Year / All), three headline numbers, habit
strength bars, the four-week consistency grid, weekday bars with the weakest-day sentence, the
eight-week trend line, and the mood/motivation findings. `buildStats()` in `data/Stats.kt` is pure
and tested; the screen only draws.

Two decisions worth knowing:

- **Stats count settled days only.** Today is excluded because it has not closed — counting it would
  drag every percentage down all morning and back up all evening. The empty state says so.
- **The per-habit detail screen (`design/HabitDetail.dc.html`) was not built.** The strength bars
  already answer "which habit is failing", and a detail view is a second full screen for a question
  the list mostly answers. Add it when the strength row genuinely needs somewhere to lead —
  the natural entry point is tapping a strength row.

### Immediately — Phase 5: Settings

Build against `design/Settings.dc.html`. Everything it needs already exists as data:

1. **Palette picker** — `Palettes.all` is a list of 7; `TrackerTheme(palette = …)` already takes one.
   Persist the choice in DataStore (the dependency is declared, unused so far) and read it in
   `TrackerApp` before `TrackerTheme`.
2. **Light / dark / system override** — `TrackerTheme` already takes `dark: Boolean`.
3. **Week start** — currently the constant `TrackerRepository.weekStart` with a `ponytail:` comment
   on it. Moving it to DataStore is the whole change; every date question already routes through it.
4. **Language** — `AppCompatDelegate.setApplicationLocales` plus `locales_config.xml`. MainActivity
   is already an `AppCompatActivity` for exactly this.
5. **Samsung battery card** — `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, and the walkthrough for
   Settings → Battery → Background usage limits. This matters more than it looks: One UI silently
   killing alarms is the single most common reason habit reminders stop working.

The Settings route already exists (gear in the Today header) and currently renders a placeholder.

Remaining phases in order: 6 Notifications · 7 Widgets · 8 Hebrew/bidi · 9 Export/import ·
10 CI + APK release flow · 11 On-device verification.

### Blocked on Ido

- **Creating the GitHub remote and pushing.** Claude Code's permission classifier refused
  `gh repo create --public --push`. He runs it himself:
  `! gh repo create habit-tracker --public --source=. --remote=origin --push`
  (or approves the same command when prompted). The ten local commits are ready to go.
- **The keystore** for Phase 10's release flow — generated once, never committed, base64'd into
  repo secrets `KEYSTORE_BASE64` / `KEYSTORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD`.
- **Design canvas feedback** — not blocking.
- **Whether "This week" belongs on Today at all.** Weekly-quota habits with slack currently sit in
  their own optional section there, per the approved design. If he meant those to be Habits-page-only
  too, it is one `when` branch in `buildToday()`.

### On-device verification checklist (Phase 11, cannot be automated)

1. Add, complete and archive a habit; confirm rollover at 03:00.
2. Add the 1×1 widget; confirm it lands in a **single cell** and the count decrements on tick.
3. Confirm the 22:00 red countdown appears, ticks down live, and clears on completion.
4. **Reboot the phone; confirm reminders still fire.**
5. Switch to Hebrew; walk the bidi test corpus and confirm every string reads correctly.
6. Switch all 7 palettes in light and dark.
7. Export → wipe → import; confirm streak and history restore.
8. Install the release APK over the debug build via the README link; confirm data survives.

---

## Second session — what changed

1. **Freeze earn-back now counts perfect days only** (`StreakEngine.qualified`), `cleanDays` renamed
   to `perfectRun`. One new test.
2. **Today filters to what is due today**, and a fourth **Habits** tab holds everything defined.
   Five new tests in `TodayUiTest`.
3. **Phase 3 shipped**: theme layer with 7 palettes, both variable fonts in `res/font`, the
   repository, Today / Habits / Add-Edit screens, nav and bottom bar. `assembleDebug` is green.
4. **Phase 4 shipped too**: the Stats screen, with `buildStats()` as a pure tested function.
5. **Git repo initialised**, commits by area (scaffold, engine, data, design, UI, freeze fix, page
   split, stats). The GitHub remote is the one thing still outstanding — the permission classifier
   refuses `gh repo create --public --push` from a tool call, twice asked.

Two judgement calls worth re-reading above: what was deliberately left out of Phase 3, and the open
question about whether "This week" belongs on Today.

---

## The last thing said in the first session

> **Phase 2 complete — 75 tests, zero failures.**
>
> ```
> DayBoundaryTest     9      DueCalculatorTest  11
> HabitStrengthTest   9      MoodInsightsTest    9
> StatsWindowTest     9      StreakEngineTest   17
> DayCloserTest      11
>                    ──────────────────────────────
>                    75 tests, 0 failures
> ```
>
> The two tests that matter most:
>
> - **`changing the rules cannot rewrite a settled day`** — settles a week under one schedule, then
>   re-settles under a stricter one and asserts the stored verdicts don't move. That's your "changing
>   this will not ruin current streak" requirement, proved rather than asserted.
> - **`spending a freeze then running clean earns it back in two weeks`** — your exact scenario:
>   14-day streak, 8-of-11 on two consecutive days, both freezes spent, streak still reads 14, two
>   clean weeks restore both.
>
> Also verified: a rest day for a gym habit produces `EMPTY`, not a miss, so it can't break anything
> or poison a percentage.
>
> **On the second image**, four things are now in:
> - **Mood + motivation** (1–10, optional per day). The finding is a difference of two averages, and
>   it stays silent below 4 rated days per side or an 8-point gap — it will say nothing rather than
>   invent an insight.
> - **Four-week grid, not a calendar month** — as you asked. Weeks keep the weekday columns aligned,
>   which is what lets you read down a column and see Saturdays failing; a calendar month starts on
>   an arbitrary weekday and destroys that.
> - **Optional emoji**, blank by default. It's simple: a small grid of common habit icons with a
>   keyboard fallback.
> - **No seeded habits** — app starts empty, spreadsheet names appear only as ignorable suggestions
>   on the Add screen.
>
> **On the freeze rule:** I checked rather than guessing. Duolingo preserves the streak number
> without incrementing it — which is what I'd already built, so no change.
>
> Two things worth knowing:
> - The toolchain needed real archaeology. AGP 9 ships Kotlin built in, so the compiler plugins must
>   match **Kotlin 2.2.10** (what AGP bundles), not the newest 2.4.10 — and `compileSdk` had to go to
>   **37.1** because current AndroidX demands it. `targetSdk` stays **36** deliberately, since that's
>   the Android 16 behaviour your phone actually runs.
> - **`gh` isn't logged in yet.** When we reach the GitHub repo, you'll need to run `! gh auth login`
>   yourself — it's interactive and I can't drive it.
>
> **Next up is Phase 3: the Today screen and add/edit habit, built against the canvas.** Your design
> edits are still welcome — nothing built so far depends on them.

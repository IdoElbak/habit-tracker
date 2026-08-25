# Tracker

An offline habit tracker for Android. No account, no server, no `INTERNET` permission — everything
lives on the phone.

It exists to push a day's tasks to completion, not to file them: a checklist that resets every day
but keeps every day forever, a streak worth protecting, reminders that get more urgent as the day
runs out, and a 1×1 home-screen widget showing how the day is going.

## The rules that make it work

- **A day rolls over at 03:00.** Ticking something at 01:30 counts for the evening you are still in.
- **Only what is due today is on Today.** A habit scheduled for Mondays is simply absent on Tuesday;
  it is on the Habits page, and it comes back on Monday. A weekly-quota habit sits under "Optional"
  while it has slack — ticking it there takes it off a later day — and moves up to "Due today" the
  moment the sessions owed equal the days left.
- **Miss one and the day still counts** — but only when four or more habits were due. On a light day
  everything is required.
- **Two freezes, spent automatically** on a genuinely bad day. A frozen day holds the streak where it
  is rather than extending it. **Seven days that counted restore one freeze** — a day saved by the
  allowance counts, and a spent freeze does not wipe the progress behind the next one. Only actually
  breaking the streak resets it.
- **A settled day is never recomputed.** Changing a rule tomorrow cannot reach back and damage a
  streak already earned — there is a test that proves it.
- **Archive, never delete.** Past ticks and percentages survive.

## The four pages

| Page | What it is for |
|---|---|
| **Today** | What has to happen now: everything due, plus optional weekly-quota habits under their own heading. Mood and motivation at the bottom. |
| **Week** | Every habit against its own full week — 7 for a daily habit, the quota for a weekly one, the chosen days for a weekday one. The spreadsheet view this app replaces. |
| **Habits** | The definitions only: name and how often. No ticking, no progress. |
| **Stats** | Strength, heatmap, day-of-week bars, trends. |

Settings is a gear in the Today header rather than a fifth tab.

## Build

```bash
export JAVA_HOME="C:/Program Files/Java/jdk-21.0.10"   # not set globally on this machine
./gradlew testDebugUnitTest     # the engine and the Today rules
./gradlew assembleDebug         # app/build/outputs/apk/debug/
```

`local.properties` is not committed; it needs `sdk.dir=C:/Android/sdk` (forward slashes).

## Layout

```
core/model      schedules and due states
core/engine     day boundary, due calculator, streaks, habit strength, stats windows, mood insights
data            Room entities/DAOs, the day closer, the repository the UI reads
ui              Compose screens, palette tokens, typography
design          the design canvas artboards this UI is built against
```

Fonts are [Figtree](https://github.com/erikdkennedy/figtree) and
[Familjen Grotesk](https://github.com/kavelbaser/familjen-grotesk), both under the SIL Open Font
License 1.1.

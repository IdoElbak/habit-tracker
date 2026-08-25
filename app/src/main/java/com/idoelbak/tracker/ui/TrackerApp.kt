package com.idoelbak.tracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.runtime.collectAsState
import com.idoelbak.tracker.ui.theme.TrackerTheme
import com.idoelbak.tracker.ui.theme.theme

private const val ROUTE_TODAY = "today"
private const val ROUTE_WEEK = "week"
private const val ROUTE_HABITS = "habits"
private const val ROUTE_STATS = "stats"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_EDIT = "edit"

private data class Tab(val route: String, val label: String, val glyph: String)

/**
 * Four tabs, in the order the day is actually lived: what to do now, how the week is going, what a
 * habit is, and what all of it adds up to. Settings is a gear in the Today header instead of a fifth
 * tab -- it is visited once a month, not once an hour.
 */
private val Tabs = listOf(
    Tab(ROUTE_TODAY, "Today", Glyphs.TODAY),
    Tab(ROUTE_WEEK, "Week", Glyphs.WEEK),
    Tab(ROUTE_HABITS, "Habits", Glyphs.LIST),
    Tab(ROUTE_STATS, "Stats", Glyphs.BARS)
)

@Composable
fun TrackerApp(vm: TrackerViewModel = viewModel()) {
    val nav = rememberNavController()
    val current by nav.currentBackStackEntryAsState()
    val route = current?.destination?.route
    val onATab = Tabs.any { it.route == route }

    val today by vm.today.collectAsState()
    val week by vm.week.collectAsState()
    val habits by vm.habits.collectAsState()

    TrackerTheme {
        Scaffold(
            containerColor = theme.background,
            bottomBar = { if (onATab) BottomBar(route) { nav.switchTo(it) } },
            floatingActionButton = {
                if (route == ROUTE_TODAY || route == ROUTE_HABITS) {
                    FloatingActionButton(
                        onClick = { nav.navigate(ROUTE_EDIT) },
                        containerColor = theme.primary,
                        contentColor = theme.onPrimary
                    ) {
                        Glyph(Glyphs.PLUS, 22.dp, theme.onPrimary, strokeWidth = 2f)
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = nav,
                startDestination = ROUTE_TODAY,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                composable(ROUTE_TODAY) {
                    TodayScreen(
                        ui = today,
                        onToggle = { vm.toggle(it) },
                        onRate = { mood, motivation -> vm.rate(mood, motivation) },
                        onAdd = { nav.navigate(ROUTE_EDIT) },
                        onSettings = { nav.navigate(ROUTE_SETTINGS) }
                    )
                }

                composable(ROUTE_WEEK) { WeekScreen(week) }

                composable(ROUTE_HABITS) {
                    HabitsScreen(
                        habits = habits,
                        onOpen = { nav.navigate("$ROUTE_EDIT?id=$it") },
                        onAdd = { nav.navigate(ROUTE_EDIT) },
                        onUnarchive = { vm.unarchive(it) }
                    )
                }

                composable(ROUTE_STATS) {
                    Placeholder("Stats", "Charts, heatmap and habit strength land in the next phase.")
                }

                composable(ROUTE_SETTINGS) {
                    Placeholder("Settings", "Palettes, language and reminders land in a later phase.")
                }

                composable(
                    route = "$ROUTE_EDIT?id={id}",
                    arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L })
                ) { entry ->
                    val id = entry.arguments?.getLong("id")?.takeIf { it > 0 }
                    EditHabitScreen(
                        existing = habits.firstOrNull { it.id == id },
                        onSave = { name, emoji, schedule ->
                            vm.save(id, name, emoji, schedule)
                            nav.popBackStack()
                        },
                        onArchive = {
                            id?.let { vm.archive(it) }
                            nav.popBackStack()
                        },
                        onCancel = { nav.popBackStack() }
                    )
                }
            }
        }
    }
}

private fun androidx.navigation.NavHostController.switchTo(route: String) {
    if (currentDestination?.route == route) return
    navigate(route) {
        popUpTo(ROUTE_TODAY) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun BottomBar(route: String?, onPick: (String) -> Unit) = Row(
    Modifier
        .fillMaxWidth()
        .background(theme.surface)
        .padding(20.dp, 12.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    Tabs.forEach { tab ->
        val active = tab.route == route
        Column(
            Modifier
                .weight(1f)
                .clickable { onPick(tab.route) },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Glyph(tab.glyph, 21.dp, if (active) theme.primary else theme.muted, strokeWidth = 2f)
            Text(
                tab.label,
                style = MaterialTheme.typography.labelSmall,
                color = if (active) theme.primary else theme.muted
            )
        }
    }
}

@Composable
private fun Placeholder(title: String, note: String) = Box(
    Modifier.fillMaxSize().padding(32.dp),
    contentAlignment = Alignment.Center
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = theme.ink)
        Text(
            note,
            style = MaterialTheme.typography.bodyMedium,
            color = theme.muted,
            textAlign = TextAlign.Center
        )
    }
}

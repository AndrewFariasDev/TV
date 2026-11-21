package dev.neonlauncher.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AppSortingTest {
    private val sampleApps = listOf(
        app(pkg = "com.zeta", label = "Zeta Player", lastLaunched = 1_000L),
        app(pkg = "com.alpha", label = "alpha Stream", lastLaunched = 0L),
        app(pkg = "com.beta", label = "Beta TV", lastLaunched = 5_000L),
        app(pkg = "com.gamma", label = "Gamma Hidden", lastLaunched = 9_000L),
    )

    @Test
    fun visibleAppsExcludeHiddenAndSortAlphabeticallyCaseInsensitive() {
        val visible = AppSorting.visibleApps(
            apps = sampleApps,
            hiddenPackages = setOf("com.gamma"),
            sortOrder = SortOrder.ALPHABETICAL,
        )

        assertEquals(
            listOf("com.alpha", "com.beta", "com.zeta"),
            visible.map { it.packageName },
        )
    }

    @Test
    fun visibleAppsSortByMostRecentWithAlphabeticalTieBreakerForUnlaunched() {
        val apps = sampleApps + app(pkg = "com.aardvark", label = "Aardvark", lastLaunched = 0L)
        val visible = AppSorting.visibleApps(
            apps = apps,
            hiddenPackages = setOf("com.gamma"),
            sortOrder = SortOrder.RECENT,
        )

        assertEquals(
            listOf("com.beta", "com.zeta", "com.aardvark", "com.alpha"),
            visible.map { it.packageName },
        )
    }

    @Test
    fun favoriteAppsExcludeHiddenAndFollowActiveSortOrder() {
        val hidden = setOf("com.gamma")
        val favorites = setOf("com.zeta", "com.alpha", "com.gamma")

        val visible = AppSorting.visibleApps(sampleApps, hidden, SortOrder.RECENT)
        val favoriteRecent = AppSorting.favoriteApps(visible, favorites, SortOrder.RECENT)
        assertEquals(listOf("com.zeta", "com.alpha"), favoriteRecent.map { it.packageName })

        val visibleAlpha = AppSorting.visibleApps(sampleApps, hidden, SortOrder.ALPHABETICAL)
        val favoriteAlpha = AppSorting.favoriteApps(visibleAlpha, favorites, SortOrder.ALPHABETICAL)
        assertEquals(listOf("com.alpha", "com.zeta"), favoriteAlpha.map { it.packageName })
    }

    @Test
    fun hiddenAppsReturnOnlyHiddenSortedAlphabetically() {
        val hidden = AppSorting.hiddenApps(
            apps = sampleApps,
            hiddenPackages = setOf("com.gamma", "com.alpha"),
        )

        assertEquals(listOf("com.alpha", "com.gamma"), hidden.map { it.packageName })
    }

    private fun app(
        pkg: String,
        label: String,
        lastLaunched: Long,
    ): AppInfo = AppInfo(
        packageName = pkg,
        activityName = "$pkg.MainActivity",
        label = label,
        isLeanback = true,
        isSystemApp = false,
        hasBanner = false,
        lastLaunchedAt = lastLaunched,
    )
}

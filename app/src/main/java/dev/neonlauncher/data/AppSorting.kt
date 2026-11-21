package dev.neonlauncher.data

/**
 * Pure Kotlin sorting and filtering helpers for installed applications.
 */
object AppSorting {
    private val alphabeticalComparator: Comparator<AppInfo> =
        compareBy<AppInfo, String>(String.CASE_INSENSITIVE_ORDER) { it.label }
            .thenBy { it.packageName }
            .thenBy { it.activityName }

    private val recentComparator: Comparator<AppInfo> =
        compareByDescending<AppInfo> { it.lastLaunchedAt }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.label }
            .thenBy { it.packageName }

    /** Sorts [apps] according to [sortOrder] with deterministic alphabetical tie-breaking. */
    fun sortApps(apps: List<AppInfo>, sortOrder: SortOrder): List<AppInfo> {
        val comparator = when (sortOrder) {
            SortOrder.ALPHABETICAL -> alphabeticalComparator
            SortOrder.RECENT -> recentComparator
        }
        return apps.sortedWith(comparator)
    }

    /** Returns non-hidden apps sorted according to [sortOrder]. */
    fun visibleApps(
        apps: List<AppInfo>,
        hiddenPackages: Set<String>,
        sortOrder: SortOrder,
    ): List<AppInfo> {
        val filtered = apps.filterNot { it.packageName in hiddenPackages }
        return sortApps(filtered, sortOrder)
    }

    /** Returns visible favorite apps sorted according to [sortOrder]. */
    fun favoriteApps(
        visibleApps: List<AppInfo>,
        favoritePackages: Set<String>,
        sortOrder: SortOrder,
    ): List<AppInfo> {
        val favorites = visibleApps.filter { it.packageName in favoritePackages }
        return sortApps(favorites, sortOrder)
    }

    /** Returns hidden apps sorted alphabetically for easy management in Settings. */
    fun hiddenApps(
        apps: List<AppInfo>,
        hiddenPackages: Set<String>,
    ): List<AppInfo> {
        return apps
            .filter { it.packageName in hiddenPackages }
            .sortedWith(alphabeticalComparator)
    }
}

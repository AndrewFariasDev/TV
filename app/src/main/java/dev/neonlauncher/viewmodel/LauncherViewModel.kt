package dev.neonlauncher.viewmodel

import android.app.Application
import android.content.Intent
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.neonlauncher.NeonLauncherApp
import dev.neonlauncher.R
import dev.neonlauncher.core.DeviceCapabilities
import dev.neonlauncher.core.DeviceCapabilitiesProvider
import dev.neonlauncher.core.HomeAliasManager
import dev.neonlauncher.core.SafeModeManager
import dev.neonlauncher.data.AppInfo
import dev.neonlauncher.data.AppSorting
import dev.neonlauncher.data.BackgroundStyle
import dev.neonlauncher.data.NeonThemeConfig
import dev.neonlauncher.data.QuickAction
import dev.neonlauncher.data.QuickActionsResolver
import dev.neonlauncher.data.SortOrder
import dev.neonlauncher.data.ThemeJsonParser
import dev.neonlauncher.data.UserPreferences
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Top-level modal overlays rendered by Neon Launcher. */
enum class ActiveOverlay {
    NONE,
    SETTINGS,
    ONBOARDING,
    SAFE_MODE,
}

/** Immutable UI state consumed by [dev.neonlauncher.ui.screens.NeonLauncherRoot]. */
data class LauncherUiState(
    val isLoading: Boolean = true,
    val allApps: List<AppInfo> = emptyList(),
    val favoriteApps: List<AppInfo> = emptyList(),
    val hiddenApps: List<AppInfo> = emptyList(),
    val favoritePackages: Set<String> = emptySet(),
    val quickActions: List<QuickAction> = emptyList(),
    val sortOrder: SortOrder = SortOrder.ALPHABETICAL,
    val themes: List<NeonThemeConfig> = emptyList(),
    val activeTheme: NeonThemeConfig,
    val backgroundStyleOverride: BackgroundStyle? = null,
    val use24HourClock: Boolean = true,
    val performanceMode: Boolean = false,
    val performanceModeIsAuto: Boolean = false,
    val highContrastMode: Boolean = false,
    val activeOverlay: ActiveOverlay = ActiveOverlay.NONE,
    val contextMenuApp: AppInfo? = null,
    val safeModeCrashSummary: String? = null,
    val capabilities: DeviceCapabilities,
    val statusMessageRes: Int? = null,
) {
    val effectiveBackgroundStyle: BackgroundStyle
        get() = backgroundStyleOverride ?: activeTheme.backgroundStyle
}

/** Shared callback contract invoked by the Compose TV UI. */
interface LauncherActions {
    fun onLaunchApp(app: AppInfo)
    fun onOpenContextMenu(app: AppInfo)
    fun onDismissContextMenu()
    fun onToggleFavorite(packageName: String)
    fun onHideApp(packageName: String)
    fun onUnhideApp(packageName: String)
    fun onOpenAppInfo(packageName: String)
    fun onUninstallApp(packageName: String)
    fun onToggleSortOrder()
    fun onQuickAction(action: QuickAction)
    fun onOpenSettings()
    fun onCloseOverlay()
    fun onSelectTheme(themeId: String)
    fun onSelectBackgroundStyle(style: BackgroundStyle)
    fun onToggle24HourClock(enabled: Boolean)
    fun onTogglePerformanceMode(enabled: Boolean)
    fun onToggleHighContrast(enabled: Boolean)
    fun onResetSettings()
    fun onRequestSetDefaultHome()
    fun onReturnToStockLauncher()
    fun onCompleteOnboarding()
    fun onOpenOnboarding()
    fun onSafeModeReenableHome()
    fun onSafeModeContinueManual()
    fun onSafeModeResetAll()
    fun onClearStatusMessage()
}

/** One-shot Activity navigation effects executed by [dev.neonlauncher.MainActivity]. */
sealed interface LauncherEffect {
    data class LaunchActivity(
        val intent: Intent,
        @param:StringRes val failureMessageRes: Int,
    ) : LauncherEffect

    data class RequestDefaultHome(val intent: Intent) : LauncherEffect
}

private data class TransientControlState(
    val appsLoaded: Boolean = false,
    val prefsLoaded: Boolean = false,
    val manualOverlay: ActiveOverlay? = null,
    val contextMenuApp: AppInfo? = null,
    val safeModePending: Boolean = false,
    val safeModeCrashSummary: String? = null,
    val statusMessageRes: Int? = null,
)

private data class CatalogSnapshot(
    val apps: List<AppInfo>,
    val themes: List<NeonThemeConfig>,
    val quickActions: List<QuickAction>,
    val capabilities: DeviceCapabilities,
)

/**
 * Coordinates installed apps, DataStore preferences, theme bundles, device capabilities,
 * Quick Controls, and Safe Mode recovery state for Neon Launcher.
 */
class LauncherViewModel(application: Application) : AndroidViewModel(application), LauncherActions {
    private val app = application as NeonLauncherApp
    private val themeRepository = app.themeRepository
    private val preferencesRepository = app.preferencesRepository
    private val appCatalogRepository = app.appCatalogRepository

    private val rawAppsFlow = MutableStateFlow<List<AppInfo>>(emptyList())
    private val themesFlow = MutableStateFlow(listOf(ThemeJsonParser.CyberCyanFallback))
    private val quickActionsFlow = MutableStateFlow<List<QuickAction>>(emptyList())
    private val capabilitiesFlow = MutableStateFlow(DeviceCapabilitiesProvider.inspect(app))
    private val latestPrefsFlow = MutableStateFlow<UserPreferences?>(null)

    private val transientFlow = MutableStateFlow(
        TransientControlState(
            safeModePending = SafeModeManager.isSafeModePending(app),
            safeModeCrashSummary = SafeModeManager.getCrashSummary(app),
        ),
    )

    private val _effects = MutableSharedFlow<LauncherEffect>(extraBufferCapacity = 8)
    val effects: SharedFlow<LauncherEffect> = _effects.asSharedFlow()

    private val catalogFlow = combine(
        rawAppsFlow,
        themesFlow,
        quickActionsFlow,
        capabilitiesFlow,
    ) { apps, themes, quickActions, capabilities ->
        CatalogSnapshot(
            apps = apps,
            themes = themes,
            quickActions = quickActions,
            capabilities = capabilities,
        )
    }

    val uiState: StateFlow<LauncherUiState> = combine(
        catalogFlow,
        preferencesRepository.preferencesFlow,
        transientFlow,
    ) { catalog, prefs, transient ->
        buildUiState(catalog, prefs, transient)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = LauncherUiState(
            isLoading = true,
            themes = themesFlow.value,
            activeTheme = ThemeJsonParser.CyberCyanFallback,
            activeOverlay = if (SafeModeManager.isSafeModePending(app)) {
                ActiveOverlay.SAFE_MODE
            } else {
                ActiveOverlay.NONE
            },
            safeModeCrashSummary = SafeModeManager.getCrashSummary(app),
            capabilities = capabilitiesFlow.value,
        ),
    )

    init {
        viewModelScope.launch {
            preferencesRepository.preferencesFlow.collect { prefs ->
                val wasFirstLoad = latestPrefsFlow.value == null
                latestPrefsFlow.value = prefs
                if (wasFirstLoad) {
                    transientFlow.update { it.copy(prefsLoaded = true) }
                    refreshApps(prefs.recentLaunchTimes)
                }
            }
        }
        viewModelScope.launch {
            val loadedThemes = themeRepository.loadThemes()
            themesFlow.value = loadedThemes
        }
        refreshCapabilitiesAndActions()
    }

    /** Refreshes capabilities, quick actions, and installed apps when returning to the launcher. */
    fun refreshAll(clearIconCache: Boolean = false) {
        if (clearIconCache) {
            appCatalogRepository.clearCache()
        }
        refreshCapabilitiesAndActions()
        viewModelScope.launch {
            refreshApps(latestPrefsFlow.value?.recentLaunchTimes.orEmpty())
        }
    }

    /** Handles Home key re-entry while already running (`singleTask` `onNewIntent`). */
    fun onHomeIntentReceived() {
        transientFlow.update { state ->
            val nextOverlay = if (state.manualOverlay == ActiveOverlay.SETTINGS ||
                state.manualOverlay == ActiveOverlay.ONBOARDING
            ) {
                null
            } else {
                state.manualOverlay
            }
            state.copy(contextMenuApp = null, manualOverlay = nextOverlay)
        }
    }

    fun reportStatusMessage(@StringRes messageRes: Int) {
        transientFlow.update { it.copy(statusMessageRes = messageRes) }
    }

    override fun onLaunchApp(app: AppInfo) {
        val intent = appCatalogRepository.createLaunchIntent(app)
        if (intent == null) {
            reportStatusMessage(R.string.status_launch_failed)
            return
        }
        transientFlow.update { it.copy(contextMenuApp = null) }
        viewModelScope.launch {
            preferencesRepository.recordLaunch(app.packageName, System.currentTimeMillis())
        }
        _effects.tryEmit(LauncherEffect.LaunchActivity(intent, R.string.status_launch_failed))
    }

    override fun onOpenContextMenu(app: AppInfo) {
        transientFlow.update { it.copy(contextMenuApp = app) }
    }

    override fun onDismissContextMenu() {
        transientFlow.update { it.copy(contextMenuApp = null) }
    }

    override fun onToggleFavorite(packageName: String) {
        viewModelScope.launch {
            preferencesRepository.toggleFavorite(packageName)
        }
    }

    override fun onHideApp(packageName: String) {
        transientFlow.update { it.copy(contextMenuApp = null) }
        viewModelScope.launch {
            preferencesRepository.hideApp(packageName)
        }
    }

    override fun onUnhideApp(packageName: String) {
        viewModelScope.launch {
            preferencesRepository.unhideApp(packageName)
        }
    }

    override fun onOpenAppInfo(packageName: String) {
        val intent = appCatalogRepository.createAppInfoIntent(packageName)
        if (intent == null) {
            reportStatusMessage(R.string.status_action_unavailable)
            return
        }
        transientFlow.update { it.copy(contextMenuApp = null) }
        _effects.tryEmit(LauncherEffect.LaunchActivity(intent, R.string.status_action_unavailable))
    }

    override fun onUninstallApp(packageName: String) {
        val intent = appCatalogRepository.createUninstallIntent(packageName)
        if (intent == null) {
            reportStatusMessage(R.string.status_action_unavailable)
            return
        }
        transientFlow.update { it.copy(contextMenuApp = null) }
        _effects.tryEmit(LauncherEffect.LaunchActivity(intent, R.string.status_action_unavailable))
    }

    override fun onToggleSortOrder() {
        val current = uiState.value.sortOrder
        val next = if (current == SortOrder.ALPHABETICAL) {
            SortOrder.RECENT
        } else {
            SortOrder.ALPHABETICAL
        }
        viewModelScope.launch {
            preferencesRepository.setSortOrder(next)
        }
    }

    override fun onQuickAction(action: QuickAction) {
        val intent = QuickActionsResolver.resolveIntent(app, action.type)
        if (intent == null) {
            reportStatusMessage(R.string.status_action_unavailable)
            return
        }
        _effects.tryEmit(LauncherEffect.LaunchActivity(intent, R.string.status_action_unavailable))
    }

    override fun onOpenSettings() {
        transientFlow.update {
            it.copy(contextMenuApp = null, manualOverlay = ActiveOverlay.SETTINGS)
        }
    }

    override fun onCloseOverlay() {
        val currentOverlay = uiState.value.activeOverlay
        if (currentOverlay == ActiveOverlay.SAFE_MODE) return
        transientFlow.update { it.copy(manualOverlay = null) }
        if (currentOverlay == ActiveOverlay.ONBOARDING &&
            latestPrefsFlow.value?.onboardingCompleted == false
        ) {
            viewModelScope.launch {
                preferencesRepository.setOnboardingCompleted(true)
            }
        }
    }

    override fun onSelectTheme(themeId: String) {
        viewModelScope.launch {
            preferencesRepository.setThemeId(themeId)
        }
    }

    override fun onSelectBackgroundStyle(style: BackgroundStyle) {
        viewModelScope.launch {
            preferencesRepository.setBackgroundStyleOverride(style)
        }
    }

    override fun onToggle24HourClock(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setUse24HourClock(enabled)
        }
    }

    override fun onTogglePerformanceMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setPerformanceModeOverride(enabled)
        }
    }

    override fun onToggleHighContrast(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setHighContrastMode(enabled)
        }
    }

    override fun onResetSettings() {
        viewModelScope.launch {
            preferencesRepository.resetSettings(keepOnboardingCompleted = true)
            reportStatusMessage(R.string.status_settings_reset)
        }
    }

    override fun onRequestSetDefaultHome() {
        val intent = HomeAliasManager.createSetHomeIntent(app)
        refreshCapabilitiesAndActions()
        if (intent == null) {
            reportStatusMessage(R.string.status_action_unavailable)
            return
        }
        _effects.tryEmit(LauncherEffect.RequestDefaultHome(intent))
    }

    override fun onReturnToStockLauncher() {
        val switched = HomeAliasManager.returnToStockLauncher(app)
        refreshCapabilitiesAndActions()
        if (!switched) {
            reportStatusMessage(R.string.status_action_unavailable)
        } else {
            transientFlow.update { it.copy(manualOverlay = null, contextMenuApp = null) }
        }
    }

    override fun onCompleteOnboarding() {
        transientFlow.update { it.copy(manualOverlay = null) }
        viewModelScope.launch {
            preferencesRepository.setOnboardingCompleted(true)
        }
    }

    override fun onOpenOnboarding() {
        transientFlow.update {
            it.copy(contextMenuApp = null, manualOverlay = ActiveOverlay.ONBOARDING)
        }
    }

    override fun onSafeModeReenableHome() {
        HomeAliasManager.setHomeAliasEnabled(app, true)
        SafeModeManager.clearSafeMode(app)
        refreshCapabilitiesAndActions()
        transientFlow.update {
            it.copy(
                safeModePending = false,
                safeModeCrashSummary = null,
                statusMessageRes = R.string.status_home_reenabled,
            )
        }
    }

    override fun onSafeModeContinueManual() {
        SafeModeManager.clearSafeMode(app)
        refreshCapabilitiesAndActions()
        transientFlow.update {
            it.copy(
                safeModePending = false,
                safeModeCrashSummary = null,
            )
        }
    }

    override fun onSafeModeResetAll() {
        SafeModeManager.clearSafeMode(app)
        refreshCapabilitiesAndActions()
        viewModelScope.launch {
            preferencesRepository.resetSettings(keepOnboardingCompleted = true)
            transientFlow.update {
                it.copy(
                    safeModePending = false,
                    safeModeCrashSummary = null,
                    manualOverlay = null,
                    contextMenuApp = null,
                    statusMessageRes = R.string.status_settings_reset,
                )
            }
        }
    }

    override fun onClearStatusMessage() {
        transientFlow.update { it.copy(statusMessageRes = null) }
    }

    private fun refreshCapabilitiesAndActions() {
        capabilitiesFlow.value = DeviceCapabilitiesProvider.inspect(app)
        quickActionsFlow.value = QuickActionsResolver.resolveAvailableActions(app)
    }

    private suspend fun refreshApps(recentLaunchTimes: Map<String, Long>) {
        val loaded = appCatalogRepository.loadApps(recentLaunchTimes)
        rawAppsFlow.value = loaded
        transientFlow.update { it.copy(appsLoaded = true) }
    }

    private fun buildUiState(
        catalog: CatalogSnapshot,
        prefs: UserPreferences,
        transient: TransientControlState,
    ): LauncherUiState {
        val mergedApps = catalog.apps.map { item ->
            val recentTs = prefs.recentLaunchTimes[item.packageName] ?: item.lastLaunchedAt
            if (recentTs != item.lastLaunchedAt) item.copy(lastLaunchedAt = recentTs) else item
        }

        val visibleApps = AppSorting.visibleApps(
            apps = mergedApps,
            hiddenPackages = prefs.hiddenPackages,
            sortOrder = prefs.sortOrder,
        )
        val favoriteApps = AppSorting.favoriteApps(
            visibleApps = visibleApps,
            favoritePackages = prefs.favoritePackages,
            sortOrder = prefs.sortOrder,
        )
        val hiddenApps = AppSorting.hiddenApps(
            apps = mergedApps,
            hiddenPackages = prefs.hiddenPackages,
        )

        val activeTheme = themeRepository.resolveTheme(catalog.themes, prefs.themeId)
        val perfOverride = prefs.performanceModeOverride
        val performanceMode = perfOverride ?: catalog.capabilities.isLowPerformanceByDefault
        val performanceModeIsAuto = perfOverride == null && catalog.capabilities.isLowPerformanceByDefault

        val activeOverlay = when {
            transient.safeModePending -> ActiveOverlay.SAFE_MODE
            transient.manualOverlay != null -> transient.manualOverlay
            transient.prefsLoaded && !prefs.onboardingCompleted -> ActiveOverlay.ONBOARDING
            else -> ActiveOverlay.NONE
        }

        return LauncherUiState(
            isLoading = !transient.appsLoaded || !transient.prefsLoaded,
            allApps = visibleApps,
            favoriteApps = favoriteApps,
            hiddenApps = hiddenApps,
            favoritePackages = prefs.favoritePackages,
            quickActions = catalog.quickActions,
            sortOrder = prefs.sortOrder,
            themes = catalog.themes,
            activeTheme = activeTheme,
            backgroundStyleOverride = prefs.backgroundStyleOverride,
            use24HourClock = prefs.use24HourClock,
            performanceMode = performanceMode,
            performanceModeIsAuto = performanceModeIsAuto,
            highContrastMode = prefs.highContrastMode,
            activeOverlay = activeOverlay,
            contextMenuApp = transient.contextMenuApp,
            safeModeCrashSummary = transient.safeModeCrashSummary,
            capabilities = catalog.capabilities,
            statusMessageRes = transient.statusMessageRes,
        )
    }
}

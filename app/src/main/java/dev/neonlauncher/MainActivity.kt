package dev.neonlauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dev.neonlauncher.ui.screens.NeonLauncherRoot
import dev.neonlauncher.viewmodel.ActiveOverlay
import dev.neonlauncher.viewmodel.LauncherEffect
import dev.neonlauncher.viewmodel.LauncherViewModel
import kotlinx.coroutines.launch

/**
 * Main Home and Leanback launcher Activity hosting [NeonLauncherRoot],
 * Home-role intent handling, package change listening, and Safe Mode crash hook.
 */
class MainActivity : ComponentActivity() {
    private val viewModel: LauncherViewModel by viewModels()
    private var receiverRegistered = false

    private val homeRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        viewModel.refreshAll()
    }

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            viewModel.refreshAll(clearIconCache = true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG && intent?.getBooleanExtra(EXTRA_DEBUG_SIMULATE_CRASH, false) == true) {
            throw RuntimeException("Simulated crash for Safe Mode test")
        }

        val neonApp = application as NeonLauncherApp

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val state = viewModel.uiState.value
                    when {
                        state.contextMenuApp != null -> viewModel.onDismissContextMenu()
                        state.activeOverlay == ActiveOverlay.SETTINGS -> viewModel.onCloseOverlay()
                        state.activeOverlay == ActiveOverlay.APP_PICKER -> viewModel.onCloseOverlay()
                        state.activeOverlay == ActiveOverlay.ONBOARDING -> viewModel.onCompleteOnboarding()
                        else -> Unit // Never exit the TV launcher when pressing Back on Home.
                    }
                }
            },
        )

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        is LauncherEffect.LaunchActivity -> {
                            runCatching { startActivity(effect.intent) }
                                .onFailure { viewModel.reportStatusMessage(effect.failureMessageRes) }
                        }
                        is LauncherEffect.RequestDefaultHome -> {
                            runCatching { homeRoleLauncher.launch(effect.intent) }
                                .onFailure { viewModel.reportStatusMessage(R.string.status_action_unavailable) }
                        }
                    }
                }
            }
        }

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            NeonLauncherRoot(
                uiState = uiState,
                iconLoader = neonApp.appCatalogRepository,
                actions = viewModel,
            )
        }
    }

    override fun onStart() {
        super.onStart()
        registerPackageListener()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshAll()
    }

    override fun onStop() {
        unregisterPackageListener()
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) {
            viewModel.onHomeIntentReceived()
        }
    }

    private fun registerPackageListener() {
        if (receiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(
            this,
            packageReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED,
        )
        receiverRegistered = true
    }

    private fun unregisterPackageListener() {
        if (!receiverRegistered) return
        runCatching { unregisterReceiver(packageReceiver) }
        receiverRegistered = false
    }

    companion object {
        const val EXTRA_DEBUG_SIMULATE_CRASH = "dev.neonlauncher.DEBUG_SIMULATE_CRASH"
    }
}

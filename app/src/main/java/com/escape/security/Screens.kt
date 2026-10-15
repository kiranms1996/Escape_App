package com.escape.security

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun EscapeTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val view = LocalView.current
    val activity = LocalActivity.current
    SideEffect {
        if (activity != null) {
            WindowCompat.getInsetsController(activity.window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    val scheme = if (dark) darkColorScheme(
        primary = Color(0xFF80CBC4), secondary = Color(0xFFB0CCC8),
        background = Color(0xFF101418), surface = Color(0xFF101418))
    else lightColorScheme(primary = Color(0xFF006A62), secondary = Color(0xFF466864))
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
fun VerificationScreen(title: String, detail: String, cancel: () -> Unit) {
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding().padding(28.dp), verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text("ESCAPE SECURITY", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator()
            Spacer(Modifier.height(24.dp))
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            Text(detail, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(24.dp))
            OutlinedButton(onClick = cancel) { Text("Cancel and go home") }
        }
    }
}

private val limitationText = "Escape is a personal, best-effort app locker. Another app may display content before verification. It cannot guarantee interception before launch or zero content exposure. Force-stop, uninstall, safe mode, disabled permissions, missing events, multi-window, notifications, and OriginOS restrictions can bypass protection. Use the target app's own security and hide sensitive notifications."
private val privacyText = "Settings and selected package names stay in private local storage, with backup disabled. No usage history, face images, biometric templates, analytics, or network uploads. Android performs authentication. Escape has no camera or internet permission."
private val detectionText = "To detect selected apps opening, Escape's accessibility service receives window-change events and uses only their package name. It receives events across apps so it can notice when you leave a protected app. It does not read screen content, text, typing, or accessibility nodes, and does not perform gestures or automate interactions. Package names are used in memory and never saved as usage history. Enablement is optional and must be done by you in Android Settings."
private val credentialText = "Device-credential fallback accepts your phone's PIN, pattern, or password. Anyone who knows that credential can authenticate. Strong biometrics are preferred; Android chooses the sensor. Escape cannot force face authentication, and Vivo's screen-unlock face recognition may not be exposed to apps. Class 2-only biometrics are excluded by this prototype's strong-biometric policy."

@Composable
fun OnboardingScreen(settings: LocalSettings, finish: () -> Unit) {
    val context = LocalContext.current
    var page by rememberSaveable { mutableIntStateOf(0) }
    var fallback by remember { mutableStateOf(settings.credentialFallback) }
    var consent by remember { mutableStateOf(settings.detectionConsent) }
    var tick by remember { mutableIntStateOf(0) }
    RefreshOnResume { tick++ }
    val ready = remember(tick, fallback) { Authentication.available(context, fallback) }
    Surface(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.safeDrawingPadding().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(vertical = 24.dp)) {
            item { Text("ESCAPE SECURITY  /  ${page + 1} OF 3", style = MaterialTheme.typography.labelLarge) }
            item { Text(listOf("A little more privacy", "Your identity, verified by Android", "App detection, with your consent")[page], style = MaterialTheme.typography.headlineLarge) }
            item { Text(listOf(limitationText, credentialText, detectionText)[page], style = MaterialTheme.typography.bodyLarge) }
            if (page == 0) item { InfoCard("Local by design", privacyText) }
            if (page == 1) {
                item { InfoCard("Authenticator availability", Authentication.report(context)) }
                item { ToggleRow("Allow device-credential fallback", fallback, { fallback = it; settings.credentialFallback = it }) }
                item { OutlinedButton(onClick = { context.openSettings(Intent(Settings.ACTION_BIOMETRIC_ENROLL)
                    .putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, Authentication.mask(fallback))) }) { Text("Open Android enrollment") } }
                if (!ready) item { Text("Set up an allowed authenticator before finishing. You can return here after enrollment.", color = MaterialTheme.colorScheme.error) }
            }
            if (page == 2) {
                item { ToggleRow("I agree to package-only app detection", consent, {
                    consent = it; settings.detectionConsent = it
                }) }
                item { Text("Without consent and system enablement, detection stays off. You can enable it later from Status. No overlay, usage-access, camera, or battery-exemption permission is required.") }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (page > 0) OutlinedButton(onClick = { page-- }) { Text("Back") }
                    Button(enabled = page < 2 || ready, onClick = { if (page < 2) page++ else finish() }) {
                        Text(if (page < 2) "Continue" else "Finish setup")
                    }
                }
            }
        }
    }
}

@Composable
private fun RefreshOnResume(action: () -> Unit) {
    val owner = LocalLifecycleOwner.current
    val latest by rememberUpdatedState(action)
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) latest() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
}

@Composable
fun DashboardScreen(settings: LocalSettings) {
    val context = LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var search by rememberSaveable { mutableStateOf("") }
    var apps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf(false) }
    var protected by remember { mutableStateOf(settings.protectedPackages) }
    var fallback by remember { mutableStateOf(settings.credentialFallback) }
    var consent by remember { mutableStateOf(settings.detectionConsent) }
    var disclosure by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    RefreshOnResume { refresh++ }
    val connected by RuntimeState.connected.collectAsStateWithLifecycle()
    val issue by RuntimeState.launchIssue.collectAsStateWithLifecycle()
    val enabled = remember(refresh, connected) { serviceEnabled(context) }
    val authReady = remember(refresh, fallback) { Authentication.available(context, fallback) }
    val exclusions = remember(refresh) { excludedPackages(context) }
    val active = enabled && connected && consent && authReady && issue == null && protected.isNotEmpty()
    LaunchedEffect(refresh) {
        runCatching { withContext(Dispatchers.IO) { launchableApps(context) } }
            .onSuccess { apps = it; loaded = true; loadError = false }
            .onFailure { loaded = true; loadError = true }
    }
    if (disclosure) AlertDialog(onDismissRequest = { disclosure = false },
        title = { Text("Allow package-only detection?") }, text = { Text(detectionText) },
        confirmButton = { TextButton(onClick = {
            consent = true; settings.detectionConsent = true; RuntimeState.reset()
            disclosure = false
            context.openSettings(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }) { Text("Agree and open Settings") } },
        dismissButton = { TextButton(onClick = { disclosure = false }) { Text("Not now") } })

    Scaffold(bottomBar = {
        NavigationBar {
            listOf("Apps", "Status", "Help").forEachIndexed { index, label ->
                NavigationBarItem(selected = tab == index, onClick = { tab = index },
                    icon = { Text(listOf("▦", "◉", "?")[index]) }, label = { Text(label) })
            }
        }
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)
            .padding(horizontal = 20.dp), contentPadding = PaddingValues(vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Text("Escape Security", style = MaterialTheme.typography.headlineLarge) }
            item { InfoCard(if (active) "Best-effort detection ready" else "Protection needs attention",
                "${protected.size} app(s) selected · ${if (connected) "service connected" else "service disconnected"}. " +
                    if (active) "Interception is not guaranteed. Test each app on your Vivo." else "Review Status before relying on app detection.") }
            when (tab) {
                0 -> {
                    item { Text("Protected apps", style = MaterialTheme.typography.titleLarge) }
                    item { OutlinedTextField(value = search, onValueChange = { search = it },
                        label = { Text("Search apps or package names") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                    item { Text("Choose launchable apps in this Android profile. Escape settings always require authentication. Home and Android Settings are excluded for recovery.") }
                    val filtered = apps.filter { it.packageName !in exclusions &&
                        (it.label.contains(search, true) || it.packageName.contains(search, true)) }
                    if (!loaded) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                    if (loadError) item { Text("Could not load installed apps. Leave and reopen Escape to retry.") }
                    if (loaded && !loadError && filtered.isEmpty()) item { Text("No matching launchable apps.") }
                    items(filtered, key = { it.packageName }) { app ->
                        ToggleRow(app.label, app.packageName in protected, { selected ->
                            protected = if (selected) protected + app.packageName else protected - app.packageName
                            settings.protectedPackages = protected
                            RuntimeState.reset()
                        }, app.packageName)
                        HorizontalDivider()
                    }
                    val missing = protected - apps.map { it.packageName }.toSet()
                    if (missing.isNotEmpty() && loaded) item {
                        InfoCard("Unavailable selections", "${missing.size} selected app(s) are no longer launchable or visible in this profile.")
                        TextButton(onClick = { protected = protected - missing; settings.protectedPackages = protected; RuntimeState.reset() }) {
                            Text("Remove unavailable selections")
                        }
                    }
                }
                1 -> {
                    item { Text("Permission & status", style = MaterialTheme.typography.titleLarge) }
                    item { InfoCard("App detection", "Consent: ${if (consent) "granted" else "not granted"}\nAndroid service toggle: ${if (enabled) "enabled" else "disabled"}\nService connection: ${if (connected) "connected" else "not connected"}") }
                    if (issue != null) item { InfoCard("Detection degraded", issue!!) }
                    item { Button(onClick = { disclosure = true }) { Text("Review and enable detection") } }
                    if (consent) item { OutlinedButton(onClick = {
                        consent = false; settings.detectionConsent = false; RuntimeState.reset()
                    }) { Text("Withdraw detection consent") } }
                    item { Text("Withdrawing consent stops processing immediately. To unbind the service, turn Escape off in Android Accessibility settings.") }
                    item { InfoCard("System authentication", Authentication.report(context)) }
                    item { ToggleRow("Allow device-credential fallback", fallback, { next ->
                        if (Authentication.available(context, next)) {
                            fallback = next; settings.credentialFallback = next; RuntimeState.reset()
                        } else android.widget.Toast.makeText(context, "No ready authenticator for that choice. Enroll a strong biometric first.", android.widget.Toast.LENGTH_LONG).show()
                    }) }
                    item { Text(credentialText) }
                    item { OutlinedButton(onClick = { context.openSettings(Intent(Settings.ACTION_BIOMETRIC_ENROLL)
                        .putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, Authentication.mask(fallback))) }) { Text("Android authentication settings") } }
                    item { InfoCard("Background operation", "Android battery exemption: ${if (context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)) "enabled" else "not enabled"}. This does not measure Vivo restrictions. Escape requests no exemption automatically.\nAndroid ${Build.VERSION.RELEASE} · API ${Build.VERSION.SDK_INT}\n${Build.MANUFACTURER} ${Build.MODEL}\nFirmware: ${Build.DISPLAY}") }
                    item { OutlinedButton(onClick = { context.openSettings(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) }) { Text("Open Escape app info") } }
                    item { OutlinedButton(onClick = { context.openSettings(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }) { Text("Open Android battery settings") } }
                    item { Text("Usage access, draw-over-apps, camera, internet, notification access, and device administrator are not requested.") }
                }
                2 -> {
                    item { InfoCard("Security boundaries", limitationText) }
                    item { InfoCard("Your data", privacyText) }
                    item { InfoCard("OriginOS troubleshooting", "If prompts stop appearing, first confirm consent, the accessibility toggle, and service connection in Status. Open Escape app info and review the background/battery options your firmware actually shows. Search phone Settings for background power and startup controls. Exact X300 Pro OriginOS menu paths and startup behavior were not verified for your firmware; names differ by build and region. Consult vivo Support using the firmware shown in Status. Do not assume a battery exemption guarantees detection.") }
                    item { OutlinedButton(onClick = { context.openSettings(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.vivo.com/in/support"))) }) { Text("Open vivo Support") } }
                    item { InfoCard("When access is denied", "A failed biometric attempt, cancellation, timeout, or invalid session returns home. Reopen the protected app to try again. Escape does not repeatedly reopen the prompt after a failure. If the Android lockout persists, wait or use device credentials when allowed. If Escape has no allowed authenticator, restore enrollment in phone Settings. Clearing app data or uninstalling resets Escape and also removes protection.") }
                    item { InfoCard("Sideloading & recovery", "Android may restrict accessibility enablement for a sideloaded APK. Read the system's warning and proceed only if you trust this build. Escape cannot enable its own service. The debug build is for personal testing. Google Play distribution needs a separate accessibility policy/declaration review, disclosure/consent, privacy policy, and Data safety assessment; approval is not implied.") }
                    item { InfoCard("Test before relying on it", "Try Home/Recents, app-to-app switches, notification deep links, screen off/on, cancelled credentials, permission loss, reboot, split-screen, and extended idle. Target-app notifications, widgets, previews, multi-window, and work/cloned profiles are outside the interception guarantee. See docs/DEVICE_TESTING.md in the project.") }
                }
            }
        }
    }
}

private fun serviceEnabled(context: android.content.Context): Boolean {
    val target = ComponentName(context, ProtectionService::class.java)
    val manager = context.getSystemService(AccessibilityManager::class.java)
    return manager.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        .any { ComponentName(it.resolveInfo.serviceInfo.packageName, it.resolveInfo.serviceInfo.name) == target }
}

@Composable
private fun InfoCard(title: String, text: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit, detail: String? = null) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp)
        .toggleable(value = checked, role = Role.Switch, onValueChange = onChange).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            if (detail != null) Text(detail, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

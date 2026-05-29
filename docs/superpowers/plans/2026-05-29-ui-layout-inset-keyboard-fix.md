# UI Layout Inset & Keyboard Handling Fix — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix bottom action bar content overlap and keyboard-covering UI in this Jetpack Compose Android app.

**Architecture:** Enable edge-to-edge window mode, upgrade Compose to 1.6.8 for improved inset APIs, add `imePadding()` to the content Column so it contracts when the soft keyboard opens, and remove manual navigation bar coloring that conflicts with edge-to-edge.

**Tech Stack:** Jetpack Compose 1.6.8, Material3 1.2.1, Android edge-to-edge, Kotlin 1.5.13 compose compiler extension.

---

## File Structure

| File | Action | Responsibility |
|------|--------|----------------|
| `app/build.gradle` | Modify | Bump 11 Compose dependency versions from 1.5.4→1.6.8 and material3 1.1.2→1.2.1 |
| `app/src/main/java/com/andrea/pythontoarduino/MainActivity.kt` | Modify | Add `enableEdgeToEdge()` before `super.onCreate()` |
| `app/src/main/java/com/andrea/pythontoarduino/ui/PythonToArduinoUI.kt` | Modify | Add `imePadding()` modifier to content Column, add import |
| `app/src/main/java/com/andrea/pythontoarduino/ui/theme/Theme.kt` | Modify | Remove `SideEffect` block with manual `setNavigationBarColor` |

Each task is independent and can be committed separately. Task 1 (deps) should be done first since Tasks 2-3 depend on the upgraded APIs.

---

### Task 1: Upgrade Compose Dependencies

**Files:**
- Modify: `app/build.gradle:69-90`

- [ ] **Step 1: Bump all Compose dependency versions**

In `app/build.gradle`, update the `dependencies` block. The changes are all version string replacements in the dependency declarations:

```gradle
dependencies {
    implementation 'androidx.activity:activity-compose:1.8.2'
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3"
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3"
    implementation 'androidx.compose.ui:ui:1.6.8'                                    // was 1.5.4
    implementation 'androidx.compose.ui:ui-graphics'
    implementation 'androidx.compose.material3:material3:1.2.1'                      // was 1.1.2
    implementation 'androidx.core:core-ktx:1.13.1'
    implementation 'androidx.activity:activity-ktx:1.7.2'
    implementation "androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1"
    implementation "androidx.lifecycle:lifecycle-livedata-ktx:2.6.1"
    implementation "androidx.lifecycle:lifecycle-runtime-ktx:2.6.1"
    implementation "androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1"
    implementation "androidx.compose.runtime:runtime-livedata:1.6.8"                // was 1.5.4
    implementation "androidx.compose.ui:ui-tooling-preview:1.6.8"                   // was 1.5.4
    implementation "androidx.compose.material:material-icons-extended:1.6.8"        // was 1.5.4
    implementation "androidx.compose.material:material-icons-core:1.6.8"            // was 1.5.4
    implementation "androidx.compose.foundation:foundation:1.6.8"                   // was 1.5.4
    implementation 'androidx.compose.animation:animation:1.6.8'                     // was 1.5.4
    implementation 'androidx.compose.animation:animation-core:1.6.8'                // was 1.5.4
    // ... rest unchanged (com.chaquo.python, nanohttpd, accompanist, splashscreen, debug/test deps)
}
```

Only the version numbers change — no new dependencies added, none removed. The full `dependencies` block should remain the same except for these 9 version bumps.

- [ ] **Step 2: Verify Gradle sync compiles**

Run:
```bash
./gradlew :app:dependencies --configuration debugCompileClasspath 2>&1 | head -5
```

Expected: No resolution failures. Output should show Compose 1.6.8 versions in the tree.

- [ ] **Step 3: Commit**

```bash
git add app/build.gradle
git commit -m "build: upgrade Compose deps to 1.6.8, material3 to 1.2.1"
```

---

### Task 2: Enable Edge-to-Edge in MainActivity

**Files:**
- Modify: `app/src/main/java/com/andrea/pythontoarduino/MainActivity.kt:1-43`

- [ ] **Step 1: Add `enableEdgeToEdge()` import and call**

In `MainActivity.kt`, add the import near the other `androidx.activity` imports:

```kotlin
import androidx.activity.enableEdgeToEdge
```

Then, in `onCreate`, call `enableEdgeToEdge()` before `super.onCreate(savedInstanceState)`:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()                              // NEW: enables edge-to-edge for proper inset handling
    val splashScreen = installSplashScreen()
    super.onCreate(savedInstanceState)
    // ... rest unchanged
```

The full `onCreate` method signature and body remain the same except for this one added line and import.

- [ ] **Step 2: Verify the import resolves**

Run:
```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -20
```

Expected: `enableEdgeToEdge` resolves from `activity-compose:1.8.2` which includes it via `ComponentActivity.enableEdgeToEdge`. Should compile without errors.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/andrea/pythontoarduino/MainActivity.kt
git commit -m "feat: enable edge-to-edge for proper window insets"
```

---

### Task 3: Add IME Padding to Content Column

**Files:**
- Modify: `app/src/main/java/com/andrea/pythontoarduino/ui/PythonToArduinoUI.kt:420-432`

- [ ] **Step 1: Add `imePadding` import**

In the existing `androidx.compose.foundation.layout` imports block (around line 19-27), add:

```kotlin
import androidx.compose.foundation.layout.imePadding
```

- [ ] **Step 2: Add `imePadding()` to the content Column modifier chain**

In `PythonToArduinoUI.kt`, locate the `Scaffold` content `Column` (currently at lines 421-431). Change the modifier chain from:

```kotlin
Column(
    modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .background(
            Brush.linearGradient(
                colors = listOf(Color(0xFF121921), Color(0xFF002B44)),
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
        )
) {
```

To:

```kotlin
Column(
    modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .imePadding()
        .background(
            Brush.linearGradient(
                colors = listOf(Color(0xFF121921), Color(0xFF002B44)),
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
        )
) {
```

The modifier order matters: `padding(paddingValues)` handles the bottom bar offset, then `imePadding()` adds space when the keyboard opens, then `background` fills the resulting area.

- [ ] **Step 3: Verify compilation**

Run:
```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -20
```

Expected: No errors. The `imePadding()` modifier is available in `compose.foundation:1.6.8`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/andrea/pythontoarduino/ui/PythonToArduinoUI.kt
git commit -m "fix: add imePadding to content Column for keyboard handling"
```

---

### Task 4: Remove Manual Navigation Bar Coloring

**Files:**
- Modify: `app/src/main/java/com/andrea/pythontoarduino/ui/theme/Theme.kt:120-141`

- [ ] **Step 1: Remove the `SideEffect` block that sets navigation bar color**

In `Theme.kt`, the `PythonToArduinoTheme` composable currently has this block (lines 129-134):

```kotlin
SideEffect {
    systemUiController.setNavigationBarColor(
        color = Color.Black,
        darkIcons = false
    )
}
```

Remove the entire `SideEffect` block. The function should become:

```kotlin
@Composable
fun PythonToArduinoTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colors,
        typography = Typography(),
        content = content
    )
}
```

The `systemUiController` variable and its import can remain — they're harmless but unused. No need to clean up unless the compiler warns (it won't, since `SideEffect` was the only reference).

- [ ] **Step 2: Verify compilation**

Run:
```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -20
```

Expected: No errors. The unused `rememberSystemUiController` variable will produce no warning at this Compose/Kotlin version.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/andrea/pythontoarduino/ui/theme/Theme.kt
git commit -m "refactor: remove manual nav bar coloring for edge-to-edge compatibility"
```

---

### Task 5: Build Verification

**Files:**
- No file changes

- [ ] **Step 1: Full debug build**

Run:
```bash
./gradlew :app:assembleDebug 2>&1 | tail -30
```

Expected: `BUILD SUCCESSFUL` with an APK generated at `app/build/outputs/apk/debug/`. No compilation errors, no resource conflicts.

- [ ] **Step 2: Verify no lint warnings from upgraded deps**

Run:
```bash
./gradlew :app:lintDebug 2>&1 | grep -i "compose\|inset\|ime" | head -20
```

Expected: No warnings about Compose version mismatches or inset handling.

- [ ] **Step 3: Final commit if any build fixes needed**

If the build fails or produces warnings that need addressing, fix them and commit. If everything is clean, no commit needed.

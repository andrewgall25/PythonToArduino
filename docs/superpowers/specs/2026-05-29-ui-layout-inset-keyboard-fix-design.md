# UI Layout Inset & Keyboard Handling Fix

## Problem

1. The bottom `BottomNavBar` in the `Scaffold` overlaps content — the main content area fills full screen size instead of stopping above the bottom bar.
2. When the Android soft keyboard appears, it covers part of the UI. The layout does not resize or scroll to keep content visible.

## Root Causes

- **Scaffold inset handling**: The `Scaffold` applies `padding(paddingValues)` to its content, but without edge-to-edge setup, the `paddingValues` do not include navigation bar or IME insets. The content fills behind system bars.
- **No IME padding**: The inner `Column` has no `Modifier.imePadding()`, so when the keyboard opens the content is not pushed up.
- **Manual system bar coloring**: `Theme.kt` uses `setNavigationBarColor(Color.Black)` from Accompanist, which conflicts with edge-to-edge window insets.
- **Old Compose version (1.5.4)**: Lacks the improved `consumedWindowInsets` parameter on `Scaffold` and stable IME handling improvements from Compose 1.6.

## Solution

### 1. Upgrade Compose dependencies (`app/build.gradle`)

Bump all Compose libraries from `1.5.4` → `1.6.8` and `material3` from `1.1.2` → `1.2.1`. This provides:

- `consumedWindowInsets` parameter on `Scaffold`
- Improved `imePadding()` behavior
- Better `WindowInsets` integration

Changed dependencies:
- `ui:1.5.4` → `1.6.8`
- `ui-graphics`, `ui-tooling-preview`, `ui-tooling`, `ui-test-manifest`: `1.5.4` → `1.6.8`
- `material-icons-extended`, `material-icons-core`: `1.5.4` → `1.6.8`
- `foundation:1.5.4` → `1.6.8`
- `animation`, `animation-core`: `1.5.4` → `1.6.8`
- `material3:1.1.2` → `1.2.1`
- `runtime-livedata:1.5.4` → `1.6.8`

### 2. Enable edge-to-edge (`MainActivity.kt`)

Add `enableEdgeToEdge()` call before `super.onCreate(savedInstanceState)`. Import:
- `androidx.activity.enableEdgeToEdge`

This makes the window draw behind system bars, allowing Compose's `Scaffold` to properly calculate `paddingValues` that include navigation bar and IME insets.

### 3. Fix Scaffold inset handling (`PythonToArduinoUI.kt`)

In the content `Column`:
- Add `Modifier.imePadding()` to the chain so the column contracts when the keyboard opens.
- Keep existing `Modifier.fillMaxSize().padding(paddingValues)` — the Scaffold's `paddingValues` will now correctly include the bottom bar height and navigation bar inset (thanks to edge-to-edge).

Import addition needed:
- `androidx.compose.foundation.layout.imePadding`

### 4. Remove manual nav bar coloring (`Theme.kt`)

Remove the `SideEffect` block that calls `systemUiController.setNavigationBarColor(Color.Black, darkIcons = false)`.

Edge-to-edge mode handles navigation bar coloring via the Material3 `surface` color scheme, and the `BottomNavBar` already sets its own background color. This removes the conflict between manual coloring and Compose insets.

The `rememberSystemUiController` import and the `accompanist-systemuicontroller` dependency can remain (they're harmless), but the SideEffect call should be removed.

## Files Modified

| File | Change |
|------|--------|
| `app/build.gradle` | Bump Compose deps to 1.6.8, material3 to 1.2.1 |
| `MainActivity.kt` | Add `enableEdgeToEdge()` before `super.onCreate()` |
| `ui/PythonToArduinoUI.kt` | Add `imePadding()` to content Column |
| `ui/theme/Theme.kt` | Remove `SideEffect` with `setNavigationBarColor` |

## Risk Assessment

- **Compose upgrade**: 1.5.4 → 1.6.8 is a minor version bump within the same Compose BOM era. Breaking changes are minimal. The `kotlinCompilerExtensionVersion 1.5.13` is compatible with Compose 1.6.x.
- **Edge-to-edge**: On API 24+ (this app's minSdk is 24), `enableEdgeToEdge()` is safe. The Scaffold's bottom bar provides the visual anchor at the bottom.
- **WebView impact**: The Monaco WebView editor is inside a `weight(1f)` container. When IME padding pushes the column up, the weight-based layout automatically shrinks the WebView area — no WebView-specific changes needed.

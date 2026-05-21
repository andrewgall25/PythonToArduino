# Console Section Redesign — Design Spec

## Context

The ConsoleSection currently uses a hacker-terminal aesthetic (bright green `#00FF00` on near-black `#0D0D0D`, system monospace font). This redesign replaces it with a modern Monaco Editor-inspired look matching the app's existing dark blue gradient theme, using the GitHub Dark Glow palette and JetBrainsMono font.

## Design Decisions

- **Palette**: GitHub Dark Glow — `#0d1117` background, `#161b22` input area, `#30363d` borders, `#238636` green accent for actions
- **Font**: JetBrainsMono (already in `assets/`; needs copying to `res/font/` for Compose `FontFamily` usage)
- **Timestamps**: `HH:mm` labels between message batches (not per-line), in subtle `#484f58` color
- **Header**: Green status dot + "Console" title + icon toolbar (copy + trash/clear icons)
- **Input**: Pill-shaped card with green send button, subtle green border glow on focus
- **Message colors**: Color-coded by type (commands in `#ff7b72`, arduino output in `#a5d6ff`, actions in `#d2a8ff`, success in `#6a9955`, general in `#e6edf3`)

## Color Palette

| Element | Color | Usage |
|---------|-------|-------|
| Background | `#0d1117` | Console section background |
| Card bg | `#161b22` | Input field card background |
| Border | `#30363d` | All borders (input, cards) |
| Border subtle | `#21262d` | Header separator line |
| Text primary | `#e6edf3` | General output text |
| Text secondary | `#8b949e` | Toolbar icons, secondary text |
| Text dimmed | `#484f58` | Timestamps, placeholders |
| Accent green | `#238636` | Send button, status dot, focus border |
| Syntax red | `#ff7b72` | Command input lines |
| Syntax blue | `#a5d6ff` | Arduino serial output |
| Syntax purple | `#d2a8ff` | Action messages (LED ON, etc.) |
| Syntax green | `#6a9955` | Success messages, comments |
| Syntax orange | `#ce9178` | Error messages |

## Architecture

### Data Model Change

Current `output` is a plain `String` accumulated in `MainViewModel`. The redesign needs timestamps appended when new messages arrive. Two options:

**Chosen: Wrap lines with timestamp inline** — When appending to `output`, prefix each new batch with a timestamp line like `[14:32]`. This keeps the model as a `String` (minimal ViewModel change) and the `LazyColumn` renders timestamp lines as distinct styled items.

The ViewModel will prepend `[HH:mm]` before each batch of new output. The timestamp line is detected in the composable by a regex matching `^\[\d{2}:\d{2}\]$` and rendered in the dimmed `#484f58` color with smaller font.

### New Callbacks

- `onClearOutput: () -> Unit` — Clears the console (trash icon) — `clearOutput()` already exists in MainViewModel at line 151
- `onCopyOutput: () -> Unit` — Copies console content to clipboard (copy icon) — needs new method in MainViewModel

### Layout Constraint

The Console tab content lives inside `Box(modifier = Modifier.weight(1f))` in PythonToArduinoUI.kt (line 474), with `ActionButtons` rendered below that Box (line 506). The ActionButtons must remain in their current position at the bottom and must NOT visually overlap with any console elements. The ConsoleSection must fill the entire weighted space (from below the tab row to above the action buttons) without overflow.

### File Changes

1. **`res/font/jetbrainsmono_regular.ttf`** — Copy from `assets/JetBrainsMonoNL-Regular.ttf` to `res/font/`
2. **`ConsoleSection.kt`** — Full rewrite of colors, font, layout, toolbar, timestamp rendering, clear/copy actions. Must use `Modifier.fillMaxSize()` to fill the weighted Box without overflowing into ActionButtons.
3. **`MainViewModel.kt`** — Add timestamp prefix logic when appending output; add `copyOutput()` method (clearOutput already exists at line 151)
4. **`PythonToArduinoUI.kt`** — Pass `onClearOutput` and `onCopyOutput` callbacks to `ConsoleSection`

## Component Layout

```
Column (background: #0d1117, padding 16dp)
├── Row (header, border-bottom #21262d)
│   ├── Green dot (10dp, #238636, glow shadow)
│   ├── "Console" text (JetBrainsMono, 15sp, bold, #e6edf3)
│   └── Row (icon toolbar)
│       ├── Copy icon (#8b949e → #e6edf3 on hover)
│       └── Trash icon (#8b949e → #f85149 on hover)
├── Card (weight 1f, background #0d1117, no border)
│   └── LazyColumn (auto-scroll, padding 8dp)
│       └── items: output.lines()
│           ├── If matches [HH:mm] → dimmed timestamp style
│           └── Else → color-coded message style
├── Spacer (8dp)
└── Card (input, pill shape, bg #161b22, border #30363d)
    └── Row
        ├── TextField (JetBrainsMono, #e6edf3 text, #484f58 placeholder)
        └── Send button (green circle #238636, white arrow icon)
```

## Verification

1. Build the app: `./gradlew assembleDebug`
2. Install on device/emulator and navigate to Console tab
3. Verify: JetBrainsMono renders correctly, timestamps appear between batches, clear button empties console, copy puts text on clipboard, colors match the GitHub Dark Glow palette, send button and input field work as before

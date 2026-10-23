# Technical notes

Detailed behaviour, native mechanisms and validation notes for Lockscreen Studio,
kept as the project's change history. The user-facing overview is the
[README](../README.md); research write-ups are [AOD-research.md](AOD-research.md) and
[Shade-dark-mode.md](Shade-dark-mode.md). Sections are ordered newest first; the
"owner" in older notes is the maintainer whose RMX3521 the features were tested on. The app was called
"Evan Lockscreen Studio" up to 0.7.0; exports then lived in `Download/EvanLockscreen`
as `Evan-*.theme`. From 0.8.0 they are `Download/LockscreenStudio/Lockscreen-*.theme`.

## Unreleased: pieces, extras, charging and depth

All of these are drawn into the theme by `StudioTheme`. The host checks cover the pure
parts: day-number maths and the charging condition are evaluated by a small expression
evaluator in `V2Check`.

**Checked against Realme's engine.** These points were read from the decompiled
`com.oplus.uiengine` APK (the phone's lock-screen engine):

- `battery_state` is 0 normal, 1 charging, 2 low (under 20 %, unplugged) and 3 full
  (plugged in at 100 %). The charging condition (1 or 3) matches.
- `PositionAnimation` reads `<Position x y time>` children and `AlphaAnimation` reads
  `<Alpha a time>`. Animations loop once the last keyframe's time has passed. The earlier
  `<Item>` keyframes were ignored, which is why Breathe and Float did nothing.
- `rotation`, `pivotX/Y`, `alpha`, `visibility`, `x`, `y`, `w` and `h` are expressions on both
  `Image` and `Group`. `srcid` loads `name_<int>.png`.
- The expression functions used are all present: `ifelse`, `eq`, `gt`, `lt`, `ge`, `min`,
  `max`, `int` and `%`. A `Var` takes `name`, `type`, `expression` and `const`. One whose
  expression starts with `ifelse` skips a change registration, so the yearly counter is
  written `0+ifelse(...)`.
- `month` is 0-based and `day_of_week` is 1 for Sunday. `second` ticks from a one-second
  handler that starts when `#second` is used; the root `useVariableUpdater` attribute is
  never read.
- `hour24` is reported as 12 during the midnight hour when the phone is set to 12-hour time.
  Studio now uses `hour`, which is always 0–23, for the digits, Words, Rings, warm colours
  and the day/evening/night schedule.
- `SensorBinder type="gravity"` maps to Android's gravity sensor, and `Variable index` picks
  the axis. `showSysWallpaper` is not read; the lock screen is transparent wherever nothing
  is drawn.

Still to see on a phone: how the photo-fill texture lines up, and the look of the shadow
and rim.

- **Stretch.** `sx`/`sy` (and `mins.sx`/`mins.sy`), 50–200 %, scale digit images and
  positions horizontally or vertically. The preview scales the font size by height and adds
  `scaleX` for the rest. Vertical, Arc, Analog and Rings keep their proportions.
- **Per-part styles.** `styleH`/`styleM` hold optional `font`, `weight`, `finish` and `color`.
  `styleD` holds `weight` and `color`. `Spec.part()` merges them over the shared style, so
  they work whether or not the clock is split.
- **Movable date.** `detachDate` gives every layout a separate date group placed with `meta`.
- **Day counters** (up to three) and **daily quote.** `studio_day` is today's Julian day
  number, built from `#year`, `#month` (0-based) and `#date`. A counter is a `Var` holding
  days to the target, or to its next yearly occurrence; it is capped at 999 and hidden when
  negative. The number picks one of three digit groups by its length. The quote is one
  pre-rendered line per entry, chosen with `#studio_day % n`.
- **Charging styles** use `gt(eq(#battery_state,1)+eq(#battery_state,3),0)` (charging or full).
  - **Glow** is a halo with an alpha pulse.
  - **Colour shift** swaps in digit images drawn in the charging colour.
  - **Battery fill** has five fill levels per digit, picked from `#battery_level/20`.
  - **Charge ring** has 21 arcs, picked from `#battery_level/5`.
  - **Energy bar** is an image whose width follows `#battery_level`.
- **Rings** layout. It uses 12 hour-arc images and 60 minute-arc images, with the time in
  the centre. **Seconds hand** (Analog) rotates with `#second*6`; the theme then uses the
  `DateTime.Second` updater.
- **Digit depth.** `depthFront` marks which of the four digit positions sit in front of the
  subject. The clock is emitted twice. The first pass, before `subject.png`, skips the front
  digits. A second `studio_front` group, after the subject, has the same group transforms
  and draws only the front digits. Arc, Analog, Words and Rings have no per-digit images,
  so depth is off for them.
- **Subject shadow and rim light.** Both are built once from the subject's alpha.
  - **Shadow:** downscaled, blurred, painted black and drawn under the subject. It is offset
    downward and toward the clock.
  - **Rim:** the subject in the clock colour minus itself shifted away from the clock. This
    leaves a lit edge on the side facing the clock. It is drawn above the subject.
- **Photo fill** (`finish: photo`). It needs a photo or gradient background. Each digit
  position gets its own images, filled through a `BitmapShader` of the background. The
  shader's matrix is the inverse of the digit's full transform: image box, piece rotation,
  inner group and placed group. As a result the texture lines up with the wallpaper behind
  it. Hour tens need only 0–2 and minute tens only 0–5. It cannot be combined with the
  hourly weight, which would multiply the image count.
- **Weight through the hour** (`weightFlow`). It renders six weights from the variable font.
  The image is chosen with `digit + 10*int(#minute/10)`.
- **Parallax** (`movement: parallax`). The clock moves with the gravity sensor like Tilt.
  The background, subject, shadow and rim move the other way by a smaller amount. They are
  drawn 4.5 % larger so no edge shows. The subject is cut from the photo, so it moves with
  the photo: moving it on its own would reveal its copy in the background.

## New in 0.8.0: preview, split pieces, layouts and finishes

- **Expanded preview** fills the screen at the phone's real shape. One finger drags by
  the finger's offset (the old code set the clock centre to the touch point). Two
  fingers pinch to resize and twist to rotate, and the corner handle does both with one
  finger. Rotation snaps to multiples of 15° within 3°, and a centre-line snap applies
  within 1.5 units. The toolbar has Split/Join, a Hours/Minutes/Date chooser, Reset and
  Undo; "More" shows the old preview pills.
- **Limits** now shared by editor and renderer: size 80–600, rotation ±180°,
  horizontal −80…80, vertical −10…80 (same centre-anchored percentage mapping as
  before), date size 20–120.
- **Split pieces** (`split`, `mins{x,y,size,rot}`, `meta{x,y,rot}`):
  `StudioTheme.splitTime` writes three top-level groups per schedule/warm slot. They are
  the hours (2 digits at `size`), the minutes (2 digits rendered at `mins.size`), and
  seconds/date/caption/info. Each is centred and rotated about itself and shares the
  movement animation and alpha. Grid, Tower, Edge, Arc and Analog are already
  single-digit shapes or a dial, so they can't split (`NO_SPLIT`).
- **Layouts** `bigmin`, `diagonal`, `wave`, `offset`, `wide` are new `geometry()`
  branches. The editor mirrors them in `freeGeom()` using absolutely positioned digits
  in em. `analog` draws a dial plus full-size hand images; the hands use
  `rotation="#hour12*30+#minute*0.5"` and `rotation="#minute*6"` about the dial centre.
  The arithmetic-expression rotation is still to be confirmed on Realme's engine.
- **Finishes**: `outlineglow` and `halftone` configure the Paint. `chrome`, `holo` and
  `splittone` add a canvas-sized shader. `retro`, `emboss`, `glitch` and `duo` draw
  extra passes. `stencil` draws a plate and clears the glyph (PorterDuff CLEAR), so
  the wallpaper shows through. Date and caption text never take a finish. The editor
  approximates each with CSS.
- **Sliders**: range inputs ignore direct touches. A vertical swipe scrolls, a tap does
  nothing, and only a horizontal drag of 8 px or more changes the value, by the finger's
  offset. Keyboard input still works.
- **Colours**: "From your photo" and "From your wallpaper" (`WallpaperColors` for the
  lock wallpaper, falling back to home; no permission) for both colour and colour 2.
  "Use current Material You colour" was removed.
- **Theme Store package**: the helper uses `com.oplus.themestore` or
  `com.heytap.themestore`, whichever is installed (`pm path`), for both theme and shade
  imports. The completion log tag is the same in both.
- The default caption is empty; it previously contained the maintainer's email.

## New in 0.7.0

- **Shizuku once.** After the first grant the studio reconnects automatically
  whenever Shizuku is running; there is no Connect tap on each launch.
- **Independent applies.** Lock screen (clock apply), shade colours (*Apply shade
  colours*) and blur (*Apply blur*) are separate. Blur only sets
  `debug.systemui.gaussblur_level` and restarts SystemUI, with no theme import; it
  also works in dark mode and resets after a reboot.
- **Curated shade looks:** Phone default, Frosted white, Clear glass, Teal frost,
  Ocean, Mint, Lavender, Rose, Sunset amber and Graphite, plus your saved looks.
- **AOD display** (Tools → Native AOD → Display): on/off and All day /
  Scheduled (From–Until) / Power saving. It writes the secure settings Realme's AOD app
  writes (`Setting_AodSwitchEnable`, `Setting_AodEnable`,
  `Setting_AodEnableImmediate`, `Setting_AodUserSetTime`,
  `Setting_AodUserEnergySavingSet`, `Setting_AodSetTime*`), which SystemUI reads
  directly. Power saving reuses the OEM value (1 or 2) the phone wrote itself.
  Originals are backed up to `/data/local/tmp/evan-lockscreen-aod/display.bin`
  before the first write and restored exactly. Turning AOD on requires a style
  already chosen once in Settings; Light OS is rejected.
  Checked against AOD 14.20.43; on-phone behaviour is still to be confirmed.
- AOD "show battery/date/notification" toggles are not offered: the AOD app bakes
  them into the style, so writing the settings alone would not refresh the display.


## Native notification shade (0.6.0)

Open the new **Shade** destination and connect Shizuku. The screen loads the
phone's current values. Change the controls, then tap **Apply shade** to send all
shade choices in one transaction. **Undo edits** returns to the last phone
readback; **Refresh from phone** never discards unapplied edits. The native service imports a theme, verifies the
installed resources, restores both captured lockscreen-engine settings and
restarts SystemUI once. The panel briefly closes during restart.

Controls:
- **Panel**: colour and colour strength (0–100%, a blending control rather than proven panel opacity).
- **Cards**: colour, card opacity (0% removes the fill, ~25% gives frosted cards) and colour strength.
  *Cards over blur* sets the separate blurred-card colour.
- **Blur**: one choice of keep current, device default (-1), off/see-through (0) or styles 1–3.
  Styles 1–3 all showed blur on this phone; they are not a calibrated strength scale.
- Built-in looks and up to 12 saved looks, a light/dark preview and a copyable phone report.
- Restore of the SystemUI module and blur value captured before the first Studio shade apply.

### Dark mode

Panel and card colours only show in **light mode**. This is a Realme/Oplus framework
rule, not an app bug: `OplusThemeResources.checkUpdate()` calls
`OplusBaseFile.rejectTheme()`, which drops every theme package except `icons`,
the lockscreen and the launcher while night mode is active. SystemUI is not on that
list, so its theme `colors.xml` is ignored at night. Applying in dark mode is allowed;
the colours appear when the phone switches to light mode, and the editor says so.
Blur is a system property rather than a theme resource. See [Shade-dark-mode.md](Shade-dark-mode.md).

The first backup captures your CURRENT appearance, which can include the teal
terminal test; Restore is not a factory-theme reset. Turning custom panel/card
appearance off removes the four managed colour overrides while retaining other
SystemUI assets/colours. Restore uses the initial SystemUI module exactly and
retains the current clock and other native theme modules.

Ordinary clock and icon applies now carry forward current native modules rather
than dropping the shade. Clock activation repairs the engine class as well as the
package, following the later on-phone repair test. Engine reads target user 0;
invalid/error text is rejected before it can be saved or written back.

The resource path was verified in LIGHT MODE on RMX3521 Android 14. Reboot
survival remains unverified. No boot receiver or persistent
background reapply loop was added. The debug blur property is not advertised as
reboot-persistent. The existing AOD, lockscreen design, icon and scene features
remain available.

Local validation: all existing Java checks plus ShadeCheck (ARGB extremes and
intermediate values, merged native modules, both engine settings, later-clock
retention on restore, durable backup, invalid reads, native rejection/timeout),
9 responsive editor cases, and 5 shade interaction/viewport cases. Actual Android
installation, native service execution and rendering of this combined app flow
still need a phone test. Earlier terminal results do not substitute for that test.

Build with `python3 build.py`. `SignApk compare OLD.apk NEW.apk` verifies certificate
continuity after compiling SignApk with the apksig dependency. (Up to 0.7.0, builds were
signed with a development key committed to the repository; from 0.8.0 releases are signed
with a private key held in GitHub Secrets, see CONTRIBUTING.md.)


The supplied HTML is a visual reference, adapted into a responsive Android editing interface. Phones use one continuous workspace scroller with a sticky five-destination navigation bar. Short screens use a compact preview; wider screens keep the preview beside the editor. The preview can also be collapsed manually or expanded separately. There is no scroll-triggered resize and no tiny nested scrolling control panel.

Install over 0.5.2: package `np.evan.lockscreen`, version code 11, same development signing certificate. Photos, saved scenes and design preferences remain in the app.

## Native AOD music and smart updates

Open **Tools → Native AOD**, connect Shizuku and read settings. Choose **Vertical digital** for the layout that worked on the owner's phone, or choose **Keep my current AOD style** to leave the existing clock/picture selected. Tap **Enable native AOD music** for persistent mode. Subsequent song changes use the OS media listener: neither repeated theme application nor a running Studio/Shizuku session is required. Android may override settings when changing its own AOD mode or after firmware updates; Studio reports a mismatch rather than repeatedly forcing its settings back.

Four existing layouts can appear: vertical digital, horizontal digital, left aligned digital and analog dial. The Shizuku helper reads the installed AOD APK and lists only the known assets containing `AodMediaView` with its media-support flag. This is structural validation, not handset confirmation. Only vertical digital has been confirmed by the owner. Selecting **Keep my current style** does not establish whether that style can show music. The app carries none of the OEM's layout files.

**Test selected style · 60 seconds** remains available before persistent enable. Lock the phone, check title/artist and change tracks. **Keep enabled** cancels that test's restoration and persists the mode. Otherwise the helper restores after 60 seconds; sleep can delay the timer and stopped Shizuku prevents it until recovery. **Restore previous AOD** restores exact saved values, including absent settings, in the original Android user. Failed restores retain their backup for retry. Explicit permanent mode survives helper restarts; interrupted tests (including 0.5.1 backups) recover by restoring. Settings changed outside Studio are preserved. Reapplying a different built-in style retains the initial restoration baseline; intervening outside changes become the new baseline for those changed keys.

**Also enable native smart updates** enables Realme's existing smart-card switch when applying/enabling/testing. Unchecked retains the pre-Studio smart-update choice. This switch does not register new card providers. **Open Google Maps** launches the installed Maps app so the owner can start navigation and check native AOD compatibility. Live Google Maps directions, a map view, artwork and playback controls are not implemented or verified. The app does not request notification access, location or Internet permissions. **Open phone settings** opens Android settings; search for Always-On Display there to manage native schedules and options. Returning to Studio refreshes AOD state.

The working music route sets secure `Setting_AodClockModeFolder` (unless keeping the current style), system `aod_smart_display_music_info_enabled`, and secure `Setting_AodSceneMusicSwitchEnable`; optional smart updates use secure `Setting_AodSceneInfoSwitchEnable`. All originals are atomically backed up before settings change. Writes and restores have readbacks. AOD must already be enabled; Light OS is rejected. Other AOD power, schedule, brightness and burn-in settings remain OS-controlled.

The backup stays at `/data/local/tmp/evan-lockscreen-aod/backup.bin`. The app's restore action needs Shizuku, including before uninstalling if the owner wants the old settings back. Explicit enable changes system settings, so uninstalling Studio alone does not undo them. An unreadable backup blocks new enabling until recovery; it is not silently discarded.

No custom AOD import or deletion trigger is invoked. The working native song-info route is the scope of this release. Native findings and remaining artwork/control work are documented in [AOD-research.md](AOD-research.md).

## Responsive editor

- Android density is handled by the viewport's CSS-pixel mapping; Android font scale is passed explicitly to the page. Text autosizing is disabled to avoid double scaling. Core touch targets are at least 48 CSS pixels. Navigation text is kept compact at large font scales, with full-sized editor labels below it.
- Clock contains layouts, position, searchable font selection, typography fine tuning, date, caption and time format. The font picker has category filters and search; it does not load a 30-tile font grid into every editing panel.
- Wallpaper contains background selection, Atmos integration and subject depth. Depth controls guide you through selecting a photo, separating/importing a subject and then adjusting it. Inapplicable controls are hidden until a subject is available.
- Style contains colour/finish, information and motion subpages. Scenes contains presets, saved looks, scheduling and backups. Tools contains application/recovery, icon packs and app/engine details.
- Navigation restores each section's relative scroll position and open fine-tuning groups. Text entry keeps focus while preview updates; the phone shell moves out of the way while editing with a keyboard.
- Slider previews are coalesced with animation frames. Incremental DOM updates retain background, subject and clock-animation nodes instead of rebuilding all preview layers on each input event. Saving remains debounced.
- Bottom sheets scroll within the available height, isolate background controls and contain keyboard focus. The apply confirmation updates its countdown in place, preserving the Keep/Revert buttons.
- Landscape, split-window size changes, display density and font-scale configuration changes update without recreating the active applying workflow.

## Customisation features

| Area | Implementation |
| --- | --- |
| Layout | Single, Stacked, Stairs, Hero, Split, Grid, Tower, Edge, Arc and Words. Native digit/word image assets change with the engine's hour/minute variables. |
| Typography | All 30 named font families are bundled for offline preview and native rendering, with their licenses. Weight, slant, spacing, line height, minutes scale, alignment, rotation, curve, size and positioning affect exported clock artwork. |
| Colour | Two-tone minutes, custom hex/colour picker, palette suggestions from your photo and readability estimates. Current Material You accent can be sampled. |
| Finish | Solid, Glow, Outline, Neon, Gradient, Shadow and Glass, plus opacity and glow strength. These are rendered into the clock image assets. Glass is a translucent fill and outline, not a live wallpaper-blur shader. |
| Date | Six formats, chosen typeface, upper case, tracking, size and caption. The selected font is rendered into date pieces; weekday/month/day remain live native variables. |
| Time | 12/24-hour time, optional seconds, configurable warm-colour hours, night dimming and battery-dependent dimming. |
| Motion | Still, Breathe, Float and gravity-driven Tilt are emitted as native MAML-style groups/animations/sensor bindings. |
| Depth | ML Kit separation for one still photo, manual transparent PNG import, scale/offset/reset and the same crop as the background. A fixed subject is disabled in rotating/live Atmos mode. |
| Information | Live battery, live day-progress bar, Android's next alarm captured when applying, and manually entered weather text. There is no live weather-provider connection. |
| Preview tools | Full preview, dragging, hide/show, night preview, sample notifications, safe zones and hold-to-compare with a stock-style clock. Preview notification cards never become fake phone notifications. |
| Scenes | All 13 supplied starting styles, named scenes, shared photo references, delete/undo, share codes and native JSON export/import. Backups include settings, scene IDs and styles; photos are selected separately. |
| Scheduling | Day 06:00, evening 18:00 and night 23:00 clock styles are packaged together. The native engine changes visibility by time, including while the app is closed. Scheduled slots use the currently selected background/subject. |
| Setup | Real Shizuku connection, Atmos launch, wallpaper picker, Theme Store launch, actual app/engine details, native theme export and recovery. |
| Icons | Installed icon packs using appfilter mappings, six shapes, and independent launcher/icon theme application/export. |

Notification and safe-zone tools are available in Preview tools and the expanded preview. Newly exported native motion, gravity, day-progress, date pieces, scheduling, icon shapes and the new layout families still require physical testing on Realme's engine. This app is a normal native keyguard theme client; Android manages PIN/fingerprint authentication. Black background mode is a black lock-screen design and does not configure the phone's separate always-on display.

## Applying and recovery

1. Start Shizuku and connect in the studio.
2. Edit your design. The page saves native preferences automatically.
3. Tap **Apply to lock screen** and wait for Theme Store's completion. The app then runs the proven single package-setting command for `com.oplus.uiengine`, with no extra 5–30 second delay. **Activate clock now** is an immediate backup.
4. With **Confirm after applying** enabled, tap **Keep it** within 15 seconds. The rollback timer lives in the Shizuku process and can restore the last confirmed Studio theme even if the editor closes. If no previous Studio export is known or available, it selects the default lock screen. It does not promise to recover an unrelated external theme. Shizuku must remain running for recovery.
5. If Theme Store's completion log is unavailable, the app reports that uncertainty and does not assume application succeeded or arm a confirmation. Inspect the phone and activate the clock after staging completes.

The Shizuku helper remains idle between actions so an armed rollback survives closing the editor. If Shizuku stops, recovery and applying become unavailable until it is restarted.

## Atmos, partial updates and colours

Atmos transparency was confirmed by the owner on this Realme 9 / Realme UI 5 with Theme Store 10.3.5. Apply Atmos to home and lock first; choose **Atmos / live** in this editor. A clock export contains the lock bundle and an empty wallpaper transport ZIP, with no opaque background image in live mode. The icon export contains icon/launcher modules and no lock bundle. Icon-only application may still cause OEM selection resets; the immediate backup can reselect the existing clock engine.

Clock edits need a new clock theme apply. Time, date, battery, progress, day/night choices, scheduled styles, motion and hide/show use native variables or triggers and run without repeating a theme apply. Sampling a new Material You colour regenerates coloured assets on the next Apply. Merely selecting UI Engine again cannot recolour the bytes of an existing PNG. The studio does not reapply a full theme for each Atmos playlist rotation and does not forward Atmos's wallpaper double-tap gesture through its keyguard layer.

## ML Kit

Use **Wallpaper → Subject depth effect → Separate subject with ML Kit** with a still photo background. Google Play services may download the model on first use; retry when it becomes available. Input is limited to a 1024-pixel longest side, and unusable masks are rejected. Native-crash backoff and a retry/reset control are retained. Your manual PNG remains available as a fallback. The native clock app and Atmos do not share private files.

## Validation

- Java compilation, 24 legacy layout checks and 72 legacy feature combinations for migration.
- 2,430 combinations of V2 geometry across ten distinct layouts, plus confirmation/keep/revert state transitions.
- Exact module staging, independent icon modules, empty wallpaper transport, immediate activation, bounded setting retries and restoration checks.
- Browser checks of the adaptive editor and Android bridge: 30 local fonts, all layouts/finishes, undo/redo, scene persistence, scheduled scene payloads, backup identity, depth, information, confirmation, immediate activation and icon actions. Requests to external sites are blocked and the UI test verifies none occur.
- Nine additional viewport/density/font-scale combinations cover 320×568 through 1280×800, densities 1–3.5, text scaling through 200%, short landscape and keyboard-sized viewports. Checks cover visible navigation, minimum navigation touch sizes, horizontal overflow, scroll stability, retained preview nodes, frame coalescing, font search and focus preservation. These simulate WebView layout, not physical Android IME or GPU performance.
- AOD host checks: read-only inspection, backup before write, exact restoration including missing values, user isolation, manual changes, stale timer isolation, process restart recovery, partial apply, ignored writes, denied restore/retry, disabled AOD and Light OS rejection. UI bridge checks cover AOD scan/test/restore/copy, escaping, countdown completion and preservation of the applied keyguard state.
- Signed APK archive and matching update certificate checks. Host checks do not replace real-device rendering and recovery tests. Only the earlier base clock, package activation and Atmos transparency were physically demonstrated in the conversation.

## Source and build

The main interface is `assets/studio.html`; native V2 rendering is `StudioTheme.java`, with `StudioFonts.java` and the included font catalog. The original theme builder remains for earlier saved designs. Native application uses `ShellService.java`, and its independent recovery timer is in `ApplyRecovery.java`.

Run `EVAN_ANDROID_TOOLS=/path/to/android-tools python3 build.py` on Linux with Python 3 and Java 17+. Without that variable, build tools download into `.tools`. Google Play services/ML Kit dependencies are resolved by `mlkit-deps.py`. Bundled fonts are included in the source archive; `fetch-fonts.py` can restore them from Google's font repository. Roboto Slab uses the Apache license, and the other bundled families carry their included OFL licenses. Shizuku's MIT license is included. Local builds sign with a generated development key in `.signing/` (see CONTRIBUTING.md).

For UI checks: `STUDIO_CHROME=/path/to/chrome node tests/ui-check.cjs` and `STUDIO_CHROME=/path/to/chrome node tests/responsive-ui-check.cjs`. Output: `dist/Lockscreen-Studio-<version>.apk`. Native theme syntax reference: https://zhuti.designer.xiaomi.com/docs/grammar/ (Xiaomi's documentation does not guarantee support in Realme's fork).

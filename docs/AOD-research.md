# Native AOD findings, 18 October 2026

Inspected owner-supplied firmware applications: System UI 14.00.16 (1410016), Always-On Display 14.20.43 (1420043), and Diverse engine / `com.oplus.uiengine` 13.2.77 (13277). This is static analysis of those builds, not an assertion about all Realme firmware.

## Verified in code

- System UI loads `com.oplus.aodimpl.AodManager` from `com.oplus.uiengine` and supplies a plugin callback. The AOD settings APK contains the bundled layout assets used by that rendering context.
- `AodMediaDataListener.Companion.isAodMediaSupport` chooses a compiled package list based on region. The export list contains `com.spotify.music`; the domestic list contains NetEase, Kugou, Heytap Music and QQ Music. Unsupported packages return before the full AOD panel receives their media data. A separate setting enables that panel but does not change its package list. It also expects semantic playback actions.
- The full `AodMediaControlPanel` binds artwork from `MediaData.getArtwork()` and buttons from media actions. Its content is live; it does not need a theme import for every track.
- `AodMediaView` is a separate native title/artist view. It obtains metadata through `AodPluginCallImpl` / `NotificationMediaHelper` and registers an `IMediaObserver`. The helper reads from the ordinary notification/media pipeline and does not use the full-panel package filter in its metadata getter. Other firmware or upstream gates can still affect availability.
- The bundled `aod/aod_layout_0010000.xml` includes `TimeView`, `DateView`, `AodMediaView`, battery and notification views. The media view has `supportVisiableMedia="true"` in this firmware's XML syntax.
- The metadata view checks whether its host is the real AOD, whether its display switch is on, whether the AOD mode supports it, and whether media metadata is available. Its presence in a preview does not prove that it works in doze on the handset.
- `Setting_AodClockModeFolder` and `Setting_AodSceneMusicSwitchEnable` are secure settings. `aod_smart_display_music_info_enabled` is a system setting.
- `LayoutXmlParseImpl` handles `Var`, `VariableBinders` / `ContentProviderBinder`, and `ExternalCommands`. This establishes that the engine has dynamic bindings. An app-owned provider, image update path, control action dispatch and allowed loading of custom layouts remain unverified.
- `EGRender.renderLayout` opens `layout.xml` inside an existing layout directory, or falls back to a bundled asset path. Ordinary apps do not get access to System UI's private AOD directories just because this loader exists. `AodStoreService` is protected by the OEM settings permission.

## 0.5.1 test

The app temporarily chooses the known built-in vertical template, enables legacy song info and disables the separate restricted music panel. No firmware APK is patched or re-signed, and no arbitrary files are loaded into System UI. The test backs up and restores those three values and uses the original Android user.

The owner reported “yes it worked” after the 0.5.1 native song-info test. This confirms visible native song information on this handset. The short reply does not separately establish which player was used, a track-change test, artwork or playback controls. The UI continues to distinguish verified settings readbacks from what the owner can see on the physical display.

## Work still required for a custom media AOD

1. Native song-info route confirmed by the owner; retain as the working baseline.
2. Establish an accepted custom AOD layout import or installation route without root.
3. Validate provider refreshes and an artwork image source in that installed native layout.
4. Validate playback actions against a genuine active media session and the AOD touch/gesture dispatcher.
5. Only then expose artwork size, positioning, clock style and player controls as normal editor features.

The native Spotify panel's package list cannot be extended merely by choosing a different colour or clock theme. The current lab does not attempt to spoof an allowed package or replace System UI.

## Follow-up: native import transport

Static analysis of the same owner-supplied builds identifies a concrete file transfer chain:

- AOD `com.oplus.aod.store.c.y` maps paths relative to its files directory to FileProvider content URIs, grants System UI read access, and builds a JSON object.
- `c.a` writes that map into secure `Setting_AodFileUris` and increments `Setting_AodStyleEdited`.
- System UI `com.oplus.systemui.aod.utils.BaseAodProviderObserver` observes the edit trigger, opens each URI using `ContentResolver.openFileDescriptor`, copies it below its own files directory, and, for store style type 1, unpacks ZIPs into the corresponding directory.
- Completion resets the edit trigger and reports secure `flag_copy_file_result`. This is an import acknowledgement, not proof the native renderer accepted a layout.

The observer also calls `deleteCurrentFolder` before copying, and the store branch calls `deleteStoreFile`. The former can reach deletion of the AOD root content when a selected path does not exist. A naive settings-only rollback cannot restore deleted custom picture or animation files. Therefore the next build must not expose this trigger until its deletion behavior and a preservation/restore strategy are established. No import trigger was run on the owner's phone by this investigation.

An app-owned provider URI has not yet been validated as an accepted source. This transport discovery is not a successful custom-layout import, and no artwork or controls are implemented by it.

Evidence: `analysis/aod-current/store-import.txt`, `analysis/systemui-aod/native-import-observer.txt` in the development workspace.

## 0.5.2: native mode instead of custom import

The owner chose to keep the working native path. This build adds explicit permanent enable, Keep after a temporary trial, exact restore, and four allowlisted on-device native styles. Each selectable built-in style must exist in the installed AOD APK and contain the native media view. The current-style option only changes music switches, retaining the selected clock/picture. No AOD edit/import trigger is invoked.

System UI `plugins.seedling.common.observer.AODEnableObserver.getKey` returns secure `Setting_AodSceneInfoSwitchEnable`. Its `AODEntry` / `AodViewModel` receives OEM Seedling service data and forwards title, description, icon and service identifiers to the native plugin. An opt-in in Studio enables this existing switch, with backup/restore. It does not turn arbitrary navigation notifications into cards or register Google Maps with the OEM service. Maps navigation/card rendering remains unverified.

Static evidence: `analysis/systemui-aod/smart-updates.txt`. No map, mock directions or fabricated media session is used to imply successful navigation support.

# Notification shade colours in dark mode

Findings from the owner's RMX3521 (Realme UI 5, Android 14): SystemUI 14.00.16
(1410016), Theme Store 10.3.5 and `oplus-framework.jar`.

## Why Studio's shade colours do not show in dark mode

Studio writes four colours into `/data/theme/com.android.systemui`
(`assets/colors.xml`), and they work in light mode. In dark mode the framework skips
the whole SystemUI theme package:

- `android.content.res.OplusThemeResources.checkUpdate()` computes
  `reject = OplusBaseFile.rejectTheme(resources, packageName)` and, when it is true,
  loads no theme zip and reports no values or drawables.
- `OplusBaseFile.rejectTheme()` is `isNightMode(...) && !sNightWhites.contains(pkg)`.
- `sNightWhites` contains only `icons`, the lockscreen package and
  `com.android.launcher`.
- `OplusThemeZipFile` reads only `assets/colors.xml`, `framework-res/assets/colors.xml`,
  `oplus-framework-res/assets/colors.xml` and the `assets/values-{cn,en,tw}/colors.xml`
  files. There is no night-specific file name.

No theme file layout or `themeInfo.xml` flag changes this. Store themes behave the
same way: SystemUI uses its own `values-night` colours at night.

SystemUI's own night values for the four resources are
`notification_scrim_bg_mask_color` `#ff333333`,
`notification_material_background_color` `#ff404040`,
`notification_material_background_color_compass` `#ff404040` and
`notification_material_blur_bg` `#c0282828`.

## Routes that do not work without root

- Fabricated runtime overlays: Android 14's `OverlayManagerService` rejects
  fabricated-overlay registration from the shell UID ("Non-root shell cannot fabricate
  overlays"), and `cmd overlay fabricate` requires root. SystemUI declares no
  `<overlayable>`, so an app-owned overlay would also fail idmap policy checks unless
  it is preinstalled or platform-signed.

## Remaining candidate (needs an on-phone test)

`OplusAccessibleResources` / `OplusMaterialFile` is a separate colour layer that
does **not** use `rejectTheme`. It loads night values at night, and SystemUI opts
in through manifest meta-data `color_material_enable=true`. With the system's custom
accent colour selected (`mMaterialColor & 0x20000`), it reads
`data/oplus/uxres/uxcolor/ux_custom_color.xml` and, at night, also
`ux_custom_color_night.xml`. Both use `<resources><color name="…">` and are
resolved by resource name in each app. Resolving by name means the four SystemUI
names would apply only to SystemUI.

These files are written by `com.oplus.uxdesign`, which was not inspected. This route
is usable only if Shizuku's shell user can write that directory and the system does
not rewrite the files. Check with:

```
adb shell ls -lZ /data/oplus/uxres/uxcolor/
adb shell cat /data/oplus/uxres/uxcolor/ux_custom_color_night.xml
adb shell touch /data/oplus/uxres/uxcolor/evan-probe && adb shell rm /data/oplus/uxres/uxcolor/evan-probe && echo writable
```

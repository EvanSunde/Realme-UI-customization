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

## Material colour layer: tested, blocked for Shizuku

`OplusAccessibleResources` / `OplusMaterialFile` is a separate colour layer that
does **not** use `rejectTheme`. It loads night values at night, and SystemUI opts in
through manifest meta-data `color_material_enable=true`. Depending on the active
system colour mode (`mMaterialColor`), it reads one of these sets from
`data/oplus/uxres/uxcolor/`:

- custom colour: `ux_custom_color.xml` and `ux_custom_color_night.xml`
- wallpaper colours: `coui_theme_color_wallpaper.xml` and `coui_theme_color_wallpaper_night.xml`

The wallpaper files use `<resources><group><index/><child name id><color name>`. The
framework resolves each colour by resource name in each app. These files are written
by `com.oplus.uxdesign` (u0_a201).

On-phone result (2026-10-19): the directory and files are mode 777/766, but every
write from the adb/Shizuku shell is refused. Creating a file and appending zero bytes
to `ux_custom_color_night.xml` or `coui_theme_color_wallpaper_night.xml` both fail with
"Permission denied". Unix permissions allow it, so SELinux is denying shell access to
`uxicons_data_file`. **This route needs root.**

## Conclusion

Without root, Realme UI 5 cannot show custom notification shade colours in dark mode:
the theme layer is rejected at night, the material colour files cannot be written,
and the shell cannot fabricate overlays. With root, a fabricated overlay using a
`night` configuration, or an edit to the material night file, would be the routes to
try. Studio applies in light mode and explains the limit in dark mode.

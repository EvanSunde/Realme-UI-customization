# Contributing

Thanks for helping. Device reports matter as much as code here: the project depends on
OEM firmware behaviour that can only be confirmed on real phones.

## Reporting a device result or bug

Use the issue templates. Include your phone model, realme UI / ColorOS version, and the
**Copy report** output from the Shade or AOD screen (it lists the native settings Studio
read). Please say what you saw on the screen, not only what the app reported.

## Project layout

| Path | What it is |
| --- | --- |
| `assets/studio.html` | The whole editor UI (HTML/CSS/JS in a WebView) |
| `src/np/evan/lockscreen/MainActivity.java` | WebView host, Android bridge, Shizuku connection |
| `src/np/evan/lockscreen/ShellService.java` | Helper that runs inside Shizuku with shell privileges |
| `StudioTheme.java`, `ThemeBuilder.java`, `StudioFonts.java` | Lock screen clock rendering into native theme files |
| `ShadeLab.java`, `ShadeOptions.java`, `NativeThemeFiles.java` | Notification shade colours, blur, theme packaging |
| `AodLab.java`, `AodDisplay.java` | Always-On Display music mode and display/schedule settings |
| `tests/*.java` | Host checks run by `build.py` (no device needed) |
| `tests/*.cjs` | Headless-browser checks of the editor |
| `docs/` | Technical notes, firmware research, release notes |

## Building and testing

```sh
python3 build.py                 # tools download into .tools/; runs every Java check
npm ci && npx playwright install chromium
npm run test:ui
```

`build.py` signs with `.signing/dev.keystore`, which it generates on first use. That key
never leaves your machine and is git-ignored. To use your own Chrome for the UI checks,
set `STUDIO_CHROME=/path/to/chrome`.

CI runs both on every push and pull request.

## Conventions

- **Match the surrounding code.** The Java and JS are written in a compact style; keep
  new code consistent with the file you're in.
- **Never change a phone setting without a backup and a readback.** Every native write
  must back up the original first, verify the value afterwards and be restorable from
  the UI. Look at `AodDisplay` and `ShadeLab` for the pattern, and add a host check in
  `tests/`.
- **No root, no patching.** Don't modify, re-sign or replace OEM apps, and don't load
  files into SystemUI outside the Theme Store / settings routes the firmware already uses.
- **Be honest in the UI.** Only claim what was checked. If something is statically
  verified but not confirmed on a phone, say so in the UI and docs.
- When adding a helper transaction code in `ShellService`, bump the Shizuku service
  `version(...)` in `MainActivity` so installed helpers restart with the new code.

## Making a release (maintainers)

Releases are built and signed by `.github/workflows/release.yml`:

1. Bump `versionCode` and `versionName` in `AndroidManifest.xml`.
2. Add `docs/releases/v<versionName>.md` with the release notes.
3. Push. The workflow builds, runs all checks, signs, creates the `v<versionName>` tag
   and publishes the release with the APK and its signing-certificate fingerprint.

The release key exists only in repository secrets. Create it once on your own computer:

```sh
keytool -genkeypair -alias release -keyalg RSA -keysize 4096 -validity 10000 \
  -keystore release.keystore -storetype PKCS12 -dname "CN=Lockscreen Studio"
base64 -w0 release.keystore > release.keystore.b64
```

Then add these under **Settings → Secrets and variables → Actions → New repository
secret**:

| Secret | Value |
| --- | --- |
| `RELEASE_KEYSTORE_BASE64` | contents of `release.keystore.b64` |
| `RELEASE_KEYSTORE_PASSWORD` | the keystore password you chose |
| `RELEASE_KEY_ALIAS` | `release` |
| `RELEASE_KEY_PASSWORD` | optional; only if the key password differs |

Keep `release.keystore` and its password backed up somewhere safe and private. Losing it
means users must uninstall to update, and leaking it lets anyone publish fake updates.
Delete `release.keystore.b64` once the secret is saved.

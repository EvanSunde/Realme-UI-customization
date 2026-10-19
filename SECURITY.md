# Security policy

Lockscreen Studio runs a helper inside Shizuku with **shell (adb) privileges**. It
can change system settings and install themes, so security problems matter.

## Reporting a vulnerability

Please don't open a public issue. Use GitHub's
[private vulnerability reporting](https://github.com/EvanSunde/Realme-UI-customization/security/advisories/new)
and include the steps to reproduce, the affected version and your device or firmware.
You should get a reply within a week.

## Scope

In scope:
- Ways for another app to make the Shizuku helper run commands or write settings or files
  it shouldn't.
- Path traversal or file-handling problems in theme import/export.
- Anything that lets the app change a setting it can't restore.

Out of scope:
- The behaviour of Shizuku, Theme Store or SystemUI themselves.
- APKs that weren't downloaded from this repository's Releases page.

## Verifying a download

Every release lists its signing-certificate SHA-256 in the release notes. Builds up to
and including 0.7.0 were signed with a development key that was published in this
repository, so treat those as unverifiable. Install releases only from this repository.

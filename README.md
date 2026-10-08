# budge.it

A small budget tracker for Android. Set a daily or weekly budget, log what you spend, and see
what is left **today** at a glance, right from a home-screen widget.

Everything stays on your phone. There are no accounts, no internet access and no cloud backup.

<!-- Add screenshots here, e.g. ![App](docs/app.png) ![Widget](docs/widget.png) -->

## Features

- **Daily or weekly budget.** Weekly budgets are turned into a daily allowance that stays the same all day
  and is recalculated each morning.
- **Home-screen widget.** Shows what is left today, a color-changing progress bar, and a "+" button to add a
  transaction without opening the app.
- **Your own day start.** Choose when a new day begins (for example 05:00) with a slot-machine style time roller.
- **Light, dark or system theme.**
- **Tiny and private.** About 2 MB, and only the last two weeks of transactions are kept.

## Install

**Requirements:** an Android phone running **Android 8.0 or newer**, and about 2 MB of free space.

1. On your phone, open the [latest release](../../releases/latest) and download **budgeit.apk**.
2. Open the downloaded file. If Android asks, allow your browser (or Files app) to **install unknown apps**.
3. Tap **Install**, then open **budge.it**.
4. Set your budget on the first screen.

### If the install is blocked

Android is cautious about apps that do not come from the Play Store.

- **"App blocked to protect device" (Google Play Protect):** open the Play Store, tap your profile picture,
  then **Play Protect**, then the gear icon, and turn off **Scan apps with Play Protect** for the install. You
  can turn it back on afterwards.
- **Auto Blocker / Advanced Protection:** if your phone has one, switch it off in Settings under Security
  while you install.
- **"App not installed":** if you installed an earlier test build of budge.it, uninstall it first. Builds signed
  with a different key cannot be installed over each other.

### Add the widget

Long-press an empty spot on your home screen, choose **Widgets**, find **budge.it**, and drag it out.
Tap the widget to open the app, or the "+" to add a transaction.

### Battery saver

Battery saver can pause the app's refresh at the start of a new day, so the widget would show yesterday's
numbers. If battery saver is on, budge.it asks once to be exempt. Battery saver stays on for everything else.

## Build it yourself

You need Android Studio (or a JDK 17+ and the Android SDK).

```
./gradlew testDebugUnitTest      # fast unit tests
./gradlew assembleDebug          # debug build
```

The release build is signed with a private key that is not part of this repository. Without it, the release
build falls back to the debug key, which is fine for trying it out.

## Built with

Kotlin, Jetpack Compose, Room, DataStore and Glance (for the widget). The font is
[Kdam Thmor Pro](https://fonts.google.com/specimen/Kdam+Thmor+Pro) (SIL Open Font License, see
`Kdam_Thmor_Pro/OFL.txt`).

## License

MIT, see [LICENSE](LICENSE). The Kdam Thmor Pro font keeps its own license (SIL Open Font License).

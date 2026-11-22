# Minimal HDMI Launcher

A small, offline-first Android TV launcher for a television used primarily as
an HDMI display. It provides direct HDMI 1/2/3 and Composite selection,
Settings access, automatic restoration of the last selected input, and a
temporary input overlay over the live picture.

It intentionally does not provide an app drawer, recommendations, accounts,
wallpapers, analytics, network access, or Google Play integration.

## Tested hardware

This launcher was developed and tested on a Xiaomi P1 32-inch TV running:

```text
Android 10
Xiaomi/croods/croods
MediaTek TV input framework
```

The input IDs are therefore specific to this firmware:

```text
HDMI 1    com.mediatek.tvinput/.hdmi.HDMIInputService/HW4
HDMI 2    com.mediatek.tvinput/.hdmi.HDMIInputService/HW5
HDMI 3    com.mediatek.tvinput/.hdmi.HDMIInputService/HW6
Composite com.mediatek.tvinput/.composite.CompositeInputService/HW1
```

It may work on other Android TV devices using the standard `TvView` API, but
the input IDs must be changed in `MainActivity.java`.

## Features

- Native Android TV Home activity.
- Direct passthrough tuning through `TvView` and `TvContract`.
- CEC remains owned by the TV's native framework.
- Last selected input is stored locally and restored on startup.
- Input cards appear over the current video for four seconds.
- The active input has a persistent border.
- Opening the input menu focuses the active input.
- Back hides the overlay without stopping the selected input.
- Official Google Material Icons embedded as local vector resources.
- No third-party runtime dependencies.
- No permissions requiring network, account, or storage access.

## Build

Requirements:

- JDK 17
- Android SDK platform 34
- Android SDK build tools 34.0.0

Build a release APK:

```sh
./gradlew assembleRelease
```

The APK is written to:

```text
app/build/outputs/apk/release/app-release.apk
```

## Install for testing

Install without changing the current launcher:

```sh
adb connect TV_IP:5555
adb install -r app/build/outputs/apk/release/app-release.apk
adb shell am start -n com.piergabory.minimalhdmi/.MainActivity
```

## Make it the Home launcher

First verify that the launcher starts and all inputs work. Then:

```sh
adb shell cmd package set-home-activity --user 0 \
  com.piergabory.minimalhdmi/.MainActivity
```

On some Google TV/Xiaomi firmware, the stock launcher must also be disabled
for Android to resolve the new Home activity:

```sh
adb shell pm disable-user --user 0 com.google.android.tvlauncher
```

On Google TV builds, the setup-wizard fallback may also need to be disabled:

```sh
adb shell pm disable-user --user 0 \
  com.google.android.tungsten.setupwraith
```

Do not disable the TV's HDMI, CEC, display, audio, Bluetooth, Settings, or
vendor input services.

## Restore the stock launcher

Package names vary by firmware. For the Xiaomi P1 configuration tested here:

```sh
adb shell pm enable --user 0 com.google.android.tvlauncher
adb shell pm enable --user 0 com.google.android.tungsten.setupwraith
adb shell cmd package set-home-activity --user 0 \
  com.google.android.tvlauncher/.MainActivity
```

On newer Google TV builds the stock Home activity may instead be:

```text
com.google.android.apps.tv.launcherx/.home.HomeActivity
```

Always query the current Home activity before changing it:

```sh
adb shell cmd package resolve-activity --brief \
  -a android.intent.action.MAIN \
  -c android.intent.category.HOME
```

## Design boundaries

This launcher does not implement HDMI-CEC, ARC/eARC, Dolby processing, HDCP,
or power control. Those remain in the television's native vendor framework.
The launcher only asks Android's TV input framework to tune a passthrough
input.

## License

The launcher source is released under the MIT License. Google Material Icons
are Apache-2.0 licensed; see `NOTICE`.

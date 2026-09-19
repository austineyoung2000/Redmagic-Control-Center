# RedMagic Control Center

[![Android CI](https://github.com/austineyoung2000/Redmagic-Control-Center/actions/workflows/android.yml/badge.svg?branch=sixteen)](https://github.com/austineyoung2000/Redmagic-Control-Center/actions/workflows/android.yml)
![Version](https://img.shields.io/badge/version-2.0.0-red)
![Android](https://img.shields.io/badge/Android-9%2B-3DDC84?logo=android&logoColor=white)
![Device](https://img.shields.io/badge/device-RedMagic%2011%20Pro-red)
![Root](https://img.shields.io/badge/root-required-orange)
![Kotlin](https://img.shields.io/badge/Kotlin-Android-7F52FF?logo=kotlin&logoColor=white)

A root-powered hardware control center built specifically for the **RedMagic 11 Pro / NX809J**.

RedMagic Control Center combines cooling, liquid-pump, lighting, shoulder-trigger, Magic Key, Game Mode, charging, call-lighting, and profile controls in one Material-style Android application. It uses hardware paths and behavior validated on real RedMagic 11 Pro hardware instead of depending on the stock Game Space interface.

> [!WARNING]
> This application writes directly to kernel and vendor hardware interfaces through root. It is restricted to the RedMagic 11 Pro / NX809J. Do not install it on another device without porting and validating every hardware path.

## Current platform

| Item | Configuration |
|---|---|
| Application ID | `com.elitedarkkaiser.redmagic` |
| Version | `2.0.0` |
| Development branch | `sixteen` |
| Minimum Android | Android 9 / API 28 |
| Target and compile SDK | API 35 |
| Language | Kotlin |
| Java compatibility | Java 17 |
| UI | Material Components |
| Root | Required |
| Supported device | RedMagic 11 Pro / NX809J |

## Compatibility

The official target is the NX809J running stock RedMagic Android 16 firmware.

LineageOS-based and other custom ROMs can work when they retain the stock RedMagic vendor and kernel interfaces. Compatibility requires equivalent implementations of:

- `/sys/kernel/fan/*`
- `/proc/driver/micropump/*`
- `/sys/class/leds/aw22xxx_led/*`
- `/sys/class/leds/sar0/*`
- `/sys/class/leds/sar1/*`
- RedMagic/Nubia Magic Key system settings

The app verifies the NX809J model and scans the fan, pump, LED, trigger, and slider interfaces before exposing the main controls.

## Application layout

The application contains five main tabs:

1. Home
2. Cooling
3. Controls
4. Hardware
5. Lighting

## Home

### Live Dashboard

The dashboard displays:

- Device model and root status
- Current CPU temperature
- Fan state, level, and RPM
- Pump state, frequency, and speed
- Current foreground application
- ROM/build fingerprint
- CPU and installed RAM

Fan and pump telemetry is collected through a batched root read and cached to reduce shell activity. Dashboard polling pauses when the activity is no longer visible, and a manual refresh remains available.

### Diagnostics

The capability scanner reports whether the expected fan, pump, LED, trigger, and slider hardware interfaces are available. Missing interfaces are reported rather than silently treated as working.

## Cooling

### Fan control

- Fan power on/off
- Manual levels `0` through `5`
- Live RPM reading
- Fahrenheit or Celsius display
- Quiet, Balanced, and Turbo curve presets
- Automatic temperature-based fan control

Automatic fan levels are:

| Temperature | Level |
|---|---:|
| Below 95°F / 35°C | 0 |
| 95–103°F / 35–39°C | 1 |
| 104–112°F / 40–44°C | 2 |
| 113–121°F / 45–49°C | 3 |
| 122–130°F / 50–54°C | 4 |
| 131°F / 55°C and above | 5 |

A 5°F downward hysteresis prevents rapid changes near thresholds. The service does not rewrite the fan level when the desired state already matches the last applied state.

### Micropump control

| Profile | Frequency | Speed |
|---|---:|---:|
| Slow | 4 | 40 |
| Medium | 4 | 60 |
| Quick | 4 | 80 |
| OC / Experimental | 4 | 90 |

Automatic pump mode uses Slow below 95°F, Medium from 95°F through 104°F, and Quick at 105°F or higher. The OC profile is manual and intentionally marked experimental.

### Screen-off cooling policy

Normal fan and pump activity is blocked while the screen is off unless the device is hot. Cooling remains permitted around 100°F / 38°C or above. Shutdown commands and repeated state writes are deduplicated to reduce root work and battery use.

## Controls

The Controls tab provides root verification and RedMagic Magic Key configuration.

Stock Magic Key actions include:

- Camera
- Game Space
- Sound Mode
- Flashlight
- Voice Recorder
- Disabled

The Magic Key can alternatively launch a selected user or system application. Stock-action mode and app-launch mode are mutually exclusive. The app picker supports app-name and package-name searches.

## Hardware

### Shoulder triggers

The app can enable or disable the trigger hardware, configure automatic startup, and map the left and right triggers independently.

Available mappings include:

- None
- Volume Up
- Volume Down
- Play / Pause
- Next Track
- Previous Track

Intent Unlock provides configurable tap counts before trigger actions become active, helping prevent accidental input.

Manual **Disable Triggers** stops the service and hardware without erasing the Auto-start preference. Automatic startup remains paused until the user presses **Enable Triggers** or restarts the phone.

### Master profiles

Master profiles capture the wider application state, including:

- Fan, pump, lighting, and automatic-control state
- Game Mode profile and selected games
- Per-game profile assignments
- Charging Mode profiles
- Incoming and connected-call profiles
- Call fan-pause preference
- RGB Studio configuration
- Temperature-unit preference
- Magic Key mode and selected application
- Real-time preview preference
- Trigger preferences

Profiles can be named, applied, deleted, exported as a portable JSON backup, and imported on another installation.

### Automation rules

Saved Master Profiles can be assigned to power connected, power disconnected, battery low, battery recovered, and first-unlock-after-restart events. Android broadcasts trigger the rules only when those events occur; the automation engine performs no continuous polling. Rules are included in portable JSON backups and are cleared automatically if their assigned profile is deleted.

## Lighting

The Lighting tab controls:

- Cooling fan LED
- Rear logo LED
- Shoulder LED strips

Each zone can be enabled, disabled, and configured independently. Effects include Steady, Breathe, Flashing, and Rapid where supported. Colors include Red, Orange, Yellow, Green, Cyan, Blue, Purple, and Pink. Confirmed stock fan-light presets are also supported.

The **Real-time preview** switch is located inside LED Zones and controls whether changes are written immediately while editing a profile.

## RGB Studio

RGB Studio provides a persistent multi-zone color cycle with:

- Synchronized or independent LED zones
- Selectable ordered color sequence
- Steady, Breathe, Flash, and Rapid effects
- Per-zone speeds from 0.5 to 6 seconds
- Immediate, 1, 5, 15, or 30-minute screen-off timeouts
- Apply-to-all action
- Explicit service stop control

RGB Studio uses the shared persistent root broker instead of launching `su` for every animation frame.

## Game Mode

Game Mode applies a saved fan, pump, and LED profile when a selected application becomes active. It uses Usage Access and accessibility foreground-app events instead of continuous idle polling.

While a selected game is active, a slow two-minute verification poll checks state. Polling stops when the game closes or the screen turns off, and the normal hardware profile is restored.

## Charging Mode

Charging Mode applies dedicated fan, logo, and shoulder LED profiles while power is connected. Android power and battery broadcasts drive the service, so charging state is not continuously polled. The previous valid lighting owner is restored after charging ends.

## Call Lighting

Call Lighting supports separate profiles for ringing and connected calls. An optional fan-pause feature saves the previous fan state, stops automatic fan control, turns the fan off during the call, then restores the previous state when the call ends.

## LED priority

The ownership system prevents lower-priority modes from overwriting higher-priority lighting:

1. Charging Mode
2. Call Lighting
3. Game Mode
4. RGB Studio
5. Normal saved LEDs

When a mode ends, the next valid owner is restored. Normal and Game Mode LED writes are blocked while the screen is off.

## Temperature monitoring

Temperature is read from readable Linux thermal zones without opening a root shell. The monitor shares one cached value between the dashboard, Auto Fan, and Auto Pump and reacts immediately to Android thermal-status events.

| State | Sampling interval |
|---|---:|
| App visible and interactive | 3 seconds |
| Background and hot | 5 seconds |
| Background, cool, and interactive | 15 seconds |
| Screen off and cool | 30 seconds |

The worker thread stops when no subscribers remain. The Fahrenheit/Celsius preference is also respected by the shared foreground notification.

## Root execution architecture

Normal privileged commands use one serialized persistent root shell. This lowers process churn, preserves command ordering, and shares one broker between cooling, lighting, RGB Studio, and trigger actions.

If the persistent shell fails, it is recreated automatically. A one-shot `su -c` path remains as a compatibility fallback for root providers that reject interactive shells. Command exit codes are checked.

The two trigger input readers remain dedicated blocking processes because each must continuously consume its kernel input stream.

## Hardware write safeguards

- Fan levels are clamped to `0–5`.
- Fan PWM values are clamped to `0–255`.
- Pump profiles generate known fixed commands.
- Stock fan-light presets use an allowlist.
- Duplicate writes to the same hardware resource are skipped for two seconds.
- Fan and pump telemetry caches are invalidated after successful writes.
- Automatic services skip unchanged states.
- Root hardware writes are serialized.
- Screen-off cooling and LED policies prevent unnecessary hardware activity.

These controls reduce risk and redundant work but cannot eliminate every risk of root-level hardware access.

## Foreground services

Auto Fan, Auto Pump, fan-light persistence, and RGB Studio use Android's `specialUse` foreground-service type. This describes continuous user-enabled internal hardware control and avoids incorrectly consuming Android 15's six-hour `dataSync` allowance.

Android displays an ongoing notification while continuous hardware services are active.

## Boot behavior

After boot or user unlock, the app can restore enabled behavior for:

- Trigger auto-start
- Charging Mode
- Call Lighting
- RGB Studio

A temporary manual trigger disable is cleared by a full restart.

## Permissions

| Permission or access | Purpose |
|---|---|
| Root / superuser | Write fan, pump, LED, trigger, and Magic Key state |
| Foreground Service | Keep explicitly enabled hardware controls active |
| Foreground Service Special Use | Correctly classify continuous hardware-control services |
| Notifications | Display required foreground-service state |
| Boot Completed | Restore user-enabled services after restart |
| Usage Access | Detect selected foreground games |
| Accessibility Service | Receive foreground-app events and support triggers |
| Phone State | Apply ringing and connected-call lighting |
| Display-over-other-apps app-op | Support trigger setup where required |

## Privacy and integrity

The app is designed to operate locally.

- The manifest does **not** request Android's `INTERNET` permission.
- No advertising framework is included.
- No analytics SDK is included.
- No cloud account is required.
- No remote-control service is included.
- No device telemetry is uploaded.
- Profiles and settings remain in local Android application storage.

GitHub and reference links open in the user's chosen browser. The app itself does not download web content.

The public repository contains the application source, Gradle configuration, and GitHub Actions workflow. Because the app receives root access, users should install trusted builds and review changes before granting permanent superuser permission.

## Battery and performance design

- Shared persistent root process
- Batched root telemetry reads
- Thirty-second hardware telemetry cache
- Dashboard polling paused outside the foreground
- Adaptive non-root temperature monitoring
- Event-driven charging and phone-state handling
- Event-driven Master Profile automation rules
- Event-assisted Game Mode activation
- Two-minute Game Mode checks only while a selected game is active
- Fan, pump, and general write deduplication
- Background-priority worker threads
- Screen-off cooling and lighting policies
- Configurable RGB screen-off timeout
- Worker cleanup when services stop

## First launch

On first launch, the app validates the device, requests root, applies approved feature permissions, scans the hardware interfaces, and then opens the main interface.

The root-assisted setup can grant Usage Access, notification permission, phone-state permission, display-over-other-apps access, and accessibility-service activation. Review the root request before approving it.

## Installation

1. Open the repository's [Releases](https://github.com/austineyoung2000/Redmagic-Control-Center/releases) page.
2. Download the signed release APK.
3. Allow installation from the browser or file manager if Android requests it.
4. Install the APK.
5. Open the application and grant root.

Development artifacts are available from successful [Android CI](https://github.com/austineyoung2000/Redmagic-Control-Center/actions/workflows/android.yml) runs.

## Building locally

Requirements:

- Git
- JDK 17
- Android SDK Platform 35
- Android Build Tools 35.0.0

```bash
git clone https://github.com/austineyoung2000/Redmagic-Control-Center.git
cd Redmagic-Control-Center
git checkout sixteen
./gradlew assembleDebug
```

The debug APK is generated in `app/build/outputs/apk/debug/`.

Signed release builds use:

- `SIGNING_STORE_FILE`
- `SIGNING_STORE_PASSWORD`
- `SIGNING_KEY_ALIAS`
- `SIGNING_KEY_PASSWORD`

Never commit signing credentials or keystores.

## GitHub Actions and releases

Android CI runs for pushes and pull requests involving `main` or `sixteen`, manual workflow dispatches, and version tags beginning with `v`.

The workflow builds debug and signed release APKs, uploads both artifacts, and creates a GitHub Release when a version tag is pushed. Release signing material is supplied through encrypted repository secrets.

## Troubleshooting

### Root access is missing

- Confirm Magisk, KernelSU, APatch, or another compatible `su` provider is installed.
- Verify RedMagic Control is allowed in the root manager.
- Remove an existing denied entry and reopen the app if necessary.
- Use **Controls → Check Root**.

### Hardware is reported as missing

Confirm the ROM retains the stock RedMagic vendor and kernel interfaces. Use Home diagnostics to identify the unavailable subsystem.

### Game Mode does not activate

- Grant Usage Access.
- Enable the accessibility service.
- Select at least one game.
- Save a Game Mode profile.
- Confirm the screen is on and unlocked.

### Triggers do not auto-start

- Enable Auto-start triggers.
- Confirm the trigger nodes are detected.
- Confirm the accessibility service is enabled.
- If manually disabled, press Enable Triggers or restart.

### Lighting is replaced unexpectedly

Check whether Charging Mode, Call Lighting, Game Mode, or RGB Studio currently owns the LEDs.

### Fan or pump values show `?`

Verify root access and confirm that the expected vendor nodes exist on the current ROM, then refresh the dashboard.

## Known limitations

- The app is intentionally restricted to NX809J.
- Other RedMagic generations require validation and porting.
- Vendor paths can change between firmware releases.
- Custom ROM support depends on retained stock vendor and kernel interfaces.
- Root denial prevents hardware control.
- Game Mode requires Usage Access.
- Call Lighting requires phone-state access.
- Continuous services display an Android notification.
- The OC pump profile is experimental.
- Profiles are local and are not cloud-synchronized.
- Clearing application data removes saved profiles.
- Magic Key system settings can remain active until changed again or reset by the ROM.

## Reporting issues

Use the repository [issue tracker](https://github.com/austineyoung2000/Redmagic-Control-Center/issues) and include:

- Device model
- ROM and build fingerprint
- Android and app versions
- Root solution
- Steps to reproduce
- Relevant Logcat output
- Home diagnostics results
- Whether the problem occurs on stock firmware

Do not publish phone numbers, account credentials, signing material, or unrelated personal logs.

## Contributing

Changes should preserve NX809J device gating, background-thread execution, LED ownership, screen-off safety, write deduplication, local privacy, profile compatibility, and CI build compatibility.

Changes to sysfs, procfs, vendor settings, foreground services, or root execution should be tested on supported physical hardware.

## Project links

- [Repository](https://github.com/austineyoung2000/Redmagic-Control-Center)
- [Releases](https://github.com/austineyoung2000/Redmagic-Control-Center/releases)
- [Android CI](https://github.com/austineyoung2000/Redmagic-Control-Center/actions/workflows/android.yml)
- [Issue tracker](https://github.com/austineyoung2000/Redmagic-Control-Center/issues)

## Disclaimer

This project is independent and is not affiliated with, endorsed by, or maintained by RedMagic, Nubia, or ZTE.

Root access and direct hardware control can cause unexpected behavior, instability, increased heat, battery drain, or hardware stress when used incorrectly. You are responsible for reviewing and testing the software on your device.

Use the application at your own risk.

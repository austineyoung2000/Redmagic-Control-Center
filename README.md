# RedMagic Control Center

[![Android CI](https://github.com/austineyoung2000/Redmagic-Control-Center/actions/workflows/android.yml/badge.svg?branch=sixteen)](https://github.com/austineyoung2000/Redmagic-Control-Center/actions/workflows/android.yml)
![Android](https://img.shields.io/badge/Android-9%2B-3DDC84?logo=android&logoColor=white)
![Device](https://img.shields.io/badge/Device-RedMagic%2011%20Pro-red)
![Root](https://img.shields.io/badge/Root-Required-orange)
![Kotlin](https://img.shields.io/badge/Kotlin-Android-7F52FF?logo=kotlin&logoColor=white)

A root-powered hardware control center built specifically for the **RedMagic 11 Pro / NX809J**.

RedMagic Control Center exposes cooling, liquid-pump, lighting, shoulder-trigger, Magic Key, Game Mode, charging, call-lighting, and hardware-profile controls through one Material-style interface.

The project is built around hardware interfaces confirmed on real RedMagic 11 Pro hardware. It is intended to provide direct, predictable control without depending on the stock Game Space interface.

> [!WARNING]
> This application writes directly to kernel and vendor hardware interfaces using root. It is restricted to the RedMagic 11 Pro / NX809J and should not be installed on unsupported devices without first porting and validating every hardware path.

## Current platform

| Item | Current configuration |
|---|---|
| Application ID | `com.elitedarkkaiser.redmagic` |
| Current version | `1.0.1rc2` |
| Primary development branch | `sixteen` |
| Minimum Android version | Android 9 / API 28 |
| Target Android version | Android 15 / API 35 |
| Compile SDK | API 35 |
| Language | Kotlin |
| Java compatibility | Java 17 |
| Interface | Material Components / programmatic Android UI |
| Root requirement | Required |
| Official device target | RedMagic 11 Pro / NX809J |

## Device and ROM compatibility

### Officially targeted

- RedMagic 11 Pro
- Model identifier `NX809J`
- Stock RedMagic Android 16 firmware

### Compatible custom ROMs

The app can also work on LineageOS-based and other custom ROM builds when they retain the stock RedMagic vendor, kernel, thermal, LED, trigger, slider, and micropump interfaces.

A custom ROM alone does not guarantee compatibility. The following interfaces must still exist and behave like their stock equivalents:

- `/sys/kernel/fan/*`
- `/proc/driver/micropump/*`
- `/sys/class/leds/aw22xxx_led/*`
- `/sys/class/leds/sar0/*`
- `/sys/class/leds/sar1/*`
- RedMagic/Nubia Magic Key system settings

The app performs a capability scan and displays whether fan, pump, LED, trigger, and slider interfaces were detected. It also applies an NX809J model gate before opening the main controls.

## Application layout

The application is divided into five primary tabs:

1. Home
2. Cooling
3. Controls
4. Hardware
5. Lighting

## Home

The Home tab provides an overview of the device and the current application state.

### Live Dashboard

The dashboard displays:

- Device model
- Root availability
- Current CPU temperature
- Fan power state
- Current fan level
- Fan RPM
- Pump state
- Pump frequency
- Pump speed
- Current foreground application
- ROM/build fingerprint
- CPU description
- Installed RAM

The dashboard supports automatic foreground refreshes and a manual **Refresh Dashboard** action.

Hardware telemetry reads are batched so fan and pump values can be collected through one root command rather than starting a separate root process for each value.

### Diagnostics

The device capability scanner checks for the hardware interfaces used by:

- Cooling fan
- Liquid cooling pump
- Fan, logo, and shoulder LEDs
- Shoulder triggers
- Magic Key slider

Missing hardware nodes are reported instead of being silently treated as available.

## Cooling

The Cooling tab manages the physical cooling fan and liquid cooling micropump.

### Manual fan control

Available controls include:

- Fan power on or off
- Manual fan levels from `0` through `5`
- Live fan RPM reading
- Current temperature display
- Fahrenheit or Celsius display preference

Selecting level `0` disables the fan. Selecting levels `1` through `5` enables the fan and applies the requested hardware level.

### Fan curves

Three selectable fan-curve presets are available:

- **Quiet** — low noise and conservative cooling
- **Balanced** — moderate cooling for normal use
- **Turbo** — high cooling and maximum fan levels

The selected curve is saved locally and its visual state is restored when the interface is rebuilt.

### Automatic fan control

Automatic fan control runs in a foreground hardware-control service and selects fan levels from the current device temperature:

| Temperature | Fan level |
|---|---:|
| Below 95°F / 35°C | 0 |
| 95–103°F / 35–39°C | 1 |
| 104–112°F / 40–44°C | 2 |
| 113–121°F / 45–49°C | 3 |
| 122–130°F / 50–54°C | 4 |
| 131°F / 55°C and above | 5 |

A 5°F downward hysteresis is used to prevent the fan from rapidly switching between adjacent levels when the temperature is close to a threshold.

The service skips hardware writes when the requested level already matches the last applied level.

### Pump control

The built-in micropump can be enabled or disabled independently.

Manual profiles are:

| Profile | Frequency | Speed |
|---|---:|---:|
| Slow | 4 | 40 |
| Medium | 4 | 60 |
| Quick | 4 | 80 |
| OC / Experimental | 4 | 90 |

The OC profile is intentionally marked experimental because it operates beyond the normal Quick profile.

### Automatic pump control

Automatic pump mode selects a profile from temperature:

| Temperature | Pump profile |
|---|---|
| Below 95°F / 35°C | Slow |
| 95–104°F / 35–40°C | Medium |
| 105°F / 41°C and above | Quick |

Pump writes are skipped when the desired profile already matches the last applied profile.

### Screen-off cooling policy

To avoid unnecessary battery use:

- Normal fan and pump activity is blocked while the display is off unless the device is hot.
- Cooling remains permitted at approximately 100°F / 38°C or higher.
- Fan and pump shutdown writes are deduplicated.
- Automatic monitoring continues at a reduced screen-off interval.
- Cooling can resume when temperature or screen state requires it.

## Controls

The Controls tab manages root status and the RedMagic Magic Key/slider.

### System controls

- Check current root access
- Refresh current hardware status

### Magic Key stock actions

The Magic Key can be assigned to:

- Camera
- Game Space
- Sound Mode
- Flashlight
- Voice Recorder
- Disabled

Stock Magic Key functions and application-launch mode are mutually exclusive.

### Magic Key app launch

The application picker can assign the Magic Key to launch a selected user or system application.

The picker supports searching by:

- Application name
- Package name

Clearing the selected application disables app-launch mode.

## Hardware

The Hardware tab contains shoulder-trigger tools and reusable profiles.

### Shoulder triggers

The app can:

- Enable trigger hardware
- Disable trigger hardware and its service
- Automatically start triggers after boot
- Map the left and right triggers independently
- Pause trigger auto-start for the current boot session
- Restore trigger auto-start after a restart

Supported trigger actions include:

- None
- Volume Up
- Volume Down
- Play / Pause
- Next Track
- Previous Track

### Intent Unlock

Intent Unlock helps prevent accidental trigger activation.

The left/top and right/bottom triggers can use configurable tap counts before their mapped actions become active.

### Trigger session disable

When **Disable Triggers** is selected:

- Trigger hardware is disabled.
- The trigger service is stopped.
- Auto-start remains saved but is temporarily blocked.
- Triggers stay disabled until the user manually enables them or restarts the phone.

This allows a user to temporarily stop triggers without permanently changing their boot preference.

### Hardware profiles

Hardware profiles save and restore:

- Fan enabled state
- Fan level
- Automatic fan state
- Fan curve
- Pump enabled state
- Pump profile
- Automatic pump state
- Fan LED state, effect, and color
- Logo LED state, effect, and color
- Shoulder LED state, effect, and color
- Trigger configuration
- Trigger mappings
- Trigger auto-start behavior

Profiles can be named, applied, and deleted from the Hardware tab.

### Master profiles

Master profiles capture a broader application snapshot containing:

- Full hardware profile
- Game Mode profile
- Selected game packages
- Charging Mode configuration
- Call Lighting configuration
- Incoming and connected-call LED profiles
- Pump state
- Selected fan curve
- Automatic fan state
- Real-time preview preference
- Trigger preferences

Master profiles are intended for switching between complete device setups rather than changing a single hardware feature.

## Lighting

The Lighting tab controls all three RedMagic LED zones:

- Cooling fan LED
- Rear logo LED
- Shoulder LED strips

### Real-time preview

The Real-time preview switch is located inside **LED Zones**.

When enabled, color and effect selections are applied immediately while configuring an LED profile. It can be disabled to avoid unnecessary preview writes.

### Per-zone configuration

Each LED zone can be enabled, disabled, and configured separately.

Supported effects include:

- Steady
- Breathe
- Flashing
- Rapid, where supported by the selected control

Available colors include:

- Red
- Orange
- Yellow
- Green
- Cyan
- Blue
- Purple
- Pink

The fan LED also supports confirmed stock effect presets.

## RGB Studio

RGB Studio provides a configurable color-cycle service across all LED zones.

Features include:

- Enable or disable the RGB cycle
- Synchronize all LED zones
- Run zones at independent speeds
- Select one or more colors
- Preserve the defined color order
- Steady, Breathe, Flash, and Rapid effects
- Per-zone cycle speeds from 0.5 to 6 seconds
- Apply one effect and color to every zone
- Save and start the cycle
- Stop the RGB service from the dialog
- Screen-off timeouts

Available screen-off timeouts are:

- Immediate
- 1 minute
- 5 minutes
- 15 minutes
- 30 minutes

RGB Studio reuses the shared persistent root command broker instead of launching a new `su` process for every animation frame.

## Game Mode

Game Mode applies a saved hardware profile when one of the selected applications becomes active.

A Game Mode profile can control:

- Fan power and level
- Pump power and profile
- Fan LED
- Logo LED
- Shoulder LEDs
- LED effects and colors

Game detection uses Android Usage Access and accessibility foreground-app events.

The service is event-driven while idle. When a selected game is active, a slow verification poll runs every two minutes. Polling stops when the screen turns off or when the tracked game is no longer active.

When Game Mode ends, the normal saved hardware and lighting state is restored.

## Charging Mode

Charging Mode applies a dedicated LED profile while the phone is connected to power.

Separate charging profiles are available for:

- Fan LED
- Logo LED
- Shoulder LEDs

Charging state is driven by Android power and battery broadcasts rather than constant polling.

When charging ends, the app clears charging ownership and restores the correct previous lighting state.

## Call Lighting

Call Lighting supports separate LED profiles for:

- Incoming/ringing calls
- Connected calls

An optional **Pause fan during calls** setting can:

1. Save the current fan state.
2. Stop automatic fan control.
3. Turn the fan off during the call.
4. Restore the previous fan state when the call ends.
5. Restart automatic fan control when it was previously enabled.

Call state is received through Android telephony events rather than polling.

## LED ownership and priority

Several features can request control of the same LED hardware. The app uses an ownership system so lower-priority features do not overwrite higher-priority states.

The effective priority is:

1. Charging Mode
2. Call Lighting
3. Game Mode
4. RGB Studio
5. Normal saved LEDs

When a higher-priority mode ends, the app restores the next valid owner instead of blindly applying a normal LED profile.

Normal and Game Mode LED writes are blocked while the screen is off. Charging and call behavior retain their required priority.

## Temperature monitoring

Temperature is read directly from readable Linux thermal zones without opening a root shell.

The monitor:

- Normalizes raw millidegree and degree values
- Shares one cached reading between consumers
- Notifies Auto Fan, Auto Pump, and the visible dashboard
- Responds immediately to Android thermal-status events
- Stops its worker thread when no subscribers remain

Adaptive sampling intervals are:

| State | Interval |
|---|---:|
| App visible and screen interactive | 3 seconds |
| Background control while hot | 5 seconds |
| Background control while cool and interactive | 15 seconds |
| Background control while screen off and cool | 30 seconds |

The Fahrenheit/Celsius preference is also respected by the shared foreground-service notification.

## Root execution architecture

Most hardware controls require root because Android applications cannot normally write to the RedMagic kernel and vendor interfaces.

The app uses one serialized persistent root shell for normal command execution.

Benefits include:

- Avoiding a separate `su` process for every command
- Lower command latency
- Reduced process churn
- Ordered hardware writes
- Shared execution between cooling, lighting, RGB Studio, and trigger actions
- Automatic recovery if the persistent shell closes
- One-shot `su -c` fallback for incompatible root providers
- Exit-code validation for root commands

The two shoulder-trigger input readers remain dedicated blocking processes because each must continuously consume its own kernel input stream.

## Hardware write protection

The controller includes several safeguards:

- Fan levels are clamped to `0–5`.
- Fan PWM values are clamped to `0–255`.
- Known pump profiles generate fixed commands.
- Fan stock presets are restricted to an allowlist.
- Duplicate writes to the same hardware resource are skipped for two seconds.
- Fan and pump telemetry caches are invalidated after successful writes.
- Automatic services avoid reapplying an unchanged state.
- Hardware writes are serialized through the shared root broker.

These safeguards reduce redundant writes but cannot eliminate every risk associated with root-level hardware control.

## Foreground services

Continuous fan, pump, fan-light persistence, and RGB Studio operations use Android's `specialUse` foreground-service classification.

This classification accurately describes continuous user-enabled internal hardware control and avoids incorrectly charging the services against Android 15's six-hour `dataSync` foreground-service limit.

Android displays foreground-service notifications while these features are active.

## Boot behavior

After boot or user unlock, the app can restore enabled event-driven services for:

- Trigger auto-start
- Charging Mode
- Call Lighting
- RGB Studio

A manual trigger session disable is cleared after a full device restart.

## Permissions

| Permission or access | Purpose |
|---|---|
| Root / superuser | Write fan, pump, LED, trigger, and Magic Key hardware state |
| Foreground Service | Keep explicitly enabled continuous hardware controls active |
| Foreground Service Special Use | Classify continuous internal hardware-control services correctly |
| Notifications | Display required foreground-service state |
| Boot Completed | Restore user-enabled services after reboot |
| Usage Access | Detect selected foreground games |
| Accessibility Service | Receive foreground-app events and support trigger behavior |
| Phone State | Apply incoming and connected-call lighting |
| Display over other apps app-op | Support trigger-related setup where required |

Permissions are requested for specific features. Game Mode and Call Lighting require their corresponding access to work correctly.

## Privacy and project integrity

The application is designed to operate locally on the phone.

### No network permission

The manifest does **not** request Android's `INTERNET` permission. The application cannot directly upload telemetry, profiles, device information, or usage data over the network.

GitHub and reference buttons open external URLs through the user's chosen browser rather than downloading content inside the app.

### No analytics or advertising SDKs

The project does not include:

- Advertising frameworks
- Analytics SDKs
- Cloud accounts
- Remote-control services
- Remote profile storage
- Device tracking libraries

### Local storage

Preferences and profiles are stored locally using Android application storage. Clearing application data removes saved profiles and settings. Android system backup behavior may apply because application backup is enabled.

### Publicly auditable source

The repository contains the Android source, Gradle configuration, and GitHub Actions workflow used to build the application.

Because the application receives root access, users should install builds from a trusted source and review changes before granting permanent superuser permission.

## Battery and performance design

The app is designed to avoid unnecessary background work:

- One persistent serialized root command shell
- Batched fan and pump telemetry reads
- Thirty-second hardware telemetry caching
- Dashboard polling paused when the activity is not visible
- Adaptive non-root temperature monitoring
- Event-driven charging detection
- Event-driven phone-state handling
- Event-assisted Game Mode detection
- Two-minute Game Mode verification only while a selected game is active
- Automatic fan and pump write deduplication
- General duplicate hardware-write suppression
- Background-priority worker threads
- Screen-off cooling and lighting policies
- Configurable RGB screen-off timeout
- Worker threads stopped when their services are destroyed

## First launch

On first launch:

1. The app verifies the device model.
2. The app checks root availability.
3. The root manager requests superuser access.
4. The setup flow applies the optional access needed for supported features.
5. The app scans the expected hardware interfaces.
6. The main interface opens after validation succeeds.

The root setup can grant:

- Usage Access
- Display-over-other-apps app-op
- Notification permission
- Phone-state permission
- Accessibility service activation

Review the root request before granting access.

## Installation

### GitHub release

1. Open the repository's [Releases](https://github.com/austineyoung2000/Redmagic-Control-Center/releases) page.
2. Download the signed release APK.
3. Allow installation from the browser or file manager if Android requests it.
4. Install the APK.
5. Open the application and grant root when prompted.

Do not install the application on an unsupported device.

### GitHub Actions artifact

Development builds may also be downloaded from successful runs under [Android CI](https://github.com/austineyoung2000/Redmagic-Control-Center/actions/workflows/android.yml).

Artifacts from branch builds can expire and may not use the same signing identity as a published release.

## Building locally

### Requirements

- Git
- JDK 17
- Android SDK Platform 35
- Android Build Tools 35.0.0

### Clone and build

```bash
git clone https://github.com/austineyoung2000/Redmagic-Control-Center.git
cd Redmagic-Control-Center
git checkout sixteen
./gradlew assembleDebug
```

The debug APK is generated under:

```text
app/build/outputs/apk/debug/
```

### Signed release builds

Release signing uses these environment variables:

- `SIGNING_STORE_FILE`
- `SIGNING_STORE_PASSWORD`
- `SIGNING_KEY_ALIAS`
- `SIGNING_KEY_PASSWORD`

When all four variables are available, Gradle uses the configured signing key for release builds.

Never commit a keystore, signing password, or other signing credential to the repository.

## Continuous integration

The Android CI workflow runs for:

- Pushes to `main`
- Pushes to `sixteen`
- Pull requests targeting either branch
- Manual workflow dispatches
- Version tags beginning with `v`

GitHub Actions performs:

- Repository checkout
- JDK 17 setup
- Android SDK 35 setup
- Android Build Tools 35.0.0 installation
- Debug APK compilation
- Signed release APK compilation
- Debug artifact upload
- Signed release artifact upload
- GitHub Release creation for version tags

Release signing credentials are supplied through encrypted GitHub repository secrets.

## Troubleshooting

### Root access is missing

- Confirm that a compatible root solution is installed.
- Open the root manager and verify that RedMagic Control has superuser permission.
- Remove an existing denied entry and reopen the app if necessary.
- Use **Controls → Check Root** to verify access.

The app supports normal `su` access supplied by root solutions such as Magisk, KernelSU, and APatch.

### Hardware is reported as missing

Confirm that the ROM retains the stock RedMagic vendor and kernel interfaces.

A ROM can identify itself as NX809J while still omitting a required device node. Use the diagnostics card on the Home tab to identify which subsystem is unavailable.

### Game Mode does not activate

- Grant Usage Access.
- Enable the app's accessibility service.
- Select at least one game.
- Save a Game Mode profile.
- Confirm that the screen is on and unlocked.
- Confirm that the selected package matches the installed game.

### Triggers do not start automatically

- Enable **Auto-start triggers**.
- Confirm that trigger hardware is detected.
- Confirm that the trigger accessibility service is enabled.
- If triggers were manually disabled, press **Enable Triggers** or restart the phone.

### Lighting is immediately replaced

A higher-priority lighting owner may currently be active. Check the modes in this order:

1. Charging Mode
2. Call Lighting
3. Game Mode
4. RGB Studio
5. Normal LED settings

### Foreground notification remains visible

A continuous hardware service is still active. Check whether any of these features are enabled:

- Auto Fan
- Auto Pump
- Persistent LED restoration
- RGB Studio

Disable the corresponding feature to stop its service.

### Temperature is unavailable

The current ROM may not expose a readable thermal zone at one of the supported paths. Confirm that `/sys/class/thermal/thermal_zone*/temp` is readable by the application.

### Fan or pump values show question marks

The vendor hardware nodes may be temporarily unavailable, the root shell may have failed, or the current ROM may use different paths. Refresh the dashboard after confirming root access.

## Known limitations

- The application is intentionally restricted to NX809J.
- Other RedMagic generations require separate hardware validation and porting.
- Vendor paths can change between firmware releases.
- Custom ROM support depends on the retained stock vendor and kernel implementation.
- Root denial prevents hardware controls from operating.
- Game Mode requires Usage Access.
- Call Lighting requires phone-state access.
- Continuous hardware services display an Android notification.
- The OC pump profile is experimental.
- Profiles are stored locally and are not cloud-synchronized.
- Clearing application data removes saved profiles and settings.
- Uninstalling the app does not necessarily reset Magic Key system settings.
- Some ROM updates may require hardware paths or vendor values to be reconfirmed.

## Reporting problems

Open reports through the repository's [issue tracker](https://github.com/austineyoung2000/Redmagic-Control-Center/issues).

Include:

- Device model
- ROM name
- Build fingerprint
- Android version
- App version
- Root solution
- Steps required to reproduce the problem
- Relevant Logcat output
- Whether the issue occurs on stock firmware
- Home diagnostics results

Do not include:

- Private phone numbers
- Account credentials
- Signing credentials
- Root-manager secrets
- Unrelated system logs containing personal information

## Contributing

Contributions should preserve:

- NX809J hardware safety checks
- Background-thread execution for root work
- LED ownership priority
- Screen-off safety behavior
- Hardware-write deduplication
- Local-only privacy design
- Existing profile compatibility
- GitHub Actions build compatibility

Changes affecting sysfs, procfs, vendor settings, foreground-service behavior, or root execution should be tested on real supported hardware before release.

Pull requests should clearly explain:

- What behavior changes
- Which hardware interfaces are affected
- How the change was tested
- Whether profile compatibility changes
- Whether new permissions or services are introduced

## Project links

- [Repository](https://github.com/austineyoung2000/Redmagic-Control-Center)
- [Releases](https://github.com/austineyoung2000/Redmagic-Control-Center/releases)
- [Android CI](https://github.com/austineyoung2000/Redmagic-Control-Center/actions/workflows/android.yml)
- [Issue tracker](https://github.com/austineyoung2000/Redmagic-Control-Center/issues)

## Disclaimer

This project is independent and is not affiliated with, endorsed by, or maintained by RedMagic, Nubia, or ZTE.

Root access and direct hardware control can cause unexpected behavior, instability, increased heat, battery drain, or hardware stress when used incorrectly.

You are responsible for reviewing and testing the software on your device.

Use the application at your own risk.

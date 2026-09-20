# RedMagic Control Center 2.1.0

Version 2.1.0 is a major feature and reliability update for the RedMagic 11 Pro / NX809J.

## Highlights

- Added six Android Quick Settings tiles for common hardware controls.
- Added a battery-conscious home-screen cooling widget.
- Added a 30-minute thermal-history graph and Active Mode Inspector.
- Added event-driven Master Profile automation rules.
- Replaced the old Hardware Profiles system with portable, versioned Master Profile backups.
- Added a redesigned searchable Material app picker shared by Magic Key and Game Mode.
- Added optional dual-app slider assignments with daily scheduling and previous-mode restoration.
- Added confirmed Magic Key Android shortcut launching through ZTE mode 17.
- Added optional hardware haptic feedback with three strengths.
- Added Fahrenheit/Celsius settings and automatic system light/dark appearance.
- Added stricter NX809J device gating and capability-aware hardware handling.
- Improved Game Mode, Call Lighting, Charging Mode, RGB Studio, and normal LED ownership transitions.

## Profiles and compatibility

Master Profile schema version 5 stores the complete supported configuration, including per-game profiles, RGB Studio, Magic Key shortcut targets, dual-slider schedules, trigger preferences, hardware haptics, and temperature units. Older Master Profile backups remain importable.

The application remains intentionally restricted to the RedMagic 11 Pro / NX809J. Compatible custom ROMs must retain the required stock vendor and kernel interfaces.

## Installation

Download and install the signed release APK attached to this release. Root access is required. Review the README before installing or granting permanent superuser access.

The debug APK is included for testing and diagnostics. Normal users should install the signed release APK.

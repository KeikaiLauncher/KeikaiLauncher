# Changelog

## Unreleased

### Added

* This changelog document
* Norwegian Bokmål translation
* Configuration options to make _KeikaiLauncher_ to expect Android Navigation Bar to be either on the left, on the
  right, or be hidden when device is rotated 90° or 270°

### Fixed

* Launcher rotation not respecting system rotation settings #9
* A bug with sorting by recent and by usage when apps are closed immediately after launching
* Resources leak causing _KeikaiLauncher_ to fail to display applications sometimes #19


## 1.0.0 - 2018-11-06

First official release under a new name after forking [Hayai Launcher](https://github.com/edgarvperes/HayaiLauncher).

This release wouldn't happen without @avuton who did all the heavy lifting and coding!

Also thanks to @edio, @gaul, @wbrawner for bringing the day of this release closer and to all the contributors of _Hayai
Launcher_.

### Added
* Support for orientation change
* Better scaling on larger devices
* Support for Android's UsageStats subsystem (API 21+) for smarter sorting

### Changed
* All images have been replaced with native vectors

### Fixed
* Installs, removes and package modifications (including external removable storage) from home screen
* Behaviour of _home_ button and search bar interactions

### Deleted
* Unused code and resources 

Besides of that, a lot of cleanup and re-factoring work was done to make _Keikai Launcher_ more developer-friendly
* Added project wide code formatting and inspections
* Updated all build requirements 
* Fixed many deprecation warnings
* Made better compartmentalization
* Switched to out-of-the-box solutions where possible
* Updated to the latest APIs
* and many more


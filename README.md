# A fork of HayaiLauncher (name change pending)

It is a fast, [free](https://en.wikipedia.org/wiki/Free_software), minimalist Android Launcher. Even though this fork is heavily modified this launcher is a tribute to the ideas and concepts of HayaiLauncher.


## Changes from mainline
So far, this fork has quite a few features not found in mainline, including:

### User interfacing

* All images have been replaced with native vectors.
* Dynamic layouts which allow orientation change and better scaling for larger devices.
* Removes tons of unused resources, and cleans up existing code to be leaner.
* Less crappy about screen.
* Do fewer things and get the same or better results.
* Installs, removes and package modifications now work (including external removable storage).
* Better behaviour with 'home' button presses and search bar interactions.

### Development stuff

* Tons of cleanups.
* Actual project wide code formatting and inspections. (next branch)
* Update all build requirements.
* Fewer depreciations.
* Better compartmentalization.
* Less custom code when native code is available.
* Fewer fields when the variable doesn't require state storage.
* More static loading.

The app has a very small APK size and currently requires no permissions.

## Download

There are currently no prebuilt binaries.

## Screenshots

Pending (see [Issue #1](https://github.com/avuton/HayaiLauncher/issues/1))

## Branch stuff
* [master](https://github.com/avuton/HayaiLauncher) - This branch have commits considered finished and stable.
* [next](https://github.com/avuton/HayaiLauncher/tree/next) - This branch has commits which are unfinished, under development and potentially unstable. This branch gets force merged all the time, so, the git commit structures will never be stable.
* [hacks](https://github.com/avuton/HayaiLauncher/tree/hacks) - This branch is upstream of the 'next' branch, it includes commits which will never be part of master, but can be considered pretty useful.

# KeikaiLauncher

*Discontinued: Please fork and improve, I simply lack time*

*A fork of HayaiLauncher*

It is a fast, [free](https://en.wikipedia.org/wiki/Free_software), minimalist Android Launcher. Even though this fork is heavily modified this launcher is a tribute to the ideas and concepts of HayaiLauncher.


## Changes from mainline
So far, this fork has quite a few features not found in HayaiLauncher, including:

### User interfacing

* All images have been replaced with native vectors.
* Dynamic layouts which allow orientation change and better scaling for larger devices.
* Removes tons of unused resources, and cleans up existing code to be leaner.
* Less crappy about screen.
* Do fewer things and get the same or better results.
* Installs, removes and package modifications now work (including external removable storage).
* Better behaviour with 'home' button presses and search bar interactions.
* Support for Android's UsageStats subsystem (API 21+).

### Development stuff

* Tons of cleanups, hopefully it's a bit easier to read.
* Actual project wide code formatting and inspections.
* Update all build requirements.
* Fewer deprecations.
* Better compartmentalization.
* Less custom code when native code is available.
* Fewer fields when the variable doesn't require state storage.
* More static loading.
* Updates and targets latest APIs.

The app has a very small APK size and currently requires no permissions.

## Download

There are currently no prebuilt binaries.

## Screenshots

<table style="width:100%">
<tr>
<th>
<a href="https://user-images.githubusercontent.com/396546/27193525-faef906e-51b3-11e7-8a44-56f66307156e.png">
<img alt="Nexus 6P Portrait" width="30%" 
    src="https://user-images.githubusercontent.com/396546/27193525-faef906e-51b3-11e7-8a44-56f66307156e.png">
</a>
</th>
<th>

<a href="https://user-images.githubusercontent.com/396546/27193524-faec6678-51b3-11e7-9510-b84700823e42.png">
<img alt="Nexus 6P Landscape" width="40%"
    src="https://user-images.githubusercontent.com/396546/27193524-faec6678-51b3-11e7-9510-b84700823e42.png">
</a></th></tr>
<td align="center">
    Nexus 6P (Landscape)
</td><td align="center">
    Nexus 6P (Portrait)
</td>


</table>

## Branch stuff
* [master](https://github.com/avuton/KeikaiLauncher) - This branch has commits considered finished and stable.
* [next](https://github.com/avuton/KeikaiLauncher/tree/next) - This branch has commits which are unfinished, under development and potentially unstable. This branch gets force merged all the time, so, the git commit structures will never be stable.
* [hacks](https://github.com/avuton/KeikaiLauncher/tree/hacks) - This branch is upstream of the 'next' branch, it includes commits which will never be part of master, but might be considered pretty useful.

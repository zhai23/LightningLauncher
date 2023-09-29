# Structure of Packages
This documentation has been added for anyone who wants to contribute or modify code.

I've also provided specific documentation in a comment at the top of every class.

## Adapter
Adapters for grid views; provide the views for app icons and group buttons.

## Browser
Everything related to the integrated browser, built to enable easy background audio.
The BrowserService provides WebViews which contain the website content, which are then operated on
by instances of BrowserActivity. These views are kept in memory and active for background audio.

## Helper
Abstract classes which provide helper functions which are used by the launcher.

## Launcher
Everything directly related to the launcher itself.
The LauncherService provides views which contain the launcher interface, which are then operated on
by instances of LauncherActivity. These views are kept in memory for faster loading.

## Lib
Miscellaneous libraries with somewhat generic helper functions that may be taken from or reused in
other projects.

## Support
Semi-independent processes which provide useful functionality to the launcher activity.

## View
Customized versions of android views which provide some additional functionality used in the launcher.
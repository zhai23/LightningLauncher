# LightningLauncher

LightningLauncher is a launcher designed for Oculus Quest that supports both official and sideloaded apps and games.

It has been built with a focus on speed, reliability, and convenience.

It can run on any Android device, but has been tailored for the lastest updates to the Oculus/Meta Quest OS.

## Features
- Simple, sleek interface
- Loads in 1-2 seconds
- Up to date, high res icons from [custom respositories](https://github.com/basti564/LauncherIcons), thanks to [basti564](https://github.com/basti564/)
- Automatic grouping of VR & 2D apps  (Into the default "Apps" and "Tools" groups, as long as they're not renamed)
- Flexible manual grouping & hiding
- Convenient & reliable Shortcut Service OR Shortcut Icon to add to your dock
- Customizable native UI
- Support for multitasking and launching without closing your game


## Releases

Find the latest releases [here](https://github.com/threethan/LightningLauncher/releases).


## Setup Guide
[Developer Mode / Apk installation guide](https://levelup.gitconnected.com/install-android-apps-onto-the-oculus-quest-2-without-going-through-the-app-store-a3336cac3a0e)

1. Install LightningLauncher from [releases](https://github.com/threethan/LightningLauncher/releases/latest).
2. (Recommended) Select one (or both) of the options below for easier use

### Option 1 - Shortcut Service
1. Open LightningLauncher
2. Click the settings cog on the top-right, then select "Library Shortcut Service"
3. Click "Lightning Launcher" on the list
4. Turn on the switch to enable the service

Now LightningLauncher will open when you hover your cursor over the library icon for about half a second. This works even in games!
> **Note**
> This will make the default app launcher difficult, but not impossible, to open. The service can be disabled the same way you enabled it.

### Option 2 - Shortcut Icon
1. If Facebook Messenger is installed, uninstall it.
2. Install LauncherRedirect from [releases](https://github.com/threethan/LightningLauncher/releases/latest).
3. Find "Messenger" on the apps screen. Select the three dots, then "Pin to Universal Menu."

Now you can use the Messenger icon on the dock to open LightningLauncher from anywhere - even in game!
> **Note**
> You will periodically see popups informing you that your messenger app is modified. If you select restore, you will need to repeat Steps 2&3.
> 
## Multitasking
- You can open the launcher however you'd like during gameplay. Note that performance of 2D apps will be poor while a demanding game is running.
- By long-pressing an app, you can set it to open in its own window, independent of the launcher. This allows for painless multitasking similar to the native launcher, but *some apps may not always launch if set to open in their own windows*
- Alternatively, the launcher shortcut and service/main can both be run separately if using the Quest's multitasking, giving you two windows.

## Screenshots
*Need to be updated to show 3.0 interface...*
![goodscr0](https://github.com/threethan/LightningLauncher/assets/12588584/8a8645f0-f983-473b-a0a8-bba28d39bbda)
![goodscr1](https://github.com/threethan/LightningLauncher/assets/12588584/47e83a44-e540-43fe-81ac-2e2a84bd0f23)
![goodscr2](https://github.com/threethan/LightningLauncher/assets/12588584/5e5abb5b-a5d5-4f00-9385-6a0f707acc36)
![goodscr3](https://github.com/threethan/LightningLauncher/assets/12588584/d20ee2b2-4e18-4107-ad54-ec1a72295f53)

## Similar Projects

[PiLauncherNext](https://github.com/Veticia/PiLauncherNext), a more faithful fork of the original PiLauncher.

[DreamGrid](https://github.com/basti564/DreamGrid), the basis of this app. Slower, but supports more platforms and languages.

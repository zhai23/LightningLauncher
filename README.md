# LightningLauncher

LightningLauncher is a launcher designed for Oculus Quest that supports both official and sideloaded apps and games.

It has been built with a focus on speed, flexiblity, and convenience.

It can run on any Android device, but has been tailored for the lastest updates to the Oculus/Meta Quest OS.

![image](https://github.com/threethan/LightningLauncher/assets/12588584/5a0acec5-2102-4afe-adb3-0f7bb4972623)

## Features
- Simple, sleek interface
- Loads in seconds
- Add websites/web-apps - with background audio - to your homescreen *(Discord, Spotify and YouTube all work!)*
- High res banner images for VR apps from [custom respositories](https://github.com/basti564/LauncherIcons), thanks to [basti564](https://github.com/basti564/)
- Automatic grouping of VR & 2D apps *(Into the default "Games" and "Tools" groups, as long as they're not renamed)*
- Flexible manual grouping & hiding of apps *(Groups can also be hidden)*
- Convenient & reliable Shortcut Service OR Shortcut Icon to add to your dock
- Highly customizable interface that can closely match the default launcher
- Support for multitasking and launching without closing your game


## Releases

Find the latest releases [here](https://github.com/threethan/LightningLauncher/releases).

## Setup Guide
1. Download LightningLauncher from [releases](https://github.com/threethan/LightningLauncher/releases/latest).
2. Install the LightningLauncher apk to your Quest. If you don't know how, you can follow [this guide](https://innovate.it.miami.edu/_assets/pdf/tutorial-for-installing-app.pdf).
3. (Recommended) Select one (or both) of the options below for ease of use

### Option 1 - Shortcut Service
1. Open LightningLauncher
2. Click the settings cog on the top-right, then select "Library Shortcut Service"
3. Click the button to "Continue to Accessibility Settings"
4. Click "Lightning Launcher" on the list
5. Turn on the switch to enable the service

Now LightningLauncher will open when you hover your cursor over the library icon for about half a second. This works even in games!
> **Note**
> This will make the default app launcher difficult to open. The service can be disabled at any time in the same way you enabled it.

### Option 2 - Shortcut Icon
1. If Facebook Messenger is installed, uninstall it
2. Install LauncherRedirect from [releases](https://github.com/threethan/LightningLauncher/releases/latest)
3. Find "Messenger" on the apps screen. Select the three dots, then "Pin to Universal Menu"

Now you can use the Messenger icon on the dock to open LightningLauncher from anywhere - even in game!
> **Note**
> You will periodically see popups informing you that your messenger app is modified. If you select restore, you will need to repeat Steps 2&3.

## Multitasking
- You can open the launcher however you'd like during gameplay. Note that performance of 2D apps will be poor while a demanding game is running.
- By long-pressing an app, you can set it to open in its own window, independent of the launcher. This allows for painless multitasking similar to the native launcher, but *some apps may not always launch if set to open in their own windows*

## Similar Projects

[PiLauncherNG]([https://github.com/Veticia/PiLauncherNext](https://github.com/ValentineShilov/PiLauncherNG) is a more faithful fork of the original PiLauncher

[DreamGrid](https://github.com/basti564/DreamGrid) no longer works properly on Oculus devices, but may be better for PicoVR

[Oculess](https://github.com/basti564/Oculess) can provide background audio for all apps if configured as your device's owner *(Advanced)*

[QuestHiddenSettings](https://github.com/threethan/QuestHiddenSettings) helps you set up system-wide adblock which works in LightningLauncher's browser

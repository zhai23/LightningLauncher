# LightningLauncher
### [Now available on SideQuest!](https://sidequestvr.com/app/21783)

LightningLauncher is a launcher designed for Oculus Quest that supports both official and sideloaded apps and games.

It has been built with a focus on speed, flexiblity, and convenience.

It can run on any Android device, but has been tailored for the lastest updates to the Oculus/Meta Quest OS.

![image](https://github.com/threethan/LightningLauncher/assets/12588584/5a0acec5-2102-4afe-adb3-0f7bb4972623)

# Features
- Simple, sleek interface
- Loads in seconds
- Add websites/web-apps - with background audio - to your homescreen *(Discord, Spotify and YouTube all work!)*
- High res banner images for VR apps from [custom respositories](https://github.com/basti564/LauncherIcons), thanks to [basti564](https://github.com/basti564/)
- Automatic grouping of VR & 2D apps *(Into the default "Games" and "Tools" groups, as long as they're not renamed)*
- Flexible manual grouping & hiding of apps *(Groups can also be hidden)*
- Convenient & reliable Shortcut Service OR Shortcut Icon to add to your dock
- Highly customizable interface that can closely match the default launcher
- Support for multitasking and launching without closing your game

# Setup Guide
*You can also [get Lightning Launcher on SideQuest](https://sidequestvr.com/app/21783) if you'd prefer*
1. Download LightningLauncher from [releases](https://github.com/threethan/LightningLauncher/releases/latest).
2. Install the LightningLauncher apk to your Quest. If you don't know how, you can follow [this guide](https://innovate.it.miami.edu/_assets/pdf/tutorial-for-installing-app.pdf).
3. (Recommended) Set up a shortcut option in `Settings->Shortcut Settings`

**Everything should be decently explained in the app itself, but keep reading if you want more detailed info!**

# Shortcut Options
## Messenger Icon
1. If Facebook Messenger is installed, uninstall it
2. Install the Messenger Shortcut from `Settings->Shortcut Settings` 
3. Find "Messenger" on the apps screen of the default launcher. Hover over it, select the three dots, then "Pin to Universal Menu"
  
Now you can use the Messenger icon on the dock to open LightningLauncher from anywhere!

_With this option only, you can drag the messenger icon to the left or right multitasking spaces in the quest to open Lightning Launcher there_

**Note:** You will periodically see popups informing you that your messenger app is modified. If you select restore, you will need to repeat Steps 2&3.

## Explore Shortcut
1. Install the Explore Shortcut from `Settings->Shortcut Settings`
2. (Recommended) Disable the Explore app using the button in `Settings->Shortcut Settings` This will prevent it from opening on startup or when you click its icon
3. Activate the service, following the simple instructions that pop up
4. Close the LightningLauncher window

Now LightningLauncher will open whenever you hover over or click on the Explore icon on the dock.

**Note:** You will no longer be able to use the Explore app

## App Library Shortcut
1. Install the Library Shortcut from `Settings->Shortcut Settings`
2. Activate the service, following the simple instructions that pop up
3. Close the LightningLauncher window

Now LightningLauncher will open whenever you hover over the App Library *(default launcher)* icon on the dock.

**Note:** You will no longer be able to use the Explore app


# Websites & Web Apps
## Adding Websites
1. Go into Edit Mode, either through the settings or by pressing the B button on the controller
2. On the bottom right, select `Add Website`
3. You can select from one of the presets, or type in your any website URL
4. Click the add button to add the website to the currently selected group

Once added, you can change the icon, label, and group of websites like any other app

## Using Websites
- Once a website is added, it will show up on the Launcher like an app
- You will be prompted the first time any website requests to use the mic, its highly recommended to accept this!
  - Once permission is granted, you'll never be asked again
  - If you fail to grant the permission, you must grant LightningLauncher permission to use the microphone through the Android Settings app
  - You can still universally mute your mic through the Quest's quick panel
- Clicking on it will open the website in an internal brower, which works as you'd expect
- When you close the browser or the entire window, the website *will not stop*
  - It will continue playing and/or recording audio in the background
  - If you reopen a website running in the background, it will resume and not reload
  - Websites the are currently running will display a small indicator on their icons, click this indicator to kill them
  - Most media will get paused when the headset goes to sleep or is taken off your head, and must be resumed manually
- If you navigate to a different url than you started on, you'll see a \[+\] icon by the current URL. Click it to add the current page to the Launcher

# Extras
## Multitasking
- You can open the launcher however you'd like during gameplay. Note that performance of 2D apps will be poor while a demanding game is running.
- By long-pressing an app or website, you can set it to open in its own window, independent of the launcher. This allows for painless multitasking similar to the native launcher, but *some apps may not always launch if set to open in their own windows*
- Each shortcut option opens in its own window. If you move the windows so they don't overlap, you can have three instances of Lightning Launcher open at once
- You can only open one instance of each added website, but you can open multiple different websites at once

## Similar Projects

[PiLauncherNG](https://github.com/ValentineShilov/PiLauncherNG) is a more faithful fork of the original PiLauncher

[DreamGrid](https://github.com/basti564/DreamGrid) no longer works properly on Oculus devices, but may be better for PicoVR

[Oculess](https://github.com/basti564/Oculess) can provide background audio for all apps if configured as your device's owner *(Advanced)*

[QuestHiddenSettings](https://github.com/threethan/QuestHiddenSettings) helps you set up system-wide adblock which works in LightningLauncher's browser

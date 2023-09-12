package com.facebook.orca

import android.app.Activity
import android.content.Intent
import android.net.Uri

class MainActivity : Activity() {
    private val downloadUrl = "https://github.com/threethan/LightningLauncher/releases/"
    private var openedWeb = false
    private fun launch(): Boolean {
        var launchIntent = packageManager.getLaunchIntentForPackage("com.threethan.launcher")
        launchIntent?.let { return launchIt(it); }
        launchIntent = packageManager.getLaunchIntentForPackage("com.basti564.dreamgrid")
        launchIntent?.let { return launchIt(it); }
        launchIntent = packageManager.getLaunchIntentForPackage("com.veticia.piLauncherNext")
        launchIntent?.let { return launchIt(it); }
        return false
    }
    private fun launchIt(launchIntent: Intent): Boolean {
        launchIntent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
        finish()
        startActivity(launchIntent)
        return true
    }

    override fun onResume() {
        super.onResume()
        if (!launch() && !openedWeb) {
            openedWeb = true
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(downloadUrl)
            )
            startActivity(browserIntent)
        }
    }
}
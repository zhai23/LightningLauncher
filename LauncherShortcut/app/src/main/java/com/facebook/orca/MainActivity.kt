package com.facebook.orca

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    private val DOWNLOAD_URL = "https://github.com/threethan/LightningLauncher/releases/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    var OPENED_WEB = false
    private fun launch(): Boolean {
        var launchIntent = packageManager.getLaunchIntentForPackage("com.threethan.launcher")
        launchIntent?.let { return launchIt(it); }
        launchIntent = packageManager.getLaunchIntentForPackage("com.basti564.dreamgrid")
        launchIntent?.let { return launchIt(it); }
        launchIntent = packageManager.getLaunchIntentForPackage("com.veticia.piLauncherNext")
        launchIntent?.let { return launchIt(it); }
        return false;
    }
    private fun launchIt(launchIntent: Intent): Boolean {
        launchIntent!!.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
        startActivity(launchIntent)
        return true
    }

    override fun onResume() {
        super.onResume()
        if (!launch() && !OPENED_WEB) {
            OPENED_WEB = true
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(DOWNLOAD_URL)
            )
            startActivity(browserIntent)
        }
    }
}
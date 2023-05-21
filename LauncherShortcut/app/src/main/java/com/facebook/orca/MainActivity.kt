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

    fun launch(): Boolean {
        var launchIntent = packageManager.getLaunchIntentForPackage("com.threethan.launcher")
        launchIntent?.let { startActivity(it); return true; }
        launchIntent = packageManager.getLaunchIntentForPackage("com.basti564.dreamgrid")
        launchIntent?.let { startActivity(it); return true; }
        launchIntent = packageManager.getLaunchIntentForPackage("com.veticia.piLauncherNext")
        launchIntent?.let { startActivity(it); return true; }
        return false;
    }

    override fun onResume() {
        super.onResume()
        if (!launch()) {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(DOWNLOAD_URL)
            )
            startActivity(browserIntent)
        }
    }
}
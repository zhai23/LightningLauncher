package com.threethan.launcher.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.threethan.launcher.data.Settings;
import com.threethan.launcher.helper.Compat;
import com.threethan.launchercore.Core;

/** Spawns a LauncherActivitySearchable or QuestBlurActivity as needed */
public class StartActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Core.init(this);

        Intent intent = new Intent(this, shouldBlur()
                ? LauncherActivityBlurred.class : LauncherActivitySearchable.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        finish();

        startActivity(intent);
    }

    private boolean shouldBlur() {
        return Compat.getDataStore().getBoolean(Settings.KEY_NEW_BLUR, Settings.DEFAULT_NEW_BLUR);
    }
}

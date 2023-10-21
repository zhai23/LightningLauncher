package com.threethan.launcher.support;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.threethan.launcher.helper.Platform;

import java.io.IOException;

/*
    AddShortcutActivity

    Allows 3rd party apps to add shortcuts to the launcher. Not well tested!
 */
public class AddShortcutActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        super.onCreate(savedInstanceState);
        LauncherApps launcherApps = (LauncherApps) this.getSystemService(Context.LAUNCHER_APPS_SERVICE);

        LauncherApps.PinItemRequest pinItemRequest = launcherApps.getPinItemRequest(getIntent());


        if (pinItemRequest == null) {
            this.finish();

            return;
        }

        ShortcutInfo shortcutInfo = pinItemRequest.getShortcutInfo();

        pinItemRequest.accept();

        assert shortcutInfo != null;
        String label = "";

        if (shortcutInfo.getShortLabel() != null) {
            label = shortcutInfo.getShortLabel().toString();
        } else if (shortcutInfo.getLongLabel() != null) {
            label = shortcutInfo.getLongLabel().toString();
        }

        String json = getFixedGsonWriter().toJson(shortcutInfo);
        //noinspection deprecation
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        Platform.addWebsite(sharedPreferences, json, label);

        this.finish();
    }
    public static void launchShortcut(Activity activity, String json) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return;

        LauncherApps launcherApps = (LauncherApps) activity.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        Log.v("JSON", json);
        ShortcutInfo shortcutInfo = getFixedGsonReader().fromJson(json, ShortcutInfo.class);
        launcherApps.startShortcut(shortcutInfo, null, null);
    }
    protected static Gson getFixedGsonWriter() {
        return new GsonBuilder()
                .setExclusionStrategies(new ExclusionStrategy() {
                    @Override
                    public boolean shouldSkipField(FieldAttributes f) {
                        return f.getName().equals("mChangingConfigurations");
                    }

                    @Override
                    public boolean shouldSkipClass(Class<?> clazz) {
                        return false;
                    }
                }).create();
    }
    protected static Gson getFixedGsonReader() {
        return new GsonBuilder()
                .setExclusionStrategies(new ExclusionStrategy() {
                    @Override
                    public boolean shouldSkipField(FieldAttributes f) {
                        return f.getName().equals("mChangingConfigurations");
                    }

                    @Override
                    public boolean shouldSkipClass(Class<?> clazz) {
                        return false;
                    }
                })
                .registerTypeAdapter(CharSequence.class, new TypeAdapter<CharSequence>() {
                    @Override
                    public void write(JsonWriter out, CharSequence value) throws IOException {
                        out.value(value.toString());
                    }

                    @Override
                    public CharSequence read(JsonReader in) throws IOException {
                        return in.nextString();
                    }
                }).create();
    }
}
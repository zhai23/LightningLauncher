package com.threethan.launcher.support;

import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

// A thread-safe wrapper around SharedPreferences.Editor
public class SafeSharedPreferenceEditor implements SharedPreferences.Editor {
    private static SharedPreferences.Editor editor;

    public SafeSharedPreferenceEditor(SharedPreferences.Editor from) {
        editor = from;
    }
    @Override
    synchronized public SharedPreferences.Editor putString(String key, @Nullable String value) {
        editor.putString(key, value);
        return this;
    }

    @Override
    synchronized public SharedPreferences.Editor putStringSet(String key, @Nullable Set<String> values) {
        try {
            editor.putStringSet(key, (values == null ? null : Collections.unmodifiableSet(values)));
        } catch (ConcurrentModificationException e) {
            // If we fail to write, try again in a bit. This could cause issues but at least prevents crash
            e.printStackTrace();
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    putStringSet(key, values);
                }
            }, 100);
        }
        return this;
    }

    @Override
    synchronized public SharedPreferences.Editor putInt(String key, int value) {
        editor.putInt(key, value);
        return this;
    }

    @Override
    synchronized public SharedPreferences.Editor putLong(String key, long value) {
        editor.putLong(key, value);
        return this;
    }

    @Override
    synchronized public SharedPreferences.Editor putFloat(String key, float value) {
        editor.putFloat(key, value);
        return this;
    }

    @Override
    synchronized public SharedPreferences.Editor putBoolean(String key, boolean value) {
        editor.putBoolean(key, value);
        return this;
    }

    @Override
    synchronized public SharedPreferences.Editor remove(String key) {
        editor.remove(key);
        return this;
    }

    @Override
    synchronized public SharedPreferences.Editor clear() {
        editor.clear();
        return this;
    }

    @Override
    synchronized public boolean commit() {
        return editor.commit();
    }

    @Override
    synchronized public void apply() {
        try {
            editor.apply();
        } catch (ConcurrentModificationException e) {
            e.printStackTrace();
        }
    }
}

package com.threethan.launcher.support;

import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import java.util.Set;

// A thread-safe wrapper around SharedPreferences.Editor
public class SynchronizedSharedPreferenceEditor implements SharedPreferences.Editor {
    private final SharedPreferences.Editor editor;

    public SynchronizedSharedPreferenceEditor(SharedPreferences.Editor editor) {
        this.editor = editor;
    }
    @Override
    synchronized public SharedPreferences.Editor putString(String key, @Nullable String value) {
        return editor.putString(key, value);
    }

    @Override
    synchronized public SharedPreferences.Editor putStringSet(String key, @Nullable Set<String> values) {
        return editor.putStringSet(key, values);
    }

    @Override
    synchronized public SharedPreferences.Editor putInt(String key, int value) {
        return editor.putInt(key, value);
    }

    @Override
    synchronized public SharedPreferences.Editor putLong(String key, long value) {
        return editor.putLong(key, value);
    }

    @Override
    synchronized public SharedPreferences.Editor putFloat(String key, float value) {
        return editor.putFloat(key, value);
    }

    @Override
    synchronized public SharedPreferences.Editor putBoolean(String key, boolean value) {
        return editor.putBoolean(key, value);
    }

    @Override
    synchronized public SharedPreferences.Editor remove(String key) {
        return editor.remove(key);
    }

    @Override
    synchronized public SharedPreferences.Editor clear() {
        return editor.clear();
    }

    @Override
    synchronized public boolean commit() {
        return editor.commit();
    }

    @Override
    synchronized public void apply() {
        editor.apply();
    }
}

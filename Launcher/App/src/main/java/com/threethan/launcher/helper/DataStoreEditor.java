package com.threethan.launcher.helper;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.datastore.migrations.SharedPreferencesView;
import androidx.datastore.preferences.core.MutablePreferences;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.core.PreferencesKeys;
import androidx.datastore.preferences.rxjava3.RxPreferenceDataStoreBuilder;
import androidx.datastore.rxjava3.RxDataStore;

import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;
import kotlin.NotImplementedError;

/** @noinspection unused, UnusedReturnValue */
public class DataStoreEditor implements SharedPreferences, SharedPreferences.Editor {
    private static final String TAG = "DataStoreEditor";
    private static final Map<String, RxDataStore<Preferences>> dataStoreByName = new HashMap<>();
    RxDataStore<Preferences> dataStoreRX;

    /** @noinspection rawtypes*/
    final static Class[] classes = new Class[]{
            String.class, Integer.class, Long.class,
            Float.class, Double.class, Boolean.class, Set.class};

    // Contructors
    public DataStoreEditor(Context context, String name) {
        dataStoreRX = getDataStore(context, name);
    }
    public DataStoreEditor(Context context) {
        dataStoreRX = getDataStore(context, "default");
    }
    synchronized private RxDataStore<Preferences> getDataStore(Context context, String name) {
        if (dataStoreByName.containsKey(name)) return dataStoreByName.get(name);
        RxDataStore<Preferences> ds = new RxPreferenceDataStoreBuilder(context, name).build();
        dataStoreByName.put(name, ds);
        return ds;
    }

    /**
     * Migrates a specific sharedPreference instance to this DataStore
     * Data WILL be overridden!
     * You should include your own mechanism to avoid running this more than once.
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void migrateFrom(SharedPreferences sharedPreferences) {
        SharedPreferencesView sharedPreferencesView
                = new SharedPreferencesView(sharedPreferences, sharedPreferences.getAll().keySet());
        Map<String, Object> allPrefs = sharedPreferencesView.getAll();
        allPrefs.forEach((BiConsumer<String, Object>) this::putValue);
    }
    /**
     * Migrates default sharedPreferences to this DataStore (calls migrateFrom)
     * Data WILL be overridden!
     * You should include your own mechanism to avoid running this more than once.
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void migrateDefault(Context context) {
        //noinspection deprecation
        migrateFrom(PreferenceManager.getDefaultSharedPreferences(context));
    }

    // Utility Functions

    /** @noinspection unchecked*/
    public static  <T> Preferences.Key<T> getKey(String key, Class<T> tClass) {
        if (tClass == String.class ) return (Preferences.Key<T>) PreferencesKeys.stringKey   (key);
        if (tClass == Integer.class) return (Preferences.Key<T>) PreferencesKeys.intKey      (key);
        if (tClass == Long.class   ) return (Preferences.Key<T>) PreferencesKeys.longKey     (key);
        if (tClass == Float.class  ) return (Preferences.Key<T>) PreferencesKeys.floatKey    (key);
        if (tClass == Double.class ) return (Preferences.Key<T>) PreferencesKeys.doubleKey   (key);
        if (tClass == Boolean.class) return (Preferences.Key<T>) PreferencesKeys.booleanKey  (key);
        if (tClass == Set.class    ) return (Preferences.Key<T>) PreferencesKeys.stringSetKey(key);
        throw new InvalidParameterException("Invalid preference class: "+tClass+
                ", must be one of "+ Arrays.toString(classes));
    }
    /** @noinspection unchecked*/
    public static  <T> Preferences.Key<T> getKey(String key, T val) {
        if (val instanceof String ) return (Preferences.Key<T>) PreferencesKeys.stringKey   (key);
        if (val instanceof Integer) return (Preferences.Key<T>) PreferencesKeys.intKey      (key);
        if (val instanceof Long   ) return (Preferences.Key<T>) PreferencesKeys.longKey     (key);
        if (val instanceof Float  ) return (Preferences.Key<T>) PreferencesKeys.floatKey    (key);
        if (val instanceof Double ) return (Preferences.Key<T>) PreferencesKeys.doubleKey   (key);
        if (val instanceof Boolean) return (Preferences.Key<T>) PreferencesKeys.booleanKey  (key);
        if (val instanceof Set    ) return (Preferences.Key<T>) PreferencesKeys.stringSetKey(key);
        throw new InvalidParameterException("Invalid preference class, must be one of "
                + Arrays.toString(classes));
    }

    public <T> T getValue(String key, @Nullable T def, Class<T> tClass) {
        Preferences.Key<T> prefKey = getKey(key, tClass);
        T nullFallback = null;
        @SuppressLint("UnsafeOptInUsageWarning")
        Single<T> value = dataStoreRX.data().firstOrError()
                .map(prefs -> prefs.get(prefKey)).onErrorReturn(throwable -> def);
        return value.blockingGet();
    }
    public <T> void getValue(String key, @Nullable T def, Consumer<T> consumer, Class<T> tClass) {
        Preferences.Key<T> prefKey = getKey(key, tClass);
        Single<T> value = dataStoreRX.data().firstOrError()
                .map(prefs -> prefs.get(prefKey)).onErrorReturn(throwable -> def);
        Disposable subscribe = value.subscribe(consumer);
    }
    public <T> void getValue(String key, @NonNull T def, Consumer<T> consumer) {
        Preferences.Key<T> prefKey = getKey(key, def);
        Single<T> value = dataStoreRX.data().firstOrError()
                .map(prefs -> prefs.get(prefKey)).onErrorReturn(throwable -> def);
        Disposable subscribe = value.subscribe(consumer);
    }
    public <T> void putValue(String key, @Nullable T value, Class<T> tClass) {
        boolean returnvalue;
        Preferences.Key<T> prefKey = getKey(key, tClass);

        Single<Preferences> updateResult =  dataStoreRX.updateDataAsync(prefsIn -> {
            MutablePreferences mutablePreferences = prefsIn.toMutablePreferences();
            mutablePreferences.set(prefKey, value);
            return Single.just(mutablePreferences);
        }).doOnError(t -> {
            // Retry on error
            Log.i(TAG, "Error writing value, retrying..."+ t.toString());
            putValue(key, value, tClass);
        });
    }
    public <T> void putValue(String key, @NonNull T value) {
        boolean returnvalue;
        Preferences.Key<T> prefKey = getKey(key, value);

        Single<Preferences> updateResult =  dataStoreRX.updateDataAsync(prefsIn -> {
            MutablePreferences mutablePreferences = prefsIn.toMutablePreferences();
            mutablePreferences.set(prefKey, value);
            return Single.just(mutablePreferences);
        }).doOnError(t -> {
            // Retry on error
            Log.i(TAG, "Error writing value, retrying..."+ t.toString());
            putValue(key, value);
        });
    }

    public <T> void removeValue(String key, Class<T> tClass){
        boolean returnvalue;
        Preferences.Key<T> prefKey = getKey(key, tClass);
        @SuppressLint("UnsafeOptInUsageWarning")
        Single<Preferences> updateResult =  dataStoreRX.updateDataAsync(prefsIn -> {
            MutablePreferences mutablePreferences = prefsIn.toMutablePreferences();
            T remove = mutablePreferences.remove(prefKey);
            return null;
        });
    }

    public DataStoreEditor clear(){
        boolean returnvalue;
        @SuppressLint("UnsafeOptInUsageWarning")
        Single<Preferences> updateResult =  dataStoreRX.updateDataAsync(prefsIn -> {
            MutablePreferences mutablePreferences = prefsIn.toMutablePreferences();
            mutablePreferences.clear();
            return null;
        });
        return this;
    }

    // String
    public String getString(String key, String def) {
        return getValue(key, def, String.class);
    }
    public void getString(String key, @NonNull String def, Consumer<String> consumer) {
        getValue(key, def, consumer, String.class);
    }
    public DataStoreEditor putString(String key, String value) {
        putValue(key, value, String.class);
        return this;
    }
    public DataStoreEditor removeString(String key) {
        removeValue(key, String.class);
        return this;
    }
    // Int
    public int getInt(String key, int def) {
        return getValue(key, def, Integer.class);
    }
    public void getInt(String key, int def, Consumer<Integer> consumer) {
        getValue(key, def, consumer, Integer.class);
    }
    public DataStoreEditor putInt(String key, int value) {
        putValue(key, value, Integer.class);
        return this;
    }
    public DataStoreEditor removeInt(String key) {
        removeValue(key, Integer.class);
        return this;
    }
    // Long
    public long getLong(String key, long def) {
        return getValue(key, def, Long.class);
    }
    public void getLong(String key, long def, Consumer<Long> consumer) {
        getValue(key, def, consumer, Long.class);
    }
    public DataStoreEditor putLong(String key, long value) {
        putValue(key, value, Long.class);
        return this;
    }
    public DataStoreEditor removeLong(String key) {
        removeValue(key, Long.class);
        return this;
    }

    // Float
    public float getFloat(String key, float def) {
        return getValue(key, def, Float.class);
    }
    public void getFloat(String key, float def, Consumer<Float> consumer) {
        getValue(key, def, consumer, Float.class);
    }
    public DataStoreEditor putFloat(String key, float value) {
        putValue(key, value, Float.class);
        return this;
    }
    public DataStoreEditor removeFloat(String key) {
        removeValue(key, Float.class);
        return this;
    }

    // Double
    public double getDouble(String key, Double def) {
        return getValue(key, def, Double.class);
    }
    public void getDouble(String key, double def, Consumer<Double> consumer) {
        getValue(key, def, consumer, Double.class);
    }
    public DataStoreEditor putDouble(String key, double value) {
        putValue(key, value, Double.class);
        return this;
    }
    public DataStoreEditor removeDouble(String key) {
        removeValue(key, Double.class);
        return this;
    }

    // Boolean
    public boolean getBoolean(String key, boolean def) {
        return getValue(key, def, Boolean.class);
    }
    public void getBoolean(String key, boolean def, Consumer<Boolean> consumer) {
        getValue(key, def, consumer, Boolean.class);
    }
    public DataStoreEditor putBoolean(String key, boolean value) {
        putValue(key, value, Boolean.class);
        return this;
    }
    public DataStoreEditor removeBoolean(String key) {
        removeValue(key, Boolean.class);
        return this;
    }

    // String Set
    /**
     * Synchronously fetches a string set.
     * The set returned is made modifiable for compatibility!
     */
    public Set<String> getStringSet(String key, Set<String> def) {
        //noinspection unchecked
        return new HashSet<String>(getValue(key, def, Set.class));
    }

    /**
     * Asynchronously fetches a string set.
     * The set returned is unmodifiable!
     */
    public void getStringSet(String key, @NonNull Set<String> def, Consumer<Set<String>> consumer) {
        getValue(key, def, consumer::accept, Set.class);
    }
    public DataStoreEditor putStringSet(String key, Set<String> value) {
        putValue(key, value, Set.class);
        return this;
    }
    public DataStoreEditor removeStringSet(String key) {
        removeValue(key, Set.class);
        return this;
    }

    // Compat
    /**
     * This is included for compatiblity, but is slow!
     * recommended to use removeType functions instead.
     * @noinspection rawtypes
     */
    @Deprecated
    @Override
    public DataStoreEditor remove(String key) {
        for (Class tClass : classes) {
            //noinspection unchecked
            removeValue(key, tClass);
        }
        return this;
    }

    /**
     * This is included for compatiblity, but is slow!
     * recommended to use removeType functions instead.
     * @noinspection rawtypes
     */
    @Override
    public boolean contains(String key) {
        for (Class tClass : classes) {
            //noinspection unchecked
            if (getValue(key, null, tClass) != null) return true;
        }
        return false;
    }

    /**
     * Commit/apply is no longer needed, and does nothing.
     * Keep in mind that all write operations are performed asynchronously.
     */
    @Deprecated
    public boolean commit() {return false;}
    /**
     * Commit/apply is no longer needed, and does nothing.
     * Keep in mind that all write operations are performed asynchronously.
     */
    @Deprecated
    public void apply() {}


    // Compat (sharedpref)

    /**
     * This is included for compatiblity, but simply returns the same object,
     * which implements both SharedPreferences and SharedPreferences.Editor
     */
    @Override
    @Deprecated
    public DataStoreEditor edit() {
        return this;
    }

    /**
     * Not implemented!
     */
    @Override
    @Deprecated
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        throw new NotImplementedError();
    }

    /**
     * Not implemented!
     */
    @Override
    @Deprecated
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        throw new NotImplementedError();
    }

    /**
     * Not implemented!
     */
    @Override
    @Deprecated
    public Map<String, ?> getAll() {
        throw new NotImplementedError();
    }
}


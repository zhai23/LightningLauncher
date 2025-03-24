package com.threethan.launcher.data;

import android.graphics.Color;

import com.threethan.launcher.R;
import com.threethan.launcher.activity.chainload.ChainLoadActivity;
import com.threethan.launcher.activity.chainload.ChainLoadActivityHuge;
import com.threethan.launcher.activity.chainload.ChainLoadActivityLarge;
import com.threethan.launcher.activity.chainload.ChainLoadActivityPhone;
import com.threethan.launcher.activity.chainload.ChainLoadActivitySmall;
import com.threethan.launcher.activity.chainload.ChainLoadActivityWide;
import com.threethan.launchercore.lib.StringLib;
import com.threethan.launchercore.util.App;

import java.util.HashMap;
import java.util.Map;

/**
 * This abstract class just stores a number of default settings used by other classes;
 * it's a glorified text file.
 * <p>
 * Anything that's prefixed with "KEY_" is a string which is used to save/load a given setting
 * to/from sharedPreferences
 * <p>
 * It does not contain any code. Actual access of settings occurs in various other classes,
 * primarily SettingsManager
 */
public abstract class Settings {
    // Backgrounds
    public static final int[] BACKGROUND_DRAWABLES = {
            R.drawable.bg_px_blue,
            R.drawable.bg_px_grey,
            R.drawable.bg_px_red,
            R.drawable.bg_px_white,
            R.drawable.bg_px_orange,
            R.drawable.bg_px_green,
            R.drawable.bg_px_purple,
            R.drawable.bg_meta_dark,
            R.drawable.bg_meta_light,
            R.drawable.bg_warm_dark,
    };
    public static final int[] BACKGROUND_COLORS = {
            Color.parseColor("#25374f"),
            Color.parseColor("#eaebea"),
            Color.parseColor("#f89b94"),
            Color.parseColor("#d9d4da"),
            Color.parseColor("#f9ce9b"),
            Color.parseColor("#e4eac8"),
            Color.parseColor("#74575c"),
            Color.parseColor("#323232"),
            Color.parseColor("#c6d1df"),
            Color.parseColor("#140123"),
    };
    public static final boolean[] BACKGROUND_DARK = {
            true,
            false,
            false,
            false,
            false,
            false,
            true,
            true,
            false,
            true,
    };
    // Theme/background
    public static final String KEY_BACKGROUND = "KEY_CUSTOM_THEME";
    public static final String KEY_BACKGROUND_ALPHA = "KEY_CUSTOM_ALPHA";
    public static final String KEY_DARK_MODE = "KEY_DARK_MODE";
    public static final String KEY_GROUPS_ENABLED = "KEY_GROUPS_ENABLED";
    public static final String KEY_DETAILS_LONG_PRESS = "KEY_DETAILS_LONG_PRESS";
    public static final String KEY_AUTO_HIDE_EMPTY = "KEY_AUTO_HIDE_EMPTY";
    public static final String KEY_SEARCH_WEB = "KEY_SEARCH_WEB";
    public static final String KEY_SEARCH_HIDDEN = "KEY_SEARCH_HIDDEN";
    public static final int DEFAULT_BACKGROUND_VR = 0;
    public static final int DEFAULT_BACKGROUND_TV = 9;
    public static final int DEFAULT_ALPHA = 255;
    public static final boolean DEFAULT_DARK_MODE = true;
    public static final boolean DEFAULT_GROUPS_ENABLED = true;
    public static boolean DEFAULT_DETAILS_LONG_PRESS = false;
    public static final boolean DEFAULT_AUTO_HIDE_EMPTY = true;
    public static final boolean DEFAULT_SEARCH_WEB = true;
    public static final boolean DEFAULT_SEARCH_HIDDEN = true;
    public static final String CUSTOM_BACKGROUND_PATH = "background.png";

    // Basic UI keys
    public static final String KEY_SCALE = "KEY_CUSTOM_SCALE";
    public static final String KEY_MARGIN = "KEY_CUSTOM_MARGIN";
    public static final int DEFAULT_SCALE = 110;
    public static final int MAX_SCALE = 160;
    public static final int MIN_SCALE = 60;
    public static final int DEFAULT_MARGIN = 20;
    public static final String KEY_NEW_MULTITASK = "KEY_NEWER_MULTITASK";
    public static final boolean DEFAULT_NEW_MULTITASK = true;
    public static final String KEY_ALLOW_CHAIN_LAUNCH = "KEY_ALLOW_CHAIN_LAUNCH";
    public static final boolean DEFAULT_ALLOW_CHAIN_LAUNCH = true;
    public static final String KEY_EDIT_MODE = "KEY_EDIT_MODE";
    public static final String KEY_SEEN_WEBSITE_POPUP = "KEY_SEEN_WEBSITE_POPUP";
    public static final String KEY_SEEN_ADDONS = "KEY_SEEN_ADDONS";

    // banner-style display by app type
    public static final String KEY_BANNER = "prefTypeIsWide";
    public static final Map<App.Type, Boolean> FALLBACK_BANNER = new HashMap<>();
    static {
        FALLBACK_BANNER.put(App.Type.PHONE, false);
        FALLBACK_BANNER.put(App.Type.WEB, false);
        FALLBACK_BANNER.put(App.Type.VR, true);
        FALLBACK_BANNER.put(App.Type.TV, true);
        FALLBACK_BANNER.put(App.Type.PANEL, false);
    }
    public static final String KEY_BANNER_OVERRIDE = "prefAppIsWide";


    // show names by display type
    public static final String KEY_SHOW_NAMES_SQUARE = "KEY_CUSTOM_NAMES";
    public static final String KEY_SHOW_NAMES_BANNER = "KEY_CUSTOM_NAMES_WIDE";
    public static final String KEY_SHOW_TIMES_BANNER = "KEY_SHOW_PLAYTIMES_WIDE";
    public static final boolean DEFAULT_SHOW_NAMES_SQUARE = true;
    public static final boolean DEFAULT_SHOW_NAMES_BANNER = false;
    public static final boolean DEFAULT_SHOW_TIMES_BANNER = true;

    public static final String KEY_GROUPS = "prefAppGroups";
    public static final String KEY_GROUP_APP_LIST = "prefAppList";
    public static final String KEY_SELECTED_GROUPS = "prefSelectedGroups";
    public static final String KEY_WEBSITE_LIST = "prefWebAppNames";
    public static final String KEY_LAUNCH_SIZE = "prefLaunchSize";
    public static final String KEY_LAUNCH_BROWSER = "prefLaunchBrowser";
    public static final String KEY_DEFAULT_BROWSER = "KEY_DEFAULT_BROWSER";

    /** @noinspection rawtypes*/
    public static final Class[] launchSizeClasses = {
            null,
            ChainLoadActivityPhone.class,
            ChainLoadActivitySmall.class,
            ChainLoadActivity.class,
            ChainLoadActivityLarge.class,
            ChainLoadActivityHuge.class,
            ChainLoadActivityWide.class,
    };
    // group
    public static final String KEY_DEFAULT_GROUP = "prefDefaultGroupForType";
    public static final Map<App.Type, String> FALLBACK_GROUPS = new HashMap<>();
    static {
        FALLBACK_GROUPS.put(App.Type.PHONE, "Apps");
        FALLBACK_GROUPS.put(App.Type.WEB, "Apps");
        FALLBACK_GROUPS.put(App.Type.VR, StringLib.setStarred("Games", true));
        FALLBACK_GROUPS.put(App.Type.TV, StringLib.setStarred("Media", true));
        FALLBACK_GROUPS.put(App.Type.PANEL, "Apps");
    }

    public static final int MAX_GROUPS = 20;
    public static final int GROUP_WIDTH_DP = 225;

    public static final String HIDDEN_GROUP = "HIDDEN!";
    public static final String UNSUPPORTED_GROUP = "UNSUPPORTED!";

}

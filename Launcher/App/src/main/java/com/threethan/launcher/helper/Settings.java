package com.threethan.launcher.helper;

import android.graphics.Color;

import com.threethan.launcher.R;
import com.threethan.launcher.lib.StringLib;

public abstract class Settings {
    // Backgrounds
    public static final int[] BACKGROUND_DRAWABLES = {
            R.drawable.bg_px_blue,
            R.drawable.bg_px_grey,
            R.drawable.bg_px_red,
            R.drawable.bg_px_yellow,
            R.drawable.bg_px_white,
            R.drawable.bg_px_orange,
            R.drawable.bg_px_green,
            R.drawable.bg_px_purple,
            R.drawable.bg_meta_dark,
            R.drawable.bg_meta_light,
    };
    public static final int[] BACKGROUND_COLORS = {
            Color.parseColor("#25374f"),
            Color.parseColor("#eaebea"),
            Color.parseColor("#f89b94"),
            Color.parseColor("#f2eac9"),
            Color.parseColor("#d9d4da"),
            Color.parseColor("#f9ce9b"),
            Color.parseColor("#e4eac8"),
            Color.parseColor("#74575c"),
            Color.parseColor("#202a36"),
            Color.parseColor("#c6d1df"),
    };
    public static final boolean[] BACKGROUND_DARK = {
            true,
            false,
            false,
            false,
            false,
            false,
            false,
            true,
            true,
            false,
    };

    // Theme/background
    public static final String KEY_BACKGROUND = "KEY_CUSTOM_THEME";
    public static final String KEY_DARK_MODE = "KEY_DARK_MODE";
    public static final String KEY_GROUPS_ENABLED = "KEY_GROUPS_ENABLED";
    public static final int DEFAULT_BACKGROUND = 0;
    public static final boolean DEFAULT_DARK_MODE = true;
    public static final boolean DEFAULT_GROUPS_ENABLED = true;
    public static final int PICK_ICON_CODE = 450;
    public static final int PICK_THEME_CODE = 95;
    public static final String CUSTOM_BACKGROUND_PATH = "background.png";

    // Basic UI keys
    public static final String KEY_SCALE = "KEY_CUSTOM_SCALE";
    public static final String KEY_MARGIN = "KEY_CUSTOM_MARGIN";
    public static final int DEFAULT_SCALE = 112;
    public static final int DEFAULT_MARGIN = 32;
    public static final String KEY_EDIT_MODE = "KEY_EDIT_MODE";
    public static final String KEY_SEEN_LAUNCH_OUT_POPUP = "KEY_SEEN_LAUNCH_OUT_POPUP";
    public static final String KEY_SEEN_HIDDEN_GROUPS_POPUP = "KEY_SEEN_HIDDEN_GROUPS_POPUP";
    public static final String KEY_SEEN_WEBSITE_POPUP = "KEY_SEEN_WEBSITE_POPUP";
    public static final String KEY_SEEN_ADDONS = "KEY_SEEN_ADDONS";
    public static final String KEY_VR_SET = "KEY_VR_SET";
    public static final String KEY_2D_SET = "KEY_2D_SET";
    public static final String KEY_SUPPORTED_SET = "KEY_SUPPORTED_SET";
    public static final String KEY_UNSUPPORTED_SET = "KEY_UNSUPPORTED_SET";

    // banner-style display by app type
    public static final String KEY_WIDE_VR = "KEY_WIDE_VR";
    public static final String KEY_WIDE_2D = "KEY_WIDE_2D";
    public static final String KEY_WIDE_WEB = "KEY_WIDE_WEB";
    public static final boolean DEFAULT_WIDE_VR = true;
    public static final boolean DEFAULT_WIDE_2D = false;
    public static final boolean DEFAULT_WIDE_WEB = false;
    public static final String DONT_DOWNLOAD_ICONS = "DONT_DOWNLOAD_ICONS";

    // show names by display type
    public static final String KEY_SHOW_NAMES_SQUARE = "KEY_CUSTOM_NAMES";
    public static final String KEY_SHOW_NAMES_BANNER = "KEY_CUSTOM_NAMES_WIDE";
    public static final boolean DEFAULT_SHOW_NAMES_SQUARE = true;
    public static final boolean DEFAULT_SHOW_NAMES_BANNER = true;

    public static final String KEY_GROUPS = "prefAppGroups";
    public static final String KEY_GROUP_APP_LIST = "prefAppList";
    public static final String KEY_LAUNCH_OUT = "prefLaunchOutList";
    public static final String KEY_SELECTED_GROUPS = "prefSelectedGroups";
    public static final String KEY_WEBSITE_LIST = "prefWebAppNames";

    // group
    public static final String KEY_GROUP_2D = "KEY_DEFAULT_GROUP_2D";
    public static final String KEY_GROUP_VR = "KEY_DEFAULT_GROUP_VR";
    public static final String KEY_GROUP_WEB = "KEY_DEFAULT_GROUP_WEB";
    public static final String DEFAULT_GROUP_2D = "Apps";
    public static final String DEFAULT_GROUP_VR = StringLib.setStarred("Games", true);
    public static final String DEFAULT_GROUP_WEB = "Apps";

}

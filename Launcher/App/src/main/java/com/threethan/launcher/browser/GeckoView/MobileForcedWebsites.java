package com.threethan.launcher.browser.GeckoView;

import com.threethan.launcher.lib.StringLib;

import java.util.HashSet;
import java.util.Set;

// A list of websites that MUST be opened as mobile
// Other websites open as desktop
public abstract class MobileForcedWebsites {
    private static final Set<String> sites;
    static {
        sites = new HashSet<>();
        sites.add("open.spotify.com");
    }
    public static Set<String> get() {
        return sites;
    }
    public static boolean check(String url) {
        return sites.contains(StringLib.baseUrlWithoutScheme(url));
    }
}

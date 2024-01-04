package com.threethan.launcher.lib;

import com.threethan.launcher.helper.App;

public class StringLib {
    private static final String STAR = "★";
    public static String toggleStar(String in) {
        if (hasStar(in)) return in.replace(STAR, "");
        else return STAR + in;
    }
    public static boolean hasStar(String in) {
        return in.startsWith(STAR);
    }
    public static String withoutStar(String in) {
        if (hasStar(in)) return in.replace(STAR, "");
        else return in;
    }
    public static String forSort(String in) {
        return in.toLowerCase().replace(STAR, " ");
    }
    public static String setStarred(String in, boolean starred) {
        in = in.trim();
        if (hasStar(in) != starred) return toggleStar(in);
        else return in;
    }
    public static String fixUrl(String url) {
        if (App.isShortcut(url)) return "json://" + url; //URL was actually json!
        if (!url.contains("://")) url = "https://" + url;
        return url;
    }

    public static boolean isInvalidUrl(String url) {
        if (url.startsWith("about:")) return false;
        return ((!url.contains("://") || !url.contains(".")));
    }
    public static String toValidFilename(String string) {
        if (string.startsWith("json://")) // Hash json that would otherwise be too long
            return  "shortcut-json-hash-" + string.hashCode();

        string = string.replaceAll("[^A-Za-z0-9.]", "");

        if (string.length()>50) return string.substring(0, 10) + string.hashCode();
        return string;
    }
    public static String toTitleCase(String string) {
        if (string == null) return null;
        boolean whiteSpace = true;
        StringBuilder builder = new StringBuilder(string);
        for (int i = 0; i < builder.length(); ++i) {
            char c = builder.charAt(i);
            if (whiteSpace) {
                if (!Character.isWhitespace(c)) {
                    builder.setCharAt(i, Character.toTitleCase(c));
                    whiteSpace = false;
                }
            } else if (Character.isWhitespace(c)) whiteSpace = true;
            else builder.setCharAt(i, Character.toLowerCase(c));
        }
        return builder.toString();
    }

    public static String baseUrlWithScheme(String string) {
        try {
            return string.split("//")[0] + "//" + string.split("/")[2];
        } catch (Exception ignored) { return string; }
    }
    public static final String GOOGLE_SEARCH_PRE = "https://www.google.com/search?q=";
    public static final String YOUTUBE_SEARCH_PRE = "https://www.youtube.com/results?search_query=";
    public static final String APKPURE_SEARCH_PRE = "https://apkpure.com/search?q=";
    public static final String APKMIRROR_SEARCH_PRE = "https://www.apkmirror.com/?post_type=app_release&searchtype=apk&s=";

    public static String googleSearchForUrl(String string) {
        return GOOGLE_SEARCH_PRE+string;
    }
    public static String youTubeSearchForUrl(String string) {
        return YOUTUBE_SEARCH_PRE+string;
    }

    public static String apkPureSearchForUrl(String string) {
        return APKPURE_SEARCH_PRE+string;
    }
    public static String apkMirrorSearchForUrl(String string) {
        return APKMIRROR_SEARCH_PRE+string;
    }
    public static boolean isSearchUrl(String url) {
        return url.contains("search?q=") || url.contains("?search_query=") || url.contains("&searchtype=");
    }
}

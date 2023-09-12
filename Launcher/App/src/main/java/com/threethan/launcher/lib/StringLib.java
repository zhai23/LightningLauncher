package com.threethan.launcher.lib;

import android.util.Patterns;

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
        if (!url.contains("//")) url = "https://" + url;
        return url;
    }
    public static boolean compareUrl(String url1, String url2) {
        return stripUrl(url1).compareTo(stripUrl(url2)) == 0;
    }
    private static String stripUrl(String url) {
        return url.replace("/","").replace("http:","")
                .replace("https:","").replace("www.","");
    }
    public static boolean isInvalidUrl(String url) {
        return (!Patterns.WEB_URL.matcher(url).matches() || !url.contains("."));
    }
    public static String toValidFilename(String string) {
        return string.replace("/","").replace("&","")
                .replace("=","").replace(":","");
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
}

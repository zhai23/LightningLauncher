package com.threethan.launcher.lib;

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

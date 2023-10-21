package com.threethan.launcher.helper;

import java.util.ArrayList;
import java.util.List;

/*
    PanelAppList

    Literally just stores a list of panel apps.
    It's in it's own class since it doesn't really fit anywhere else.
 */
public abstract class PanelAppList {
    protected static final List<PanelApp> panelAppList;

    static {
        panelAppList = new ArrayList<>();
        panelAppList.add(new PanelApp("Meta Quest Browser", "systemux://browser"));
        panelAppList.add(new PanelApp("Camera", "systemux://sharing"));
        panelAppList.add(new PanelApp("People", "systemux://aui-social-v2"));
//        panelAppList.add(new PanelApp("Quick Settings", "systemux://quick_settings"));
        panelAppList.add(new PanelApp("Quest Settings", "systemux://settings"));
//        panelAppList.add(new PanelApp("Notifications", "systemux://notifications"));
//        panelAppList.add(new PanelApp("Profile", "systemux://aui-profile"));
    }

    public static List<PanelApp> get() {
        return panelAppList;
    }
}

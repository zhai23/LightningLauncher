package com.threethan.launcher.activity.dialog;

import android.app.AlertDialog;
import android.content.Context;

public abstract class AbstractDialog<T extends Context> {
    /** @noinspection unused*/
    public abstract AlertDialog show();
    protected final T a;
    public AbstractDialog(T context) {
        this.a = context;
    }
}

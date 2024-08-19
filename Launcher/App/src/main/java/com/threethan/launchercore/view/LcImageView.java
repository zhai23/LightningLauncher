package com.threethan.launchercore.view;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


public class LcImageView extends androidx.appcompat.widget.AppCompatImageView {

    public LcImageView(@NonNull Context context) {
        super(context);
    }

    public LcImageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        LcToolTipHelper.init(this, attrs);
    }

    public LcImageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LcToolTipHelper.init(this, attrs);
    }
}

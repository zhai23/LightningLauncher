package com.threethan.launcher.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.GridView;

public class DynamicHeightGridView extends GridView {

    public DynamicHeightGridView(Context context) {
        super(context);
    }

    public DynamicHeightGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DynamicHeightGridView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        fixHeight();
    }

    private void fixHeight() {

        int items = getAdapter().getCount();
        int columns = getNumColumns();
        int rows = (int) Math.ceil((double) items / (double)columns);

        setMeasuredDimension(getMeasuredWidth(), getMeasuredHeight()*rows + getVerticalSpacing()*rows);
    }

}

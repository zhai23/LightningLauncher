package com.threethan.launcher.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
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
        if (getAdapter() == null) return;
        int items = getAdapter().getCount();
        int columns = getNumColumns();

        int rows = (int) Math.ceil((double) items / (double)columns);
        int pad = getPaddingTop()+getPaddingBottom();

        setMeasuredDimension(getMeasuredWidth(),
                (getMeasuredHeight()-pad)*rows + getVerticalSpacing()*rows + pad);
    }
    public void setMargin(int margin, boolean names) {
        setVerticalSpacing(names ? margin/2 : margin);
        setHorizontalSpacing(margin);
    }
}

package com.threethan.launcher.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.Response;

/*
    EditTextWatched

    A custom version of an editText which allows a listener to be added for whenever the content
    is changed. Used for the search bar.
 */
@SuppressLint("AppCompatCustomView")
public class EditTextWatched extends EditText {
    private Response.Listener<String> onEdited;

    public EditTextWatched(@NonNull Context context) {
        super(context);
    }

    public EditTextWatched(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public EditTextWatched(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setOnEdited(Response.Listener<String> value) {
        onEdited = value;
    }
    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        if (onEdited != null) onEdited.onResponse(text.toString());
    }
}

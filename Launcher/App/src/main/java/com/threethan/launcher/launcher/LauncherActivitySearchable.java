package com.threethan.launcher.launcher;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.pm.ApplicationInfo;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.threethan.launcher.R;
import com.threethan.launcher.adapter.AppsAdapter;
import com.threethan.launcher.helper.Launch;
import com.threethan.launcher.view.EditTextWatched;

import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderScriptBlur;

public class LauncherActivitySearchable extends LauncherActivityEditable {
    private boolean searching = false;
    protected void searchFor(String text) {
        final AppsAdapter squareAdapter = getAdapterSquare();
        final AppsAdapter bannerAdapter = getAdapterBanner();
        if (squareAdapter != null) {
            squareAdapter.filterBy(text);
            appGridViewSquare.setAdapter(squareAdapter);
        }
        if (bannerAdapter != null) {
            bannerAdapter.filterBy(text);
            appGridViewBanner.setAdapter(bannerAdapter);
        }
    }

    void showSearchBar() {
        searching = true;

        final int endMargin = 275;

        BlurView searchBar = rootView.findViewById(R.id.blurViewSearchBar);
        View topBar = rootView.findViewById(R.id.topBarLayout);
        ObjectAnimator alphaIn  = ObjectAnimator.ofFloat(searchBar, "alpha", 1f);
        ObjectAnimator alphaOut = ObjectAnimator.ofFloat(topBar   , "alpha", 0f);
        alphaIn .setDuration(100);
        alphaOut.setDuration(300);
        alphaIn .start();
        alphaOut.start();

        searchBar.setOverlayColor(Color.parseColor(darkMode ? "#4A000000" : "#50FFFFFF"));

        float blurRadiusDp = 15f;
        searchBar.setClipToOutline(true);

        View windowDecorView = getWindow().getDecorView();
        ViewGroup rootViewGroup = windowDecorView.findViewById(android.R.id.content);

        Drawable windowBackground = windowDecorView.getBackground();
        //noinspection deprecation
        searchBar.setupWith(rootViewGroup, new RenderScriptBlur(getApplicationContext())) // or RenderEffectBlur
                .setFrameClearDrawable(windowBackground) // Optional
                .setBlurRadius(blurRadiusDp);

        // Update then deactivate bv
        searchBar.setActivated(false);
        searchBar.setActivated(true);
        searchBar.setActivated(false);

        rootView.findViewById(R.id.blurViewSearchBar).setVisibility(View.VISIBLE);

        EditTextWatched searchText = findViewById(R.id.searchText);
        searchText.setTextColor(Color.parseColor(darkMode ? "#FFFFFF" : "#000000"));
        ((ImageView) findViewById(R.id.searchHintIcon)).setImageTintList(
                ColorStateList.valueOf(Color.parseColor(darkMode ? "#FFFFFF" : "#000000")));
        ((ImageView) findViewById(R.id.searchCancelIcon)).setImageTintList(
                ColorStateList.valueOf(Color.parseColor(darkMode ? "#FFFFFF" : "#000000")));

        ValueAnimator viewAnimator = ValueAnimator.ofInt(endMargin, 0);
        viewAnimator.setDuration(300);
        viewAnimator.setInterpolator(new DecelerateInterpolator());
        viewAnimator.addUpdateListener(animation -> {
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) searchBar.getLayoutParams();
            lp.setMarginEnd((int) animation.getAnimatedValue());
            lp.setMarginStart((int) animation.getAnimatedValue());
            searchBar.setLayoutParams(lp);
            searchBar.requestLayout();
        });
        viewAnimator.start();
        searchText.setText("");
        searchText.requestFocus();
    }
    void hideSearchBar() {
        searching = false;

        View searchBar = rootView.findViewById(R.id.blurViewSearchBar);
        View topBar = rootView.findViewById(R.id.topBarLayout);
        ObjectAnimator alphaIn  = ObjectAnimator.ofFloat(topBar   , "alpha", 1f);
        ObjectAnimator alphaOut = ObjectAnimator.ofFloat(searchBar, "alpha", 0f);
        alphaIn .setDuration(100);
        alphaOut.setDuration(300);
        alphaIn .start();
        alphaOut.start();
        searchBar.postDelayed(() -> searchBar.setVisibility(View.GONE), 250);

        refreshAdapters();

        // Hide KB
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(searchBar.getWindowToken(),0);
    }

    @Override
    public void refreshInterface() {
        hideSearchBar();
        super.refreshInterface();
    }

    @Override
    protected void init() {
        super.init();
        // Set logo button
        View searchView = rootView.findViewById(R.id.searchClickableEditText);
        searchView.setOnClickListener(view -> {
            if (searching) hideSearchBar();
            else showSearchBar();
        });
        // It seems like the normal method for showing the keyboard doesn't work on quest
        // so we'll put an invisible EditText above the search button and give it a frame
        // to take focus and show the keyboard.
        // Afterwards, focus is moved to the actual search box via showSearchBar()
        searchView.setOnFocusChangeListener((v, hasFocus) -> searchView.post(() -> {
            if (hasFocus) {
                if (searching) hideSearchBar();
                else showSearchBar();
            }
        }));

        EditTextWatched searchText = findViewById(R.id.searchText);
        searchText.setOnEdited(this::searchFor);
        searchText.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    // Perform action on Enter key press
                    if (getAdapterBanner() != null && getAdapterBanner().getCount() == 1)
                        Launch.launchApp(this, (ApplicationInfo) getAdapterBanner().getItem(0));
                    else if (getAdapterSquare() != null && getAdapterSquare().getCount() == 1)
                        Launch.launchApp(this, (ApplicationInfo) getAdapterBanner().getItem(0));
                }
                return true;
            }
            return false;
        });

        findViewById(R.id.searchCancelIcon).setOnClickListener(v -> hideSearchBar());
    }

    @Override
    public void onBackPressed() {
        if (searching) hideSearchBar();
        else super.onBackPressed();
    }
}

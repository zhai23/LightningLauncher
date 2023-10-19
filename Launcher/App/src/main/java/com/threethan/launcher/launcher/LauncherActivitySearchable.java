package com.threethan.launcher.launcher;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.pm.ApplicationInfo;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.threethan.launcher.R;
import com.threethan.launcher.adapter.AppsAdapter;
import com.threethan.launcher.helper.Keyboard;
import com.threethan.launcher.helper.Launch;
import com.threethan.launcher.helper.Platform;
import com.threethan.launcher.view.EditTextWatched;

import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderScriptBlur;

/*
    LauncherActivitySearchable

    The class handles the additional interface elements and properties related to searching.
    It sends the current search term to the GroupsAdapter when updated, and open the first app
    when enter is pressed.

    This is the activity class that will actually be used; though its parent classes should operate
    fine if called independently.
 */

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
        updateTopSearchResult();
        resetScroll();
    }
    ObjectAnimator alphaIn;
    ObjectAnimator alphaOut;
    void showSearchBar() {
        try {
            clearTopSearchResult();
            searching = true;

            final int endMargin = 275;

            BlurView searchBar = rootView.findViewById(R.id.blurViewSearchBar);
            View topBar = rootView.findViewById(R.id.topBarLayout);
            if (alphaIn  != null) alphaIn .end();
            if (alphaOut != null) alphaOut.end();
            alphaIn = ObjectAnimator.ofFloat(searchBar, "alpha", 1f);
            alphaOut = ObjectAnimator.ofFloat(topBar, "alpha", 0f);
            alphaIn.setDuration(100);
            alphaOut.setDuration(300);
            alphaIn.start();
            alphaOut.start();
            topBar.postDelayed(this::fixState, 300);
            searchBar.setVisibility(View.VISIBLE);

            searchBar.setOverlayColor(Color.parseColor(darkMode ? "#4A000000" : "#50FFFFFF"));

            float blurRadiusDp = 15f;
            searchBar.setClipToOutline(true);

            View windowDecorView = getWindow().getDecorView();
            ViewGroup rootViewGroup = (ViewGroup) windowDecorView;

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
            viewAnimator.setDuration(200);
            viewAnimator.setInterpolator(new DecelerateInterpolator());
            viewAnimator.addUpdateListener(animation -> {
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) searchBar.getLayoutParams();
                lp.setMargins((int) animation.getAnimatedValue() + dp(24), 0, (int) animation.getAnimatedValue() + dp(25), 0);
                searchBar.setLayoutParams(lp);
                searchBar.requestLayout();
            });
            viewAnimator.start();
            if (getCurrentFocus() != null) getCurrentFocus().clearFocus();
            searchText.setText("");
            searchText.post(searchText::requestFocus);
            if (Platform.isVr(this)) postDelayed(() -> {
                Keyboard.hide(this, searchBar);
                Keyboard.show(this);
            }, 50);


            if (groupsEnabled) updatePadding();
            View scrollInterior = findViewById(R.id.mainScrollInterior);
            ValueAnimator padAnimator = ValueAnimator.ofInt(scrollInterior.getPaddingTop(), dp(64));
            padAnimator.setDuration(200);
            padAnimator.setInterpolator(new DecelerateInterpolator());
            padAnimator.addUpdateListener(animation -> {
                scrollInterior.setPadding(0, (Integer) animation.getAnimatedValue(), 0,0);
                resetScroll();
            });
            padAnimator.start();


        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    void hideSearchBar() {
        try {
            searching = false;
            Keyboard.hide(this, mainView);

            View searchBar = rootView.findViewById(R.id.blurViewSearchBar);
            View topBar = rootView.findViewById(R.id.topBarLayout);
            if (alphaIn != null) alphaIn.end();
            if (alphaOut != null) alphaOut.end();
            alphaIn = ObjectAnimator.ofFloat(topBar, "alpha", 1f);
            alphaOut = ObjectAnimator.ofFloat(searchBar, "alpha", 0f);
            alphaIn.setDuration(100);
            alphaOut.setDuration(300);
            alphaIn.start();
            alphaOut.start();
            searchBar.postDelayed(() -> searchBar.setVisibility(View.GONE), 250);

            searchBar.postDelayed(this::fixState, 300);
            topBar.setVisibility(View.VISIBLE);
            refreshAdapters();

            View scrollInterior = findViewById(R.id.mainScrollInterior);
            ValueAnimator padAnimator = ValueAnimator.ofInt(scrollInterior.getPaddingTop(), 0);
            padAnimator.setDuration(groupsEnabled ? 0 : 300);
            padAnimator.setInterpolator(new DecelerateInterpolator());
            padAnimator.addUpdateListener(animation ->
                    scrollInterior.setPadding(0, (Integer) animation.getAnimatedValue(), 0,0));

            padAnimator.start();

        } catch (NullPointerException ignored) {}
        clearTopSearchResult();

    }
    protected void fixState() {
        try {
            if (alphaIn != null) alphaIn.end();
            if (alphaOut != null) alphaOut.end();
            View searchBar = rootView.findViewById(R.id.blurViewSearchBar);
            View topBar = rootView.findViewById(R.id.topBarLayout);
            searchBar.setVisibility(searching ? View.VISIBLE : View.GONE);
            topBar.setVisibility(!searching ? View.VISIBLE : View.GONE);
            searchBar.setAlpha(searching ? 1F : 0F);
            topBar.post(() -> topBar.setAlpha(1F)); // Prevent flicker on start
            View scrollInterior = findViewById(R.id.mainScrollInterior);
            scrollInterior.setPadding(0, searching ? dp(64):0, 0, 0);
            scrollInterior.post(this::resetScroll);
        } catch (NullPointerException ignored) {}
    }

    @Override
    public void refreshInterface() {
        searching = false;

        fixState();
        super.refreshInterface();
    }

    @Override
    protected void init() {
        super.init();
        // Set logo button
        View searchIcon = rootView.findViewById(R.id.searchIcon);
        searchIcon.setOnHoverListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_HOVER_ENTER)
                searchIcon.setBackgroundResource(R.drawable.bkg_hover_button_bar_hovered);
            else if (event.getAction() == MotionEvent.ACTION_HOVER_EXIT)
                searchIcon.setBackground(null);
            return false;
        });
        // It seems like the normal method for showing the keyboard doesn't work on quest
        // so we'll put an invisible EditText above the search button and give it a frame
        // to take focus and show the keyboard.
        // Afterwards, focus is moved to the actual search box via showSearchBar()
        searchIcon.setOnFocusChangeListener((v, hasFocus) -> searchIcon.post(() -> {
            if (hasFocus) {
                if (searching) hideSearchBar();
                else showSearchBar();
            }
        }));
        View searchBg = rootView.findViewById(R.id.blurViewSearchIcon);
        searchBg.setOnClickListener((v) -> showSearchBar());

        EditTextWatched searchText = findViewById(R.id.searchText);
        searchText.setOnEdited(this::searchFor);

        searchText.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_UP &&
            keyCode == KeyEvent.KEYCODE_ENTER) {
                // Launch the first visible icon when enter is pressed
                updateTopSearchResult();
                if (currentTopSearchResult != null) try {
                    Keyboard.hide(this, searchBg);
                    Launch.launchApp(this, currentTopSearchResult);
                    return true;
                } catch (Exception ignored) {
                    return false;
                }
            }
            return false;
        });

        searchText.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus && searching) {
                Keyboard.show(this);
                updateTopSearchResult();
            } else {
                Keyboard.hide(this, mainView);
                clearTopSearchResult();
            }
        });

        findViewById(R.id.searchCancelIcon).setOnClickListener(v -> hideSearchBar());

        searching = false;
    }
    private void
    updateTopSearchResult() {
        EditTextWatched searchText = findViewById(R.id.searchText);
        if (searchText == null) clearTopSearchResult();
        else if (searchText.getText().toString().isEmpty()) clearTopSearchResult();
        else {
            // Highlight top result
            if (getAdapterBanner() != null && getAdapterBanner().getCount() > 0)
                changeTopSearchResult((ApplicationInfo) getAdapterBanner().getItem(0));
            else if (getAdapterSquare() != null && getAdapterSquare().getCount() > 0)
                changeTopSearchResult((ApplicationInfo) getAdapterSquare().getItem(0));
            else clearTopSearchResult();
        }
    }
    private void clearTopSearchResult() {
        if (currentTopSearchResult == null) return;
        clearFocusPackageNames.add(currentTopSearchResult.packageName);
        currentTopSearchResult = null;
    }
    private void changeTopSearchResult(ApplicationInfo val) {
        if (currentTopSearchResult != null)
            clearFocusPackageNames.add(currentTopSearchResult.packageName);
        currentTopSearchResult = val;
    }
    @Override
    protected void postRefresh() {
        final View searchShortcutView = findViewById(R.id.searchShortcutView);
        if (searchShortcutView == null) {
            post(this::postRefresh);
            return;
        }
//        searchShortcutView.setFocusable(true);
        // Secret focusable element off the top of the screen to allow search on android tv by pressing up
        searchShortcutView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && !searching) showSearchBar();
        });
        super.postRefresh();
    }

    @Override
    public void onBackPressed() {
        if (searching) hideSearchBar();
        else super.onBackPressed();
    }

    @Override
    protected boolean getSearching() {
        return searching;
    }
}

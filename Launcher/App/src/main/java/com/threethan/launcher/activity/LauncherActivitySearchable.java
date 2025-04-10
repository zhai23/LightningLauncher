package com.threethan.launcher.activity;

import android.animation.ObjectAnimator;
import android.content.pm.ApplicationInfo;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import com.threethan.launcher.R;
import com.threethan.launcher.activity.view.EditTextWatched;
import com.threethan.launcher.helper.LaunchExt;
import com.threethan.launchercore.Core;
import com.threethan.launchercore.metadata.IconLoader;
import com.threethan.launchercore.util.Keyboard;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import eightbitlab.com.blurview.BlurView;

/**
    The class handles the additional interface elements and properties related to searching.
    It sends the current search term to the GroupsAdapter when updated, and open the first app
    when enter is pressed.

    This is the activity class that will actually be used; though its parent classes should operate
    fine if called independently.
 */

public class LauncherActivitySearchable extends LauncherActivityEditable {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Core.init(this);
        super.onCreate(savedInstanceState);
    }
    private boolean searching = false;

    protected void searchFor(String text) {
        Objects.requireNonNull(getAppAdapter()).filterBy(text);
        updateTopSearchResult();
    }
    Timer searchTimer = new Timer();
    private boolean beenNonEmpty = false;
    protected void queueSearch(String text) {
        int delay;
        searchTimer.cancel();
        switch (text.length()) {
            case 0:
                if (beenNonEmpty) hideSearchBar();
                return;
            case 1:
                delay = 600;
                break;
            case 2:
                delay = 300;
                break;
            case 3:
                delay = 200;
                break;
            case 4:
                delay = 150;
                break;
            default:
                delay = 100;
                break;
        }
        beenNonEmpty = true;
        searchTimer = new Timer();
        searchTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    runOnUiThread(() -> searchFor(text));
                } catch (Exception ignored) {}
            }
        }, delay);
    }
    BlurView searchBar;

    void showSearchBar() {
        beenNonEmpty = false;
        try {
            clearTopSearchResult();
            searching = true;

            ObjectAnimator alphaIn = ObjectAnimator.ofFloat(searchBar, "alpha", 1f);
            ObjectAnimator alphaOut = ObjectAnimator.ofFloat(topBar, "alpha", 0f);
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(searchBar, "scaleX", 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(searchBar, "scaleY", 1f);
            alphaIn.setDuration(200);
            alphaOut.setDuration(250);
            scaleX.setDuration(300);
            scaleY.setDuration(300);
            alphaIn.start();
            alphaOut.start();
            scaleX.start();
            scaleY.start();
            searchBar.setVisibility(View.VISIBLE);

            searchBar.setOverlayColor(Color.parseColor(darkMode ? "#4A000000" : "#50FFFFFF"));

            searchBar.setClipToOutline(true);

            rootView.findViewById(R.id.blurViewSearchBar).setVisibility(View.VISIBLE);

            EditTextWatched searchText = findViewById(R.id.searchText);
            searchText.setTextColor(Color.parseColor(darkMode ? "#FFFFFF" : "#000000"));
            ((ImageView) findViewById(R.id.searchHintIcon)).setImageTintList(
                    ColorStateList.valueOf(Color.parseColor(darkMode ? "#FFFFFF" : "#000000")));
            ((ImageView) findViewById(R.id.searchCancelIcon)).setImageTintList(
                    ColorStateList.valueOf(Color.parseColor(darkMode ? "#FFFFFF" : "#000000")));

            if (getCurrentFocus() != null) getCurrentFocus().clearFocus();
            searchText.setText("");
            searchText.post(searchText::requestFocus);
            Keyboard.show(this);

        } catch (NullPointerException e) {
            Log.w(TAG, "NPE when showing searchbar", e);
        }
    }

    void hideSearchBar() {
        try {
            searching = false;
            Keyboard.hide(this, mainView);

            searchBar.setVisibility(View.GONE);

            topBar.setVisibility(groupsEnabled ? View.VISIBLE : View.GONE);
            topBar.setAlpha(1F);
            searchBar.setScaleX(0.5F);
            searchBar.setScaleY(0.5F);
            topBar.postDelayed(this::fixState, 500);
            refreshAdapters();

        } catch (NullPointerException ignored) {}
        clearTopSearchResult();

    }
    protected void fixState() {
        try {
            searchBar.setVisibility(searching ? View.VISIBLE : View.GONE);
            topBar.setVisibility(!searching ? (groupsEnabled ? View.VISIBLE : View.GONE) : View.GONE);
            searchBar.setAlpha(searching ? 1F : 0F);
            topBar.post(() -> topBar.setAlpha(1F)); // Prevent flicker on start
        } catch (NullPointerException ignored) {}
    }

    @Override
    public void refreshInterface() {
        searching = false;

        if (searchBar.getVisibility() == View.VISIBLE) {
            hideSearchBar();
            fixState();
        }
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
        searchBar = rootView.findViewById(R.id.blurViewSearchBar);


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
        searchText.setOnEdited(this::queueSearch);

        searchText.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_UP &&
            keyCode == KeyEvent.KEYCODE_ENTER) {
                // Launch the first visible icon when enter is pressed
                updateTopSearchResult();
                if (currentTopSearchResult != null) try {
                    Keyboard.hide(this, searchBg);
                    LaunchExt.launchApp(this, currentTopSearchResult);
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
            if (getAppAdapter() != null && getAppAdapter().getItemCount() > 0)
                changeTopSearchResult(getAppAdapter().getItem(0));
            else clearTopSearchResult();
        }
    }
    private void clearTopSearchResult() {
        if (currentTopSearchResult == null) return;
        ApplicationInfo prevTopSearchResult = currentTopSearchResult;
        prevTopSearchResultNames.add(IconLoader.cacheName(currentTopSearchResult));
        currentTopSearchResult = null;
        currentTopSearchResultName = null;
        Objects.requireNonNull(getAppAdapter()).notifyItemChanged(prevTopSearchResult);
    }
    private void changeTopSearchResult(ApplicationInfo topRes) {
        clearTopSearchResult();
        currentTopSearchResult = topRes;
        currentTopSearchResultName = IconLoader.cacheName(topRes);
        Objects.requireNonNull(getAppAdapter()).notifyItemChanged(topRes);
    }

    /** @noinspection deprecation*/
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

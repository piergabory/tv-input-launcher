package com.piergabory.minimalhdmi;

import android.app.Activity;
import android.content.Intent;
import android.media.tv.TvContract;
import android.media.tv.TvView;
import android.os.Bundle;
import android.provider.Settings;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

/**
 * Deliberately boring Home screen for a TV used as an HDMI display.
 *
 * CEC handles automatic wake and input switching. The launcher asks Android's
 * native TV input framework to tune passthrough sources; Settings is the only
 * Android screen exposed here.
 */
public final class MainActivity extends Activity {
    private static final String SHOW_INPUTS = "com.piergabory.minimalhdmi.SHOW_INPUTS";
    private static final String[] HDMI_INPUTS = {
            "com.mediatek.tvinput/.hdmi.HDMIInputService/HW4", // HDMI 1
            "com.mediatek.tvinput/.hdmi.HDMIInputService/HW5", // HDMI 2 / ARC
            "com.mediatek.tvinput/.hdmi.HDMIInputService/HW6"  // HDMI 3
    };
    private static final String COMPOSITE_INPUT =
            "com.mediatek.tvinput/.composite.CompositeInputService/HW1";

    private FrameLayout root;
    private TvView tvView;
    private View menu;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences preferences;
    private String lastInput;
    private String activeInput;
    private final Map<String, CardVisual> inputCards = new HashMap<>();
    private View firstInputCard;
    private static final long INPUT_OVERLAY_MS = 4000;

    private static final class CardVisual {
        final View card;
        final TextView name;
        final TextView detail;
        final ImageView image;

        CardVisual(View card, TextView name, TextView detail, ImageView image) {
            this.card = card;
            this.name = name;
            this.detail = detail;
            this.image = image;
        }
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        preferences = getSharedPreferences("launcher", MODE_PRIVATE);
        lastInput = preferences.getString("last_input", null);
        activeInput = lastInput;
        buildScreen();

        if (lastInput != null) {
            tune(lastInput, false);
        }
        if (SHOW_INPUTS.equals(getIntent().getAction())) showInputMenu();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (SHOW_INPUTS.equals(intent.getAction())) showInputMenu();
    }

    private void buildScreen() {
        root = new FrameLayout(this);
        root.setBackgroundColor(0xff000000);

        tvView = new TvView(this);
        tvView.setVisibility(View.GONE);
        root.addView(tvView, new FrameLayout.LayoutParams(-1, -1));

        menu = makeMenu();
        root.addView(menu, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
    }

    private View makeMenu() {
        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setGravity(Gravity.BOTTOM);
        screen.setPadding(dp(48), dp(48), dp(48), dp(48));

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        addInputCard(buttons, "HDMI 1", "HDMI", R.drawable.ic_hdmi, HDMI_INPUTS[0]);
        addInputCard(buttons, "HDMI 2", "ARC / eARC", R.drawable.ic_hdmi, HDMI_INPUTS[1]);
        addInputCard(buttons, "HDMI 3", "HDMI", R.drawable.ic_hdmi, HDMI_INPUTS[2]);
        addInputCard(buttons, "Composite", "AV input", R.drawable.ic_composite, COMPOSITE_INPUT);
        addCard(buttons, "Settings", "TV settings", R.drawable.ic_settings, null, this::openSettings);
        screen.addView(buttons, new LinearLayout.LayoutParams(-1, -1));

        return screen;
    }

    private void addInputCard(LinearLayout row, String label, String supportingText, int icon,
                              String inputId) {
        addCard(row, label, supportingText, icon, inputId, v -> tune(inputId, true));
    }

    private void addCard(LinearLayout row, String label, String supportingText, int icon,
                         String inputId, View.OnClickListener action) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(16), dp(12), dp(16), dp(12));
        card.setFocusable(true);
        card.setFocusableInTouchMode(true);
        card.setClickable(true);

        ImageView image = new ImageView(this);
        image.setImageResource(icon);
        image.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
        card.addView(image, new LinearLayout.LayoutParams(dp(42), dp(42)));

        TextView name = new TextView(this);
        name.setText(label);
        name.setTextColor(Color.WHITE);
        name.setTextSize(17);
        name.setGravity(Gravity.CENTER);
        name.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(name, new LinearLayout.LayoutParams(-1, dp(34)));

        TextView detail = new TextView(this);
        detail.setText(supportingText);
        detail.setTextColor(0xffcac4d0);
        detail.setTextSize(12);
        detail.setGravity(Gravity.CENTER);
        card.addView(detail, new LinearLayout.LayoutParams(-1, dp(24)));

        card.setOnClickListener(action);
        card.setOnFocusChangeListener((view, focused) -> {
            updateCardAppearance(card, name, detail, image, focused, inputId);
        });
        updateCardAppearance(card, name, detail, image, false, inputId);

        if (inputId != null) {
            inputCards.put(inputId, new CardVisual(card, name, detail, image));
            if (firstInputCard == null) firstInputCard = card;
        }

        // Fill the available row evenly, leaving the screen padding to define the margins.
        LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(0, dp(128), 1f);
        size.setMargins(dp(4), 0, dp(4), 0);
        row.addView(card, size);
    }

    private void updateCardAppearance(View card, TextView name, TextView detail,
                                      ImageView image, boolean focused, String inputId) {
        boolean active = inputId != null && inputId.equals(activeInput);
        card.setBackground(cardBackground(focused, active));
        name.setTextColor(focused ? 0xff1d192b : Color.WHITE);
        detail.setTextColor(focused ? 0xff49454f : 0xffcac4d0);
        image.setColorFilter(focused ? 0xff1d192b : Color.WHITE, PorterDuff.Mode.SRC_IN);
    }

    private GradientDrawable cardBackground(boolean focused, boolean active) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(focused ? 0xffd0bcff : 0xff2b2930);
        background.setCornerRadius(dp(14));
        background.setStroke(dp(focused ? 3 : active ? 3 : 1),
                focused ? 0xffeadcff : active ? 0xffd0bcff : 0xff49454f);
        return background;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void showInputMenu() {
        handler.removeCallbacksAndMessages(null);
        if (tvView.getVisibility() != View.VISIBLE && lastInput != null) {
            tune(lastInput, false);
        }
        menu.setVisibility(View.VISIBLE);
        CardVisual selectedVisual = inputCards.get(activeInput);
        View selected = selectedVisual == null ? null : selectedVisual.card;
        (selected != null ? selected : firstInputCard).requestFocus();
        handler.postDelayed(this::hideInputMenu, INPUT_OVERLAY_MS);
    }

    private void hideInputMenu() {
        menu.setVisibility(View.GONE);
    }

    private void tune(String inputId, boolean showOverlay) {
        lastInput = inputId;
        activeInput = inputId;
        if (preferences != null) {
            preferences.edit().putString("last_input", inputId).apply();
        }
        refreshInputCards();
        tvView.setVisibility(View.VISIBLE);
        handler.removeCallbacksAndMessages(null);
        if (showOverlay) {
            menu.setVisibility(View.VISIBLE);
            handler.postDelayed(this::hideInputMenu, INPUT_OVERLAY_MS);
        } else {
            menu.setVisibility(View.GONE);
        }
        tvView.tune(inputId, TvContract.buildChannelUriForPassthroughInput(inputId));
    }

    private void refreshInputCards() {
        for (Map.Entry<String, CardVisual> entry : inputCards.entrySet()) {
            CardVisual visual = entry.getValue();
            updateCardAppearance(visual.card, visual.name, visual.detail, visual.image,
                    visual.card.hasFocus(), entry.getKey());
        }
    }


    private void openSettings(View ignored) {
        startActivity(new Intent(Settings.ACTION_SETTINGS));
    }

    @Override
    public void onBackPressed() {
        if (menu.getVisibility() == View.VISIBLE && tvView.getVisibility() == View.VISIBLE) {
            handler.removeCallbacksAndMessages(null);
            hideInputMenu();
        } else if (tvView.getVisibility() == View.VISIBLE) {
            // Keep the selected source visible; Home can still reopen this launcher.
            showInputMenu();
        } else {
            super.onBackPressed();
        }
    }
}

package com.dkonak.dartat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQUEST_CAMERA = 101;
    private static final int REQUEST_CAPTURE = 201;
    private static final int REQUEST_PICK_IMAGE = 202;
    private static final int COLOR_BACKGROUND = Color.rgb(232, 255, 240);
    private static final int COLOR_SURFACE = Color.WHITE;
    private static final int COLOR_MINT = Color.rgb(209, 254, 229);
    private static final int COLOR_MINT_SOFT = Color.rgb(204, 248, 223);
    private static final int COLOR_PRIMARY = Color.rgb(15, 82, 56);
    private static final int COLOR_PRIMARY_CONTAINER = Color.rgb(45, 106, 79);
    private static final int COLOR_TEXT = Color.rgb(0, 33, 20);
    private static final int COLOR_MUTED = Color.rgb(64, 73, 67);
    private static final int COLOR_CALORIE = Color.rgb(188, 22, 45);
    private static final int SCREEN_ANALYSIS = 0;
    private static final int SCREEN_SETTINGS = 1;
    private static final int SCREEN_ACCOUNT = 2;

    private LinearLayout analysisContent;
    private LinearLayout settingsContent;
    private LinearLayout accountContent;
    private LinearLayout bottomNav;
    private TextView appTitleText;
    private TextView settingsHeadingText;
    private TextView languageTitleText;
    private TextView languageNoteText;
    private Button turkishButton;
    private Button englishButton;
    private TextView accountHeadingText;
    private TextView freeTitleText;
    private TextView freeBadgeText;
    private TextView freeDescriptionText;
    private TextView premiumTitleText;
    private TextView premiumBadgeText;
    private TextView premiumDescriptionText;
    private Button subscribeButton;
    private TextView analysisTab;
    private TextView settingsTab;
    private TextView accountTab;
    private ImageView foodImage;
    private FrameLayout photoFrame;
    private LinearLayout uploadPlaceholder;
    private TextView homeSubtitle;
    private LinearLayout actionsLayout;
    private TextView statusText;
    private LinearLayout lockedPanel;
    private LinearLayout analyzingPanel;
    private LinearLayout resultPanel;
    private TextView resultTitleText;
    private TextView foodNameText;
    private TextView confidenceText;
    private TextView caloriesText;
    private TextView proteinText;
    private TextView carbsText;
    private TextView fatText;
    private TextView portionText;
    private TextView insightText;
    private SeekBar portionSeekBar;
    private Button watchAdButton;
    private Button newPhotoButton;

    private FoodEstimate currentEstimate;
    private Bitmap currentFoodBitmap;
    private int portionGrams = 250;
    private boolean resultUnlocked;
    private boolean aiAnalysisInProgress;
    private boolean analysisPrepared;
    private int currentScreen = SCREEN_ANALYSIS;
    private boolean english;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setStatusBarColor(COLOR_BACKGROUND);
        window.setNavigationBarColor(Color.WHITE);
        setContentView(createContentView());
        showEmptyState();
    }

    private View createContentView() {
        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(Color.WHITE);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(Color.WHITE);
        screen.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), 0, dp(16), dp(28));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.VERTICAL);
        topBar.setGravity(Gravity.CENTER);
        topBar.setPadding(0, dp(30), 0, dp(14));
        topBar.setBackgroundColor(COLOR_BACKGROUND);
        root.addView(topBar, matchWrapParams());

        appTitleText = text("", 20, COLOR_PRIMARY, Typeface.BOLD);
        appTitleText.setGravity(Gravity.CENTER);
        topBar.addView(appTitleText, matchWrapParams());

        analysisContent = new LinearLayout(this);
        analysisContent.setOrientation(LinearLayout.VERTICAL);
        root.addView(analysisContent, matchWrapParams());

        homeSubtitle = text("", 18, COLOR_MUTED, Typeface.NORMAL);
        homeSubtitle.setGravity(Gravity.CENTER);
        homeSubtitle.setPadding(dp(10), dp(20), dp(10), dp(18));
        analysisContent.addView(homeSubtitle);

        photoFrame = new FrameLayout(this);
        photoFrame.setBackground(makeRoundRect(COLOR_MINT, dp(24), Color.rgb(149, 212, 179), dp(2), dp(9)));
        LinearLayout.LayoutParams photoParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(250)
        );
        photoParams.setMargins(0, 0, 0, dp(16));
        analysisContent.addView(photoFrame, photoParams);

        foodImage = new ImageView(this);
        foodImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        foodImage.setBackground(makeRoundRect(COLOR_MINT, dp(24), 0));
        photoFrame.addView(foodImage, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        uploadPlaceholder = new LinearLayout(this);
        uploadPlaceholder.setOrientation(LinearLayout.VERTICAL);
        uploadPlaceholder.setGravity(Gravity.CENTER);
        uploadPlaceholder.setPadding(dp(24), dp(24), dp(24), dp(24));
        photoFrame.addView(uploadPlaceholder, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        TextView cameraGlyph = text("▣+", 42, Color.rgb(88, 143, 119), Typeface.BOLD);
        cameraGlyph.setGravity(Gravity.CENTER);
        uploadPlaceholder.addView(cameraGlyph);

        TextView uploadTitle = text("", 15, Color.rgb(88, 143, 119), Typeface.BOLD);
        uploadTitle.setTag("upload_title");
        uploadTitle.setGravity(Gravity.CENTER);
        uploadTitle.setPadding(0, dp(10), 0, dp(8));
        uploadPlaceholder.addView(uploadTitle);

        TextView uploadHint = text("", 13, Color.rgb(115, 157, 137), Typeface.NORMAL);
        uploadHint.setTag("upload_hint");
        uploadHint.setGravity(Gravity.CENTER);
        uploadPlaceholder.addView(uploadHint);

        actionsLayout = new LinearLayout(this);
        actionsLayout.setOrientation(LinearLayout.HORIZONTAL);
        actionsLayout.setPadding(0, 0, 0, dp(8));
        analysisContent.addView(actionsLayout);

        Button cameraButton = actionButton("");
        cameraButton.setTag("camera_button");
        cameraButton.setOnClickListener(v -> openCamera());
        actionsLayout.addView(cameraButton, weightedButtonParams(0, dp(8), dp(76)));

        Button galleryButton = secondaryButton("");
        galleryButton.setTag("gallery_button");
        galleryButton.setOnClickListener(v -> openGallery());
        actionsLayout.addView(galleryButton, weightedButtonParams(dp(8), 0, dp(76)));

        statusText = text("", 15, COLOR_MUTED, Typeface.NORMAL);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(dp(12), dp(10), dp(12), dp(12));
        analysisContent.addView(statusText, matchWrapParams());

        analyzingPanel = new LinearLayout(this);
        analyzingPanel.setOrientation(LinearLayout.VERTICAL);
        analyzingPanel.setGravity(Gravity.CENTER_HORIZONTAL);
        analyzingPanel.setPadding(dp(28), dp(38), dp(28), dp(38));
        analyzingPanel.setBackground(makeRoundRect(COLOR_SURFACE, dp(28), 0));
        LinearLayout.LayoutParams analyzingParams = matchWrapParams();
        analyzingParams.setMargins(0, dp(16), 0, 0);
        analysisContent.addView(analyzingPanel, analyzingParams);

        TextView analyzingGlyph = text("↻", 42, COLOR_PRIMARY, Typeface.BOLD);
        analyzingGlyph.setGravity(Gravity.CENTER);
        analyzingGlyph.setBackground(makeRoundRect(COLOR_MINT, dp(32), 0));
        analyzingPanel.addView(analyzingGlyph, new LinearLayout.LayoutParams(dp(64), dp(64)));

        TextView analyzingTitle = text("", 28, COLOR_TEXT, Typeface.BOLD);
        analyzingTitle.setTag("analyzing_title");
        analyzingTitle.setGravity(Gravity.CENTER);
        analyzingTitle.setPadding(0, dp(24), 0, dp(12));
        analyzingPanel.addView(analyzingTitle);

        TextView analyzingBody = text("", 18, COLOR_MUTED, Typeface.NORMAL);
        analyzingBody.setTag("analyzing_body");
        analyzingBody.setGravity(Gravity.CENTER);
        analyzingPanel.addView(analyzingBody);

        lockedPanel = new LinearLayout(this);
        lockedPanel.setOrientation(LinearLayout.VERTICAL);
        lockedPanel.setGravity(Gravity.CENTER_HORIZONTAL);
        lockedPanel.setPadding(dp(24), dp(28), dp(24), dp(28));
        lockedPanel.setBackground(makeRoundRect(COLOR_SURFACE, dp(28), 0));
        LinearLayout.LayoutParams lockedParams = matchWrapParams();
        lockedParams.setMargins(0, dp(16), 0, 0);
        analysisContent.addView(lockedPanel, lockedParams);

        TextView checkGlyph = text("✓", 40, COLOR_PRIMARY, Typeface.BOLD);
        checkGlyph.setGravity(Gravity.CENTER);
        checkGlyph.setBackground(makeRoundRect(Color.rgb(192, 248, 217), dp(32), 0));
        lockedPanel.addView(checkGlyph, new LinearLayout.LayoutParams(dp(64), dp(64)));

        TextView lockedTitle = text("", 28, COLOR_TEXT, Typeface.BOLD);
        lockedTitle.setTag("locked_title");
        lockedTitle.setGravity(Gravity.CENTER);
        lockedTitle.setPadding(0, dp(18), 0, dp(10));
        lockedPanel.addView(lockedTitle);

        TextView lockedBody = text("", 18, COLOR_MUTED, Typeface.NORMAL);
        lockedBody.setTag("locked_body");
        lockedBody.setGravity(Gravity.CENTER);
        lockedBody.setPadding(0, 0, 0, dp(18));
        lockedPanel.addView(lockedBody);

        watchAdButton = actionButton("");
        watchAdButton.setTag("watch_ad_button");
        watchAdButton.setOnClickListener(v -> unlockResultAfterAd());
        lockedPanel.addView(watchAdButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(58)
        ));

        TextView adNote = text("", 14, COLOR_MUTED, Typeface.NORMAL);
        adNote.setTag("ad_note");
        adNote.setGravity(Gravity.CENTER);
        adNote.setPadding(0, dp(16), 0, 0);
        lockedPanel.addView(adNote);

        resultPanel = new LinearLayout(this);
        resultPanel.setOrientation(LinearLayout.VERTICAL);
        resultPanel.setPadding(0, 0, 0, 0);
        LinearLayout.LayoutParams panelParams = matchWrapParams();
        panelParams.setMargins(0, 0, 0, 0);
        analysisContent.addView(resultPanel, panelParams);

        resultTitleText = text("", 1, COLOR_PRIMARY, Typeface.BOLD);
        resultTitleText.setGravity(Gravity.CENTER);
        resultTitleText.setPadding(0, 0, 0, 0);
        resultPanel.addView(resultTitleText);

        confidenceText = text("", 1, COLOR_PRIMARY, Typeface.BOLD);
        confidenceText.setGravity(Gravity.CENTER);
        confidenceText.setPadding(0, 0, 0, 0);
        LinearLayout.LayoutParams confidenceParams = new LinearLayout.LayoutParams(
                1,
                1
        );
        confidenceParams.gravity = Gravity.START;
        confidenceParams.setMargins(0, 0, 0, 0);
        resultPanel.addView(confidenceText, confidenceParams);

        LinearLayout calorieCard = new LinearLayout(this);
        calorieCard.setOrientation(LinearLayout.VERTICAL);
        calorieCard.setGravity(Gravity.CENTER);
        calorieCard.setPadding(dp(14), dp(22), dp(14), dp(22));
        calorieCard.setBackground(makeRoundRect(COLOR_SURFACE, dp(16), 0));
        LinearLayout.LayoutParams calorieParams = matchWrapParams();
        calorieParams.setMargins(0, dp(12), 0, dp(16));
        resultPanel.addView(calorieCard, calorieParams);

        foodNameText = text("", 24, COLOR_TEXT, Typeface.BOLD);
        foodNameText.setGravity(Gravity.CENTER);
        calorieCard.addView(foodNameText);

        caloriesText = text("", 40, COLOR_CALORIE, Typeface.BOLD);
        caloriesText.setGravity(Gravity.CENTER);
        caloriesText.setPadding(0, dp(10), 0, 0);
        calorieCard.addView(caloriesText);

        LinearLayout macros = new LinearLayout(this);
        macros.setOrientation(LinearLayout.HORIZONTAL);
        resultPanel.addView(macros);

        proteinText = addMacroCard(macros, "◉", "Protein", Color.rgb(206, 233, 211), COLOR_PRIMARY, 0, dp(8));
        carbsText = addMacroCard(macros, "∴", "Karb", Color.rgb(255, 243, 224), Color.rgb(230, 81, 0), dp(4), dp(4));
        fatText = addMacroCard(macros, "♧", "Yağ", Color.rgb(255, 248, 225), Color.rgb(245, 127, 23), dp(8), 0);

        LinearLayout portionCard = new LinearLayout(this);
        portionCard.setOrientation(LinearLayout.HORIZONTAL);
        portionCard.setGravity(Gravity.CENTER_VERTICAL);
        portionCard.setPadding(dp(12), dp(14), dp(12), dp(14));
        portionCard.setBackground(makeRoundRect(COLOR_SURFACE, dp(16), 0));
        LinearLayout.LayoutParams portionParams = matchWrapParams();
        portionParams.setMargins(0, dp(18), 0, dp(10));
        resultPanel.addView(portionCard, portionParams);

        TextView portionIcon = text("♨", 20, COLOR_PRIMARY, Typeface.BOLD);
        portionIcon.setGravity(Gravity.CENTER);
        portionIcon.setBackground(makeRoundRect(COLOR_MINT, dp(10), 0));
        portionCard.addView(portionIcon, new LinearLayout.LayoutParams(dp(44), dp(44)));

        LinearLayout portionLabels = new LinearLayout(this);
        portionLabels.setOrientation(LinearLayout.VERTICAL);
        portionLabels.setPadding(dp(10), 0, 0, 0);
        portionCard.addView(portionLabels, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView portionTitle = text("", 18, COLOR_TEXT, Typeface.NORMAL);
        portionTitle.setTag("portion_title");
        portionLabels.addView(portionTitle);

        TextView portionSub = text("", 12, COLOR_MUTED, Typeface.NORMAL);
        portionSub.setTag("portion_subtitle");
        portionLabels.addView(portionSub);

        portionText = text("", 18, COLOR_PRIMARY, Typeface.BOLD);
        portionText.setGravity(Gravity.CENTER);
        portionText.setBackground(makeRoundRect(COLOR_MINT, dp(10), 0));
        portionCard.addView(portionText, new LinearLayout.LayoutParams(dp(120), dp(46)));

        portionSeekBar = new SeekBar(this);
        portionSeekBar.setMax(450);
        portionSeekBar.setProgress(portionGrams - 50);
        portionSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                portionGrams = 50 + progress;
                renderEstimate();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        resultPanel.addView(portionSeekBar, matchWrapParams());

        LinearLayout analysisCard = new LinearLayout(this);
        analysisCard.setOrientation(LinearLayout.VERTICAL);
        analysisCard.setPadding(dp(18), dp(18), dp(18), dp(18));
        analysisCard.setBackground(makeRoundRect(COLOR_SURFACE, dp(16), 0));
        LinearLayout.LayoutParams analysisParams = matchWrapParams();
        analysisParams.setMargins(0, dp(18), 0, dp(28));
        resultPanel.addView(analysisCard, analysisParams);

        TextView analysisTitle = text("", 16, COLOR_PRIMARY, Typeface.BOLD);
        analysisTitle.setTag("ai_analysis_title");
        analysisTitle.setPadding(0, 0, 0, dp(12));
        analysisCard.addView(analysisTitle);

        insightText = text("", 15, COLOR_MUTED, Typeface.NORMAL);
        analysisCard.addView(insightText);

        newPhotoButton = actionButton("");
        newPhotoButton.setTag("new_photo_button");
        newPhotoButton.setOnClickListener(v -> showEmptyState());
        LinearLayout.LayoutParams newPhotoParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(58)
        );
        newPhotoParams.setMargins(0, 0, 0, dp(96));
        resultPanel.addView(newPhotoButton, newPhotoParams);

        settingsContent = createSettingsContent();
        root.addView(settingsContent, matchWrapParams());

        accountContent = createAccountContent();
        root.addView(accountContent, matchWrapParams());

        bottomNav = new LinearLayout(this);
        bottomNav.setOrientation(LinearLayout.HORIZONTAL);
        bottomNav.setGravity(Gravity.CENTER);
        bottomNav.setPadding(dp(10), dp(8), dp(10), dp(8));
        bottomNav.setBackgroundColor(COLOR_BACKGROUND);
        screen.addView(bottomNav, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(76)
        ));

        analysisTab = addBottomTab(bottomNav, "Analiz", SCREEN_ANALYSIS);
        settingsTab = addBottomTab(bottomNav, "Ayarlar", SCREEN_SETTINGS);
        accountTab = addBottomTab(bottomNav, "Hesabım", SCREEN_ACCOUNT);
        bottomNav.setOnApplyWindowInsetsListener((view, insets) -> {
            int bottom = 0;
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                bottom = insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
            } else if (android.os.Build.VERSION.SDK_INT >= 20) {
                bottom = insets.getSystemWindowInsetBottom();
            }
            view.setPadding(dp(10), dp(8), dp(10), dp(8) + bottom);
            ViewGroup.LayoutParams params = view.getLayoutParams();
            params.height = dp(76) + bottom;
            view.setLayoutParams(params);
            return insets;
        });

        updateLanguageTexts();
        switchScreen(SCREEN_ANALYSIS);
        return screen;
    }

    private void showEmptyState() {
        switchScreen(SCREEN_ANALYSIS);
        foodImage.setImageDrawable(null);
        LinearLayout.LayoutParams photoParams = (LinearLayout.LayoutParams) photoFrame.getLayoutParams();
        photoParams.height = dp(250);
        photoParams.setMargins(0, 0, 0, dp(16));
        photoFrame.setLayoutParams(photoParams);
        photoFrame.setVisibility(View.VISIBLE);
        uploadPlaceholder.setVisibility(View.VISIBLE);
        homeSubtitle.setVisibility(View.VISIBLE);
        actionsLayout.setVisibility(View.VISIBLE);
        statusText.setText(english ? "ⓘ Results are estimates, not medical advice." : "ⓘ Sonuçlar tahminidir, tıbbi tavsiye değildir.");
        statusText.setVisibility(View.VISIBLE);
        currentEstimate = null;
        currentFoodBitmap = null;
        resultUnlocked = false;
        aiAnalysisInProgress = false;
        analysisPrepared = false;
        analyzingPanel.setVisibility(View.GONE);
        lockedPanel.setVisibility(View.GONE);
        resultPanel.setVisibility(View.GONE);
        renderEstimate();
    }

    private LinearLayout createSettingsContent() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, dp(18), 0, dp(24));

        settingsHeadingText = text("", 26, COLOR_TEXT, Typeface.BOLD);
        settingsHeadingText.setPadding(0, 0, 0, dp(16));
        content.addView(settingsHeadingText);

        LinearLayout languageCard = new LinearLayout(this);
        languageCard.setOrientation(LinearLayout.VERTICAL);
        languageCard.setPadding(dp(18), dp(18), dp(18), dp(18));
        languageCard.setBackground(makeRoundRect(COLOR_SURFACE, dp(18), COLOR_MINT_SOFT));
        content.addView(languageCard, matchWrapParams());

        languageTitleText = text("", 20, COLOR_PRIMARY, Typeface.BOLD);
        languageCard.addView(languageTitleText);

        languageNoteText = text("", 14, COLOR_MUTED, Typeface.NORMAL);
        languageNoteText.setPadding(0, dp(6), 0, dp(16));
        languageCard.addView(languageNoteText);

        turkishButton = secondaryButton("");
        turkishButton.setOnClickListener(v -> {
            english = false;
            updateLanguageTexts();
            Toast.makeText(this, "Türkçe seçildi.", Toast.LENGTH_SHORT).show();
        });
        languageCard.addView(turkishButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(52)
        ));

        englishButton = secondaryButton("");
        englishButton.setOnClickListener(v -> {
            english = true;
            updateLanguageTexts();
            Toast.makeText(this, "English selected.", Toast.LENGTH_SHORT).show();
        });
        LinearLayout.LayoutParams englishParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(52)
        );
        englishParams.setMargins(0, dp(10), 0, 0);
        languageCard.addView(englishButton, englishParams);

        return content;
    }

    private LinearLayout createAccountContent() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, dp(18), 0, dp(24));

        accountHeadingText = text("", 26, COLOR_TEXT, Typeface.BOLD);
        accountHeadingText.setPadding(0, 0, 0, dp(16));
        content.addView(accountHeadingText);

        LinearLayout freeCard = planCard(true);
        content.addView(freeCard, matchWrapParams());

        LinearLayout.LayoutParams paidParams = matchWrapParams();
        paidParams.setMargins(0, dp(14), 0, 0);
        LinearLayout paidCard = planCard(false);
        content.addView(paidCard, paidParams);

        subscribeButton = actionButton("");
        subscribeButton.setEnabled(false);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(54)
        );
        buttonParams.setMargins(0, dp(16), 0, 0);
        content.addView(subscribeButton, buttonParams);

        return content;
    }

    private LinearLayout planCard(boolean freePlan) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackground(makeRoundRect(COLOR_SURFACE, dp(18), COLOR_MINT_SOFT));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(row, matchWrapParams());

        TextView titleView = text("", 20, COLOR_PRIMARY, Typeface.BOLD);
        row.addView(titleView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView badgeView = text("", 13, COLOR_PRIMARY, Typeface.BOLD);
        badgeView.setGravity(Gravity.CENTER);
        badgeView.setPadding(dp(10), dp(6), dp(10), dp(6));
        badgeView.setBackground(makeRoundRect(COLOR_MINT, dp(16), 0));
        row.addView(badgeView);

        TextView descriptionView = text("", 15, COLOR_MUTED, Typeface.NORMAL);
        descriptionView.setPadding(0, dp(10), 0, 0);
        card.addView(descriptionView);
        if (freePlan) {
            freeTitleText = titleView;
            freeBadgeText = badgeView;
            freeDescriptionText = descriptionView;
        } else {
            premiumTitleText = titleView;
            premiumBadgeText = badgeView;
            premiumDescriptionText = descriptionView;
        }
        return card;
    }

    private TextView addBottomTab(LinearLayout parent, String label, int screenId) {
        TextView tab = text(label, 15, COLOR_MUTED, Typeface.BOLD);
        tab.setGravity(Gravity.CENTER);
        tab.setPadding(dp(8), dp(10), dp(8), dp(10));
        tab.setOnClickListener(v -> switchScreen(screenId));
        parent.addView(tab, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        return tab;
    }

    private void switchScreen(int screenId) {
        currentScreen = screenId;
        analysisContent.setVisibility(screenId == SCREEN_ANALYSIS ? View.VISIBLE : View.GONE);
        settingsContent.setVisibility(screenId == SCREEN_SETTINGS ? View.VISIBLE : View.GONE);
        accountContent.setVisibility(screenId == SCREEN_ACCOUNT ? View.VISIBLE : View.GONE);
        styleBottomTab(analysisTab, screenId == SCREEN_ANALYSIS);
        styleBottomTab(settingsTab, screenId == SCREEN_SETTINGS);
        styleBottomTab(accountTab, screenId == SCREEN_ACCOUNT);
    }

    private void styleBottomTab(TextView tab, boolean selected) {
        if (tab == null) {
            return;
        }
        tab.setTextColor(selected ? COLOR_PRIMARY : COLOR_MUTED);
        tab.setBackground(selected ? makeRoundRect(Color.rgb(204, 230, 208), dp(24), 0) : null);
    }

    private void updateLanguageTexts() {
        appTitleText.setText(english ? "Free AI Calorie Calculator" : "Ücretsiz AI Kalori Hesapla");
        homeSubtitle.setText(english
                ? "Take a food photo and let AI estimate\ncalories and macros."
                : "Yemeğinin fotoğrafını çek, AI kalori ve\nmakro değerlerini tahmin etsin.");
        setTaggedText(analysisContent, "upload_title", english ? "Add your food photo here" : "Yemek fotoğrafınızı buraya ekleyin");
        setTaggedText(analysisContent, "upload_hint", english ? "Use bright lighting" : "Aydınlık ortamda çekim yapın");
        setTaggedText(analysisContent, "camera_button", english ? "▣\nTake Photo" : "▣\nFotoğraf Çek");
        setTaggedText(analysisContent, "gallery_button", english ? "▧\nChoose Gallery" : "▧\nGaleriden Seç");
        setTaggedText(analysisContent, "analyzing_title", english ? "AI is analyzing" : "AI analiz ediyor");
        setTaggedText(analysisContent, "analyzing_body",
                english ? "Your photo is being checked. You can unlock the result when it is ready."
                        : "Fotoğrafınız inceleniyor. Sonuç hazır olduğunda reklam izleyerek açabilirsiniz.");
        setTaggedText(analysisContent, "locked_title", english ? "Analysis ready" : "Analiz hazır");
        setTaggedText(analysisContent, "locked_body",
                english ? "Watch a short ad to see the AI estimate."
                        : "Sonucu görmek için kısa bir reklam izleyin. Reklamdan sonra AI tahmini gösterilecektir.");
        setTaggedText(analysisContent, "watch_ad_button", english ? "▶  Watch Ad and See Result" : "▶  Reklam İzle ve Sonucu Gör");
        setTaggedText(analysisContent, "ad_note", english ? "ⓘ Detailed AI analysis appears after the ad." : "ⓘ Reklamdan sonra detaylı AI analizi sunulacaktır.");
        setTaggedText(analysisContent, "portion_title", english ? "Portion" : "Porsiyon");
        setTaggedText(analysisContent, "portion_subtitle", english ? "Estimated amount" : "Tahmini miktar");
        setTaggedText(analysisContent, "ai_analysis_title", english ? "⚙  AI Analysis" : "⚙  AI Analizi");
        setTaggedText(analysisContent, "new_photo_button", english ? "▣  Analyze New Photo" : "▣  Yeni Fotoğraf Analiz Et");

        statusText.setText(english ? "ⓘ Results are estimates, not medical advice." : "ⓘ Sonuçlar tahminidir, tıbbi tavsiye değildir.");
        settingsHeadingText.setText(english ? "Settings" : "Ayarlar");
        languageTitleText.setText(english ? "Language" : "Dil Seçenekleri");
        languageNoteText.setText(english ? "Choose the language for the app." : "Uygulama metinleri için tercih ettiğin dili seç.");
        turkishButton.setText("Türkçe");
        englishButton.setText("English");
        accountHeadingText.setText(english ? "Account" : "Hesabım");
        freeTitleText.setText(english ? "Free" : "Ücretsiz");
        freeBadgeText.setText(english ? "Active" : "Aktif plan");
        freeDescriptionText.setText(english ? "Watch an ad to unlock each AI analysis." : "Reklam izleyerek AI analiz sonucunu gör.");
        premiumTitleText.setText(english ? "Ad-free Subscription" : "Reklamsız Abonelik");
        premiumBadgeText.setText(english ? "Soon" : "Yakında");
        premiumDescriptionText.setText(english ? "Analyze without ads for $2 per month." : "Ayda 2 dolar ile reklamsız analiz yap.");
        subscribeButton.setText(english ? "Subscription link coming soon" : "Abonelik bağlantısı yakında");
        analysisTab.setText(english ? "Analysis" : "Analiz");
        settingsTab.setText(english ? "Settings" : "Ayarlar");
        accountTab.setText(english ? "Account" : "Hesabım");

        if (currentEstimate != null) {
            renderEstimate();
        }
    }

    private void setTaggedText(View view, String tag, String value) {
        Object currentTag = view.getTag();
        if (tag.equals(currentTag) && view instanceof TextView) {
            ((TextView) view).setText(value);
            return;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                setTaggedText(group.getChildAt(index), tag, value);
            }
        }
    }

    private void openCamera() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
            return;
        }
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this, english ? "Camera app not found." : "Kamera uygulaması bulunamadı.", Toast.LENGTH_SHORT).show();
            return;
        }
        startActivityForResult(intent, REQUEST_CAPTURE);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else if (requestCode == REQUEST_CAMERA) {
            Toast.makeText(this, english ? "Camera permission is required." : "Fotoğraf çekmek için kamera izni gerekiyor.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) {
            return;
        }
        try {
            Bitmap bitmap = null;
            if (requestCode == REQUEST_CAPTURE) {
                Object extra = data.getExtras() == null ? null : data.getExtras().get("data");
                if (extra instanceof Bitmap) {
                    bitmap = (Bitmap) extra;
                }
            } else if (requestCode == REQUEST_PICK_IMAGE) {
                Uri uri = data.getData();
                if (uri != null) {
                    try (InputStream stream = getContentResolver().openInputStream(uri)) {
                        bitmap = BitmapFactory.decodeStream(stream);
                    }
                }
            }
            if (bitmap == null) {
                Toast.makeText(this, english ? "Image could not be read." : "Görsel okunamadı.", Toast.LENGTH_SHORT).show();
                return;
            }
            switchScreen(SCREEN_ANALYSIS);
            foodImage.setImageBitmap(bitmap);
            uploadPlaceholder.setVisibility(View.GONE);
            homeSubtitle.setVisibility(View.GONE);
            actionsLayout.setVisibility(View.GONE);
            photoFrame.setVisibility(View.GONE);
            final Bitmap analyzedBitmap = bitmap;
            currentFoodBitmap = bitmap;
            currentEstimate = FoodAnalyzer.analyze(bitmap);
            resultUnlocked = false;
            aiAnalysisInProgress = false;
            analysisPrepared = false;
            analyzingPanel.setVisibility(View.VISIBLE);
            lockedPanel.setVisibility(View.GONE);
            resultPanel.setVisibility(View.GONE);
            statusText.setText(english ? "AI is analyzing..." : "AI analiz ediyor...");
            statusText.setVisibility(View.GONE);
            renderEstimate();
            new Thread(() -> {
                try {
                    Thread.sleep(1200);
                } catch (InterruptedException ignored) {
                }
                runOnUiThread(() -> {
                    if (currentFoodBitmap == analyzedBitmap && !resultUnlocked && !aiAnalysisInProgress) {
                        analysisPrepared = true;
                        analyzingPanel.setVisibility(View.GONE);
                        lockedPanel.setVisibility(View.VISIBLE);
                        resultPanel.setVisibility(View.GONE);
                        statusText.setText(english ? "Analysis ready. Watch an ad to see the result." : "Analiz hazır. Sonucu görmek için reklam izle.");
                        statusText.setVisibility(View.GONE);
                        renderEstimate();
                    }
                });
            }).start();
        } catch (Exception exception) {
            Toast.makeText(this, english ? "There was a problem analyzing the photo." : "Fotoğraf analiz edilirken sorun oluştu.", Toast.LENGTH_SHORT).show();
        }
    }

    private void unlockResultAfterAd() {
        if (currentEstimate == null) {
            Toast.makeText(this, english ? "Take a food photo first." : "Önce bir yemek fotoğrafı çek.", Toast.LENGTH_SHORT).show();
            return;
        }
        watchAdButton.setEnabled(false);
        watchAdButton.setText(english ? "AI is analyzing..." : "AI analiz ediyor...");
        requestAiEstimate();
    }

    private void renderEstimate() {
        if (currentEstimate == null) {
            foodNameText.setText("Henüz analiz yok");
            confidenceText.setText(english ? "The estimated food type will appear here." : "Fotoğraf çekildiğinde burada tahmini yemek türü görünecek.");
            caloriesText.setText("0 kcal");
            proteinText.setText("0 g");
            carbsText.setText("0 g");
            fatText.setText("0 g");
            portionText.setText(portionGrams + " g");
            insightText.setText(english ? "Note: This app gives estimates only; it is not medical advice." : "Not: Bu uygulama tıbbi/nutrisyonel ölçüm yapmaz; yaklaşık fikir vermek için çalışır.");
            return;
        }
        if (!resultUnlocked) {
            resultPanel.setVisibility(View.GONE);
            if (analysisPrepared) {
                watchAdButton.setEnabled(!aiAnalysisInProgress);
                watchAdButton.setText(aiAnalysisInProgress
                        ? (english ? "AI is analyzing..." : "AI analiz ediyor...")
                        : (english ? "▶  Watch Ad and See Result" : "▶  Reklam İzle ve Sonucu Gör"));
            }
            return;
        }

        double factor = portionGrams / 100.0;
        int calories = (int) Math.round(currentEstimate.caloriesPer100g * factor);
        int protein = (int) Math.round(currentEstimate.proteinPer100g * factor);
        int carbs = (int) Math.round(currentEstimate.carbsPer100g * factor);
        int fat = (int) Math.round(currentEstimate.fatPer100g * factor);

        foodNameText.setText(currentEstimate.name);
        confidenceText.setText(String.format(Locale.getDefault(), english ? "Estimated match: %%%d" : "Tahmini eşleşme: %%%d", currentEstimate.confidence));
        caloriesText.setText(calories + " kcal");
        proteinText.setText(protein + " g");
        carbsText.setText(carbs + " g");
        fatText.setText(fat + " g");
        portionText.setText(portionGrams + " g");
        insightText.setText(currentEstimate.note);
    }

    private void requestAiEstimate() {
        if (currentFoodBitmap == null) {
            unlockWithCurrentEstimate("Fotoğraf okunamadı. Yerel tahmini gösteriyorum.");
            return;
        }
        aiAnalysisInProgress = true;
        statusText.setText(english ? "Ad completed. AI is analyzing the photo..." : "Reklam tamamlandı. Fotoğraf AI ile analiz ediliyor...");
        statusText.setVisibility(View.GONE);
        new Thread(() -> {
            try {
                FoodEstimate aiEstimate = AiFoodClient.analyze(currentFoodBitmap, BuildConfig.CALORIE_AI_ENDPOINT);
                runOnUiThread(() -> {
                    currentEstimate = aiEstimate;
                    portionGrams = aiEstimate.portionGrams;
                    portionSeekBar.setProgress(Math.max(0, Math.min(450, portionGrams - 50)));
                    unlockWithCurrentEstimate(english
                            ? "AI result is ready. Adjust the portion to update values."
                            : "AI sonucu hazır. Porsiyonu değiştirerek değerleri güncelleyebilirsin.");
                });
            } catch (Exception exception) {
                runOnUiThread(() -> unlockWithCurrentEstimate(
                        english
                                ? "AI server could not be reached. Showing the backup estimate for now."
                                : "AI sunucusuna ulaşılamadı. Şimdilik telefondaki yedek tahmini gösteriyorum."
                ));
            }
        }).start();
    }

    private void unlockWithCurrentEstimate(String message) {
        aiAnalysisInProgress = false;
        resultUnlocked = true;
        lockedPanel.setVisibility(View.GONE);
        analyzingPanel.setVisibility(View.GONE);
        LinearLayout.LayoutParams photoParams = (LinearLayout.LayoutParams) photoFrame.getLayoutParams();
        photoParams.height = dp(220);
        photoParams.setMargins(0, 0, 0, dp(14));
        photoFrame.setLayoutParams(photoParams);
        photoFrame.setVisibility(View.VISIBLE);
        resultPanel.setVisibility(View.VISIBLE);
        statusText.setText(message);
        statusText.setVisibility(View.GONE);
        renderEstimate();
    }

    private TextView addMetric(LinearLayout parent, String label, int accentColor) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(9), 0, dp(9));
        parent.addView(row, matchWrapParams());

        TextView labelView = text(label, 15, Color.rgb(63, 76, 66), Typeface.BOLD);
        row.addView(labelView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView valueView = text("", 20, accentColor, Typeface.BOLD);
        valueView.setGravity(Gravity.END);
        row.addView(valueView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return valueView;
    }

    private TextView addMacroCard(LinearLayout parent, String glyph, String label, int iconBackground,
                                  int iconColor, int leftMargin, int rightMargin) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(6), dp(12), dp(6), dp(10));
        card.setBackground(makeRoundRect(COLOR_SURFACE, dp(16), COLOR_MINT_SOFT));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(0, dp(120), 1f);
        cardParams.setMargins(leftMargin, 0, rightMargin, 0);
        parent.addView(card, cardParams);

        TextView icon = text(glyph, 19, iconColor, Typeface.BOLD);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(makeRoundRect(iconBackground, dp(20), 0));
        card.addView(icon, new LinearLayout.LayoutParams(dp(40), dp(40)));

        TextView labelView = text(label, 13, COLOR_MUTED, Typeface.NORMAL);
        labelView.setGravity(Gravity.CENTER);
        labelView.setPadding(0, dp(8), 0, dp(4));
        card.addView(labelView);

        TextView valueView = text("", 20, COLOR_TEXT, Typeface.BOLD);
        valueView.setGravity(Gravity.CENTER);
        card.addView(valueView);
        return valueView;
    }

    private Button actionButton(String label) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(label);
        button.setTextSize(16);
        button.setTextColor(Color.WHITE);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setBackground(makeRoundRect(COLOR_PRIMARY_CONTAINER, dp(28), 0));
        return button;
    }

    private Button secondaryButton(String label) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(label);
        button.setTextSize(16);
        button.setTextColor(Color.rgb(80, 104, 86));
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setBackground(makeRoundRect(Color.rgb(204, 230, 208), dp(16), 0));
        return button;
    }

    private TextView text(String value, int sp, int color, int style) {
        TextView textView = new TextView(this);
        textView.setText(value);
        textView.setTextSize(sp);
        textView.setTextColor(color);
        textView.setTypeface(Typeface.DEFAULT, style);
        textView.setLineSpacing(dp(2), 1f);
        return textView;
    }

    private LinearLayout.LayoutParams matchWrapParams() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private LinearLayout.LayoutParams weightedButtonParams(int leftMargin, int rightMargin, int height) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, height, 1f);
        params.setMargins(leftMargin, 0, rightMargin, 0);
        return params;
    }

    private GradientDrawable makeRoundRect(int fillColor, int radius, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fillColor);
        drawable.setCornerRadius(radius);
        if (strokeColor != 0) {
            drawable.setStroke(dp(1), strokeColor);
        }
        return drawable;
    }

    private GradientDrawable makeRoundRect(int fillColor, int radius, int strokeColor, int strokeWidth, int dashWidth) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fillColor);
        drawable.setCornerRadius(radius);
        if (strokeColor != 0) {
            drawable.setStroke(strokeWidth, strokeColor, dashWidth, dashWidth);
        }
        return drawable;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static class FoodAnalyzer {
        static FoodEstimate analyze(Bitmap bitmap) {
            ImageStats stats = ImageStats.from(bitmap);
            List<FoodEstimate> candidates = new ArrayList<>();
            candidates.add(score("Baklava / şerbetli tatlı", 430, 6.5, 53.0, 22.0,
                    36 + stats.goldenRatio * 88 + stats.texture * 22 + stats.brownRatio * 18
                            - stats.greenRatio * 30 - stats.redRatio * 12,
                    "Altın-sarı, kızarmış ve dokulu alanlar baklava gibi şerbetli tatlılarla daha iyi eşleşti."));
            candidates.add(score("Lahmacun / pide tarzı hamur işi", 230, 9.5, 29.0, 8.0,
                    34 + stats.breadRatio * 72 + stats.redRatio * 28 + stats.greenRatio * 16
                            + stats.texture * 18 - stats.lightNeutral * 14,
                    "İnce hamur rengi, kırmızı-sıcak tonlar ve küçük yeşil/kırmızı alanlar lahmacun-pide olasılığını artırdı."));
            candidates.add(score("Pizza / peynirli hamur işi", 265, 11.0, 32.0, 10.0,
                    32 + stats.redRatio * 42 + stats.lightNeutral * 34 + stats.goldenRatio * 24
                            + stats.texture * 10 - stats.greenRatio * 8,
                    "Kırmızı sos, açık peynir rengi ve kızarmış hamur sinyalleri pizza/peynirli hamur işiyle eşleşti."));
            candidates.add(score("Salata / sebze ağırlıklı tabak", 42, 2.1, 7.0, 0.8,
                    30 + stats.greenRatio * 110 + stats.greenDominance * 35 + stats.brightness * 10
                            - stats.breadRatio * 24 - stats.brownRatio * 16,
                    "Yeşil ve parlak alanlar yüksek göründüğü için sebze ağırlıklı bir tabak varsayıldı."));
            candidates.add(score("Izgara et / tavuk / balık", 185, 24.0, 1.0, 9.0,
                    30 + stats.brownRatio * 55 + stats.darkRatio * 26 + stats.texture * 24
                            - stats.goldenRatio * 20 - stats.lightNeutral * 14,
                    "Koyu kahverengi ve ızgara dokulu alanlar protein ağırlıklı yemeklerle eşleşti."));
            candidates.add(score("Pilav / makarna / ekmek ağırlıklı tabak", 160, 4.8, 31.0, 1.5,
                    31 + stats.lightNeutral * 66 + stats.breadRatio * 32 - stats.redRatio * 12
                            - stats.greenRatio * 12,
                    "Açık nötr renkler ve yumuşak doku karbonhidrat ağırlıklı yiyecekleri işaret ediyor."));
            candidates.add(score("Meyve porsiyonu", 58, 0.7, 14.0, 0.2,
                    28 + stats.colorfulness * 38 + stats.brightness * 15 + stats.redRatio * 22
                            + stats.greenRatio * 12 - stats.breadRatio * 18,
                    "Canlı renkler ve düşük hamur/ızgara sinyali meyve tahminini güçlendirdi."));
            candidates.add(score("Karışık tabak", 145, 9.0, 16.0, 5.0,
                    40 + stats.colorfulness * 12 + stats.texture * 10 - Math.abs(stats.greenRatio - stats.redRatio) * 16,
                    "Görüntü tek bir kategoriye net ayrılmadığında dengeli karışık tabak varsayılır."));

            FoodEstimate best = candidates.get(0);
            for (FoodEstimate candidate : candidates) {
                if (candidate.rawScore > best.rawScore) {
                    best = candidate;
                }
            }
            int confidence = Math.max(55, Math.min(93, (int) Math.round(best.rawScore)));
            return new FoodEstimate(best.name, best.caloriesPer100g, best.proteinPer100g,
                    best.carbsPer100g, best.fatPer100g, confidence, best.note, best.rawScore);
        }

        private static FoodEstimate score(String name, double calories, double protein, double carbs,
                                          double fat, double rawScore, String note) {
            return new FoodEstimate(name, calories, protein, carbs, fat, 0, note, rawScore);
        }
    }

    private static class AiFoodClient {
        static FoodEstimate analyze(Bitmap bitmap, String endpoint) throws Exception {
            String base64Image = encodeJpeg(bitmap);
            JSONObject requestJson = new JSONObject();
            requestJson.put("imageBase64", base64Image);
            requestJson.put("locale", "tr-TR");

            HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(18000);
            connection.setReadTimeout(45000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Accept", "application/json");

            try (OutputStream output = connection.getOutputStream()) {
                byte[] bytes = requestJson.toString().getBytes("UTF-8");
                output.write(bytes);
            }

            int code = connection.getResponseCode();
            InputStream responseStream = code >= 200 && code < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();
            String responseBody = readFully(responseStream);
            connection.disconnect();
            if (code < 200 || code >= 300) {
                throw new IllegalStateException(responseBody);
            }

            JSONObject responseJson = new JSONObject(responseBody);
            int portion = clampInt(responseJson.optInt("portion_grams", 250), 50, 500);
            int calories = Math.max(0, responseJson.optInt("calories", 0));
            double protein = Math.max(0, responseJson.optDouble("protein_g", 0));
            double carbs = Math.max(0, responseJson.optDouble("carbs_g", 0));
            double fat = Math.max(0, responseJson.optDouble("fat_g", 0));
            double factor = 100.0 / portion;
            String foodName = responseJson.optString("food_name", "AI yemek analizi");
            int confidence = (int) Math.round(responseJson.optDouble("confidence", 0.75) * 100);
            String notes = responseJson.optString("notes", "OpenAI görsel analizi ile tahmini değerler hesaplandı.");
            return new FoodEstimate(
                    foodName,
                    calories * factor,
                    protein * factor,
                    carbs * factor,
                    fat * factor,
                    clampInt(confidence, 55, 98),
                    "AI analizi: " + notes,
                    confidence,
                    portion
            );
        }

        private static String encodeJpeg(Bitmap bitmap) {
            int maxSide = Math.max(bitmap.getWidth(), bitmap.getHeight());
            Bitmap scaled = bitmap;
            if (maxSide > 1024) {
                float scale = 1024f / maxSide;
                scaled = Bitmap.createScaledBitmap(
                        bitmap,
                        Math.max(1, Math.round(bitmap.getWidth() * scale)),
                        Math.max(1, Math.round(bitmap.getHeight() * scale)),
                        true
                );
            }
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            scaled.compress(Bitmap.CompressFormat.JPEG, 82, stream);
            if (scaled != bitmap) {
                scaled.recycle();
            }
            return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP);
        }

        private static String readFully(InputStream stream) throws Exception {
            if (stream == null) {
                return "";
            }
            StringBuilder builder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
            }
            return builder.toString();
        }
    }

    private static class ImageStats {
        final double brightness;
        final double greenDominance;
        final double redDominance;
        final double warmDominance;
        final double lightNeutral;
        final double colorfulness;
        final double texture;
        final double greenRatio;
        final double redRatio;
        final double goldenRatio;
        final double brownRatio;
        final double breadRatio;
        final double darkRatio;

        ImageStats(double brightness, double greenDominance, double redDominance, double warmDominance,
                   double lightNeutral, double colorfulness, double texture, double greenRatio,
                   double redRatio, double goldenRatio, double brownRatio, double breadRatio,
                   double darkRatio) {
            this.brightness = brightness;
            this.greenDominance = greenDominance;
            this.redDominance = redDominance;
            this.warmDominance = warmDominance;
            this.lightNeutral = lightNeutral;
            this.colorfulness = colorfulness;
            this.texture = texture;
            this.greenRatio = greenRatio;
            this.redRatio = redRatio;
            this.goldenRatio = goldenRatio;
            this.brownRatio = brownRatio;
            this.breadRatio = breadRatio;
            this.darkRatio = darkRatio;
        }

        static ImageStats from(Bitmap bitmap) {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int stepX = Math.max(1, width / 64);
            int stepY = Math.max(1, height / 64);
            double brightness = 0;
            double greenDominance = 0;
            double redDominance = 0;
            double warmDominance = 0;
            double lightNeutral = 0;
            double colorfulness = 0;
            double texture = 0;
            double greenRatio = 0;
            double redRatio = 0;
            double goldenRatio = 0;
            double brownRatio = 0;
            double breadRatio = 0;
            double darkRatio = 0;
            double previousBrightness = -1;
            int count = 0;
            float[] hsv = new float[3];

            for (int y = 0; y < height; y += stepY) {
                for (int x = 0; x < width; x += stepX) {
                    int color = bitmap.getPixel(x, y);
                    double red = Color.red(color) / 255.0;
                    double green = Color.green(color) / 255.0;
                    double blue = Color.blue(color) / 255.0;
                    double pixelBrightness = (red + green + blue) / 3.0;
                    double max = Math.max(red, Math.max(green, blue));
                    double min = Math.min(red, Math.min(green, blue));
                    Color.RGBToHSV(Color.red(color), Color.green(color), Color.blue(color), hsv);
                    double hue = hsv[0];
                    double saturation = hsv[1];
                    double value = hsv[2];

                    brightness += pixelBrightness;
                    greenDominance += Math.max(0, green - Math.max(red, blue));
                    redDominance += Math.max(0, red - Math.max(green, blue));
                    warmDominance += Math.max(0, ((red + green) / 2.0) - blue);
                    lightNeutral += pixelBrightness > 0.58 && (max - min) < 0.22 ? 1 : 0;
                    colorfulness += max - min;
                    greenRatio += hue >= 70 && hue <= 165 && saturation > 0.22 && value > 0.18 ? 1 : 0;
                    redRatio += (hue <= 24 || hue >= 342) && saturation > 0.24 && value > 0.22 ? 1 : 0;
                    goldenRatio += hue > 28 && hue < 62 && saturation > 0.22 && value > 0.35 ? 1 : 0;
                    brownRatio += hue >= 12 && hue <= 48 && saturation > 0.20 && value > 0.16 && value < 0.68 ? 1 : 0;
                    breadRatio += hue >= 24 && hue <= 58 && saturation > 0.12 && saturation < 0.58 && value > 0.42 ? 1 : 0;
                    darkRatio += value < 0.34 && saturation > 0.12 ? 1 : 0;
                    if (previousBrightness >= 0) {
                        texture += Math.abs(pixelBrightness - previousBrightness);
                    }
                    previousBrightness = pixelBrightness;
                    count++;
                }
            }

            if (count == 0) {
                count = 1;
            }
            return new ImageStats(
                    brightness / count,
                    clamp(greenDominance / count * 3.0),
                    clamp(redDominance / count * 3.0),
                    clamp(warmDominance / count * 1.4),
                    clamp(lightNeutral / count),
                    clamp(colorfulness / count * 1.8),
                    clamp(texture / count * 4.0),
                    clamp(greenRatio / count),
                    clamp(redRatio / count),
                    clamp(goldenRatio / count),
                    clamp(brownRatio / count),
                    clamp(breadRatio / count),
                    clamp(darkRatio / count)
            );
        }

        private static double clamp(double value) {
            return Math.max(0, Math.min(1, value));
        }
    }

    private static class FoodEstimate {
        final String name;
        final double caloriesPer100g;
        final double proteinPer100g;
        final double carbsPer100g;
        final double fatPer100g;
        final int confidence;
        final String note;
        final double rawScore;
        final int portionGrams;

        FoodEstimate(String name, double caloriesPer100g, double proteinPer100g, double carbsPer100g,
                     double fatPer100g, int confidence, String note, double rawScore) {
            this(name, caloriesPer100g, proteinPer100g, carbsPer100g, fatPer100g, confidence, note, rawScore, 250);
        }

        FoodEstimate(String name, double caloriesPer100g, double proteinPer100g, double carbsPer100g,
                     double fatPer100g, int confidence, String note, double rawScore, int portionGrams) {
            this.name = name;
            this.caloriesPer100g = caloriesPer100g;
            this.proteinPer100g = proteinPer100g;
            this.carbsPer100g = carbsPer100g;
            this.fatPer100g = fatPer100g;
            this.confidence = confidence;
            this.note = note;
            this.rawScore = rawScore;
            this.portionGrams = portionGrams;
        }
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}

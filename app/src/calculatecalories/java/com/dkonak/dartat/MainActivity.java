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
import android.widget.Button;
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

    private ImageView foodImage;
    private TextView statusText;
    private LinearLayout lockedPanel;
    private LinearLayout resultPanel;
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

    private FoodEstimate currentEstimate;
    private Bitmap currentFoodBitmap;
    private int portionGrams = 250;
    private boolean resultUnlocked;
    private boolean aiAnalysisInProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(createContentView());
        showEmptyState();
    }

    private View createContentView() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(Color.rgb(247, 249, 244));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(24));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView title = text("Calculate Calories", 30, Color.rgb(27, 45, 35), Typeface.BOLD);
        root.addView(title);

        TextView subtitle = text("Yemeğinin fotoğrafını çek, porsiyonu seç ve yaklaşık kalori, protein, karbonhidrat ve yağ değerlerini gör.", 15, Color.rgb(84, 101, 89), Typeface.NORMAL);
        subtitle.setPadding(0, dp(8), 0, dp(18));
        root.addView(subtitle);

        foodImage = new ImageView(this);
        foodImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        foodImage.setBackground(makeRoundRect(Color.rgb(229, 235, 222), dp(18), 0));
        root.addView(foodImage, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(260)
        ));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(16), 0, dp(14));
        root.addView(actions);

        Button cameraButton = actionButton("Fotoğraf çek");
        cameraButton.setOnClickListener(v -> openCamera());
        actions.addView(cameraButton, weightedButtonParams(0, dp(8)));

        Button galleryButton = actionButton("Galeriden seç");
        galleryButton.setOnClickListener(v -> openGallery());
        actions.addView(galleryButton, weightedButtonParams(dp(8), 0));

        statusText = text("", 14, Color.rgb(94, 103, 96), Typeface.NORMAL);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(dp(12), dp(10), dp(12), dp(10));
        root.addView(statusText, matchWrapParams());

        lockedPanel = new LinearLayout(this);
        lockedPanel.setOrientation(LinearLayout.VERTICAL);
        lockedPanel.setPadding(dp(18), dp(18), dp(18), dp(18));
        lockedPanel.setBackground(makeRoundRect(Color.WHITE, dp(18), Color.rgb(218, 225, 213)));
        LinearLayout.LayoutParams lockedParams = matchWrapParams();
        lockedParams.setMargins(0, dp(16), 0, 0);
        root.addView(lockedPanel, lockedParams);

        TextView lockedTitle = text("Sonuç hazır", 22, Color.rgb(29, 48, 37), Typeface.BOLD);
        lockedPanel.addView(lockedTitle);

        TextView lockedBody = text("Kalori, protein, karbonhidrat ve yağ tahminini görmek için kısa reklamı izle.", 14, Color.rgb(79, 91, 82), Typeface.NORMAL);
        lockedBody.setPadding(0, dp(6), 0, dp(14));
        lockedPanel.addView(lockedBody);

        watchAdButton = actionButton("Reklam izle ve sonucu gör");
        watchAdButton.setOnClickListener(v -> unlockResultAfterAd());
        lockedPanel.addView(watchAdButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(52)
        ));

        resultPanel = new LinearLayout(this);
        resultPanel.setOrientation(LinearLayout.VERTICAL);
        resultPanel.setPadding(dp(18), dp(18), dp(18), dp(18));
        resultPanel.setBackground(makeRoundRect(Color.WHITE, dp(18), Color.rgb(218, 225, 213)));
        LinearLayout.LayoutParams panelParams = matchWrapParams();
        panelParams.setMargins(0, dp(16), 0, 0);
        root.addView(resultPanel, panelParams);

        foodNameText = text("", 22, Color.rgb(29, 48, 37), Typeface.BOLD);
        resultPanel.addView(foodNameText);

        confidenceText = text("", 14, Color.rgb(92, 103, 94), Typeface.NORMAL);
        confidenceText.setPadding(0, dp(4), 0, dp(14));
        resultPanel.addView(confidenceText);

        LinearLayout macros = new LinearLayout(this);
        macros.setOrientation(LinearLayout.VERTICAL);
        resultPanel.addView(macros);

        caloriesText = addMetric(macros, "Kalori", Color.rgb(215, 89, 48));
        proteinText = addMetric(macros, "Protein", Color.rgb(34, 113, 83));
        carbsText = addMetric(macros, "Karbonhidrat", Color.rgb(55, 95, 164));
        fatText = addMetric(macros, "Yağ", Color.rgb(142, 97, 42));

        portionText = text("", 15, Color.rgb(41, 56, 46), Typeface.BOLD);
        portionText.setPadding(0, dp(18), 0, dp(2));
        resultPanel.addView(portionText);

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

        insightText = text("", 14, Color.rgb(79, 91, 82), Typeface.NORMAL);
        insightText.setPadding(0, dp(14), 0, 0);
        resultPanel.addView(insightText);

        return scrollView;
    }

    private void showEmptyState() {
        foodImage.setImageDrawable(null);
        statusText.setText("Başlamak için tabağı iyi ışıkta, mümkünse üstten çek. Fotoğraf hazır olunca sonucu reklamdan sonra açacağız.");
        currentEstimate = null;
        currentFoodBitmap = null;
        resultUnlocked = false;
        aiAnalysisInProgress = false;
        lockedPanel.setVisibility(View.GONE);
        resultPanel.setVisibility(View.GONE);
        renderEstimate();
    }

    private void openCamera() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
            return;
        }
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this, "Kamera uygulaması bulunamadı.", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "Fotoğraf çekmek için kamera izni gerekiyor.", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, "Görsel okunamadı.", Toast.LENGTH_SHORT).show();
                return;
            }
            foodImage.setImageBitmap(bitmap);
            currentFoodBitmap = bitmap;
            currentEstimate = FoodAnalyzer.analyze(bitmap);
            resultUnlocked = false;
            aiAnalysisInProgress = false;
            lockedPanel.setVisibility(View.VISIBLE);
            resultPanel.setVisibility(View.GONE);
            statusText.setText("Analiz tamamlandı. Sonucu görmek için reklam izlemen gerekiyor.");
            renderEstimate();
        } catch (Exception exception) {
            Toast.makeText(this, "Fotoğraf analiz edilirken sorun oluştu.", Toast.LENGTH_SHORT).show();
        }
    }

    private void unlockResultAfterAd() {
        if (currentEstimate == null) {
            Toast.makeText(this, "Önce bir yemek fotoğrafı çek.", Toast.LENGTH_SHORT).show();
            return;
        }
        watchAdButton.setEnabled(false);
        watchAdButton.setText("AI analiz ediyor...");
        requestAiEstimate();
    }

    private void renderEstimate() {
        if (currentEstimate == null) {
            foodNameText.setText("Henüz analiz yok");
            confidenceText.setText("Fotoğraf çekildiğinde burada tahmini yemek türü görünecek.");
            caloriesText.setText("0 kcal");
            proteinText.setText("0 g");
            carbsText.setText("0 g");
            fatText.setText("0 g");
            portionText.setText("Porsiyon: " + portionGrams + " g");
            insightText.setText("Not: Bu uygulama tıbbi/nutrisyonel ölçüm yapmaz; yaklaşık fikir vermek için çalışır.");
            return;
        }
        if (!resultUnlocked) {
            watchAdButton.setEnabled(!aiAnalysisInProgress);
            watchAdButton.setText(aiAnalysisInProgress ? "AI analiz ediyor..." : "Reklam izle ve sonucu gör");
            return;
        }

        double factor = portionGrams / 100.0;
        int calories = (int) Math.round(currentEstimate.caloriesPer100g * factor);
        int protein = (int) Math.round(currentEstimate.proteinPer100g * factor);
        int carbs = (int) Math.round(currentEstimate.carbsPer100g * factor);
        int fat = (int) Math.round(currentEstimate.fatPer100g * factor);

        foodNameText.setText(currentEstimate.name);
        confidenceText.setText(String.format(Locale.getDefault(), "Tahmini eşleşme: %%%d", currentEstimate.confidence));
        caloriesText.setText(calories + " kcal");
        proteinText.setText(protein + " g");
        carbsText.setText(carbs + " g");
        fatText.setText(fat + " g");
        portionText.setText("Porsiyon: " + portionGrams + " g");
        insightText.setText(currentEstimate.note);
    }

    private void requestAiEstimate() {
        if (currentFoodBitmap == null) {
            unlockWithCurrentEstimate("Fotoğraf okunamadı. Yerel tahmini gösteriyorum.");
            return;
        }
        aiAnalysisInProgress = true;
        statusText.setText("Reklam tamamlandı. Fotoğraf AI ile analiz ediliyor...");
        new Thread(() -> {
            try {
                FoodEstimate aiEstimate = AiFoodClient.analyze(currentFoodBitmap, BuildConfig.CALORIE_AI_ENDPOINT);
                runOnUiThread(() -> {
                    currentEstimate = aiEstimate;
                    portionGrams = aiEstimate.portionGrams;
                    portionSeekBar.setProgress(Math.max(0, Math.min(450, portionGrams - 50)));
                    unlockWithCurrentEstimate("AI sonucu hazır. Porsiyonu değiştirerek değerleri güncelleyebilirsin.");
                });
            } catch (Exception exception) {
                runOnUiThread(() -> unlockWithCurrentEstimate(
                        "AI sunucusuna ulaşılamadı. Şimdilik telefondaki yedek tahmini gösteriyorum."
                ));
            }
        }).start();
    }

    private void unlockWithCurrentEstimate(String message) {
        aiAnalysisInProgress = false;
        resultUnlocked = true;
        lockedPanel.setVisibility(View.GONE);
        resultPanel.setVisibility(View.VISIBLE);
        statusText.setText(message);
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

    private Button actionButton(String label) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(label);
        button.setTextSize(15);
        button.setTextColor(Color.WHITE);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackground(makeRoundRect(Color.rgb(41, 119, 84), dp(14), 0));
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

    private LinearLayout.LayoutParams weightedButtonParams(int leftMargin, int rightMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(52), 1f);
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

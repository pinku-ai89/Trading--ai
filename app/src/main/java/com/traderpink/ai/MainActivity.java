package com.traderpink.ai;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private static final String API =
            "https://crimson-grass-f881.bijondebnath51.workers.dev/";

    private TextView signal;
    private TextView confidence;
    private TextView info;

    private final ExecutorService pool =
            Executors.newSingleThreadExecutor();

    private final Handler handler =
            new Handler();

    private boolean destroyed = false;

    private final Runnable autoUpdate = new Runnable() {
        @Override
        public void run() {
            if (destroyed) return;

            loadSignal();

            handler.postDelayed(this, 10000);
        }
    };

    private TextView makeText(
            String text,
            float size,
            int color) {

        TextView v = new TextView(this);

        v.setText(text);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setPadding(0, 8, 0, 8);

        return v;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL);

        root.setGravity(
                Gravity.CENTER_HORIZONTAL);

        root.setPadding(
                20,
                20,
                20,
                20);

        root.setBackgroundColor(
                Color.rgb(11, 16, 32));

        TextView title =
                makeText(
                        "🤖 Trader Pink AI 📈",
                        26,
                        Color.WHITE);

        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView subtitle =
                makeText(
                        "EURUSD Smart 1 Minute Signal Engine",
                        16,
                        Color.LTGRAY);

        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle);

        TextView market =
                makeText(
                        "EURUSD • 1 MIN",
                        19,
                        Color.WHITE);

        market.setGravity(Gravity.CENTER);
        root.addView(market);

        signal =
                makeText(
                        "WAIT",
                        48,
                        Color.WHITE);

        signal.setGravity(Gravity.CENTER);
        root.addView(signal);

        confidence =
                makeText(
                        "Confidence: 0%",
                        20,
                        Color.WHITE);

        confidence.setGravity(Gravity.CENTER);
        root.addView(confidence);

        info =
                makeText(
                        "Loading...",
                        15,
                        Color.WHITE);

        root.addView(info);

        Button update =
                new Button(this);

        update.setText(
                "🔄 UPDATE SIGNAL");

        update.setOnClickListener(
                v -> loadSignal());

        root.addView(update);

        Button floating =
                new Button(this);

        floating.setText(
                "🤖 START FLOATING BOT");

        floating.setOnClickListener(
                v -> startFloatingBot());

        root.addView(floating);

        setContentView(root);

        loadSignal();

        handler.postDelayed(
                autoUpdate,
                10000);
    }

    private void loadSignal() {

        pool.execute(() -> {

            HttpURLConnection connection = null;

            try {

                URL url =
                        new URL(
                                API +
                                "?t=" +
                                System.currentTimeMillis());

                connection =
                        (HttpURLConnection)
                                url.openConnection();

                connection.setRequestMethod("GET");

                connection.setConnectTimeout(
                        10000);

                connection.setReadTimeout(
                        10000);

                connection.setUseCaches(false);

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        connection.getInputStream()));

                StringBuilder response =
                        new StringBuilder();

                String line;

                while (
                        (line = reader.readLine())
                                != null) {

                    response.append(line);
                }

                reader.close();

                JSONObject data =
                        new JSONObject(
                                response.toString());

                String finalSignal =
                        data.optString(
                                "signal",
                                "WAIT");

                int finalConfidence =
                        data.optInt(
                                "confidence",
                                0);

                String trend =
                        data.optString(
                                "trend",
                                "--");

                String closedTime =
                        data.optString(
                                "closed_candle_time",
                                "--");

                String nextTime =
                        data.optString(
                                "next_candle_time",
                                "--");

                String analysisMode =
                        data.optString(
                                "analysis_mode",
                                "--");

                String reason =
                        data.optString(
                                "decision_reason",
                                "");

                JSONObject candle =
                        data.optJSONObject(
                                "candle");

                String candleDirection =
                        "--";

                if (candle != null) {

                    candleDirection =
                            candle.optString(
                                    "direction",
                                    "--");
                }

                String finalText =
                        "Trend: " +
                        trend +

                        "\nCandle: " +
                        candleDirection +

                        "\n\nSignal Candle:\n" +
                        closedTime +

                        "\n\nNext Candle:\n" +
                        nextTime +

                        "\n\nAnalysis:\n" +
                        analysisMode;

                if (!reason.isEmpty()) {

                    finalText +=
                            "\n\nReason:\n" +
                            reason;
                }

                final String displayText =
                        finalText;

                runOnUiThread(() -> {

                    signal.setText(
                            finalSignal);

                    confidence.setText(
                            "Confidence: " +
                            finalConfidence +
                            "%");

                    info.setText(
                            displayText);

                    if (finalSignal.equalsIgnoreCase(
                            "BUY")) {

                        signal.setTextColor(
                                Color.rgb(
                                        50,
                                        220,
                                        120));

                    } else if (
                            finalSignal.equalsIgnoreCase(
                                    "SELL")) {

                        signal.setTextColor(
                                Color.rgb(
                                        255,
                                        80,
                                        100));

                    } else {

                        signal.setTextColor(
                                Color.WHITE);
                    }
                });

            } catch (Exception e) {

                runOnUiThread(() -> {

                    signal.setText("WAIT");

                    confidence.setText(
                            "Connection Error");

                    info.setText(
                            "Worker connection failed.\n" +
                            "Please check internet connection.");

                    signal.setTextColor(
                            Color.WHITE);
                });

            } finally {

                if (connection != null) {

                    connection.disconnect();
                }
            }
        });
    }

    private void startFloatingBot() {

        if (
                android.os.Build.VERSION.SDK_INT >= 23 &&
                !Settings.canDrawOverlays(this)) {

            Intent intent =
                    new Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse(
                                    "package:" +
                                    getPackageName()));

            startActivity(intent);

            return;
        }

        Intent serviceIntent =
                new Intent(
                        this,
                        FloatingService.class);

        if (
                android.os.Build.VERSION.SDK_INT >= 26) {

            startForegroundService(
                    serviceIntent);

        } else {

            startService(
                    serviceIntent);
        }
    }

    @Override
    protected void onDestroy() {

        destroyed = true;

        handler.removeCallbacks(
                autoUpdate);

        pool.shutdownNow();

        super.onDestroy();
    }
}

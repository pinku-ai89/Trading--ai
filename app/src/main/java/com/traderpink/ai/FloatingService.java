package com.traderpink.ai;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

import org.json.*;

import java.io.*;
import java.net.*;

public class FloatingService extends Service {

    private static final String API =
            "https://crimson-grass-f881.bijondebnath51.workers.dev/";

    private WindowManager windowManager;
    private View floatingView;
    private TextView signalView;
    private TextView infoView;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private final Runnable updater =
            new Runnable() {

        @Override
        public void run() {

            updateSignal();

            handler.postDelayed(
                    this,
                    10000);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId) {

        createNotificationChannel();

        startForeground(
                1001,
                createNotification());

        showFloatingWindow();

        updateSignal();

        handler.removeCallbacks(
                updater);

        handler.postDelayed(
                updater,
                10000);

        return START_STICKY;
    }

    private void showFloatingWindow() {

        if (floatingView != null) {
            return;
        }

        LinearLayout main =
                new LinearLayout(this);

        main.setOrientation(
                LinearLayout.VERTICAL);

        main.setPadding(
                18,
                14,
                18,
                14);

        main.setBackgroundColor(
                Color.rgb(
                        20,
                        25,
                        45));

        TextView title =
                new TextView(this);

        title.setText(
                "🤖 Trader Pink AI");

        title.setTextColor(
                Color.WHITE);

        title.setTextSize(18);

        title.setGravity(
                Gravity.CENTER);

        main.addView(title);

        TextView market =
                new TextView(this);

        market.setText(
                "EURUSD • 1 MIN");

        market.setTextColor(
                Color.LTGRAY);

        market.setTextSize(14);

        market.setGravity(
                Gravity.CENTER);

        main.addView(market);

        signalView =
                new TextView(this);

        signalView.setText(
                "WAIT");

        signalView.setTextColor(
                Color.WHITE);

        signalView.setTextSize(32);

        signalView.setGravity(
                Gravity.CENTER);

        signalView.setPadding(
                0,
                10,
                0,
                5);

        main.addView(signalView);

        infoView =
                new TextView(this);

        infoView.setText(
                "Loading...");

        infoView.setTextColor(
                Color.WHITE);

        infoView.setTextSize(13);

        main.addView(infoView);

        Button close =
                new Button(this);

        close.setText(
                "✕ CLOSE");

        close.setOnClickListener(
                v -> stopSelf());

        main.addView(close);

        floatingView = main;

        windowManager =
                (WindowManager)
                        getSystemService(
                                WINDOW_SERVICE);

        int type;

        if (
                Build.VERSION.SDK_INT >= 26) {

            type =
                    WindowManager.LayoutParams
                            .TYPE_APPLICATION_OVERLAY;

        } else {

            type =
                    WindowManager.LayoutParams
                            .TYPE_PHONE;
        }

        WindowManager.LayoutParams params =
                new WindowManager.LayoutParams(
                        340,
                        WindowManager.LayoutParams
                                .WRAP_CONTENT,
                        type,
                        WindowManager.LayoutParams
                                .FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams
                                .FLAG_LAYOUT_NO_LIMITS,
                        PixelFormat.TRANSLUCENT);

        params.gravity =
                Gravity.TOP |
                Gravity.RIGHT;

        params.x = 10;
        params.y = 120;

        try {

            windowManager.addView(
                    floatingView,
                    params);

        } catch (Exception e) {

            floatingView = null;
        }
    }

    private void updateSignal() {

        new Thread(() -> {

            HttpURLConnection connection =
                    null;

            try {

                URL url =
                        new URL(
                                API +
                                "?t=" +
                                System.currentTimeMillis());

                connection =
                        (HttpURLConnection)
                                url.openConnection();

                connection.setRequestMethod(
                        "GET");

                connection.setConnectTimeout(
                        10000);

                connection.setReadTimeout(
                        10000);

                connection.setUseCaches(
                        false);

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        connection.getInputStream()));

                StringBuilder result =
                        new StringBuilder();

                String line;

                while (
                        (line = reader.readLine())
                                != null) {

                    result.append(line);
                }

                reader.close();

                JSONObject data =
                        new JSONObject(
                                result.toString());

                String finalSignal =
                        data.optString(
                                "signal",
                                "WAIT");

                int confidence =
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

                String text =
                        "Confidence: " +
                        confidence +
                        "%\n" +

                        "Trend: " +
                        trend +
                        "\n" +

                        "Candle: " +
                        candleDirection +
                        "\n\n" +

                        "Signal Candle:\n" +
                        closedTime +
                        "\n\n" +

                        "Next Candle:\n" +
                        nextTime;

                if (!reason.isEmpty()) {

                    text +=
                            "\n\n" +
                            reason;
                }

                final String finalText =
                        text;

                new Handler(
                        Looper.getMainLooper())
                        .post(() -> {

                    if (signalView == null) {
                        return;
                    }

                    signalView.setText(
                            finalSignal);

                    infoView.setText(
                            finalText);

                    if (
                            finalSignal.equalsIgnoreCase(
                                    "BUY")) {

                        signalView.setTextColor(
                                Color.rgb(
                                        50,
                                        220,
                                        120));

                    } else if (
                            finalSignal.equalsIgnoreCase(
                                    "SELL")) {

                        signalView.setTextColor(
                                Color.rgb(
                                        255,
                                        80,
                                        100));

                    } else {

                        signalView.setTextColor(
                                Color.WHITE);
                    }
                });

            } catch (Exception e) {

                new Handler(
                        Looper.getMainLooper())
                        .post(() -> {

                    if (signalView == null) {
                        return;
                    }

                    signalView.setText(
                            "WAIT");

                    signalView.setTextColor(
                            Color.WHITE);

                    infoView.setText(
                            "Connection Error");
                });

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }

        }).start();
    }

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= 26) {

            NotificationChannel channel =
                    new NotificationChannel(
                            "trader_pink_ai",
                            "Trader Pink AI",
                            NotificationManager
                                    .IMPORTANCE_LOW);

            NotificationManager manager =
                    (NotificationManager)
                            getSystemService(
                                    NOTIFICATION_SERVICE);

            if (manager != null) {

                manager.createNotificationChannel(
                        channel);
            }
        }
    }

    private Notification createNotification() {

        Intent intent =
                new Intent(
                        this,
                        MainActivity.class);

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        intent,
                        PendingIntent.FLAG_IMMUTABLE |
                        PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder;

        if (Build.VERSION.SDK_INT >= 26) {

            builder =
                    new Notification.Builder(
                            this,
                            "trader_pink_ai");

        } else {

            builder =
                    new Notification.Builder(
                            this);
        }

        return builder
                .setContentTitle(
                        "Trader Pink AI 🤖📈")
                .setContentText(
                        "EURUSD 1M Signal Engine চলছে")
                .setSmallIcon(
                        android.R.drawable
                                .ic_dialog_info)
                .setContentIntent(
                        pendingIntent)
                .setOngoing(true)
                .build();
    }

    @Override
    public void onDestroy() {

        handler.removeCallbacks(
                updater);

        if (
                floatingView != null &&
                windowManager != null) {

            try {

                windowManager.removeView(
                        floatingView);

            } catch (Exception ignored) {
            }
        }

        floatingView = null;

        super.onDestroy();
    }
}

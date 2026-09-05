package com.traderpink.ai;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.*;
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

    private WindowManager.LayoutParams windowParams;

    private float downRawX;
    private float downRawY;
    private int startX;
    private int startY;

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

        handler.removeCallbacks(updater);

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
                12,
                18,
                14);

        main.setBackgroundColor(
                Color.rgb(
                        20,
                        25,
                        45));

        /*
         * TOP BAR
         * Title + small X button
         */
        LinearLayout topBar =
                new LinearLayout(this);

        topBar.setOrientation(
                LinearLayout.HORIZONTAL);

        topBar.setGravity(
                Gravity.CENTER_VERTICAL);

        TextView title =
                new TextView(this);

        title.setText(
                "🤖 Trader Pink AI");

        title.setTextColor(
                Color.WHITE);

        title.setTextSize(17);

        title.setGravity(
                Gravity.CENTER_VERTICAL);

        topBar.addView(
                title,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams
                                .WRAP_CONTENT,
                        1));

        TextView close =
                new TextView(this);

        close.setText("×");

        close.setTextColor(
                Color.WHITE);

        close.setTextSize(28);

        close.setGravity(
                Gravity.CENTER);

        close.setPadding(
                10,
                0,
                4,
                0);

        close.setOnClickListener(
                v -> {

                    handler.removeCallbacks(
                            updater);

                    stopSelf();
                });

        topBar.addView(
                close,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams
                                .WRAP_CONTENT,
                        LinearLayout.LayoutParams
                                .WRAP_CONTENT));

        main.addView(topBar);

        /*
         * MARKET
         */
        TextView market =
                new TextView(this);

        market.setText(
                "EURUSD • 1 MIN");

        market.setTextColor(
                Color.LTGRAY);

        market.setTextSize(14);

        market.setGravity(
                Gravity.CENTER);

        market.setPadding(
                0,
                3,
                0,
                3);

        main.addView(market);

        /*
         * SIGNAL LABEL
         */
        TextView signalLabel =
                new TextView(this);

        signalLabel.setText(
                "SIGNAL");

        signalLabel.setTextColor(
                Color.LTGRAY);

        signalLabel.setTextSize(12);

        signalLabel.setGravity(
                Gravity.CENTER);

        main.addView(signalLabel);

        /*
         * FINAL 1M SIGNAL
         */
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
                5,
                0,
                5);

        main.addView(signalView);

        /*
         * INFO
         */
        infoView =
                new TextView(this);

        infoView.setText(
                "Confidence: --%\n" +
                "Candle: --");

        infoView.setTextColor(
                Color.WHITE);

        infoView.setTextSize(13);

        infoView.setGravity(
                Gravity.CENTER);

        main.addView(infoView);

        /*
         * NEXT SIGNAL BUTTON
         */
        Button next =
                new Button(this);

        next.setText(
                "NEXT SIGNAL");

        next.setOnClickListener(
                v -> updateSignal());

        main.addView(next);

        floatingView = main;

        /*
         * WINDOW MANAGER
         */
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

        windowParams =
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

        windowParams.gravity =
                Gravity.TOP |
                Gravity.RIGHT;

        windowParams.x = 10;
        windowParams.y = 120;

        /*
         * DRAG / MOVE
         */
        View.OnTouchListener dragListener =
                new View.OnTouchListener() {

            @Override
            public boolean onTouch(
                    View v,
                    MotionEvent event) {

                switch (event.getAction()) {

                    case MotionEvent.ACTION_DOWN:

                        downRawX =
                                event.getRawX();

                        downRawY =
                                event.getRawY();

                        startX =
                                windowParams.x;

                        startY =
                                windowParams.y;

                        return true;

                    case MotionEvent.ACTION_MOVE:

                        int dx =
                                (int)
                                (event.getRawX()
                                - downRawX);

                        int dy =
                                (int)
                                (event.getRawY()
                                - downRawY);

                        /*
                         * Because the window is anchored
                         * to TOP + RIGHT, X movement
                         * is inverted.
                         */
                        windowParams.x =
                                startX - dx;

                        windowParams.y =
                                startY + dy;

                        if (
                                windowManager != null &&
                                floatingView != null) {

                            try {

                                windowManager.updateViewLayout(
                                        floatingView,
                                        windowParams);

                            } catch (Exception ignored) {
                            }
                        }

                        return true;

                    case MotionEvent.ACTION_UP:

                        return true;
                }

                return false;
            }
        };

        /*
         * পুরো Floating panel ধরে
         * drag করা যাবে।
         */
        main.setOnTouchListener(
                dragListener);

        try {

            windowManager.addView(
                    floatingView,
                    windowParams);

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
                                        connection
                                                .getInputStream()));

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

                /*
                 * FINAL SIGNAL
                 *
                 * Worker-এর final signal-ই
                 * Floating Bot দেখাবে।
                 */
                String finalSignal =
                        data.optString(
                                "signal",
                                "WAIT");

                int confidence =
                        data.optInt(
                                "confidence",
                                0);

                String closedTime =
                        data.optString(
                                "closed_candle_time",
                                "--");

                String nextTime =
                        data.optString(
                                "next_candle_time",
                                "--");

                String trend =
                        data.optString(
                                "trend",
                                "--");

                String reason =
                        data.optString(
                                "decision_reason",
                                "");

                String text =
                        "Confidence: " +
                        confidence +
                        "%\n\n" +

                        "Candle: " +
                        closedTime +
                        "\n\n" +

                        "Next: " +
                        nextTime +
                        "\n\n" +

                        "Trend: " +
                        trend;

                if (!reason.isEmpty()) {

                    text +=
                            "\n\n" +
                            reason;
                }

                final String displayText =
                        text;

                new Handler(
                        Looper.getMainLooper())
                        .post(() -> {

                    if (
                            signalView == null ||
                            infoView == null) {

                        return;
                    }

                    signalView.setText(
                            finalSignal);

                    infoView.setText(
                            displayText);

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

                    if (
                            signalView == null ||
                            infoView == null) {

                        return;
                    }

                    signalView.setText(
                            "WAIT");

                    signalView.setTextColor(
                            Color.WHITE);

                    infoView.setText(
                            "Confidence: --%\n" +
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

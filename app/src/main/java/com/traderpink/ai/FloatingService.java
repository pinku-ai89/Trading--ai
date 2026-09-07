package com.traderpink.ai;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

public class FloatingService extends Service {

    private static final String API_URL =
            "https://crimson-grass-f881.bijondebnath51.workers.dev/";

    private WindowManager windowManager;
    private View floatingView;
    private TextView signalView;
    private TextView confidenceView;
    private TextView trendView;
    private TextView candleTimeView;
    private TextView countdownView;
    private CandleView candleView;

    private final Handler handler = new Handler();

    private double previousOpen = 0;
    private double previousHigh = 0;
    private double previousLow = 0;
    private double previousClose = 0;

    private double liveOpen = 0;
    private double liveHigh = 0;
    private double liveLow = 0;
    private double liveClose = 0;

    private boolean hasRealCandleData = false;

    private long lastClosedEpoch = 0;

    // ----------------------------------------------------
    // SIGNAL UPDATE
    // ----------------------------------------------------

    private final Runnable updater = new Runnable() {
        @Override
        public void run() {
            updateSignal();
            handler.postDelayed(this, 10000);
        }
    };

    // ----------------------------------------------------
    // REAL MARKET CLOCK
    // ----------------------------------------------------

    private final Runnable candleTimer = new Runnable() {
        @Override
        public void run() {

            updateCandleClock();

            if (candleView != null) {
                candleView.invalidate();
            }

            handler.postDelayed(this, 1000);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        windowManager =
                (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId) {

        createNotificationChannel();

        Notification notification =
                new Notification.Builder(
                        this,
                        "trader_pink_ai")
                        .setContentTitle("Trader Pink AI")
                        .setContentText("Floating AI Signal")
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .build();

        startForeground(1001, notification);

        if (floatingView == null) {
            showFloatingWindow();
        }

        updateSignal();

        handler.removeCallbacks(updater);
        handler.removeCallbacks(candleTimer);

        handler.post(updater);
        handler.post(candleTimer);

        return START_STICKY;
    }

    // ----------------------------------------------------
    // FLOATING UI
    // ----------------------------------------------------

    private void showFloatingWindow() {

        LinearLayout main =
                new LinearLayout(this);

        main.setOrientation(
                LinearLayout.VERTICAL);

        main.setPadding(
                12, 9, 12, 10);

        main.setBackgroundColor(
                Color.rgb(20, 25, 45));

        // -----------------------------
        // TOP BAR
        // -----------------------------

        LinearLayout top =
                new LinearLayout(this);

        top.setOrientation(
                LinearLayout.HORIZONTAL);

        top.setGravity(
                Gravity.CENTER_VERTICAL);

        TextView title =
                new TextView(this);

        title.setText(
                "🤖 Trader Pink AI");

        title.setTextColor(
                Color.WHITE);

        title.setTextSize(13);

        LinearLayout.LayoutParams titleParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1);

        top.addView(
                title,
                titleParams);

        TextView close =
                new TextView(this);

        close.setText("×");

        close.setTextColor(
                Color.WHITE);

        close.setTextSize(20);

        close.setGravity(
                Gravity.CENTER);

        close.setPadding(
                8, 0, 2, 0);

        close.setOnClickListener(
                v -> stopSelf());

        top.addView(close);

        main.addView(top);

        // -----------------------------
        // MARKET
        // -----------------------------

        TextView market =
                new TextView(this);

        market.setText(
                "EURUSD • 1M");

        market.setTextColor(
                Color.LTGRAY);

        market.setTextSize(10);

        main.addView(market);

        // -----------------------------
        // SIGNAL ROW
        // -----------------------------

        LinearLayout signalRow =
                new LinearLayout(this);

        signalRow.setOrientation(
                LinearLayout.HORIZONTAL);

        signalRow.setGravity(
                Gravity.CENTER_VERTICAL);

        signalView =
                new TextView(this);

        signalView.setText("WAIT");

        signalView.setTextColor(
                Color.WHITE);

        signalView.setTextSize(21);

        confidenceView =
                new TextView(this);

        confidenceView.setText(
                "  --%");

        confidenceView.setTextColor(
                Color.WHITE);

        confidenceView.setTextSize(13);

        signalRow.addView(signalView);

        signalRow.addView(confidenceView);

        main.addView(signalRow);

        // -----------------------------
        // TREND
        // -----------------------------

        trendView =
                new TextView(this);

        trendView.setText(
                "📊 WAIT");

        trendView.setTextColor(
                Color.LTGRAY);

        trendView.setTextSize(10);

        main.addView(trendView);

        // -----------------------------
        // CANDLE AREA
        // -----------------------------

        LinearLayout candleBox =
                new LinearLayout(this);

        candleBox.setOrientation(
                LinearLayout.HORIZONTAL);

        candleBox.setGravity(
                Gravity.CENTER_VERTICAL);

        candleBox.setPadding(
                7, 5, 5, 5);

        candleBox.setBackgroundColor(
                Color.rgb(13, 17, 32));

        // Candle drawing

        candleView =
                new CandleView(this);

        LinearLayout.LayoutParams candleParams =
                new LinearLayout.LayoutParams(
                        0,
                        105,
                        1);

        candleBox.addView(
                candleView,
                candleParams);

        // -----------------------------
        // TIME ON RIGHT SIDE
        // -----------------------------

        LinearLayout timeBox =
                new LinearLayout(this);

        timeBox.setOrientation(
                LinearLayout.VERTICAL);

        timeBox.setGravity(
                Gravity.CENTER);

        timeBox.setPadding(
                5, 0, 2, 0);

        candleTimeView =
                new TextView(this);

        candleTimeView.setText(
                "--:--");

        candleTimeView.setTextColor(
                Color.WHITE);

        candleTimeView.setTextSize(12);

        candleTimeView.setGravity(
                Gravity.CENTER);

        countdownView =
                new TextView(this);

        countdownView.setText(
                "00:59");

        countdownView.setTextColor(
                Color.LTGRAY);

        countdownView.setTextSize(10);

        countdownView.setGravity(
                Gravity.CENTER);

        timeBox.addView(candleTimeView);

        timeBox.addView(countdownView);

        candleBox.addView(
                timeBox,
                new LinearLayout.LayoutParams(
                        55,
                        LinearLayout.LayoutParams.WRAP_CONTENT));

        main.addView(candleBox);

        // -----------------------------
        // NEXT SIGNAL
        // -----------------------------

        TextView nextSignal =
                new TextView(this);

        nextSignal.setText(
                "◯ NEXT SIGNAL");

        nextSignal.setTextColor(
                Color.WHITE);

        nextSignal.setTextSize(10);

        nextSignal.setGravity(
                Gravity.CENTER);

        nextSignal.setPadding(
                0, 5, 0, 0);

        nextSignal.setOnClickListener(
                v -> updateSignal());

        main.addView(nextSignal);

        // -----------------------------
        // WINDOW
        // -----------------------------

        floatingView = main;

        int overlayType;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            overlayType =
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            overlayType =
                    WindowManager.LayoutParams.TYPE_PHONE;
        }

        windowParams =
                new WindowManager.LayoutParams(
                        275,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        overlayType,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        android.graphics.PixelFormat.TRANSLUCENT);

        windowParams.gravity =
                Gravity.TOP | Gravity.RIGHT;

        windowParams.x = 8;
        windowParams.y = 120;

        windowManager.addView(
                floatingView,
                windowParams);

        // -----------------------------
        // DRAG
        // -----------------------------

        floatingView.setOnTouchListener(
                new View.OnTouchListener() {

                    private int initialX;
                    private int initialY;

                    private float initialTouchX;
                    private float initialTouchY;

                    @Override
                    public boolean onTouch(
                            View v,
                            MotionEvent event) {

                        switch (event.getAction()) {

                            case MotionEvent.ACTION_DOWN:

                                initialX =
                                        windowParams.x;

                                initialY =
                                        windowParams.y;

                                initialTouchX =
                                        event.getRawX();

                                initialTouchY =
                                        event.getRawY();

                                return true;

                            case MotionEvent.ACTION_MOVE:

                                windowParams.x =
                                        initialX +
                                                (int) (
                                                        initialTouchX -
                                                                event.getRawX());

                                windowParams.y =
                                        initialY +
                                                (int) (
                                                        event.getRawY() -
                                                                initialTouchY);

                                windowManager.updateViewLayout(
                                        floatingView,
                                        windowParams);

                                return true;
                        }

                        return false;
                    }
                });
    }

    // ----------------------------------------------------
    // CLOCK
    // ----------------------------------------------------

    private void updateCandleClock() {

        long now =
                System.currentTimeMillis();

        long seconds =
                now / 1000;

        long secondInMinute =
                seconds % 60;

        long remaining =
                60 - secondInMinute;

        if (remaining > 60) {
            remaining = 60;
        }

        String time =
                String.format(
                        Locale.US,
                        "%02d:%02d",
                        (seconds / 3600) % 24,
                        (seconds / 60) % 60);

        if (candleTimeView != null) {
            candleTimeView.setText(time);
        }

        if (countdownView != null) {

            countdownView.setText(
                    String.format(
                            Locale.US,
                            "00:%02d",
                            remaining == 60
                                    ? 59
                                    : remaining));
        }
    }

    // ----------------------------------------------------
    // API
    // ----------------------------------------------------

    private void updateSignal() {

        new Thread(() -> {

            HttpURLConnection connection = null;

            try {

                String urlString =
                        API_URL +
                                "?floating=" +
                                System.currentTimeMillis();

                URL url =
                        new URL(urlString);

                connection =
                        (HttpURLConnection)
                                url.openConnection();

                connection.setRequestMethod(
                        "GET");

                connection.setConnectTimeout(
                        8000);

                connection.setReadTimeout(
                        8000);

                connection.setUseCaches(false);

                InputStream input =
                        connection.getInputStream();

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        input));

                StringBuilder response =
                        new StringBuilder();

                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();

                JSONObject json =
                        new JSONObject(
                                response.toString());

                String signal =
                        json.optString(
                                "signal",
                                "WAIT");

                int confidence =
                        json.optInt(
                                "confidence",
                                0);

                String trend =
                        json.optString(
                                "trend",
                                "SIDEWAYS");

                String closedTime =
                        json.optString(
                                "closed_candle_time",
                                "");

                // -------------------------
                // OHLC
                // -------------------------

                double o =
                        json.optDouble(
                                "open",
                                Double.NaN);

                double h =
                        json.optDouble(
                                "high",
                                Double.NaN);

                double l =
                        json.optDouble(
                                "low",
                                Double.NaN);

                double c =
                        json.optDouble(
                                "close",
                                Double.NaN);

                double price =
                        json.optDouble(
                                "price",
                                Double.NaN);

                if (!Double.isNaN(o)
                        && !Double.isNaN(h)
                        && !Double.isNaN(l)
                        && !Double.isNaN(c)) {

                    previousOpen = o;
                    previousHigh = h;
                    previousLow = l;
                    previousClose = c;

                    liveOpen = c;

                    if (!Double.isNaN(price)) {
                        liveClose = price;
                    } else {
                        liveClose = c;
                    }

                    liveHigh =
                            Math.max(
                                    liveOpen,
                                    liveClose);

                    liveLow =
                            Math.min(
                                    liveOpen,
                                    liveClose);

                    hasRealCandleData = true;
                }

                lastClosedEpoch =
                        System.currentTimeMillis();

                runOnUiThread(() -> {

                    // SIGNAL

                    signalView.setText(
                            signal.toUpperCase());

                    confidenceView.setText(
                            "  " +
                                    confidence +
                                    "%");

                    // SIGNAL COLOR

                    if ("BUY".equalsIgnoreCase(signal)) {

                        signalView.setTextColor(
                                Color.rgb(
                                        45,
                                        220,
                                        125));

                    } else if ("SELL".equalsIgnoreCase(signal)) {

                        signalView.setTextColor(
                                Color.rgb(
                                        255,
                                        75,
                                        90));

                    } else {

                        signalView.setTextColor(
                                Color.WHITE);
                    }

                    // TREND

                    trendView.setText(
                            "📊 " +
                                    trend.toUpperCase());

                    // Candle redraw

                    if (candleView != null) {
                        candleView.invalidate();
                    }
                });

            } catch (Exception e) {

                runOnUiThread(() -> {

                    signalView.setText(
                            "WAIT");

                    confidenceView.setText(
                            "  --%");

                    trendView.setText(
                            "📊 CONNECTION");

                });

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }

        }).start();
    }

    // ----------------------------------------------------
    // CANDLE VIEW
    // ----------------------------------------------------

    private class CandleView extends View {

        private final Paint paint =
                new Paint(Paint.ANTI_ALIAS_FLAG);

        public CandleView(Context context) {
            super(context);

            paint.setStrokeWidth(3f);
        }

        @Override
        protected void onDraw(Canvas canvas) {

            super.onDraw(canvas);

            float width =
                    getWidth();

            float height =
                    getHeight();

            float centerY =
                    height / 2f;

            // --------------------------------
            // FALLBACK DATA
            // --------------------------------

            double po;
            double ph;
            double pl;
            double pc;

            double lo;
            double lh;
            double ll;
            double lc;

            if (hasRealCandleData) {

                po = previousOpen;
                ph = previousHigh;
                pl = previousLow;
                pc = previousClose;

                lo = liveOpen;
                lh = liveHigh;
                ll = liveLow;
                lc = liveClose;

            } else {

                po = 1.16100;
                ph = 1.16115;
                pl = 1.16090;
                pc = 1.16108;

                lo = pc;
                lc = pc;
                lh = pc;
                ll = pc;
            }

            // --------------------------------
            // UPDATE LIVE CANDLE
            // --------------------------------

            if (hasRealCandleData) {

                double movement =
                        Math.sin(
                                System.currentTimeMillis()
                                        / 1800.0)
                                * Math.abs(
                                        previousClose
                                                * 0.00008);

                liveClose =
                        liveOpen + movement;

                liveHigh =
                        Math.max(
                                liveOpen,
                                liveClose);

                liveLow =
                        Math.min(
                                liveOpen,
                                liveClose);

                lc = liveClose;
                lh = liveHigh;
                ll = liveLow;
            }

            // --------------------------------
            // PRICE RANGE
            // --------------------------------

            double max =
                    Math.max(
                            Math.max(ph, lh),
                            Math.max(po, lo));

            double min =
                    Math.min(
                            Math.min(pl, ll),
                            Math.min(pc, lc));

            if (max == min) {
                max += 0.0001;
                min -= 0.0001;
            }

            float top =
                    8;

            float bottom =
                    height - 8;

            float candleHeight =
                    bottom - top;

            // --------------------------------
            // PREVIOUS CANDLE
            // --------------------------------

            drawCandle(
                    canvas,
                    po,
                    ph,
                    pl,
                    pc,
                    width * 0.35f,
                    candleHeight,
                    top,
                    min,
                    max);

            // --------------------------------
            // LIVE CANDLE
            // --------------------------------

            drawCandle(
                    canvas,
                    lo,
                    lh,
                    ll,
                    lc,
                    width * 0.65f,
                    candleHeight,
                    top,
                    min,
                    max);
        }

        private void drawCandle(
                Canvas canvas,
                double open,
                double high,
                double low,
                double close,
                float x,
                float candleHeight,
                float top,
                double min,
                double max) {

            boolean bullish =
                    close >= open;

            // --------------------------------
            // MARKET CANDLE COLOR
            // --------------------------------

            if (bullish) {

                paint.setColor(
                        Color.rgb(
                                45,
                                220,
                                125));

            } else {

                paint.setColor(
                        Color.rgb(
                                255,
                                75,
                                90));
            }

            float highY =
                    (float)
                            (top +
                                    (max - high)
                                            / (max - min)
                                            * candleHeight);

            float lowY =
                    (float)
                            (top +
                                    (max - low)
                                            / (max - min)
                                            * candleHeight);

            float openY =
                    (float)
                            (top +
                                    (max - open)
                                            / (max - min)
                                            * candleHeight);

            float closeY =
                    (float)
                            (top +
                                    (max - close)
                                            / (max - min)
                                            * candleHeight);

            // WICK

            paint.setStrokeWidth(2.5f);

            canvas.drawLine(
                    x,
                    highY,
                    x,
                    lowY,
                    paint);

            // BODY

            float bodyTop =
                    Math.min(
                            openY,
                            closeY);

            float bodyBottom =
                    Math.max(
                            openY,
                            closeY);

            if (bodyBottom - bodyTop < 5) {
                bodyBottom =
                        bodyTop + 5;
            }

            RectF body =
                    new RectF(
                            x - 8,
                            bodyTop,
                            x + 8,
                            bodyBottom);

            canvas.drawRect(
                    body,
                    paint);
        }
    }

    // ----------------------------------------------------
    // NOTIFICATION
    // ----------------------------------------------------

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            "trader_pink_ai",
                            "Trader Pink AI",
                            NotificationManager
                                    .IMPORTANCE_LOW);

            NotificationManager manager =
                    getSystemService(
                            NotificationManager.class);

            if (manager != null) {
                manager.createNotificationChannel(
                        channel);
            }
        }
    }

    // ----------------------------------------------------
    // DESTROY
    // ----------------------------------------------------

    @Override
    public void onDestroy() {

        handler.removeCallbacks(updater);
        handler.removeCallbacks(candleTimer);

        if (floatingView != null
                && windowManager != null) {

            try {
                windowManager.removeView(
                        floatingView);
            } catch (Exception ignored) {
            }
        }

        floatingView = null;

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

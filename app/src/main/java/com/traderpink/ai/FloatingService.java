package com.traderpink.ai;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;

import org.json.*;

import java.io.*;
import java.net.*;
import java.util.*;

public class FloatingService extends Service {

    private static final String API =
            "https://crimson-grass-f881.bijondebnath51.workers.dev/";

    private WindowManager windowManager;
    private View floatingView;

    private TextView signalView;
    private TextView confidenceView;
    private TextView trendView;
    private TextView timeView;
    private TextView candleTimerView;

    private CandleView candleView;

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

    private final Runnable candleTimer =
            new Runnable() {
                @Override
                public void run() {

                    updateCandleTimer();

                    handler.postDelayed(
                            this,
                            1000);
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

        handler.removeCallbacks(candleTimer);

        handler.post(
                candleTimer);

        return START_STICKY;
    }

    private void showFloatingWindow() {

        if (floatingView != null) {
            return;
        }

        /*
         * MAIN COMPACT PANEL
         */
        LinearLayout main =
                new LinearLayout(this);

        main.setOrientation(
                LinearLayout.VERTICAL);

        main.setPadding(
                10,
                7,
                10,
                8);

        GradientDrawable background =
                new GradientDrawable();

        background.setColor(
                Color.rgb(
                        18,
                        23,
                        40));

        background.setCornerRadius(
                18);

        main.setBackground(background);

        /*
         * TOP BAR
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

        title.setTextSize(13);

        title.setSingleLine(true);

        topBar.addView(
                title,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1));

        TextView close =
                new TextView(this);

        close.setText("×");

        close.setTextColor(
                Color.LTGRAY);

        close.setTextSize(20);

        close.setGravity(
                Gravity.CENTER);

        close.setPadding(
                6,
                0,
                2,
                0);

        close.setOnClickListener(
                v -> {

                    handler.removeCallbacks(
                            updater);

                    handler.removeCallbacks(
                            candleTimer);

                    stopSelf();
                });

        topBar.addView(close);

        main.addView(topBar);

        /*
         * MARKET
         */
        TextView market =
                new TextView(this);

        market.setText(
                "EURUSD • 1M");

        market.setTextColor(
                Color.LTGRAY);

        market.setTextSize(10);

        market.setGravity(
                Gravity.CENTER);

        market.setPadding(
                0,
                1,
                0,
                1);

        main.addView(market);

        /*
         * SIGNAL ROW
         */
        LinearLayout signalRow =
                new LinearLayout(this);

        signalRow.setOrientation(
                LinearLayout.HORIZONTAL);

        signalRow.setGravity(
                Gravity.CENTER_VERTICAL);

        signalView =
                new TextView(this);

        signalView.setText(
                "🟡 WAIT");

        signalView.setTextColor(
                Color.WHITE);

        signalView.setTextSize(17);

        signalView.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD);

        signalView.setSingleLine(true);

        signalRow.addView(
                signalView,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1));

        confidenceView =
                new TextView(this);

        confidenceView.setText(
                "--%");

        confidenceView.setTextColor(
                Color.WHITE);

        confidenceView.setTextSize(13);

        confidenceView.setGravity(
                Gravity.CENTER);

        signalRow.addView(
                confidenceView);

        timeView =
                new TextView(this);

        timeView.setText(
                "⏱ --");

        timeView.setTextColor(
                Color.LTGRAY);

        timeView.setTextSize(10);

        timeView.setGravity(
                Gravity.CENTER);

        timeView.setPadding(
                7,
                0,
                0,
                0);

        signalRow.addView(timeView);

        main.addView(signalRow);

        /*
         * TREND
         */
        trendView =
                new TextView(this);

        trendView.setText(
                "📈 UPTREND");

        trendView.setTextColor(
                Color.LTGRAY);

        trendView.setTextSize(10);

        trendView.setGravity(
                Gravity.CENTER);

        trendView.setPadding(
                0,
                1,
                0,
                2);

        main.addView(trendView);

        /*
         * NEXT SIGNAL BUTTON
         */
        TextView next =
                new TextView(this);

        next.setText(
                "◯  NEXT SIGNAL");

        next.setTextColor(
                Color.WHITE);

        next.setTextSize(10);

        next.setGravity(
                Gravity.CENTER);

        next.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD);

        GradientDrawable nextBg =
                new GradientDrawable();

        nextBg.setColor(
                Color.rgb(
                        35,
                        43,
                        68));

        nextBg.setCornerRadius(
                50);

        next.setBackground(nextBg);

        next.setPadding(
                12,
                6,
                12,
                6);

        next.setOnClickListener(
                v -> updateSignal());

        LinearLayout.LayoutParams nextParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);

        nextParams.gravity =
                Gravity.CENTER;

        nextParams.topMargin = 2;
        nextParams.bottomMargin = 4;

        main.addView(
                next,
                nextParams);

        /*
         * CANDLE BOX
         *
         * একটি মাত্র box
         * ভিতরে দুইটি candle
         */
        LinearLayout candleBox =
                new LinearLayout(this);

        candleBox.setOrientation(
                LinearLayout.HORIZONTAL);

        candleBox.setGravity(
                Gravity.CENTER_VERTICAL);

        GradientDrawable candleBg =
                new GradientDrawable();

        candleBg.setColor(
                Color.rgb(
                        11,
                        16,
                        30));

        candleBg.setCornerRadius(
                12);

        candleBg.setStroke(
                1,
                Color.rgb(
                        45,
                        55,
                        80));

        candleBox.setBackground(
                candleBg);

        candleView =
                new CandleView(this);

        LinearLayout.LayoutParams candleParams =
                new LinearLayout.LayoutParams(
                        0,
                        92,
                        1);

        candleBox.addView(
                candleView,
                candleParams);

        /*
         * RUNNING CANDLE TIMER
         */
        candleTimerView =
                new TextView(this);

        candleTimerView.setText(
                "00:59");

        candleTimerView.setTextColor(
                Color.WHITE);

        candleTimerView.setTextSize(10);

        candleTimerView.setGravity(
                Gravity.CENTER);

        candleTimerView.setPadding(
                2,
                0,
                7,
                0);

        candleBox.addView(
                candleTimerView,
                new LinearLayout.LayoutParams(
                        38,
                        LinearLayout.LayoutParams.MATCH_PARENT));

        main.addView(
                candleBox);

        floatingView = main;

        /*
         * WINDOW MANAGER
         */
        windowManager =
                (WindowManager)
                        getSystemService(
                                WINDOW_SERVICE);

        int type;

        if (Build.VERSION.SDK_INT >= 26) {

            type =
                    WindowManager.LayoutParams
                            .TYPE_APPLICATION_OVERLAY;

        } else {

            type =
                    WindowManager.LayoutParams
                            .TYPE_PHONE;
        }

        /*
         * WIDTH কমানো হয়েছে।
         * HEIGHT একই ধরনের compact রাখা হয়েছে।
         */
        windowParams =
                new WindowManager.LayoutParams(
                        275,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        type,
                        WindowManager.LayoutParams
                                .FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams
                                .FLAG_LAYOUT_NO_LIMITS,
                        PixelFormat.TRANSLUCENT);

        windowParams.gravity =
                Gravity.TOP |
                Gravity.RIGHT;

        windowParams.x = 8;
        windowParams.y = 120;

        /*
         * DRAG
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

    /*
     * =========================================================
     * SIGNAL UPDATE
     * =========================================================
     */
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

                String nextTime =
                        data.optString(
                                "next_candle_time",
                                "--");

                String trend =
                        data.optString(
                                "trend",
                                "--");

                /*
                 * Candle data থাকলে ব্যবহার করবে।
                 * Worker-এ না থাকলে fallback করবে।
                 */
                double open =
                        data.optDouble(
                                "open",
                                Double.NaN);

                double high =
                        data.optDouble(
                                "high",
                                Double.NaN);

                double low =
                        data.optDouble(
                                "low",
                                Double.NaN);

                double close =
                        data.optDouble(
                                "close",
                                Double.NaN);

                double price =
                        data.optDouble(
                                "price",
                                Double.NaN);

                final double fOpen = open;
                final double fHigh = high;
                final double fLow = low;
                final double fClose = close;
                final double fPrice = price;

                new Handler(
                        Looper.getMainLooper())
                        .post(() -> {

                            if (signalView == null) {
                                return;
                            }

                            String emoji;

                            if (
                                    finalSignal.equalsIgnoreCase(
                                            "BUY")) {

                                emoji = "🟢";

                                signalView.setTextColor(
                                        Color.rgb(
                                                50,
                                                220,
                                                120));

                            } else if (
                                    finalSignal.equalsIgnoreCase(
                                            "SELL")) {

                                emoji = "🔴";

                                signalView.setTextColor(
                                        Color.rgb(
                                                255,
                                                80,
                                                100));

                            } else {

                                emoji = "🟡";

                                signalView.setTextColor(
                                        Color.WHITE);
                            }

                            signalView.setText(
                                    emoji +
                                    " " +
                                    finalSignal);

                            confidenceView.setText(
                                    confidence +
                                    "%");

                            timeView.setText(
                                    "⏱ " +
                                    shortTime(nextTime));

                            trendView.setText(
                                    "📈 " +
                                    trend);

                            /*
                             * CandleView-কে Worker-এর
                             * data দেওয়া হচ্ছে।
                             */
                            if (candleView != null) {

                                candleView.setMarketData(
                                        fOpen,
                                        fHigh,
                                        fLow,
                                        fClose,
                                        fPrice);
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
                                    "🟡 WAIT");

                            signalView.setTextColor(
                                    Color.WHITE);

                            confidenceView.setText(
                                    "--%");

                            timeView.setText(
                                    "⏱ --");

                            trendView.setText(
                                    "Connection Error");
                        });

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }

        }).start();
    }

    /*
     * Worker-এর সময়কে ছোট করে দেখায়।
     */
    private String shortTime(
            String value) {

        if (
                value == null ||
                value.equals("--")) {

            return "--";
        }

        try {

            if (value.contains("T")) {

                String part =
                        value.substring(
                                value.indexOf("T") + 1);

                if (part.length() >= 5) {

                    return part.substring(
                            0,
                            5);
                }
            }

            if (value.length() >= 5) {

                return value.substring(
                        value.length() - 5);
            }

        } catch (Exception ignored) {
        }

        return value;
    }

    /*
     * =========================================================
     * 1 MINUTE COUNTDOWN
     * =========================================================
     */
    private void updateCandleTimer() {

        if (candleTimerView == null) {
            return;
        }

        long now =
                System.currentTimeMillis();

        /*
         * Bangladesh/local device minute boundary.
         *
         * 60 seconds থেকে countdown।
         */
        long elapsed =
                (now / 1000) % 60;

        long remaining =
                60 - elapsed;

        if (remaining == 60) {
            remaining = 59;
        }

        String timer =
                String.format(
                        Locale.US,
                        "00:%02d",
                        remaining);

        candleTimerView.setText(
                timer);

        if (candleView != null) {

            candleView.setSecondsLeft(
                    (int) remaining);
        }

        handler.postDelayed(
                () -> updateCandleTimer(),
                1000);
    }

    /*
     * =========================================================
     * CANDLE VIEW
     * =========================================================
     */
    private class CandleView extends View {

        private final Paint paint =
                new Paint(
                        Paint.ANTI_ALIAS_FLAG);

        private double previousOpen =
                Double.NaN;

        private double previousHigh =
                Double.NaN;

        private double previousLow =
                Double.NaN;

        private double previousClose =
                Double.NaN;

        private double currentPrice =
                Double.NaN;

        private int secondsLeft = 59;

        private final Random random =
                new Random();

        CandleView(Context context) {

            super(context);

            paint.setStrokeWidth(2);

            setLayerType(
                    View.LAYER_TYPE_SOFTWARE,
                    null);
        }

        void setMarketData(
                double open,
                double high,
                double low,
                double close,
                double price) {

            if (!Double.isNaN(open)) {
                previousOpen = open;
            }

            if (!Double.isNaN(high)) {
                previousHigh = high;
            }

            if (!Double.isNaN(low)) {
                previousLow = low;
            }

            if (!Double.isNaN(close)) {
                previousClose = close;
            }

            if (!Double.isNaN(price)) {
                currentPrice = price;
            }

            invalidate();
        }

        void setSecondsLeft(
                int value) {

            secondsLeft = value;

            /*
             * Running candle-কে সামান্য
             * live movement দেওয়া হচ্ছে।
             */
            if (!Double.isNaN(currentPrice)) {

                double range =
                        getBaseRange();

                double movement =
                        (random.nextDouble()
                        - 0.5)
                        * range
                        * 0.18;

                currentPrice += movement;
            }

            invalidate();
        }

        private double getBaseRange() {

            if (
                    !Double.isNaN(previousHigh) &&
                    !Double.isNaN(previousLow)) {

                double range =
                        previousHigh
                        - previousLow;

                if (range > 0) {
                    return range;
                }
            }

            return 0.00010;
        }

        @Override
        protected void onDraw(
                Canvas canvas) {

            super.onDraw(canvas);

            float width =
                    getWidth();

            float height =
                    getHeight();

            /*
             * CENTER LINE
             */
            paint.setColor(
                    Color.rgb(
                            35,
                            45,
                            65));

            paint.setStrokeWidth(1);

            canvas.drawLine(
                    0,
                    height / 2,
                    width,
                    height / 2,
                    paint);

            /*
             * DATA না পাওয়া গেলে demo candle
             */
            double base;

            if (!Double.isNaN(previousClose)) {

                base = previousClose;

            } else if (!Double.isNaN(currentPrice)) {

                base = currentPrice;

            } else {

                base = 1.0850;
            }

            double range =
                    getBaseRange();

            if (range <= 0) {
                range = 0.00010;
            }

            /*
             * PREVIOUS CLOSED CANDLE
             */
            double pOpen;

            double pClose;

            double pHigh;

            double pLow;

            if (!Double.isNaN(previousOpen) &&
                !Double.isNaN(previousClose)) {

                pOpen =
                        previousOpen;

                pClose =
                        previousClose;

                pHigh =
                        !Double.isNaN(previousHigh)
                        ? previousHigh
                        : Math.max(
                                pOpen,
                                pClose)
                        + range * 0.15;

                pLow =
                        !Double.isNaN(previousLow)
                        ? previousLow
                        : Math.min(
                                pOpen,
                                pClose)
                        - range * 0.15;

            } else {

                /*
                 * Fallback visual candle
                 */
                pOpen =
                        base + range * 0.25;

                pClose =
                        base - range * 0.10;

                pHigh =
                        pOpen + range * 0.25;

                pLow =
                        pClose - range * 0.20;
            }

            /*
             * RUNNING CANDLE
             */
            double rOpen =
                    pClose;

            double rClose;

            if (!Double.isNaN(currentPrice)) {

                rClose =
                        currentPrice;

            } else {

                /*
                 * Small live movement
                 */
                double progress =
                        (60 - secondsLeft)
                        / 60.0;

                rClose =
                        rOpen
                        + Math.sin(
                                progress * 8.0)
                        * range
                        * 0.25;
            }

            double rHigh =
                    Math.max(
                            rOpen,
                            rClose)
                    + range * 0.12;

            double rLow =
                    Math.min(
                            rOpen,
                            rClose)
                    - range * 0.12;

            /*
             * Map prices into common chart range
             */
            double chartHigh =
                    Math.max(
                            pHigh,
                            rHigh);

            double chartLow =
                    Math.min(
                            pLow,
                            rLow);

            double chartRange =
                    chartHigh - chartLow;

            if (chartRange <= 0) {
                chartRange = range;
            }

            float top =
                    8;

            float bottom =
                    height - 8;

            /*
             * দুই candle-এর অবস্থান
             */
            float previousX =
                    width * 0.38f;

            float runningX =
                    width * 0.68f;

            float candleWidth =
                    Math.max(
                            10,
                            width * 0.12f);

            /*
             * Draw previous candle
             */
            drawCandle(
                    canvas,
                    previousX,
                    candleWidth,
                    pOpen,
                    pHigh,
                    pLow,
                    pClose,
                    chartHigh,
                    chartLow,
                    top,
                    bottom);

            /*
             * Draw running candle
             */
            drawCandle(
                    canvas,
                    runningX,
                    candleWidth,
                    rOpen,
                    rHigh,
                    rLow,
                    rClose,
                    chartHigh,
                    chartLow,
                    top,
                    bottom);

            /*
             * LIVE indicator
             */
            paint.setTextSize(8);

            paint.setTypeface(
                    Typeface.DEFAULT_BOLD);

            paint.setColor(
                    Color.WHITE);

            canvas.drawText(
                    "LIVE",
                    runningX - 9,
                    height - 2,
                    paint);

            /*
             * CLOSED indicator
             */
            paint.setColor(
                    Color.LTGRAY);

            canvas.drawText(
                    "CLOSED",
                    previousX - 17,
                    height - 2,
                    paint);
        }

        private void drawCandle(
                Canvas canvas,
                float x,
                float candleWidth,
                double open,
                double high,
                double low,
                double close,
                double chartHigh,
                double chartLow,
                float top,
                float bottom) {

            float yHigh =
                    priceToY(
                            high,
                            chartHigh,
                            chartLow,
                            top,
                            bottom);

            float yLow =
                    priceToY(
                            low,
                            chartHigh,
                            chartLow,
                            top,
                            bottom);

            float yOpen =
                    priceToY(
                            open,
                            chartHigh,
                            chartLow,
                            top,
                            bottom);

            float yClose =
                    priceToY(
                            close,
                            chartHigh,
                            chartLow,
                            top,
                            bottom);

            boolean bullish =
                    close >= open;

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

            /*
             * Wick
             */
            paint.setStrokeWidth(2);

            canvas.drawLine(
                    x,
                    yHigh,
                    x,
                    yLow,
                    paint);

            /*
             * Body
             */
            float bodyTop =
                    Math.min(
                            yOpen,
                            yClose);

            float bodyBottom =
                    Math.max(
                            yOpen,
                            yClose);

            if (
                    Math.abs(
                            bodyBottom - bodyTop)
                    < 4) {

                bodyBottom =
                        bodyTop + 4;
            }

            RectF body =
                    new RectF(
                            x - candleWidth / 2,
                            bodyTop,
                            x + candleWidth / 2,
                            bodyBottom);

            paint.setStyle(
                    Paint.Style.FILL);

            canvas.drawRoundRect(
                    body,
                    2,
                    2,
                    paint);

            /*
             * Small glow
             */
            paint.setShadowLayer(
                    7,
                    0,
                    0,
                    paint.getColor());

            canvas.drawRoundRect(
                    body,
                    2,
                    2,
                    paint);

            paint.clearShadowLayer();
        }

        private float priceToY(
                double price,
                double high,
                double low,
                float top,
                float bottom) {

            if (high == low) {
                return (top + bottom) / 2;
            }

            double ratio =
                    (high - price)
                    / (high - low);

            return (float)
                    (top +
                    ratio *
                    (bottom - top));
        }
    }

    /*
     * =========================================================
     * NOTIFICATION
     * =========================================================
     */
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

        handler.removeCallbacks(
                candleTimer);

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

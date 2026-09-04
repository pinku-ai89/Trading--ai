
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

    final String API =
        "https://crimson-grass-f881.bijondebnath51.workers.dev/";

    WindowManager wm;
    View box;
    TextView info;
    Handler h=new Handler();

    public IBinder onBind(Intent i){ return null; }

    public int onStartCommand(Intent i,int f,int id){
        show();
        update();
        h.postDelayed(new Runnable(){
            public void run(){
                update();
                h.postDelayed(this,10000);
            }
        },10000);
        return START_STICKY;
    }

    void show(){
        if(box!=null)return;

        LinearLayout l=new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(15,10,15,10);
        l.setBackgroundColor(Color.rgb(20,25,45));

        TextView title=new TextView(this);
        title.setText("🤖 Trader Pink AI");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);

        info=new TextView(this);
        info.setTextColor(Color.WHITE);
        info.setTextSize(14);

        Button close=new Button(this);
        close.setText("✕");
        close.setOnClickListener(v->stopSelf());

        l.addView(title);
        l.addView(info);
        l.addView(close);

        box=l;
        wm=(WindowManager)getSystemService(WINDOW_SERVICE);

        int type=Build.VERSION.SDK_INT>=26?
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:
            WindowManager.LayoutParams.TYPE_PHONE;

        WindowManager.LayoutParams p=
            new WindowManager.LayoutParams(
                330,WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        p.gravity=Gravity.TOP|Gravity.RIGHT;
        p.x=10;p.y=120;

        wm.addView(box,p);
    }

    void update(){
        new Thread(()->{
            try{
                HttpURLConnection c=(HttpURLConnection)
                    new URL(API+"?t="+System.currentTimeMillis())
                    .openConnection();

                BufferedReader r=new BufferedReader(
                    new InputStreamReader(c.getInputStream()));

                StringBuilder s=new StringBuilder();
                String x;
                while((x=r.readLine())!=null)s.append(x);

                JSONObject j=new JSONObject(s.toString());
                JSONObject cd=j.optJSONObject("candle");

                String text=
                    "EURUSD • 1 MIN\n\n"+
                    "SIGNAL: "+j.optString("signal","WAIT")+"\n"+
                    "Confidence: "+j.optInt("confidence",0)+"%\n"+
                    "Trend: "+j.optString("trend","--")+"\n"+
                    "Candle: "+(cd==null?"--":
                    cd.optString("direction","--"))+"\n"+
                    "Body: "+(cd==null?0:
                    cd.optDouble("body_percent",0))+"%\n\n"+
                    "Signal Candle:\n"+
                    j.optString("closed_candle_time","--")+"\n"+
                    "Next Candle:\n"+
                    j.optString("next_candle_time","--");

                h.post(()->info.setText(text));

            }catch(Exception e){
                h.post(()->info.setText("WAIT\nConnection error"));
            }
        }).start();
    }

    public void onDestroy(){
        h.removeCallbacksAndMessages(null);
        if(box!=null&&wm!=null)wm.removeView(box);
        box=null;
        super.onDestroy();
    }
}

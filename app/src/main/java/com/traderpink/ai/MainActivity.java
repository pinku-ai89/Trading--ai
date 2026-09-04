package com.traderpink.ai;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {

    static final String API =
        "https://crimson-grass-f881.bijondebnath51.workers.dev/";

    TextView signal,info;
    ExecutorService pool=Executors.newSingleThreadExecutor();

    TextView t(String s,int z){
        TextView v=new TextView(this);
        v.setText(s);
        v.setTextColor(Color.WHITE);
        v.setTextSize(z);
        v.setPadding(0,8,0,8);
        return v;
    }

    @Override public void onCreate(Bundle b){
        super.onCreate(b);

        LinearLayout l=new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(20,20,20,20);
        l.setBackgroundColor(Color.rgb(11,16,32));

        l.addView(t("🤖 Trader Pink AI 📈",24));
        l.addView(t("EURUSD • 1 MIN",17));

        signal=t("WAIT",40);
        l.addView(signal);

        info=t("Loading...",16);
        l.addView(info);

        Button update=new Button(this);
        update.setText("🔄 UPDATE SIGNAL");
        update.setOnClickListener(v->load());
        l.addView(update);

        Button floating=new Button(this);
        floating.setText("🤖 START FLOATING BOT");
        floating.setOnClickListener(v->startFloat());
        l.addView(floating);

        setContentView(l);
        load();

        new Handler().postDelayed(new Runnable(){
            public void run(){
                load();
                new Handler().postDelayed(this,10000);
            }
        },10000);
    }

    void load(){
        pool.execute(()->{
            try{
                URL u=new URL(API+"?t="+System.currentTimeMillis());
                HttpURLConnection c=(HttpURLConnection)u.openConnection();
                c.setConnectTimeout(10000);
                c.setReadTimeout(10000);

                BufferedReader r=new BufferedReader(
                    new InputStreamReader(c.getInputStream()));

                StringBuilder s=new StringBuilder();
                String x;
                while((x=r.readLine())!=null)s.append(x);

                JSONObject j=new JSONObject(s.toString());
                JSONObject cd=j.optJSONObject("candle");

                String infoText=
                    "Confidence: "+j.optInt("confidence",0)+"%\n"+
                    "Trend: "+j.optString("trend","--")+"\n"+
                    "Candle: "+(cd==null?"--":
                    cd.optString("direction","--"))+
                    "  Body: "+(cd==null?0:
                    cd.optDouble("body_percent",0))+"%\n\n"+
                    "Signal Candle:\n"+
                    j.optString("closed_candle_time","--")+
                    "\n\nNext Candle:\n"+
                    j.optString("next_candle_time","--");

                runOnUiThread(()->{
                    signal.setText(j.optString("signal","WAIT"));
                    info.setText(infoText);
                });

            }catch(Exception e){
                runOnUiThread(()->{
                    signal.setText("WAIT");
                    info.setText("Connection error");
                });
            }
        });
    }

    void startFloat(){
        if(Build.VERSION.SDK_INT>=23 &&
           !Settings.canDrawOverlays(this)){
            startActivity(new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:"+getPackageName())));
            return;
        }

        startService(new Intent(this,FloatingService.class));
    }

    @Override protected void onDestroy(){
        pool.shutdownNow();
        super.onDestroy();
    }
}

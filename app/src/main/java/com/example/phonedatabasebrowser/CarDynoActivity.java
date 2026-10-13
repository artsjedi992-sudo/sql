package com.example.phonedatabasebrowser;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CarDynoActivity extends Activity implements LocationListener {
    private static final int REQUEST_LOCATION = 712;
    private LocationManager lm;
    private EditText massInput, cdInput, areaInput, crrInput, efficiencyInput;
    private TextView speedText, accelText, powerText, engineText, statusText, detailsText;
    private boolean running = false;
    private Location previous = null;
    private double smoothAccel = 0, peakWheelW = 0, peakEngineW = 0, currentWheelW = 0, currentEngineW = 0;
    private final StringBuilder csv = new StringBuilder("Date,Time,Speed_kmh,Acceleration_m_s2,WheelPower_kW,EstimatedEnginePower_kW\r\n");

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        buildUi();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(12), dp(14), dp(18));
        root.setBackgroundColor(0xFFF2F6FC);
        scroll.addView(root);
        TextView title = text("Автомобилен динамометър", 25, 0xFF15345B);
        title.setGravity(Gravity.CENTER);
        root.addView(title);
        TextView subtitle = text("GPS скорост + ускорение • оценка на мощността", 14, 0xFF526780);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle);
        root.addView(panelLabel("Параметри на автомобила"));
        LinearLayout row = new LinearLayout(this);
        row.addView(field("Маса с водача (kg)", "1250"), new LinearLayout.LayoutParams(0, dp(72), 1));
        row.addView(field("Cd", "0.29"), new LinearLayout.LayoutParams(0, dp(72), 1));
        root.addView(row);
        LinearLayout row2 = new LinearLayout(this);
        row2.addView(field("Челна площ (m²)", "2.02"), new LinearLayout.LayoutParams(0, dp(72), 1));
        row2.addView(field("Търкаляне Crr", "0.012"), new LinearLayout.LayoutParams(0, dp(72), 1));
        root.addView(row2);
        LinearLayout row3 = new LinearLayout(this);
        row3.addView(field("Ефективност КПП", "0.85"), new LinearLayout.LayoutParams(0, dp(72), 1));
        root.addView(row3);
        root.addView(text("Профил: Mitsubishi Carisma 1.8 • ръчна кутия • гуми 195/50 R15", 13, 0xFF526780));
        root.addView(text("Диаметър на гумата ≈ 576 mm • обиколка ≈ 1,81 m. Размерът е записан като справочен параметър; GPS определя скоростта.", 12, 0xFF526780));
        root.addView(panelLabel("Резултат в реално време"));
        speedText = metric("Скорост: — km/h");
        accelText = metric("Ускорение: — m/s²");
        powerText = metric("Мощност на колелата: — kW");
        engineText = metric("Оценка на двигателя: — kW / — к.с.");
        root.addView(speedText); root.addView(accelText); root.addView(powerText); root.addView(engineText);
        statusText = text("Готово. Нужен е добър GPS сигнал.", 13, 0xFF526780);
        root.addView(statusText);
        detailsText = text("Максимумите се обновяват по време на измерването.", 13, 0xFF526780);
        root.addView(detailsText);
        Button start = button("СТАРТ / НОВ ТЕСТ", 0xFF078B62);
        start.setOnClickListener(v -> startTest());
        root.addView(start);
        Button stop = button("СПРИ И ЗАПАЗИ CSV", 0xFF3157A4);
        stop.setOnClickListener(v -> stopAndSave());
        root.addView(stop);
        Button close = button("ЗАТВОРИ", 0xFF65758B);
        close.setOnClickListener(v -> finish());
        root.addView(close);
        setContentView(scroll);
    }

    private EditText field(String label, String value) {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        TextView l = text(label, 12, 0xFF526780); wrap.addView(l);
        EditText e = new EditText(this); e.setSingleLine(true); e.setText(value); e.setTextSize(16);
        e.setPadding(dp(8), 0, dp(8), 0); e.setBackground(bg(0xFFFFFFFF, 0xFFBDCADB)); wrap.addView(e);
        if (label.startsWith("Маса")) massInput=e;
        else if (label.equals("Cd")) cdInput=e;
        else if (label.startsWith("Челна")) areaInput=e;
        else if (label.startsWith("Търкаляне")) crrInput=e;
        else efficiencyInput=e;
        wrap.setPadding(dp(3), dp(3), dp(3), dp(3));
        wrap.setTag(e);
        return e;
    }

    private TextView panelLabel(String s) {
        TextView t=text(s,17,0xFF15345B); t.setPadding(0,dp(15),0,dp(6)); return t;
    }
    private TextView metric(String s) {
        TextView t=text(s,18,0xFF15345B); t.setPadding(dp(12),dp(10),dp(12),dp(10));
        t.setBackground(bg(0xFFFFFFFF,0xFFD5E0EE)); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1, -2); p.bottomMargin=dp(6); t.setLayoutParams(p); return t;
    }
    private TextView text(String s,int size,int color) {
        TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); return t;
    }
    private Button button(String s,int color) {
        Button b=new Button(this); b.setText(s); b.setTextColor(Color.WHITE); b.setBackground(bg(color,color));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(52)); p.topMargin=dp(7); b.setLayoutParams(p); return b;
    }
    private GradientDrawable bg(int color,int stroke) {
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(10)); d.setStroke(dp(1),stroke); return d;
    }
    private int dp(float v) { return (int)(v*getResources().getDisplayMetrics().density+0.5f); }
    private double val(EditText e,double fallback) {
        try { return Double.parseDouble(e.getText().toString().trim().replace(',','.')); } catch(Exception ex) { return fallback; }
    }

    private void startTest() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},REQUEST_LOCATION); return;
        }
        previous=null; smoothAccel=0; peakWheelW=0; peakEngineW=0; csv.setLength(0);
        csv.append("Date,Time,Speed_kmh,Acceleration_m_s2,WheelPower_kW,EstimatedEnginePower_kW\r\n");
        running=true;
        try {
            if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                statusText.setText("Включи GPS и натисни Старт отново."); running=false; return;
            }
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,200,0.5f,this);
            statusText.setText("Измерването работи. Използвай само на затворен, безопасен участък.");
        } catch (SecurityException e) {
            running=false; statusText.setText("Няма разрешение за GPS.");
        }
    }

    @Override public void onLocationChanged(Location loc) {
        if (!running) return;
        double speed = loc.hasSpeed() ? loc.getSpeed() : 0;
        double accel=0;
        if (previous != null) {
            double dt=(loc.getTime()-previous.getTime())/1000.0;
            if (dt>0.15 && dt<3.0 && previous.hasSpeed() && loc.hasSpeed()) {
                accel=(speed-previous.getSpeed())/dt;
                accel=Math.max(-8,Math.min(8,accel));
                smoothAccel=0.65*smoothAccel+0.35*accel;
            }
        }
        previous=loc;
        double mass=Math.max(500,val(massInput,1250));
        double cd=Math.max(0,val(cdInput,0.29));
        double area=Math.max(0.5,val(areaInput,2.02));
        double crr=Math.max(0,val(crrInput,0.012));
        double efficiency=Math.max(0.5,Math.min(1.0,val(efficiencyInput,0.85)));
        double rho=1.225, g=9.80665;
        double rolling=crr*mass*g;
        double aero=0.5*rho*cd*area*speed*speed;
        double wheelW=(mass*smoothAccel+rolling+aero)*speed;
        wheelW=Math.max(0,wheelW);
        double engineW=wheelW/efficiency;
        currentWheelW=wheelW; currentEngineW=engineW;
        peakWheelW=Math.max(peakWheelW,wheelW); peakEngineW=Math.max(peakEngineW,engineW);
        speedText.setText(String.format(Locale.getDefault(),"Скорост: %.1f km/h",speed*3.6));
        accelText.setText(String.format(Locale.getDefault(),"Ускорение: %.2f m/s²",smoothAccel));
        powerText.setText(String.format(Locale.getDefault(),"Мощност на колелата: %.1f kW",wheelW/1000));
        engineText.setText(String.format(Locale.getDefault(),"Оценка на двигателя: %.1f kW / %.1f к.с.",engineW/1000,engineW/735.5));
        detailsText.setText(String.format(Locale.getDefault(),"Пик колела: %.1f kW • пик двигател: %.1f к.с.\nGPS точност: ±%.1f m",peakWheelW/1000,peakEngineW/735.5,loc.hasAccuracy()?loc.getAccuracy():-1));
        String stamp=new SimpleDateFormat("yyyy-MM-dd,HH:mm:ss",Locale.US).format(new Date());
        csv.append(String.format(Locale.US,"%s,%.2f,%.3f,%.3f,%.3f\r\n",stamp,speed*3.6,smoothAccel,wheelW/1000,engineW/1000));
    }

    private void stopAndSave() {
        running=false;
        try { lm.removeUpdates(this); } catch(Exception ignored) {}
        if (csv.length() <= "Date,Time,Speed_kmh,Acceleration_m_s2,WheelPower_kW,EstimatedEnginePower_kW\r\n".length()) {
            statusText.setText("Няма измерени GPS точки за запис."); return;
        }
        try {
            ContentValues values=new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME,"car_dyno_"+new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(new Date())+".csv");
            values.put(MediaStore.Downloads.MIME_TYPE,"text/csv");
            values.put(MediaStore.Downloads.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS);
            android.net.Uri uri=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);
            if(uri==null) throw new Exception("Не може да се създаде файл");
            OutputStream os=getContentResolver().openOutputStream(uri);
            if(os==null) throw new Exception("Не може да се отвори файл");
            os.write(csv.toString().getBytes(StandardCharsets.UTF_8)); os.close();
            statusText.setText("CSV записан в папка Downloads.");
            Toast.makeText(this,"Записано в Downloads",Toast.LENGTH_LONG).show();
        } catch(Exception e) { statusText.setText("Грешка при запис: "+e.getMessage()); }
    }

    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grants) {
        super.onRequestPermissionsResult(requestCode,permissions,grants);
        if(requestCode==REQUEST_LOCATION && grants.length>0 && grants[0]==PackageManager.PERMISSION_GRANTED) startTest();
        else statusText.setText("Разрешението за местоположение е необходимо за GPS скорост.");
    }
    @Override public void onProviderEnabled(String provider) {}
    @Override public void onProviderDisabled(String provider) {}
    @Override public void onStatusChanged(String provider,int status,Bundle extras) {}
    @Override protected void onPause() { super.onPause(); if(running) { try { lm.removeUpdates(this); } catch(Exception ignored) {} running=false; } }
}

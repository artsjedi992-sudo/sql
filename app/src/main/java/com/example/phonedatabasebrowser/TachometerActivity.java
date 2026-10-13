package com.example.phonedatabasebrowser;

import android.Manifest;
import androidx.activity.ComponentActivity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;
import android.view.View;
import android.graphics.drawable.GradientDrawable;
import android.content.ContentValues;
import android.provider.MediaStore;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;

public class TachometerActivity extends ComponentActivity {
    private static final int CAMERA_PERMISSION_REQUEST = 301;
    private PreviewView previewView;
    private ExecutorService cameraExecutor;
    private TextView rpmText, statusText, statsText;
    private final AtomicBoolean measuring = new AtomicBoolean(false);
    private volatile boolean lastMark = false;
    private volatile long lastCrossingNs = 0L;
    private volatile double filteredRpm = 0;
    private volatile double minRpm = Double.MAX_VALUE, maxRpm = 0;
    private volatile long lastUiUpdate = 0;
    private volatile int frameCount = 0;
    private volatile long fpsWindowStart = 0;
    private volatile double fps = 0;
    private final StringBuilder log = new StringBuilder("Date,Time,RPM,CameraFPS\r\n");

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cameraExecutor = Executors.newSingleThreadExecutor();
        buildScreen();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) startCamera();
        else ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
    }

    private GradientDrawable bg(int color, int radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(radius); return d;
    }

    private TextView label(String s, int size, int color) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        t.setPadding(12, 8, 12, 8); return t;
    }

    private Button btn(String s, int color, Runnable r) {
        Button b = new Button(this); b.setText(s); b.setAllCaps(false); b.setTextColor(Color.WHITE);
        b.setBackground(bg(color, 24)); b.setOnClickListener(v -> r.run()); return b;
    }

    private void buildScreen() {
        FrameLayout root = new FrameLayout(this);
        previewView = new PreviewView(this);
        previewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        root.addView(previewView, new FrameLayout.LayoutParams(-1, -1));

        View guide = new View(this);
        android.graphics.drawable.GradientDrawable guideBg = new android.graphics.drawable.GradientDrawable();
        guideBg.setColor(0x2200FF80); guideBg.setStroke(2, 0xFF00FF80);
        guide.setBackground(guideBg);
        FrameLayout.LayoutParams gp = new FrameLayout.LayoutParams(-1, 80, Gravity.CENTER);
        gp.leftMargin = 30; gp.rightMargin = 30;
        root.addView(guide, gp);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(12, 8, 12, 8);
        panel.setBackgroundColor(0xDD102A43);
        FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(-1, -2, Gravity.TOP);
        root.addView(panel, pp);
        TextView title = label("ОПТИЧЕН ТАХОМЕТЪР", 18, Color.WHITE);
        title.setGravity(Gravity.CENTER); panel.addView(title);
        rpmText = label("--- RPM", 38, 0xFF67F5B4); rpmText.setGravity(Gravity.CENTER);
        rpmText.setTypeface(null, android.graphics.Typeface.BOLD); panel.addView(rpmText);
        statsText = label("MIN: ---   MAX: ---   FPS: ---", 14, Color.WHITE);
        statsText.setGravity(Gravity.CENTER); panel.addView(statsText);
        statusText = label("Постави контрастен светъл белег върху тъмен вал и насочи го към зелената зона.", 13, Color.WHITE);
        statusText.setGravity(Gravity.CENTER); panel.addView(statusText);

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setPadding(8, 8, 8, 8);
        controls.setBackgroundColor(0xDD102A43);
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        root.addView(controls, cp);
        controls.addView(btn("Старт / Нулиране", 0xFF079455, this::startMeasure), new LinearLayout.LayoutParams(0, -2, 1));
        controls.addView(btn("Стоп", 0xFFE04B59, this::stopMeasure), new LinearLayout.LayoutParams(0, -2, 1));
        controls.addView(btn("Запази CSV", 0xFF0878D1, this::saveLog), new LinearLayout.LayoutParams(0, -2, 1));
        TextView close = label("✕ Затвори", 15, Color.WHITE);
        close.setGravity(Gravity.CENTER); close.setBackgroundColor(0xCC000000);
        FrameLayout.LayoutParams cl = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        cl.bottomMargin = 100; root.addView(close, cl); close.setOnClickListener(v -> finish());
        setContentView(root);
    }

    private void startMeasure() {
        filteredRpm = 0; minRpm = Double.MAX_VALUE; maxRpm = 0; lastCrossingNs = 0; lastMark = false;
        synchronized (log) { log.setLength(0); log.append("Date,Time,RPM,CameraFPS\r\n"); }
        measuring.set(true);
        runOnUiThread(() -> statusText.setText("Измерването е включено. Дръж белега в зелената хоризонтална зона."));
    }

    private void stopMeasure() {
        measuring.set(false);
        runOnUiThread(() -> statusText.setText("Измерването е спряно."));
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                ImageAnalysis.Builder builder = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setTargetResolution(new android.util.Size(640, 480));
                // Request a high analysis rate where the device/camera supports it.
                ImageAnalysis analysis = builder.build();
                analysis.setAnalyzer(cameraExecutor, this::analyze);
                provider.unbindAll();
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis);
            } catch (Exception e) {
                Toast.makeText(this, "Камерата не може да се стартира: " + e.getMessage(), Toast.LENGTH_LONG).show();
                finish();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void analyze(ImageProxy proxy) {
        try {
            long now = System.nanoTime();
            if (fpsWindowStart == 0) fpsWindowStart = now;
            frameCount++;
            long elapsed = now - fpsWindowStart;
            if (elapsed >= 1_000_000_000L) {
                fps = frameCount * 1_000_000_000.0 / elapsed;
                frameCount = 0; fpsWindowStart = now;
            }
            if (!measuring.get()) return;
            android.media.Image image = proxy.getImage();
            if (image == null || image.getPlanes().length == 0) return;
            java.nio.ByteBuffer y = image.getPlanes()[0].getBuffer();
            int width = proxy.getWidth(), height = proxy.getHeight();
            int rowStride = image.getPlanes()[0].getRowStride();
            int pixelStride = image.getPlanes()[0].getPixelStride();
            int centerY = height / 2;
            int left = width / 5, right = width * 4 / 5;
            int bright = 0, samples = 0;
            for (int x = left; x < right; x += 3) {
                int index = centerY * rowStride + x * pixelStride;
                if (index >= 0 && index < y.limit()) {
                    if ((y.get(index) & 0xFF) > 185) bright++;
                    samples++;
                }
            }
            boolean mark = samples > 0 && bright >= samples * 0.22;
            if (mark && !lastMark) {
                long previous = lastCrossingNs;
                lastCrossingNs = now;
                if (previous > 0) {
                    double period = (now - previous) / 1_000_000_000.0;
                    double rpm = 60.0 / period;
                    // Reject implausible periods and common frame-noise spikes.
                    if (rpm >= 300 && rpm <= 6000) {
                        filteredRpm = filteredRpm == 0 ? rpm : filteredRpm * 0.65 + rpm * 0.35;
                        minRpm = Math.min(minRpm, filteredRpm);
                        maxRpm = Math.max(maxRpm, filteredRpm);
                        synchronized (log) {
                            Date d = new Date();
                            log.append(new SimpleDateFormat("yyyy-MM-dd,HH:mm:ss.SSS", Locale.ROOT).format(d))
                               .append(',').append(String.format(Locale.US, "%.1f", filteredRpm))
                               .append(',').append(String.format(Locale.US, "%.1f", fps)).append("\r\n");
                        }
                    }
                }
            }
            lastMark = mark;
            if (now - lastUiUpdate > 200_000_000L) {
                lastUiUpdate = now;
                final double r = filteredRpm, mn = minRpm, mx = maxRpm, fr = fps;
                runOnUiThread(() -> {
                    rpmText.setText(r > 0 ? String.format(Locale.getDefault(), "%.0f RPM", r) : "--- RPM");
                    statsText.setText("MIN: " + (mn == Double.MAX_VALUE ? "---" : String.format(Locale.getDefault(), "%.0f", mn))
                            + "   MAX: " + (mx == 0 ? "---" : String.format(Locale.getDefault(), "%.0f", mx))
                            + "   FPS: " + String.format(Locale.getDefault(), "%.0f", fr));
                    if (fr < 55) statusText.setText("Кадри: " + String.format(Locale.getDefault(), "%.0f FPS", fr)
                            + ". При 3000 об/мин е нужна надеждна честота над 50 кадъра/сек.");
                });
            }
        } catch (Exception ignored) {
        } finally {
            proxy.close();
        }
    }

    private void saveLog() {
        String data;
        synchronized (log) { data = log.toString(); }
        if (data.split("\\r\\n").length <= 1) {
            Toast.makeText(this, "Няма измервания за запис.", Toast.LENGTH_SHORT).show(); return;
        }
        String name = "tachometer_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(new Date()) + ".csv";
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, name);
            values.put(MediaStore.Downloads.MIME_TYPE, "text/csv");
            values.put(MediaStore.Downloads.RELATIVE_PATH, "Download");
            android.net.Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new Exception("Не може да се създаде CSV файл");
            try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                if (out == null) throw new Exception("Не може да се запише файлът");
                out.write(data.getBytes(StandardCharsets.UTF_8));
            }
            Toast.makeText(this, "Записан файл в Downloads: " + name, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Грешка при запис: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) startCamera();
            else { Toast.makeText(this, "Нужно е разрешение за камера.", Toast.LENGTH_LONG).show(); finish(); }
        }
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) cameraExecutor.shutdown();
    }
}

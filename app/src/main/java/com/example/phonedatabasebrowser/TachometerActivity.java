package com.example.phonedatabasebrowser;

// Импортираме клас, използван от този файл.
import android.Manifest;
// Импортираме клас, използван от този файл.
import androidx.activity.ComponentActivity;
// Импортираме клас, използван от този файл.
import android.content.pm.PackageManager;
// Импортираме клас, използван от този файл.
import android.graphics.Color;
// Импортираме клас, използван от този файл.
import android.os.Bundle;
// Импортираме клас, използван от този файл.
import android.view.Gravity;
// Импортираме клас, използван от този файл.
import android.widget.FrameLayout;
// Импортираме клас, използван от този файл.
import android.widget.LinearLayout;
// Импортираме клас, използван от този файл.
import android.widget.TextView;
// Импортираме клас, използван от този файл.
import android.widget.Button;
// Импортираме клас, използван от този файл.
import android.widget.Toast;
// Импортираме клас, използван от този файл.
import android.view.View;
// Импортираме клас, използван от този файл.
import android.graphics.drawable.GradientDrawable;
// Импортираме клас, използван от този файл.
import android.content.ContentValues;
// Импортираме клас, използван от този файл.
import android.provider.MediaStore;
// Импортираме клас, използван от този файл.
import java.io.OutputStream;
// Импортираме клас, използван от този файл.
import java.nio.charset.StandardCharsets;
// Импортираме клас, използван от този файл.
import java.text.SimpleDateFormat;
// Импортираме клас, използван от този файл.
import java.util.Date;
// Импортираме клас, използван от този файл.
import java.util.Locale;
// Импортираме клас, използван от този файл.
import java.util.concurrent.ExecutorService;
// Импортираме клас, използван от този файл.
import java.util.concurrent.Executors;
// Импортираме клас, използван от този файл.
import java.util.concurrent.atomic.AtomicBoolean;

// Импортираме клас, използван от този файл.
import androidx.camera.core.CameraSelector;
// Импортираме клас, използван от този файл.
import androidx.camera.core.ImageAnalysis;
// Импортираме клас, използван от този файл.
import androidx.camera.core.ImageProxy;
// Импортираме клас, използван от този файл.
import androidx.camera.core.Preview;
// Импортираме клас, използван от този файл.
import androidx.camera.lifecycle.ProcessCameraProvider;
// Импортираме клас, използван от този файл.
import androidx.camera.view.PreviewView;
// Импортираме клас, използван от този файл.
import androidx.core.app.ActivityCompat;
// Импортираме клас, използван от този файл.
import androidx.core.content.ContextCompat;
// Импортираме клас, използван от този файл.
import com.google.common.util.concurrent.ListenableFuture;

// Класът реализира тази част от функционалността на приложението.
public class TachometerActivity extends ComponentActivity {
    // Методът изпълнява отделна задача в работата на екрана.
    private static final int CAMERA_PERMISSION_REQUEST = 301;
    // Декларираме поле или член, който пази състояние на екрана.
    private PreviewView previewView;
    // Декларираме поле или член, който пази състояние на екрана.
    private ExecutorService cameraExecutor;
    // Декларираме поле или член, който пази състояние на екрана.
    private TextView rpmText, statusText, statsText;
    // Методът изпълнява отделна задача в работата на екрана.
    private final AtomicBoolean measuring = new AtomicBoolean(false);
    // Декларираме поле или член, който пази състояние на екрана.
    private volatile boolean lastMark = false;
    // Декларираме поле или член, който пази състояние на екрана.
    private volatile long lastCrossingNs = 0L;
    // Декларираме поле или член, който пази състояние на екрана.
    private volatile double filteredRpm = 0;
    // Декларираме поле или член, който пази състояние на екрана.
    private volatile double minRpm = Double.MAX_VALUE, maxRpm = 0;
    // Декларираме поле или член, който пази състояние на екрана.
    private volatile long lastUiUpdate = 0;
    // Декларираме поле или член, който пази състояние на екрана.
    private volatile int frameCount = 0;
    // Декларираме поле или член, който пази състояние на екрана.
    private volatile long fpsWindowStart = 0;
    // Декларираме поле или член, който пази състояние на екрана.
    private volatile double fps = 0;
    // Методът изпълнява отделна задача в работата на екрана.
    private final StringBuilder log = new StringBuilder("Date,Time,RPM,CameraFPS\r\n");

    // Реализираме callback, който Android извиква автоматично.
    @Override protected void onCreate(Bundle savedInstanceState) {
        // Извикваме стандартното поведение на родителския клас.
        super.onCreate(savedInstanceState);
        // Изчисляваме или присвояваме стойност за следващата стъпка.
        cameraExecutor = Executors.newSingleThreadExecutor();
        // Изпълняваме операцията, необходима за тази функционалност.
        buildScreen();
        // Проверяваме условието, преди да изпълним следващите действия.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) startCamera();
        // Алтернативен път, когато предишното условие не е изпълнено.
        else ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
    }

    // Методът изпълнява отделна задача в работата на екрана.
    private GradientDrawable bg(int color, int radius) {
        // Изчисляваме или присвояваме стойност за следващата стъпка.
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(radius); return d;
    }

    // Методът изпълнява отделна задача в работата на екрана.
    private TextView label(String s, int size, int color) {
        // Подготвяме променлива или обект, нужен за следващите операции.
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        // Изпълняваме операцията, необходима за тази функционалност.
        t.setPadding(12, 8, 12, 8); return t;
    }

    // Методът изпълнява отделна задача в работата на екрана.
    private Button btn(String s, int color, Runnable r) {
        // Подготвяме променлива или обект, нужен за следващите операции.
        Button b = new Button(this); b.setText(s); b.setAllCaps(false); b.setTextColor(Color.WHITE);
        // Свързваме натискането на елемента с неговото действие.
        b.setBackground(bg(color, 24)); b.setOnClickListener(v -> r.run()); return b;
    }

    // Методът изпълнява отделна задача в работата на екрана.
    private void buildScreen() {
        // Изчисляваме или присвояваме стойност за следващата стъпка.
        FrameLayout root = new FrameLayout(this);
        // Изчисляваме или присвояваме стойност за следващата стъпка.
        previewView = new PreviewView(this);
        // Изпълняваме операцията, необходима за тази функционалност.
        previewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        // Добавяме елемент към оформлението на екрана.
        root.addView(previewView, new FrameLayout.LayoutParams(-1, -1));

        // Подготвяме променлива или обект, нужен за следващите операции.
        View guide = new View(this);
        // Изчисляваме или присвояваме стойност за следващата стъпка.
        android.graphics.drawable.GradientDrawable guideBg = new android.graphics.drawable.GradientDrawable();
        // Изпълняваме операцията, необходима за тази функционалност.
        guideBg.setColor(0x2200FF80); guideBg.setStroke(2, 0xFF00FF80);
        // Изпълняваме операцията, необходима за тази функционалност.
        guide.setBackground(guideBg);
        // Изчисляваме или присвояваме стойност за следващата стъпка.
        FrameLayout.LayoutParams gp = new FrameLayout.LayoutParams(-1, 80, Gravity.CENTER);
        // Изчисляваме или присвояваме стойност за следващата стъпка.
        gp.leftMargin = 30; gp.rightMargin = 30;
        // Добавяме елемент към оформлението на екрана.
        root.addView(guide, gp);

        // Подготвяме променлива или обект, нужен за следващите операции.
        LinearLayout panel = new LinearLayout(this);
        // Изпълняваме операцията, необходима за тази функционалност.
        panel.setOrientation(LinearLayout.VERTICAL);
        // Изпълняваме операцията, необходима за тази функционалност.
        panel.setPadding(12, 8, 12, 8);
        // Изпълняваме операцията, необходима за тази функционалност.
        panel.setBackgroundColor(0xDD102A43);
        // Изчисляваме или присвояваме стойност за следващата стъпка.
        FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(-1, -2, Gravity.TOP);
        // Добавяме елемент към оформлението на екрана.
        root.addView(panel, pp);
        // Подготвяме променлива или обект, нужен за следващите операции.
        TextView title = label("ОПТИЧЕН ТАХОМЕТЪР", 18, Color.WHITE);
        // Добавяме елемент към оформлението на екрана.
        title.setGravity(Gravity.CENTER); panel.addView(title);
        // Изчисляваме или присвояваме стойност за следващата стъпка.
        rpmText = label("--- RPM", 38, 0xFF67F5B4); rpmText.setGravity(Gravity.CENTER);
        // Добавяме елемент към оформлението на екрана.
        rpmText.setTypeface(null, android.graphics.Typeface.BOLD); panel.addView(rpmText);
        // Изчисляваме или присвояваме стойност за следващата стъпка.
        statsText = label("MIN: ---   MAX: ---   FPS: ---", 14, Color.WHITE);
        // Добавяме елемент към оформлението на екрана.
        statsText.setGravity(Gravity.CENTER); panel.addView(statsText);
        // Изчисляваме или присвояваме стойност за следващата стъпка.
        statusText = label("Постави контрастен светъл белег върху тъмен вал и насочи го към зелената зона.", 13, Color.WHITE);
        // Добавяме елемент към оформлението на екрана.
        statusText.setGravity(Gravity.CENTER); panel.addView(statusText);

        // Подготвяме променлива или обект, нужен за следващите операции.
        LinearLayout controls = new LinearLayout(this);
        // Изпълняваме операцията, необходима за тази функционалност.
        controls.setOrientation(LinearLayout.HORIZONTAL);
        // Изпълняваме операцията, необходима за тази функционалност.
        controls.setPadding(8, 8, 8, 8);
        // Изпълняваме операцията, необходима за тази функционалност.
        controls.setBackgroundColor(0xDD102A43);
        // Изчисляваме или присвояваме стойност за следващата стъпка.
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        // Добавяме елемент към оформлението на екрана.
        root.addView(controls, cp);
        // Добавяме елемент към оформлението на екрана.
        controls.addView(btn("Старт / Нулиране", 0xFF079455, this::startMeasure), new LinearLayout.LayoutParams(0, -2, 1));
        // Добавяме елемент към оформлението на екрана.
        controls.addView(btn("Стоп", 0xFFE04B59, this::stopMeasure), new LinearLayout.LayoutParams(0, -2, 1));
        // Добавяме елемент към оформлението на екрана.
        controls.addView(btn("Запази CSV", 0xFF0878D1, this::saveLog), new LinearLayout.LayoutParams(0, -2, 1));
        // Подготвяме променлива или обект, нужен за следващите операции.
        TextView close = label("✕ Затвори", 15, Color.WHITE);
        // Изпълняваме операцията, необходима за тази функционалност.
        close.setGravity(Gravity.CENTER); close.setBackgroundColor(0xCC000000);
        // Изчисляваме или присвояваме стойност за следващата стъпка.
        FrameLayout.LayoutParams cl = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        // Свързваме натискането на елемента с неговото действие.
        cl.bottomMargin = 100; root.addView(close, cl); close.setOnClickListener(v -> finish());
        // Изпълняваме операцията, необходима за тази функционалност.
        setContentView(root);
    }

    // Методът изпълнява отделна задача в работата на екрана.
    private void startMeasure() {
        // Изчисляваме или присвояваме стойност за следващата стъпка.
        filteredRpm = 0; minRpm = Double.MAX_VALUE; maxRpm = 0; lastCrossingNs = 0; lastMark = false;
        // Добавяме ново съдържание към натрупваните данни.
        synchronized (log) { log.setLength(0); log.append("Date,Time,RPM,CameraFPS\r\n"); }
        // Изпълняваме операцията, необходима за тази функционалност.
        measuring.set(true);
        // Обновяваме текста, показан на екрана.
        runOnUiThread(() -> statusText.setText("Измерването е включено. Дръж белега в зелената хоризонтална зона."));
    }

    // Методът изпълнява отделна задача в работата на екрана.
    private void stopMeasure() {
        // Изпълняваме операцията, необходима за тази функционалност.
        measuring.set(false);
        // Обновяваме текста, показан на екрана.
        runOnUiThread(() -> statusText.setText("Измерването е спряно."));
    }

    // Методът изпълнява отделна задача в работата на екрана.
    private void startCamera() {
        // Изчисляваме или присвояваме стойност за следващата стъпка.
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        // Част от логиката на метода.
        future.addListener(() -> {
            // Изпълняваме операция, която може да предизвика изключение.
            try {
                // Подготвяме променлива или обект, нужен за следващите операции.
                ProcessCameraProvider provider = future.get();
                // Подготвяме променлива или обект, нужен за следващите операции.
                Preview preview = new Preview.Builder().build();
                // Изпълняваме операцията, необходима за тази функционалност.
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                // Подготвяме променлива или обект, нужен за следващите операции.
                ImageAnalysis.Builder builder = new ImageAnalysis.Builder()
                        // Част от логиката на метода.
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        // Изпълняваме операцията, необходима за тази функционалност.
                        .setTargetResolution(new android.util.Size(640, 480));
                // Request a high analysis rate where the device/camera supports it.
                ImageAnalysis analysis = builder.build();
                // Изпълняваме операцията, необходима за тази функционалност.
                analysis.setAnalyzer(cameraExecutor, this::analyze);
                // Изпълняваме операцията, необходима за тази функционалност.
                provider.unbindAll();
                // Изпълняваме операцията, необходима за тази функционалност.
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis);
            // Част от логиката на метода.
            } catch (Exception e) {
                // Показваме кратко съобщение на потребителя.
                Toast.makeText(this, "Камерата не може да се стартира: " + e.getMessage(), Toast.LENGTH_LONG).show();
                // Изпълняваме операцията, необходима за тази функционалност.
                finish();
            }
        // Изпълняваме операцията, необходима за тази функционалност.
        }, ContextCompat.getMainExecutor(this));
    }

    // Методът изпълнява отделна задача в работата на екрана.
    private void analyze(ImageProxy proxy) {
        // Изпълняваме операция, която може да предизвика изключение.
        try {
            // Подготвяме променлива или обект, нужен за следващите операции.
            long now = System.nanoTime();
            // Проверяваме условието, преди да изпълним следващите действия.
            if (fpsWindowStart == 0) fpsWindowStart = now;
            // Изпълняваме операцията, необходима за тази функционалност.
            frameCount++;
            // Подготвяме променлива или обект, нужен за следващите операции.
            long elapsed = now - fpsWindowStart;
            // Проверяваме условието, преди да изпълним следващите действия.
            if (elapsed >= 1_000_000_000L) {
                // Изчисляваме или присвояваме стойност за следващата стъпка.
                fps = frameCount * 1_000_000_000.0 / elapsed;
                // Изчисляваме или присвояваме стойност за следващата стъпка.
                frameCount = 0; fpsWindowStart = now;
            }
            // Проверяваме условието, преди да изпълним следващите действия.
            if (!measuring.get()) return;
            // Изчисляваме или присвояваме стойност за следващата стъпка.
            android.media.Image image = proxy.getImage();
            // Проверяваме условието, преди да изпълним следващите действия.
            if (image == null || image.getPlanes().length == 0) return;
            // Изчисляваме или присвояваме стойност за следващата стъпка.
            java.nio.ByteBuffer y = image.getPlanes()[0].getBuffer();
            // Подготвяме променлива или обект, нужен за следващите операции.
            int width = proxy.getWidth(), height = proxy.getHeight();
            // Подготвяме променлива или обект, нужен за следващите операции.
            int rowStride = image.getPlanes()[0].getRowStride();
            // Подготвяме променлива или обект, нужен за следващите операции.
            int pixelStride = image.getPlanes()[0].getPixelStride();
            // Подготвяме променлива или обект, нужен за следващите операции.
            int centerY = height / 2;
            // Подготвяме променлива или обект, нужен за следващите операции.
            int left = width / 5, right = width * 4 / 5;
            // Подготвяме променлива или обект, нужен за следващите операции.
            int bright = 0, samples = 0;
            // Обхождаме елементите, за да обработим всеки от тях.
            for (int x = left; x < right; x += 3) {
                // Подготвяме променлива или обект, нужен за следващите операции.
                int index = centerY * rowStride + x * pixelStride;
                // Проверяваме условието, преди да изпълним следващите действия.
                if (index >= 0 && index < y.limit()) {
                    // Проверяваме условието, преди да изпълним следващите действия.
                    if ((y.get(index) & 0xFF) > 185) bright++;
                    // Изпълняваме операцията, необходима за тази функционалност.
                    samples++;
                }
            }
            // Подготвяме променлива или обект, нужен за следващите операции.
            boolean mark = samples > 0 && bright >= samples * 0.22;
            // Проверяваме условието, преди да изпълним следващите действия.
            if (mark && !lastMark) {
                // Подготвяме променлива или обект, нужен за следващите операции.
                long previous = lastCrossingNs;
                // Изчисляваме или присвояваме стойност за следващата стъпка.
                lastCrossingNs = now;
                // Проверяваме условието, преди да изпълним следващите действия.
                if (previous > 0) {
                    // Подготвяме променлива или обект, нужен за следващите операции.
                    double period = (now - previous) / 1_000_000_000.0;
                    // Подготвяме променлива или обект, нужен за следващите операции.
                    double rpm = 60.0 / period;
                    // Reject implausible periods and common frame-noise spikes.
                    if (rpm >= 300 && rpm <= 6000) {
                        // Изпълняваме операцията, необходима за тази функционалност.
                        filteredRpm = filteredRpm == 0 ? rpm : filteredRpm * 0.65 + rpm * 0.35;
                        // Изчисляваме или присвояваме стойност за следващата стъпка.
                        minRpm = Math.min(minRpm, filteredRpm);
                        // Изчисляваме или присвояваме стойност за следващата стъпка.
                        maxRpm = Math.max(maxRpm, filteredRpm);
                        // Част от логиката на метода.
                        synchronized (log) {
                            // Подготвяме променлива или обект, нужен за следващите операции.
                            Date d = new Date();
                            // Добавяме ново съдържание към натрупваните данни.
                            log.append(new SimpleDateFormat("yyyy-MM-dd,HH:mm:ss.SSS", Locale.ROOT).format(d))
                               // Добавяме ново съдържание към натрупваните данни.
                               .append(',').append(String.format(Locale.US, "%.1f", filteredRpm))
                               // Добавяме ново съдържание към натрупваните данни.
                               .append(',').append(String.format(Locale.US, "%.1f", fps)).append("\r\n");
                        }
                    }
                }
            }
            // Изчисляваме или присвояваме стойност за следващата стъпка.
            lastMark = mark;
            // Проверяваме условието, преди да изпълним следващите действия.
            if (now - lastUiUpdate > 200_000_000L) {
                // Изчисляваме или присвояваме стойност за следващата стъпка.
                lastUiUpdate = now;
                // Подготвяме променлива или обект, нужен за следващите операции.
                final double r = filteredRpm, mn = minRpm, mx = maxRpm, fr = fps;
                // Част от логиката на метода.
                runOnUiThread(() -> {
                    // Обновяваме текста, показан на екрана.
                    rpmText.setText(r > 0 ? String.format(Locale.getDefault(), "%.0f RPM", r) : "--- RPM");
                    // Обновяваме текста, показан на екрана.
                    statsText.setText("MIN: " + (mn == Double.MAX_VALUE ? "---" : String.format(Locale.getDefault(), "%.0f", mn))
                            // Форматираме стойност за четливо показване.
                            + "   MAX: " + (mx == 0 ? "---" : String.format(Locale.getDefault(), "%.0f", mx))
                            // Форматираме стойност за четливо показване.
                            + "   FPS: " + String.format(Locale.getDefault(), "%.0f", fr));
                    // Проверяваме условието, преди да изпълним следващите действия.
                    if (fr < 55) statusText.setText("Кадри: " + String.format(Locale.getDefault(), "%.0f FPS", fr)
                            // Изпълняваме операцията, необходима за тази функционалност.
                            + ". При 3000 об/мин е нужна надеждна честота над 50 кадъра/сек.");
                });
            }
        // Част от логиката на метода.
        } catch (Exception ignored) {
        // Част от логиката на метода.
        } finally {
            // Изпълняваме операцията, необходима за тази функционалност.
            proxy.close();
        }
    }

    // Методът изпълнява отделна задача в работата на екрана.
    private void saveLog() {
        // Подготвяме променлива или обект, нужен за следващите операции.
        String data;
        // Изчисляваме или присвояваме стойност за следващата стъпка.
        synchronized (log) { data = log.toString(); }
        // Проверяваме условието, преди да изпълним следващите действия.
        if (data.split("\\r\\n").length <= 1) {
            // Показваме кратко съобщение на потребителя.
            Toast.makeText(this, "Няма измервания за запис.", Toast.LENGTH_SHORT).show(); return;
        }
        // Подготвяме променлива или обект, нужен за следващите операции.
        String name = "tachometer_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(new Date()) + ".csv";
        // Изпълняваме операция, която може да предизвика изключение.
        try {
            // Изчисляваме или присвояваме стойност за следващата стъпка.
            ContentValues values = new ContentValues();
            // Записваме или обновяваме стойност в хранилището.
            values.put(MediaStore.Downloads.DISPLAY_NAME, name);
            // Записваме или обновяваме стойност в хранилището.
            values.put(MediaStore.Downloads.MIME_TYPE, "text/csv");
            // Записваме или обновяваме стойност в хранилището.
            values.put(MediaStore.Downloads.RELATIVE_PATH, "Download");
            // Изчисляваме или присвояваме стойност за следващата стъпка.
            android.net.Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            // Проверяваме условието, преди да изпълним следващите действия.
            if (uri == null) throw new Exception("Не може да се създаде CSV файл");
            // Изпълняваме операция, която може да предизвика изключение.
            try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                // Проверяваме условието, преди да изпълним следващите действия.
                if (out == null) throw new Exception("Не може да се запише файлът");
                // Изпълняваме операцията, необходима за тази функционалност.
                out.write(data.getBytes(StandardCharsets.UTF_8));
            }
            // Показваме кратко съобщение на потребителя.
            Toast.makeText(this, "Записан файл в Downloads: " + name, Toast.LENGTH_LONG).show();
        // Част от логиката на метода.
        } catch (Exception e) {
            // Показваме кратко съобщение на потребителя.
            Toast.makeText(this, "Грешка при запис: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Реализираме callback, който Android извиква автоматично.
    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        // Извикваме стандартното поведение на родителския клас.
        super.onRequestPermissionsResult(requestCode, permissions, results);
        // Проверяваме условието, преди да изпълним следващите действия.
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            // Проверяваме условието, преди да изпълним следващите действия.
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) startCamera();
            // Алтернативен път, когато предишното условие не е изпълнено.
            else { Toast.makeText(this, "Нужно е разрешение за камера.", Toast.LENGTH_LONG).show(); finish(); }
        }
    }

    // Реализираме callback, който Android извиква автоматично.
    @Override protected void onDestroy() {
        // Извикваме стандартното поведение на родителския клас.
        super.onDestroy();
        // Проверяваме условието, преди да изпълним следващите действия.
        if (cameraExecutor != null) cameraExecutor.shutdown();
    }
}

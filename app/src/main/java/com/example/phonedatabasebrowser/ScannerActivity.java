package com.example.phonedatabasebrowser;

// Импортираме клас, използван от този файл.
import android.Manifest;
// Импортираме клас, използван от този файл.
import androidx.activity.ComponentActivity;
// Импортираме клас, използван от този файл.
import android.content.Intent;
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
import android.widget.TextView;
// Импортираме клас, използван от този файл.
import android.widget.Toast;

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
// Импортираме клас, използван от този файл.
import com.google.mlkit.vision.barcode.BarcodeScanning;
// Импортираме клас, използван от този файл.
import com.google.mlkit.vision.barcode.common.Barcode;
// Импортираме клас, използван от този файл.
import com.google.mlkit.vision.barcode.BarcodeScanner;
// Импортираме клас, използван от този файл.
import com.google.mlkit.vision.common.InputImage;

// Импортираме клас, използван от този файл.
import java.util.concurrent.ExecutorService;
// Импортираме клас, използван от този файл.
import java.util.concurrent.Executors;
// Импортираме клас, използван от този файл.
import java.util.concurrent.atomic.AtomicBoolean;

// Класът реализира тази част от функционалността на приложението.
public class ScannerActivity extends ComponentActivity {
    // Методът изпълнява отделна задача в работата на екрана.
    private static final int CAMERA_PERMISSION_REQUEST = 300;
    // Декларираме поле или член, който пази състояние на екрана.
    private PreviewView previewView;
    // Декларираме поле или член, който пази състояние на екрана.
    private ExecutorService cameraExecutor;
    // Декларираме поле или член, който пази състояние на екрана.
    private BarcodeScanner barcodeScanner;
    // Методът изпълнява отделна задача в работата на екрана.
    private final AtomicBoolean foundBarcode = new AtomicBoolean(false);

    // Реализираме callback, който Android извиква автоматично.
    @Override
    // Методът изпълнява отделна задача в работата на екрана.
    protected void onCreate(Bundle savedInstanceState) {
        // Извикваме стандартното поведение на родителския клас.
        super.onCreate(savedInstanceState);
        // Изчисляваме или присвояваме стойност за следващата стъпка.
        cameraExecutor = Executors.newSingleThreadExecutor();
        // Изчисляваме или присвояваме стойност за следващата стъпка.
        barcodeScanner = BarcodeScanning.getClient();
        // Изпълняваме операцията, необходима за тази функционалност.
        buildScreen();
        // Проверяваме условието, преди да изпълним следващите действия.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                // Част от логиката на метода.
                == PackageManager.PERMISSION_GRANTED) {
            // Изпълняваме операцията, необходима за тази функционалност.
            startCamera();
        // Част от логиката на метода.
        } else {
            // Проверяваме или заявяваме необходимото Android разрешение.
            ActivityCompat.requestPermissions(this,
                    // Създаваме нов обект за използване в тази функционалност.
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
        }
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
        TextView instruction = new TextView(this);
        // Обновяваме текста, показан на екрана.
        instruction.setText("Насочи камерата към баркода\nЗадръж неподвижно");
        // Изпълняваме операцията, необходима за тази функционалност.
        instruction.setTextColor(Color.WHITE);
        // Изпълняваме операцията, необходима за тази функционалност.
        instruction.setTextSize(18);
        // Изпълняваме операцията, необходима за тази функционалност.
        instruction.setGravity(Gravity.CENTER);
        // Изпълняваме операцията, необходима за тази функционалност.
        instruction.setPadding(20, 20, 20, 20);
        // Изпълняваме операцията, необходима за тази функционалност.
        instruction.setBackgroundColor(0x99000000);
        // Изчисляваме или присвояваме стойност за следващата стъпка.
        FrameLayout.LayoutParams instructionParams =
                // Създаваме нов обект за използване в тази функционалност.
                new FrameLayout.LayoutParams(-1, -2, Gravity.TOP);
        // Добавяме елемент към оформлението на екрана.
        root.addView(instruction, instructionParams);

        // Подготвяме променлива или обект, нужен за следващите операции.
        TextView close = new TextView(this);
        // Обновяваме текста, показан на екрана.
        close.setText("Затвори");
        // Изпълняваме операцията, необходима за тази функционалност.
        close.setTextColor(Color.WHITE);
        // Изпълняваме операцията, необходима за тази функционалност.
        close.setTextSize(16);
        // Изпълняваме операцията, необходима за тази функционалност.
        close.setGravity(Gravity.CENTER);
        // Изпълняваме операцията, необходима за тази функционалност.
        close.setPadding(28, 20, 28, 20);
        // Изпълняваме операцията, необходима за тази функционалност.
        close.setBackgroundColor(0xCC222222);
        // Свързваме натискането на елемента с неговото действие.
        close.setOnClickListener(v -> finish());
        // Изчисляваме или присвояваме стойност за следващата стъпка.
        FrameLayout.LayoutParams closeParams =
                // Създаваме нов обект за използване в тази функционалност.
                new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        // Изчисляваме или присвояваме стойност за следващата стъпка.
        closeParams.bottomMargin = 40;
        // Добавяме елемент към оформлението на екрана.
        root.addView(close, closeParams);
        // Изпълняваме операцията, необходима за тази функционалност.
        setContentView(root);
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
                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        // Част от логиката на метода.
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        // Изпълняваме операцията, необходима за тази функционалност.
                        .build();
                // Изпълняваме операцията, необходима за тази функционалност.
                analysis.setAnalyzer(cameraExecutor, this::analyzeImage);

                // Изпълняваме операцията, необходима за тази функционалност.
                provider.unbindAll();
                // Изпълняваме операцията, необходима за тази функционалност.
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis);
            // Част от логиката на метода.
            } catch (Exception e) {
                // Показваме кратко съобщение на потребителя.
                Toast.makeText(this, "Не може да се стартира камерата: " +
                        // Изпълняваме операцията, необходима за тази функционалност.
                        e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                // Изпълняваме операцията, необходима за тази функционалност.
                finish();
            }
        // Изпълняваме операцията, необходима за тази функционалност.
        }, ContextCompat.getMainExecutor(this));
    }

    // Методът изпълнява отделна задача в работата на екрана.
    private void analyzeImage(ImageProxy imageProxy) {
        // Проверяваме условието, преди да изпълним следващите действия.
        if (foundBarcode.get()) {
            // Изпълняваме операцията, необходима за тази функционалност.
            imageProxy.close();
            // Връщаме резултата или приключваме текущия метод.
            return;
        }
        // Изчисляваме или присвояваме стойност за следващата стъпка.
        android.media.Image mediaImage = imageProxy.getImage();
        // Проверяваме условието, преди да изпълним следващите действия.
        if (mediaImage == null) {
            // Изпълняваме операцията, необходима за тази функционалност.
            imageProxy.close();
            // Връщаме резултата или приключваме текущия метод.
            return;
        }
        // Изчисляваме или присвояваме стойност за следващата стъпка.
        InputImage image = InputImage.fromMediaImage(
                // Изпълняваме операцията, необходима за тази функционалност.
                mediaImage, imageProxy.getImageInfo().getRotationDegrees());
        // Част от логиката на метода.
        barcodeScanner.process(image)
                // Част от логиката на метода.
                .addOnSuccessListener(barcodes -> {
                    // Проверяваме условието, преди да изпълним следващите действия.
                    if (foundBarcode.get()) return;
                    // Обхождаме елементите, за да обработим всеки от тях.
                    for (Barcode barcode : barcodes) {
                        // Подготвяме променлива или обект, нужен за следващите операции.
                        String value = barcode.getRawValue();
                        // Проверяваме условието, преди да изпълним следващите действия.
                        if (value != null && !value.trim().isEmpty()
                                // Част от логиката на метода.
                                && foundBarcode.compareAndSet(false, true)) {
                            // Подготвяме променлива или обект, нужен за следващите операции.
                            Intent result = new Intent();
                            // Записваме или обновяваме стойност в хранилището.
                            result.putExtra("barcode_value", value);
                            // Записваме или обновяваме стойност в хранилището.
                            result.putExtra("barcode_format", barcode.getFormat());
                            // Изпълняваме операцията, необходима за тази функционалност.
                            setResult(RESULT_OK, result);
                            // Изпълняваме операцията, необходима за тази функционалност.
                            runOnUiThread(this::finish);
                            // Прекратяваме текущия цикъл или клон на switch.
                            break;
                        }
                    }
                // Част от логиката на метода.
                })
                // Част от логиката на метода.
                .addOnFailureListener(e -> {
                    // Ignore individual frames that cannot be decoded.
                })
                // Изпълняваме операцията, необходима за тази функционалност.
                .addOnCompleteListener(task -> imageProxy.close());
    }

    // Реализираме callback, който Android извиква автоматично.
    @Override
    // Методът изпълнява отделна задача в работата на екрана.
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // Извикваме стандартното поведение на родителския клас.
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Проверяваме условието, преди да изпълним следващите действия.
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            // Проверяваме условието, преди да изпълним следващите действия.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Изпълняваме операцията, необходима за тази функционалност.
                startCamera();
            // Част от логиката на метода.
            } else {
                // Показваме кратко съобщение на потребителя.
                Toast.makeText(this, "За сканиране е необходимо разрешение за камерата.", Toast.LENGTH_LONG).show();
                // Изпълняваме операцията, необходима за тази функционалност.
                setResult(RESULT_CANCELED);
                // Изпълняваме операцията, необходима за тази функционалност.
                finish();
            }
        }
    }

    // Реализираме callback, който Android извиква автоматично.
    @Override
    // Методът изпълнява отделна задача в работата на екрана.
    protected void onDestroy() {
        // Извикваме стандартното поведение на родителския клас.
        super.onDestroy();
        // Проверяваме условието, преди да изпълним следващите действия.
        if (cameraExecutor != null) cameraExecutor.shutdown();
        // Проверяваме условието, преди да изпълним следващите действия.
        if (barcodeScanner != null) barcodeScanner.close();
    }
}

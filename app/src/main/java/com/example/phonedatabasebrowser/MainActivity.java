package com.example.phonedatabasebrowser;

// Импортираме клас, използван от този файл.
import android.app.Activity;
// Импортираме клас, използван от този файл.
import android.graphics.Color;
// Импортираме клас, използван от този файл.
import android.graphics.drawable.GradientDrawable;
// Импортираме клас, използван от този файл.
import android.app.AlertDialog;
// Импортираме клас, използван от този файл.
import android.content.Intent;
// Импортираме клас, използван от този файл.
import android.content.ClipData;
// Импортираме клас, използван от този файл.
import android.content.ClipboardManager;
// Импортираме клас, използван от този файл.
import android.database.Cursor;
// Импортираме клас, използван от този файл.
import android.database.sqlite.SQLiteDatabase;
// Импортираме клас, използван от този файл.
import android.net.Uri;
// Импортираме клас, използван от този файл.
import android.os.Bundle;
// Импортираме клас, използван от този файл.
import android.provider.DocumentsContract;
// Импортираме клас, използван от този файл.
import java.io.OutputStream;
// Импортираме клас, използван от този файл.
import java.nio.charset.StandardCharsets;
// Импортираме клас, използван от този файл.
import java.text.SimpleDateFormat;
// Импортираме клас, използван от този файл.
import java.util.Date;
// Импортираме клас, използван от този файл.
import android.view.View;
// Импортираме клас, използван от този файл.
import android.widget.Button;
// Импортираме клас, използван от този файл.
import android.widget.EditText;
// Импортираме клас, използван от този файл.
import android.widget.LinearLayout;
// Импортираме клас, използван от този файл.
import android.widget.ScrollView;
// Импортираме клас, използван от този файл.
import android.widget.TextView;
// Импортираме клас, използван от този файл.
import android.widget.Toast;

// Импортираме клас, използван от този файл.
import com.google.mlkit.vision.barcode.common.Barcode;

// Импортираме клас, използван от този файл.
import java.io.File;
// Импортираме клас, използван от този файл.
import java.io.FileOutputStream;
// Импортираме клас, използван от този файл.
import java.io.InputStream;
// Импортираме клас, използван от този файл.
import java.util.ArrayList;
// Импортираме клас, използван от този файл.
import java.util.Locale;

// Класът реализира тази част от функционалността на приложението.
public class MainActivity extends Activity {
    // Методът изпълнява отделна задача в работата на екрана.
    private static final int PICK_FOLDER = 101;
    // Методът изпълнява отделна задача в работата на екрана.
    private static final int SCAN_BARCODE = 102;
    // Методът изпълнява отделна задача в работата на екрана.
    private static final int PICK_LOG_FOLDER = 103;
    // Декларираме поле или член, който пази състояние на екрана.
    private LinearLayout content;
    // Декларираме поле или член, който пази състояние на екрана.
    private Uri folderUri;
    // Декларираме поле или член, който пази състояние на екрана.
    private Uri logFolderUri;
    // Декларираме поле или член, който пази състояние на екрана.
    private LinearLayout queueContainer;
    // Декларираме поле или член, който пази състояние на екрана.
    private TextView queueCount;
    // Методът изпълнява отделна задача в работата на екрана.
    private final ArrayList<ScanEntry> scanQueue = new ArrayList<>();

    // Методът изпълнява отделна задача в работата на екрана.
    private static class ScanEntry {
        // Подготвяме променлива или обект, нужен за следващите операции.
        String value;
        // Подготвяме променлива или обект, нужен за следващите операции.
        final String date;
        // Подготвяме променлива или обект, нужен за следващите операции.
        final String time;
        // Подготвяме променлива или обект, нужен за следващите операции.
        final String format;
        // Подготвяме променлива или обект, нужен за следващите операции.
        EditText editor;

        // Част от логиката на метода.
        ScanEntry(String value, String date, String time, String format) {
            // Променяме или използваме състояние на текущия обект.
            this.value = value;
            // Променяме или използваме състояние на текущия обект.
            this.date = date;
            // Променяме или използваме състояние на текущия обект.
            this.time = time;
            // Променяме или използваме състояние на текущия обект.
            this.format = format;
        }
    }

    // Реализираме callback, който Android извиква автоматично.
    @Override public void onCreate(Bundle savedInstanceState) {
        // Извикваме стандартното поведение на родителския клас.
        super.onCreate(savedInstanceState);
        // Подготвяме променлива или обект, нужен за следващите операции.
        String savedLogFolder = getPreferences(MODE_PRIVATE).getString("log_folder_uri", null);
        // Проверяваме условието, преди да изпълним следващите действия.
        if (savedLogFolder != null) logFolderUri = Uri.parse(savedLogFolder);
        // Изпълняваме операцията, необходима за тази функционалност.
        showHome();
    }

    // Методът изпълнява отделна задача в работата на екрана.
    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

    // Методът изпълнява отделна задача в работата на екрана.
    private TextView text(String s, int size, int color) {
        // Подготвяме променлива или обект, нужен за следващите операции.
        TextView t = new TextView(this);
        // Обновяваме текста, показан на екрана.
        t.setText(s); t.setTextSize(size); t.setTextColor(color);
        // Изпълняваме операцията, необходима за тази функционалност.
        t.setPadding(dp(12), dp(9), dp(12), dp(9));
        // Връщаме резултата или приключваме текущия метод.
        return t;
    }

    // Методът изпълнява отделна задача в работата на екрана.
    private GradientDrawable shape(int color, int stroke, int radiusDp) {
        // Изчисляваме или присвояваме стойност за следващата стъпка.
        GradientDrawable d = new GradientDrawable();
        // Изпълняваме операцията, необходима за тази функционалност.
        d.setColor(color);
        // Изпълняваме операцията, необходима за тази функционалност.
        d.setCornerRadius(dp(radiusDp));
        // Проверяваме условието, преди да изпълним следващите действия.
        if (stroke != 0) d.setStroke(dp(1), stroke);
        // Връщаме резултата или приключваме текущия метод.
        return d;
    }

    // Методът изпълнява отделна задача в работата на екрана.
    private Button button(String label, Runnable action) {
        // Подготвяме променлива или обект, нужен за следващите операции.
        Button b = new Button(this);
        // Обновяваме текста, показан на екрана.
        b.setText(label);
        // Изпълняваме операцията, необходима за тази функционалност.
        b.setAllCaps(false);
        // Изпълняваме операцията, необходима за тази функционалност.
        b.setTextSize(14);
        // Изпълняваме операцията, необходима за тази функционалност.
        b.setTextColor(Color.WHITE);
        // Изпълняваме операцията, необходима за тази функционалност.
        b.setTypeface(null, android.graphics.Typeface.BOLD);
        // Изпълняваме операцията, необходима за тази функционалност.
        b.setPadding(dp(10), dp(8), dp(10), dp(8));
        // Изпълняваме операцията, необходима за тази функционалност.
        b.setBackground(shape(0xFF0878D1, 0, 12));
        // Изпълняваме операцията, необходима за тази функционалност.
        b.setElevation(dp(2));
        // Свързваме натискането на елемента с неговото действие.
        b.setOnClickListener(v -> action.run());
        // Връщаме резултата или приключваме текущия метод.
        return b;
    }

    // Методът изпълнява отделна задача в работата на екрана.
    private Button coloredButton(String label, int color, Runnable action) {
        // Подготвяме променлива или обект, нужен за следващите операции.
        Button b = button(label, action);
        // Изпълняваме операцията, необходима за тази функционалност.
        b.setBackground(shape(color, 0, 12));
        // Връщаме резултата или приключваме текущия метод.
        return b;
    }

    // Методът изпълнява отделна задача в работата на екрана.
    private void showHome() {
        // Подготвяме променлива или обект, нужен за следващите операции.
        LinearLayout root = new LinearLayout(this);
        // Изпълняваме операцията, необходима за тази функционалност.
        root.setOrientation(LinearLayout.VERTICAL);
        // Изпълняваме операцията, необходима за тази функционалност.
        root.setBackgroundColor(0xFFF1F6FC);

        // Подготвяме променлива или обект, нужен за следващите операции.
        LinearLayout header = new LinearLayout(this);
        // Изпълняваме операцията, необходима за тази функционалност.
        header.setOrientation(LinearLayout.VERTICAL);
        // Изпълняваме операцията, необходима за тази функционалност.
        header.setPadding(dp(12), dp(10), dp(12), dp(12));
        // Изпълняваме операцията, необходима за тази функционалност.
        header.setBackground(shape(0xFF0878D1, 0, 0));
        // Подготвяме променлива или обект, нужен за следващите операции.
        TextView title = text("▤  Phone Database Browser", 22, Color.WHITE);
        // Изпълняваме операцията, необходима за тази функционалност.
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        // Добавяме елемент към оформлението на екрана.
        header.addView(title);
        // Добавяме елемент към оформлението на екрана.
        header.addView(text("Сканиране  •  Таблица  •  Запис във файлове", 13, 0xFFE1F1FF));
        // Добавяме елемент към оформлението на екрана.
        root.addView(header);

        // Подготвяме променлива или обект, нужен за следващите операции.
        LinearLayout actions = new LinearLayout(this);
        // Изпълняваме операцията, необходима за тази функционалност.
        actions.setPadding(dp(8), dp(8), dp(8), dp(4));
        // Изпълняваме операцията, необходима за тази функционалност.
        actions.setOrientation(LinearLayout.HORIZONTAL);
        // Подготвяме променлива или обект, нужен за следващите операции.
        Button scan = coloredButton("📷 Сканирай баркод", 0xFF079455, this::scanBarcode);
        // Добавяме елемент към оформлението на екрана.
        actions.addView(scan, new LinearLayout.LayoutParams(0, dp(54), 1));
        // Добавяме елемент към оформлението на екрана.
        root.addView(actions);
        // Подготвяме променлива или обект, нужен за следващите операции.
        LinearLayout tachAction = new LinearLayout(this);
        // Изпълняваме операцията, необходима за тази функционалност.
        tachAction.setPadding(dp(8), dp(0), dp(8), dp(4));
        // Добавяме елемент към оформлението на екрана.
        tachAction.addView(coloredButton("⟳  Оптичен тахометър (500–3000 RPM)", 0xFF6236C9, () -> startActivity(new Intent(this, TachometerActivity.class))), new LinearLayout.LayoutParams(-1, dp(52)));
        // Добавяме елемент към оформлението на екрана.
        root.addView(tachAction);
        // Подготвяме променлива или обект, нужен за следващите операции.
        LinearLayout dynoAction = new LinearLayout(this);
        // Изпълняваме операцията, необходима за тази функционалност.
        dynoAction.setPadding(dp(8), dp(0), dp(8), dp(4));
        // Добавяме елемент към оформлението на екрана.
        dynoAction.addView(coloredButton("🚗  Автомобилен динамометър", 0xFFB45309, () -> startActivity(new Intent(this, CarDynoActivity.class))), new LinearLayout.LayoutParams(-1, dp(52)));
        // Добавяме елемент към оформлението на екрана.
        root.addView(dynoAction);

        // Подготвяме променлива или обект, нужен за следващите операции.
        LinearLayout summary = new LinearLayout(this);
        // Изпълняваме операцията, необходима за тази функционалност.
        summary.setPadding(dp(12), dp(4), dp(12), dp(4));
        // Изпълняваме операцията, необходима за тази функционалност.
        summary.setBackground(shape(0xFFE3F1FF, 0xFFB9D9F7, 12));
        // Изчисляваме или присвояваме стойност за следващата стъпка.
        queueCount = text("В ТАБЛИЦАТА: 0", 16, 0xFF103D73);
        // Изпълняваме операцията, необходима за тази функционалност.
        queueCount.setTypeface(null, android.graphics.Typeface.BOLD);
        // Добавяме елемент към оформлението на екрана.
        summary.addView(queueCount);
        // Добавяме елемент към оформлението на екрана.
        root.addView(summary, new LinearLayout.LayoutParams(-1, -2));

        // Изчисляваме или присвояваме стойност за следващата стъпка.
        ScrollView queueScroll = new ScrollView(this);
        // Изчисляваме или присвояваме стойност за следващата стъпка.
        queueContainer = new LinearLayout(this);
        // Изпълняваме операцията, необходима за тази функционалност.
        queueContainer.setOrientation(LinearLayout.VERTICAL);
        // Изпълняваме операцията, необходима за тази функционалност.
        queueContainer.setPadding(dp(8), dp(4), dp(8), dp(4));
        // Добавяме елемент към оформлението на екрана.
        queueScroll.addView(queueContainer);
        // Добавяме елемент към оформлението на екрана.
        root.addView(queueScroll, new LinearLayout.LayoutParams(-1, 0, 6));
        // Изпълняваме операцията, необходима за тази функционалност.
        renderQueue();

        // Добавяме елемент към оформлението на екрана.
        root.addView(coloredButton("💾  ЗАПИШИ ВСИЧКИ И ИЗЧИСТИ ТАБЛИЦАТА", 0xFF079455, this::saveQueuedCodes),
                // Създаваме нов обект за използване в тази функционалност.
                new LinearLayout.LayoutParams(-1, dp(58)));

        // Подготвяме променлива или обект, нужен за следващите операции.
        LinearLayout dbActions = new LinearLayout(this);
        // Изпълняваме операцията, необходима за тази функционалност.
        dbActions.setPadding(dp(8), dp(3), dp(8), dp(3));
        // Добавяме елемент към оформлението на екрана.
        dbActions.addView(coloredButton("Избери папка с бази данни", 0xFF6236C9, this::pickFolder),
                // Създаваме нов обект за използване в тази функционалност.
                new LinearLayout.LayoutParams(-1, dp(48)));
        // Добавяме елемент към оформлението на екрана.
        root.addView(dbActions);

        // Добавяме елемент към оформлението на екрана.
        root.addView(text("Файлове: scanned_codes.txt  |  scanned_codes.csv", 11, 0xFF506780));
        // Изчисляваме или присвояваме стойност за следващата стъпка.
        ScrollView dbScroll = new ScrollView(this);
        // Изчисляваме или присвояваме стойност за следващата стъпка.
        content = new LinearLayout(this);
        // Изпълняваме операцията, необходима за тази функционалност.
        content.setOrientation(LinearLayout.VERTICAL);
        // Добавяме елемент към оформлението на екрана.
        dbScroll.addView(content);
        // Добавяме елемент към оформлението на екрана.
        root.addView(dbScroll, new LinearLayout.LayoutParams(-1, 0, 2));
        // Изпълняваме операцията, необходима за тази функционалност.
        setContentView(root);
        // Проверяваме условието, преди да изпълним следващите действия.
        if (folderUri != null) scanFolder(folderUri);
    }

    // Методът изпълнява отделна задача в работата на екрана.
    private void scanBarcode() {
        // Изпълняваме операцията, необходима за тази функционалност.
        startActivityForResult(new Intent(this, ScannerActivity.class), SCAN_BARCODE);
    }

    // Реализираме callback, който Android извиква автоматично.
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Извикваме стандартното поведение на родителския клас.
        super.onActivityResult(requestCode, resultCode, data);
        // Проверяваме условието, преди да изпълним следващите действия.
        if (requestCode == SCAN_BARCODE && resultCode == RESULT_OK && data != null) {
            // Подготвяме променлива или обект, нужен за следващите операции.
            String value = data.getStringExtra("barcode_value");
            // Подготвяме променлива или обект, нужен за следващите операции.
            int format = data.getIntExtra("barcode_format", 0);
            // Проверяваме условието, преди да изпълним следващите действия.
            if (value == null || value.trim().isEmpty()) {
                // Показваме кратко съобщение на потребителя.
                Toast.makeText(this, "No readable barcode found.", Toast.LENGTH_LONG).show();
                // Връщаме резултата или приключваме текущия метод.
                return;
            }
            // Подготвяме променлива или обект, нужен за следващите операции.
            Date now = new Date();
            // Част от логиката на метода.
            scanQueue.add(new ScanEntry(value,
                    // Създаваме нов обект за използване в тази функционалност.
                    new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(now),
                    // Създаваме нов обект за използване в тази функционалност.
                    new SimpleDateFormat("HH:mm:ss", Locale.ROOT).format(now),
                    // Изпълняваме операцията, необходима за тази функционалност.
                    barcodeFormatName(format)));
            // Изпълняваме операцията, необходима за тази функционалност.
            renderQueue();
            // Показваме кратко съобщение на потребителя.
            Toast.makeText(this, "Добавен в таблицата. Общо: " + scanQueue.size(), Toast.LENGTH_SHORT).show();
        }
        // Проверяваме условието, преди да изпълним следващите действия.
        if (requestCode == PICK_LOG_FOLDER && resultCode == RESULT_OK && data != null) {
            // Подготвяме променлива или обект, нужен за следващите операции.
            Uri selectedUri = data.getData();
            // Изчисляваме или присвояваме стойност за следващата стъпка.
            logFolderUri = selectedUri;
            // Изпълняваме операция, която може да предизвика изключение.
            try {
                // Some document providers return a document URI rather than a tree URI.
                // Convert it to a tree URI before using DocumentsContract tree operations.
                if (selectedUri != null && !DocumentsContract.isTreeUri(selectedUri)) {
                    // Подготвяме променлива или обект, нужен за следващите операции.
                    String documentId;
                    // Изпълняваме операция, която може да предизвика изключение.
                    try {
                        // Изчисляваме или присвояваме стойност за следващата стъпка.
                        documentId = DocumentsContract.getDocumentId(selectedUri);
                    // Част от логиката на метода.
                    } catch (Exception ignored) {
                        // Изчисляваме или присвояваме стойност за следващата стъпка.
                        documentId = Uri.decode(selectedUri.getLastPathSegment());
                    }
                    // Изчисляваме или присвояваме стойност за следващата стъпка.
                    logFolderUri = DocumentsContract.buildTreeDocumentUri(
                            // Изпълняваме операцията, необходима за тази функционалност.
                            selectedUri.getAuthority(), documentId);
                }
                // Част от логиката на метода.
                getContentResolver().takePersistableUriPermission(logFolderUri,
                        // Подготвяме променлива или обект, нужен за следващите операции.
                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            // Част от логиката на метода.
            } catch (Exception e) {
                // Показваме кратко съобщение на потребителя.
                Toast.makeText(this, "Не успях да запазя достъпа до папката: " + e.getMessage(),
                        // Изпълняваме операцията, необходима за тази функционалност.
                        Toast.LENGTH_LONG).show();
            }
            // Записваме или обновяваме стойност в хранилището.
            getPreferences(MODE_PRIVATE).edit().putString("log_folder_uri", logFolderUri.toString()).apply();
            // Показваме кратко съобщение на потребителя.
            Toast.makeText(this, "Папката за логове е запазена.", Toast.LENGTH_LONG).show();
            // Изпълняваме операцията, необходима за тази функционалност.
            showHome();
            // Връщаме резултата или приключваме текущия метод.
            return;
        }
        // Проверяваме условието, преди да изпълним следващите действия.
        if (requestCode == PICK_FOLDER && resultCode == RESULT_OK && data != null) {
            // Изчисляваме или присвояваме стойност за следващата стъпка.
            folderUri = data.getData();
            // Изпълняваме операция, която може да предизвика изключение.
            try { getContentResolver().takePersistableUriPermission(folderUri, Intent.FLAG_GRANT_READ_URI_PERMISSION); }
            // Обработваме грешка, за да запазим контролирано поведението на приложението.
            catch (Exception ignored) {}
            // Изпълняваме операцията, необходима за тази функционалност.
            showHome();
        }
    }

    // Методът изпълнява отделна задача в работата на екрана.
    private String barcodeFormatName(int format) {
        // Избираме действие според стойността на израза.
        switch (format) {
            // Обработваме конкретен вариант на избора.
            case Barcode.FORMAT_QR_CODE: return "QR Code";
            // Обработваме конкретен вариант на избора.
            case Barcode.FORMAT_CODE_128: return "Code 128";
            // Обработваме конкретен вариант на избора.
            case Barcode.FORMAT_CODE_39: return "Code 39";
            // Обработваме конкретен вариант на избора.
            case Barcode.FORMAT_CODE_93: return "Code 93";
            // Обработваме конкретен вариант на избора.
            case Barcode.FORMAT_CODABAR: return "Codabar";
            // Обработваме конкретен вариант на избора.
            case Barcode.FORMAT_DATA_MATRIX: return "Data Matrix";
            // Обработваме конкретен вариант на избора.
            case Barcode.FORMAT_EAN_13: return "EAN-13";
            // Обработваме конкретен вариант на избора.
            case Barcode.FORMAT_EAN_8: return "EAN-8";
            // Обработваме конкретен вариант на избора.
            case Barcode.FORMAT_ITF: return "ITF";
            // Обработваме конкретен вариант на избора.
            case Barcode.FORMAT_UPC_A: return "UPC-A";
            // Обработваме конкретен вариант на избора.
            case Barcode.FORMAT_UPC_E: return "UPC-E";
            // Обработваме конкретен вариант на избора.
            case Barcode.FORMAT_PDF417: return "PDF417";
            // Обработваме конкретен вариант на избора.
            case Barcode.FORMAT_AZTEC: return "Aztec";
            // Обработваме конкретен вариант на избора.
            default: return "Other";
        }
    }


    // Методът изпълнява отделна задача в работата на екрана.
    private void pickLogFolder() {
        // Подготвяме променлива или обект, нужен за следващите операции.
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        // Част от логиката на метода.
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                // Изпълняваме операцията, необходима за тази функционалност.
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        // Изпълняваме операцията, необходима за тази функционалност.
        startActivityForResult(i, PICK_LOG_FOLDER);
    }

    // Методът изпълнява отделна задача в работата на екрана.
    private void renderQueue() {
        // Проверяваме условието, преди да изпълним следващите действия.
        if (queueContainer == null || queueCount == null) return;
        // Обхождаме елементите, за да обработим всеки от тях.
        for (ScanEntry entry : scanQueue) {
            // Проверяваме условието, преди да изпълним следващите действия.
            if (entry.editor != null) entry.value = entry.editor.getText().toString();
        }
        // Изпълняваме операцията, необходима за тази функционалност.
        queueContainer.removeAllViews();
        // Обновяваме текста, показан на екрана.
        queueCount.setText("▦  В ТАБЛИЦАТА: " + scanQueue.size());
        // Подготвяме променлива или обект, нужен за следващите операции.
        LinearLayout table = new LinearLayout(this);
        // Изпълняваме операцията, необходима за тази функционалност.
        table.setOrientation(LinearLayout.VERTICAL);
        // Изпълняваме операцията, необходима за тази функционалност.
        table.setBackground(shape(Color.WHITE, 0xFFBDD4EC, 8));
        // Изпълняваме операцията, необходима за тази функционалност.
        table.setClipToOutline(true);

        // Подготвяме променлива или обект, нужен за следващите операции.
        LinearLayout headings = new LinearLayout(this);
        // Изпълняваме операцията, необходима за тази функционалност.
        headings.setOrientation(LinearLayout.HORIZONTAL);
        // Изпълняваме операцията, необходима за тази функционалност.
        headings.setBackgroundColor(0xFF086EC4);
        // Добавяме елемент към оформлението на екрана.
        headings.addView(tableCell("№", 0.45f, true, true));
        // Добавяме елемент към оформлението на екрана.
        headings.addView(tableCell("Дата / час", 1.15f, true, true));
        // Добавяме елемент към оформлението на екрана.
        headings.addView(tableCell("Баркод / номер (редактируем)", 2.0f, true, true));
        // Добавяме елемент към оформлението на екрана.
        headings.addView(tableCell("Изтрий", 0.7f, true, true));
        // Добавяме елемент към оформлението на екрана.
        table.addView(headings);

        // Проверяваме условието, преди да изпълним следващите действия.
        if (scanQueue.isEmpty()) {
            // Подготвяме променлива или обект, нужен за следващите операции.
            TextView empty = text("Тук ще се появява всеки сканиран номер.\nМожеш да редактираш номерата или да изтриеш ненужните редове преди запис.", 14, 0xFF52677E);
            // Изпълняваме операцията, необходима за тази функционалност.
            empty.setGravity(android.view.Gravity.CENTER);
            // Изпълняваме операцията, необходима за тази функционалност.
            empty.setPadding(dp(12), dp(24), dp(12), dp(24));
            // Добавяме елемент към оформлението на екрана.
            table.addView(empty);
        // Част от логиката на метода.
        } else {
            // Обхождаме елементите, за да обработим всеки от тях.
            for (int i = 0; i < scanQueue.size(); i++) {
                // Изчисляваме или присвояваме стойност за следващата стъпка.
                final ScanEntry entry = scanQueue.get(i);
                // Подготвяме променлива или обект, нужен за следващите операции.
                LinearLayout row = new LinearLayout(this);
                // Изпълняваме операцията, необходима за тази функционалност.
                row.setOrientation(LinearLayout.HORIZONTAL);
                // Изпълняваме операцията, необходима за тази функционалност.
                row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                // Изпълняваме операцията, необходима за тази функционалност.
                row.setBackgroundColor(i % 2 == 0 ? 0xFFFFFFFF : 0xFFEAF4FF);
                // Добавяме елемент към оформлението на екрана.
                row.addView(tableCell(String.valueOf(i + 1), 0.45f, false, false));
                // Добавяме елемент към оформлението на екрана.
                row.addView(tableCell(entry.date + "\n" + entry.time, 1.15f, false, false));
                // Подготвяме променлива или обект, нужен за следващите операции.
                LinearLayout editCell = new LinearLayout(this);
                // Изпълняваме операцията, необходима за тази функционалност.
                editCell.setPadding(dp(3), dp(4), dp(3), dp(4));
                // Изчисляваме или присвояваме стойност за следващата стъпка.
                entry.editor = new EditText(this);
                // Изпълняваме операцията, необходима за тази функционалност.
                entry.editor.setSingleLine(true);
                // Изпълняваме операцията, необходима за тази функционалност.
                entry.editor.setTextSize(14);
                // Изпълняваме операцията, необходима за тази функционалност.
                entry.editor.setTextColor(0xFF102D50);
                // Изпълняваме операцията, необходима за тази функционалност.
                entry.editor.setHint("Номер");
                // Изпълняваме операцията, необходима за тази функционалност.
                entry.editor.setPadding(dp(8), dp(4), dp(8), dp(4));
                // Изпълняваме операцията, необходима за тази функционалност.
                entry.editor.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                // Изпълняваме операцията, необходима за тази функционалност.
                entry.editor.setBackground(shape(0xFFF8FBFF, 0xFFB7D4F3, 6));
                // Обновяваме текста, показан на екрана.
                entry.editor.setText(entry.value);
                // Добавяме елемент към оформлението на екрана.
                editCell.addView(entry.editor, new LinearLayout.LayoutParams(-1, dp(46)));
                // Добавяме елемент към оформлението на екрана.
                row.addView(editCell, new LinearLayout.LayoutParams(0, -2, 2.0f));
                // Подготвяме променлива или обект, нужен за следващите операции.
                LinearLayout delCell = new LinearLayout(this);
                // Изпълняваме операцията, необходима за тази функционалност.
                delCell.setGravity(android.view.Gravity.CENTER);
                // Подготвяме променлива или обект, нужен за следващите операции.
                Button delete = coloredButton("✕", 0xFFE83F55, () -> {
                    // Изчисляваме или присвояваме стойност за следващата стъпка.
                    entry.value = entry.editor.getText().toString();
                    // Изпълняваме операцията, необходима за тази функционалност.
                    scanQueue.remove(entry);
                    // Изпълняваме операцията, необходима за тази функционалност.
                    renderQueue();
                });
                // Изпълняваме операцията, необходима за тази функционалност.
                delete.setTextSize(14);
                // Добавяме елемент към оформлението на екрана.
                delCell.addView(delete, new LinearLayout.LayoutParams(dp(42), dp(42)));
                // Добавяме елемент към оформлението на екрана.
                row.addView(delCell, new LinearLayout.LayoutParams(0, dp(58), 0.7f));
                // Добавяме елемент към оформлението на екрана.
                table.addView(row);
                // Подготвяме променлива или обект, нужен за следващите операции.
                View line = new View(this);
                // Изпълняваме операцията, необходима за тази функционалност.
                line.setBackgroundColor(0xFFD7E5F3);
                // Добавяме елемент към оформлението на екрана.
                table.addView(line, new LinearLayout.LayoutParams(-1, dp(1)));
            }
        }
        // Добавяме елемент към оформлението на екрана.
        queueContainer.addView(table);
        // Добавяме елемент към оформлението на екрана.
        queueContainer.addView(text("Форматът се запазва автоматично от скенера. Дата и час са от момента на сканиране.", 11, 0xFF5D7188));
    }

    // Методът изпълнява отделна задача в работата на екрана.
    private TextView tableCell(String value, float weight, boolean header, boolean centered) {
        // Подготвяме променлива или обект, нужен за следващите операции.
        TextView t = text(value, header ? 12 : 12, header ? Color.WHITE : 0xFF163A60);
        // Изпълняваме операцията, необходима за тази функционалност.
        t.setGravity(centered ? android.view.Gravity.CENTER : android.view.Gravity.CENTER_VERTICAL);
        // Проверяваме условието, преди да изпълним следващите действия.
        if (header) t.setTypeface(null, android.graphics.Typeface.BOLD);
        // Изпълняваме операцията, необходима за тази функционалност.
        t.setPadding(dp(4), dp(10), dp(4), dp(10));
        // Връщаме резултата или приключваме текущия метод.
        return t;
    }

    // Методът изпълнява отделна задача в работата на екрана.
    private void saveQueuedCodes() {
        // Проверяваме условието, преди да изпълним следващите действия.
        if (scanQueue.isEmpty()) {
            // Показваме кратко съобщение на потребителя.
            Toast.makeText(this, "Таблицата е празна.", Toast.LENGTH_SHORT).show();
            // Връщаме резултата или приключваме текущия метод.
            return;
        }
        // Обхождаме елементите, за да обработим всеки от тях.
        for (ScanEntry entry : scanQueue) {
            // Проверяваме условието, преди да изпълним следващите действия.
            if (entry.editor != null) entry.value = entry.editor.getText().toString().trim();
            // Проверяваме условието, преди да изпълним следващите действия.
            if (entry.value.isEmpty()) {
                // Показваме кратко съобщение на потребителя.
                Toast.makeText(this, "Има празен номер. Попълни го или изтрий реда.", Toast.LENGTH_LONG).show();
                // Връщаме резултата или приключваме текущия метод.
                return;
            }
        }
        // Изпълняваме операция, която може да предизвика изключение.
        try {
            // Подготвяме променлива или обект, нужен за следващите операции.
            File dir = getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS);
            // Проверяваме условието, преди да изпълним следващите действия.
            if (dir == null) dir = getFilesDir();
            // Проверяваме условието, преди да изпълним следващите действия.
            if (!dir.exists() && !dir.mkdirs()) throw new Exception("Не може да се създаде папка за логовете");
            // Подготвяме променлива или обект, нужен за следващите операции.
            File txt = new File(dir, "scanned_codes.txt");
            // Подготвяме променлива или обект, нужен за следващите операции.
            File csvFile = new File(dir, "scanned_codes.csv");
            // Изчисляваме или присвояваме стойност за следващата стъпка.
            StringBuilder txtRows = new StringBuilder();
            // Изчисляваме или присвояваме стойност за следващата стъпка.
            StringBuilder csvRows = new StringBuilder();
            // Обхождаме елементите, за да обработим всеки от тях.
            for (ScanEntry entry : scanQueue) {
                // Добавяме ново съдържание към натрупваните данни.
                txtRows.append(entry.date).append(" ").append(entry.time).append(" | ")
                        // Добавяме ново съдържание към натрупваните данни.
                        .append(entry.format).append(" | ").append(entry.value).append(System.lineSeparator());
                // Добавяме ново съдържание към натрупваните данни.
                csvRows.append(csv(entry.date)).append(",").append(csv(entry.time)).append(",")
                        // Добавяме ново съдържание към натрупваните данни.
                        .append(csv(entry.value)).append(",").append(csv(entry.format)).append("\r\n");
            }
            // Изпълняваме операция, която може да предизвика изключение.
            try (FileOutputStream out = new FileOutputStream(txt, true)) {
                // Изпълняваме операцията, необходима за тази функционалност.
                out.write(txtRows.toString().getBytes(StandardCharsets.UTF_8));
            }
            // Подготвяме променлива или обект, нужен за следващите операции.
            boolean newCsv = !csvFile.exists() || csvFile.length() == 0;
            // Изпълняваме операция, която може да предизвика изключение.
            try (FileOutputStream out = new FileOutputStream(csvFile, true)) {
                // Проверяваме условието, преди да изпълним следващите действия.
                if (newCsv) {
                    // Изпълняваме операцията, необходима за тази функционалност.
                    out.write(new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF});
                    // Изпълняваме операцията, необходима за тази функционалност.
                    out.write("Дата,Час,Баркод,Формат\r\n".getBytes(StandardCharsets.UTF_8));
                }
                // Изпълняваме операцията, необходима за тази функционалност.
                out.write(csvRows.toString().getBytes(StandardCharsets.UTF_8));
            }
            // Подготвяме променлива или обект, нужен за следващите операции.
            int saved = scanQueue.size();
            // Изпълняваме операцията, необходима за тази функционалност.
            scanQueue.clear();
            // Изпълняваме операцията, необходима за тази функционалност.
            renderQueue();
            // Показваме кратко съобщение на потребителя.
            Toast.makeText(this, "Записани са " + saved + " реда. Таблицата е изчистена.", Toast.LENGTH_LONG).show();
        // Част от логиката на метода.
        } catch (Exception e) {
            // Показваме кратко съобщение на потребителя.
            Toast.makeText(this, "Грешка при запис: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Методът изпълнява отделна задача в работата на екрана.
    private String csv(String value) {
        // Проверяваме условието, преди да изпълним следващите действия.
        if (value == null) value = "";
        // Връщаме резултата или приключваме текущия метод.
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    // Методът изпълнява отделна задача в работата на екрана.
    private void appendToLogFile(String fileName, String line, boolean excelCsv) throws Exception {
        // Подготвяме променлива или обект, нужен за следващите операции.
        Uri fileUri = null;
        // Подготвяме променлива или обект, нужен за следващите операции.
        String treeId = DocumentsContract.getTreeDocumentId(logFolderUri);
        // Подготвяме променлива или обект, нужен за следващите операции.
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(logFolderUri, treeId);
        // Изпълняваме операция, която може да предизвика изключение.
        try (Cursor cursor = getContentResolver().query(childrenUri,
                // Създаваме нов обект за използване в тази функционалност.
                new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        // Част от логиката на метода.
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME},
                // Част от логиката на метода.
                null, null, null)) {
            // Проверяваме условието, преди да изпълним следващите действия.
            if (cursor != null) {
                // Повтаряме действията, докато условието остава вярно.
                while (cursor.moveToNext()) {
                    // Проверяваме условието, преди да изпълним следващите действия.
                    if (fileName.equals(cursor.getString(1))) {
                        // Изчисляваме или присвояваме стойност за следващата стъпка.
                        fileUri = DocumentsContract.buildDocumentUriUsingTree(logFolderUri, cursor.getString(0));
                        // Прекратяваме текущия цикъл или клон на switch.
                        break;
                    }
                }
            }
        }
        // Подготвяме променлива или обект, нужен за следващите операции.
        boolean newFile = fileUri == null;
        // Проверяваме условието, преди да изпълним следващите действия.
        if (newFile) fileUri = DocumentsContract.createDocument(
                // Изпълняваме операцията, необходима за тази функционалност.
                getContentResolver(), logFolderUri, "text/plain", fileName);
        // Проверяваме условието, преди да изпълним следващите действия.
        if (fileUri == null) throw new Exception("Не може да се създаде " + fileName);
        // Изпълняваме операция, която може да предизвика изключение.
        try (OutputStream out = getContentResolver().openOutputStream(fileUri, "wa")) {
            // Проверяваме условието, преди да изпълним следващите действия.
            if (out == null) throw new Exception("Не може да се отвори " + fileName);
            // Проверяваме условието, преди да изпълним следващите действия.
            if (newFile && excelCsv) {
                // Изпълняваме операцията, необходима за тази функционалност.
                out.write(new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF});
                // Изпълняваме операцията, необходима за тази функционалност.
                out.write("Дата,Час,Баркод,Формат\r\n".getBytes(StandardCharsets.UTF_8));
            }
            // Изпълняваме операцията, необходима за тази функционалност.
            out.write(line.getBytes(StandardCharsets.UTF_8));
            // Изпълняваме операцията, необходима за тази функционалност.
            out.flush();
        }
    }

    // Методът изпълнява отделна задача в работата на екрана.
    private void pickFolder() {
        // Подготвяме променлива или обект, нужен за следващите операции.
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        // Изпълняваме операцията, необходима за тази функционалност.
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        // Изпълняваме операцията, необходима за тази функционалност.
        startActivityForResult(i, PICK_FOLDER);
    }


    // Методът изпълнява отделна задача в работата на екрана.
    private void scanFolder(Uri tree) {
        // Изпълняваме операцията, необходима за тази функционалност.
        content.removeAllViews();
        // Добавяме елемент към оформлението на екрана.
        content.addView(text("Searching selected folder…", 16, 0xFF14233B));
        // Подготвяме променлива или обект, нужен за следващите операции.
        ArrayList<Uri> found = new ArrayList<>();
        // Изпълняваме операцията, необходима за тази функционалност.
        walk(tree, found, 0);
        // Изпълняваме операцията, необходима за тази функционалност.
        content.removeAllViews();
        // Проверяваме условието, преди да изпълним следващите действия.
        if (found.isEmpty()) {
            // Добавяме елемент към оформлението на екрана.
            content.addView(text("No .db, .sqlite, .sqlite3 or .db3 files found in this folder and its accessible subfolders.", 15, 0xFF53627A));
            // Връщаме резултата или приключваме текущия метод.
            return;
        }
        // Добавяме елемент към оформлението на екрана.
        content.addView(text("Found " + found.size() + " database file(s)", 17, 0xFF14233B));
        // Обхождаме елементите, за да обработим всеки от тях.
        for (Uri u : found) {
            // Подготвяме променлива или обект, нужен за следващите операции.
            String name = displayName(u);
            // Подготвяме променлива или обект, нужен за следващите операции.
            Button b = button("▣  " + name, () -> showDatabase(u, name));
            // Добавяме елемент към оформлението на екрана.
            content.addView(b);
        }
    }

    // Методът изпълнява отделна задача в работата на екрана.
    private void walk(Uri parent, ArrayList<Uri> found, int depth) {
        // Проверяваме условието, преди да изпълним следващите действия.
        if (depth > 12 || found.size() > 500) return;
        // Изпълняваме операция, която може да предизвика изключение.
        try {
            // Подготвяме променлива или обект, нужен за следващите операции.
            String docId = DocumentsContract.getTreeDocumentId(folderUri);
            // Проверяваме условието, преди да изпълним следващите действия.
            if (!parent.equals(folderUri)) docId = DocumentsContract.getDocumentId(parent);
            // Подготвяме променлива или обект, нужен за следващите операции.
            Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(folderUri, docId);
            // Изпълняваме операция, която може да предизвика изключение.
            try (Cursor c = getContentResolver().query(children,
                    // Създаваме нов обект за използване в тази функционалност.
                    new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                            // Част от логиката на метода.
                            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                            // Част от логиката на метода.
                            DocumentsContract.Document.COLUMN_MIME_TYPE}, null, null, null)) {
                // Проверяваме условието, преди да изпълним следващите действия.
                if (c == null) return;
                // Повтаряме действията, докато условието остава вярно.
                while (c.moveToNext()) {
                    // Подготвяме променлива или обект, нужен за следващите операции.
                    String id = c.getString(0), name = c.getString(1), mime = c.getString(2);
                    // Подготвяме променлива или обект, нужен за следващите операции.
                    Uri child = DocumentsContract.buildDocumentUriUsingTree(folderUri, id);
                    // Проверяваме условието, преди да изпълним следващите действия.
                    if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) walk(child, found, depth + 1);
                    // Алтернативен път, когато предишното условие не е изпълнено.
                    else if (isDatabase(name)) found.add(child);
                }
            }
        // Част от логиката на метода.
        } catch (Exception ignored) {}
    }

    // Методът изпълнява отделна задача в работата на екрана.
    private boolean isDatabase(String n) {
        // Проверяваме условието, преди да изпълним следващите действия.
        if (n == null) return false;
        // Подготвяме променлива или обект, нужен за следващите операции.
        String x = n.toLowerCase(Locale.ROOT);
        // Връщаме резултата или приключваме текущия метод.
        return x.endsWith(".db") || x.endsWith(".sqlite") || x.endsWith(".sqlite3") || x.endsWith(".db3");
    }

    // Методът изпълнява отделна задача в работата на екрана.
    private String displayName(Uri u) {
        // Изпълняваме операция, която може да предизвика изключение.
        try (Cursor c = getContentResolver().query(u, new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME}, null, null, null)) {
            // Проверяваме условието, преди да изпълним следващите действия.
            if (c != null && c.moveToFirst()) return c.getString(0);
        // Част от логиката на метода.
        } catch (Exception ignored) {}
        // Връщаме резултата или приключваме текущия метод.
        return u.getLastPathSegment() == null ? "database" : u.getLastPathSegment();
    }

    // Методът изпълнява отделна задача в работата на екрана.
    private void showDatabase(Uri uri, String name) {
        // Подготвяме променлива или обект, нужен за следващите операции.
        File copy = null;
        // Изпълняваме операция, която може да предизвика изключение.
        try {
            // Изчисляваме или присвояваме стойност за следващата стъпка.
            copy = File.createTempFile("dbview_", ".sqlite", getCacheDir());
            // Изпълняваме операция, която може да предизвика изключение.
            try (InputStream in = getContentResolver().openInputStream(uri);
                 // Изчисляваме или присвояваме стойност за следващата стъпка.
                 FileOutputStream out = new FileOutputStream(copy)) {
                // Проверяваме условието, преди да изпълним следващите действия.
                if (in == null) throw new Exception("Cannot read this file.");
                // Подготвяме променлива или обект, нужен за следващите операции.
                byte[] buf = new byte[8192]; int n;
                // Повтаряме действията, докато условието остава вярно.
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            }
            // Подготвяме променлива или обект, нужен за следващите операции.
            SQLiteDatabase db = SQLiteDatabase.openDatabase(copy.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
            // Подготвяме променлива или обект, нужен за следващите операции.
            ArrayList<String> tables = new ArrayList<>();
            // Изпълняваме операция, която може да предизвика изключение.
            try (Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name", null)) {
                // Повтаряме действията, докато условието остава вярно.
                while (c.moveToNext()) tables.add(c.getString(0));
            }
            // Подготвяме променлива или обект, нужен за следващите операции.
            final File dbCopy = copy;
            // Подготвяме променлива или обект, нужен за следващите операции.
            LinearLayout panel = new LinearLayout(this); panel.setOrientation(LinearLayout.VERTICAL);
            // Добавяме елемент към оформлението на екрана.
            panel.addView(text(name, 22, 0xFF14233B));
            // Добавяме елемент към оформлението на екрана.
            panel.addView(text("Tables: " + tables.size() + " · opened read-only", 14, 0xFF53627A));
            // Изчисляваме или присвояваме стойност за следващата стъпка.
            ScrollView sc = new ScrollView(this);
            // Подготвяме променлива или обект, нужен за следващите операции.
            LinearLayout list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL);
            // Проверяваме условието, преди да изпълним следващите действия.
            if (tables.isEmpty()) list.addView(text("No user tables found, or this is not a readable SQLite database.", 14, 0xFF53627A));
            // Обхождаме елементите, за да обработим всеки от тях.
            for (String table : tables) list.addView(button("▤  " + table, () -> showTable(db, table)));
            // Добавяме елемент към оформлението на екрана.
            sc.addView(list); panel.addView(sc, new LinearLayout.LayoutParams(-1, dp(330)));
            // Създаваме нов обект за използване в тази функционалност.
            new AlertDialog.Builder(this).setView(panel).setPositiveButton("Close", (d, w) -> {
                // Изпълняваме операция, която може да предизвика изключение.
                try { db.close(); } catch (Exception ignored) {}
                // Изпълняваме операцията, необходима за тази функционалност.
                dbCopy.delete();
            // Изпълняваме операцията, необходима за тази функционалност.
            }).show();
        // Част от логиката на метода.
        } catch (Exception e) {
            // Проверяваме условието, преди да изпълним следващите действия.
            if (copy != null) copy.delete();
            // Показваме кратко съобщение на потребителя.
            Toast.makeText(this, "Cannot open database: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Методът изпълнява отделна задача в работата на екрана.
    private void showTable(SQLiteDatabase db, String table) {
        // Изпълняваме операция, която може да предизвика изключение.
        try {
            // Подготвяме променлива или обект, нужен за следващите операции.
            Cursor c = db.rawQuery("SELECT * FROM " + quoteIdentifier(table) + " LIMIT 200", null);
            // Изчисляваме или присвояваме стойност за следващата стъпка.
            StringBuilder out = new StringBuilder();
            // Добавяме ново съдържание към натрупваните данни.
            out.append("Table: ").append(table).append("\nShowing up to 200 rows\n\n");
            // Обхождаме елементите, за да обработим всеки от тях.
            for (int i = 0; i < c.getColumnCount(); i++) out.append(c.getColumnName(i)).append(i == c.getColumnCount()-1 ? "\n" : "  |  ");
            // Добавяме ново съдържание към натрупваните данни.
            out.append("────────────────────────\n");
            // Подготвяме променлива или обект, нужен за следващите операции.
            int rows = 0;
            // Повтаряме действията, докато условието остава вярно.
            while (c.moveToNext()) {
                // Изпълняваме операцията, необходима за тази функционалност.
                rows++;
                // Обхождаме елементите, за да обработим всеки от тях.
                for (int i = 0; i < c.getColumnCount(); i++) {
                    // Подготвяме променлива или обект, нужен за следващите операции.
                    String value;
                    // Избираме действие според стойността на израза.
                    switch (c.getType(i)) {
                        // Обработваме конкретен вариант на избора.
                        case Cursor.FIELD_TYPE_NULL:
                            // Изчисляваме или присвояваме стойност за следващата стъпка.
                            value = "NULL";
                            // Прекратяваме текущия цикъл или клон на switch.
                            break;
                        // Обработваме конкретен вариант на избора.
                        case Cursor.FIELD_TYPE_INTEGER:
                            // Изчисляваме или присвояваме стойност за следващата стъпка.
                            value = Long.toString(c.getLong(i));
                            // Прекратяваме текущия цикъл или клон на switch.
                            break;
                        // Обработваме конкретен вариант на избора.
                        case Cursor.FIELD_TYPE_FLOAT:
                            // Изчисляваме или присвояваме стойност за следващата стъпка.
                            value = Double.toString(c.getDouble(i));
                            // Прекратяваме текущия цикъл или клон на switch.
                            break;
                        // Обработваме конкретен вариант на избора.
                        case Cursor.FIELD_TYPE_BLOB:
                            // Подготвяме променлива или обект, нужен за следващите операции.
                            byte[] blob = c.getBlob(i);
                            // Изчисляваме или присвояваме стойност за следващата стъпка.
                            StringBuilder hex = new StringBuilder();
                            // Подготвяме променлива или обект, нужен за следващите операции.
                            int preview = Math.min(blob.length, 16);
                            // Обхождаме елементите, за да обработим всеки от тях.
                            for (int j = 0; j < preview; j++) {
                                // Проверяваме условието, преди да изпълним следващите действия.
                                if (j > 0) hex.append(' ');
                                // Добавяме ново съдържание към натрупваните данни.
                                hex.append(String.format(Locale.ROOT, "%02X", blob[j] & 0xFF));
                            }
                            // Изчисляваме или присвояваме стойност за следващата стъпка.
                            value = "[BLOB: " + blob.length + " bytes" +
                                    // Изпълняваме операцията, необходима за тази функционалност.
                                    (preview > 0 ? "; hex: " + hex + (blob.length > preview ? " …" : "") : "") + "]";
                            // Прекратяваме текущия цикъл или клон на switch.
                            break;
                        // Обработваме конкретен вариант на избора.
                        case Cursor.FIELD_TYPE_STRING:
                        // Обработваме конкретен вариант на избора.
                        default:
                            // Изчисляваме или присвояваме стойност за следващата стъпка.
                            value = c.getString(i);
                            // Проверяваме условието, преди да изпълним следващите действия.
                            if (value != null && value.length() > 180) value = value.substring(0, 180) + "…";
                            // Прекратяваме текущия цикъл или клон на switch.
                            break;
                    }
                    // Добавяме ново съдържание към натрупваните данни.
                    out.append(value == null ? "" : value).append(i == c.getColumnCount()-1 ? "\n" : "  |  ");
                }
                // Добавяме ново съдържание към натрупваните данни.
                out.append("\n");
            }
            // Добавяме ново съдържание към натрупваните данни.
            out.append("\nRows shown: ").append(rows);
            // Изпълняваме операцията, необходима за тази функционалност.
            c.close();
            // Подготвяме променлива или обект, нужен за следващите операции.
            TextView tv = text(out.toString(), 12, 0xFF14233B);
            // Изпълняваме операцията, необходима за тази функционалност.
            tv.setTextIsSelectable(true);
            // Добавяме елемент към оформлението на екрана.
            ScrollView sc = new ScrollView(this); sc.addView(tv);
            // Създаваме нов обект за използване в тази функционалност.
            new AlertDialog.Builder(this).setTitle(table).setView(sc).setPositiveButton("Close", null).show();
        // Част от логиката на метода.
        } catch (Exception e) {
            // Показваме кратко съобщение на потребителя.
            Toast.makeText(this, "Cannot read table: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Методът изпълнява отделна задача в работата на екрана.
    private String quoteIdentifier(String value) {
        // Връщаме резултата или приключваме текущия метод.
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}

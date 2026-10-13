package com.example.phonedatabasebrowser;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.mlkit.vision.barcode.common.Barcode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int PICK_FOLDER = 101;
    private static final int SCAN_BARCODE = 102;
    private static final int PICK_LOG_FOLDER = 103;
    private LinearLayout content;
    private Uri folderUri;
    private Uri logFolderUri;
    private LinearLayout queueContainer;
    private TextView queueCount;
    private final ArrayList<ScanEntry> scanQueue = new ArrayList<>();

    private static class ScanEntry {
        String value;
        final String date;
        final String time;
        final String format;
        EditText editor;

        ScanEntry(String value, String date, String time, String format) {
            this.value = value;
            this.date = date;
            this.time = time;
            this.format = format;
        }
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String savedLogFolder = getPreferences(MODE_PRIVATE).getString("log_folder_uri", null);
        if (savedLogFolder != null) logFolderUri = Uri.parse(savedLogFolder);
        showHome();
    }

    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

    private TextView text(String s, int size, int color) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size); t.setTextColor(color);
        t.setPadding(dp(12), dp(9), dp(12), dp(9));
        return t;
    }

    private GradientDrawable shape(int color, int stroke, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        if (stroke != 0) d.setStroke(dp(1), stroke);
        return d;
    }

    private Button button(String label, Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setTextColor(Color.WHITE);
        b.setTypeface(null, android.graphics.Typeface.BOLD);
        b.setPadding(dp(10), dp(8), dp(10), dp(8));
        b.setBackground(shape(0xFF0878D1, 0, 12));
        b.setElevation(dp(2));
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private Button coloredButton(String label, int color, Runnable action) {
        Button b = button(label, action);
        b.setBackground(shape(color, 0, 12));
        return b;
    }

    private void showHome() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFF1F6FC);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(12), dp(10), dp(12), dp(12));
        header.setBackground(shape(0xFF0878D1, 0, 0));
        TextView title = text("▤  Phone Database Browser", 22, Color.WHITE);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        header.addView(title);
        header.addView(text("Сканиране  •  Таблица  •  Запис във файлове", 13, 0xFFE1F1FF));
        root.addView(header);

        LinearLayout actions = new LinearLayout(this);
        actions.setPadding(dp(8), dp(8), dp(8), dp(4));
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button scan = coloredButton("📷 Сканирай баркод", 0xFF079455, this::scanBarcode);
        actions.addView(scan, new LinearLayout.LayoutParams(0, dp(54), 1));
        root.addView(actions);
        LinearLayout tachAction = new LinearLayout(this);
        tachAction.setPadding(dp(8), dp(0), dp(8), dp(4));
        tachAction.addView(coloredButton("⟳  Оптичен тахометър (500–3000 RPM)", 0xFF6236C9, () -> startActivity(new Intent(this, TachometerActivity.class))), new LinearLayout.LayoutParams(-1, dp(52)));
        root.addView(tachAction);
        LinearLayout dynoAction = new LinearLayout(this);
        dynoAction.setPadding(dp(8), dp(0), dp(8), dp(4));
        dynoAction.addView(coloredButton("🚗  Автомобилен динамометър", 0xFFB45309, () -> startActivity(new Intent(this, CarDynoActivity.class))), new LinearLayout.LayoutParams(-1, dp(52)));
        root.addView(dynoAction);

        LinearLayout summary = new LinearLayout(this);
        summary.setPadding(dp(12), dp(4), dp(12), dp(4));
        summary.setBackground(shape(0xFFE3F1FF, 0xFFB9D9F7, 12));
        queueCount = text("В ТАБЛИЦАТА: 0", 16, 0xFF103D73);
        queueCount.setTypeface(null, android.graphics.Typeface.BOLD);
        summary.addView(queueCount);
        root.addView(summary, new LinearLayout.LayoutParams(-1, -2));

        ScrollView queueScroll = new ScrollView(this);
        queueContainer = new LinearLayout(this);
        queueContainer.setOrientation(LinearLayout.VERTICAL);
        queueContainer.setPadding(dp(8), dp(4), dp(8), dp(4));
        queueScroll.addView(queueContainer);
        root.addView(queueScroll, new LinearLayout.LayoutParams(-1, 0, 6));
        renderQueue();

        root.addView(coloredButton("💾  ЗАПИШИ ВСИЧКИ И ИЗЧИСТИ ТАБЛИЦАТА", 0xFF079455, this::saveQueuedCodes),
                new LinearLayout.LayoutParams(-1, dp(58)));

        LinearLayout dbActions = new LinearLayout(this);
        dbActions.setPadding(dp(8), dp(3), dp(8), dp(3));
        dbActions.addView(coloredButton("Избери папка с бази данни", 0xFF6236C9, this::pickFolder),
                new LinearLayout.LayoutParams(-1, dp(48)));
        root.addView(dbActions);

        root.addView(text("Файлове: scanned_codes.txt  |  scanned_codes.csv", 11, 0xFF506780));
        ScrollView dbScroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        dbScroll.addView(content);
        root.addView(dbScroll, new LinearLayout.LayoutParams(-1, 0, 2));
        setContentView(root);
        if (folderUri != null) scanFolder(folderUri);
    }

    private void scanBarcode() {
        startActivityForResult(new Intent(this, ScannerActivity.class), SCAN_BARCODE);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SCAN_BARCODE && resultCode == RESULT_OK && data != null) {
            String value = data.getStringExtra("barcode_value");
            int format = data.getIntExtra("barcode_format", 0);
            if (value == null || value.trim().isEmpty()) {
                Toast.makeText(this, "No readable barcode found.", Toast.LENGTH_LONG).show();
                return;
            }
            Date now = new Date();
            scanQueue.add(new ScanEntry(value,
                    new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(now),
                    new SimpleDateFormat("HH:mm:ss", Locale.ROOT).format(now),
                    barcodeFormatName(format)));
            renderQueue();
            Toast.makeText(this, "Добавен в таблицата. Общо: " + scanQueue.size(), Toast.LENGTH_SHORT).show();
        }
        if (requestCode == PICK_LOG_FOLDER && resultCode == RESULT_OK && data != null) {
            Uri selectedUri = data.getData();
            logFolderUri = selectedUri;
            try {
                // Some document providers return a document URI rather than a tree URI.
                // Convert it to a tree URI before using DocumentsContract tree operations.
                if (selectedUri != null && !DocumentsContract.isTreeUri(selectedUri)) {
                    String documentId;
                    try {
                        documentId = DocumentsContract.getDocumentId(selectedUri);
                    } catch (Exception ignored) {
                        documentId = Uri.decode(selectedUri.getLastPathSegment());
                    }
                    logFolderUri = DocumentsContract.buildTreeDocumentUri(
                            selectedUri.getAuthority(), documentId);
                }
                getContentResolver().takePersistableUriPermission(logFolderUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            } catch (Exception e) {
                Toast.makeText(this, "Не успях да запазя достъпа до папката: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
            getPreferences(MODE_PRIVATE).edit().putString("log_folder_uri", logFolderUri.toString()).apply();
            Toast.makeText(this, "Папката за логове е запазена.", Toast.LENGTH_LONG).show();
            showHome();
            return;
        }
        if (requestCode == PICK_FOLDER && resultCode == RESULT_OK && data != null) {
            folderUri = data.getData();
            try { getContentResolver().takePersistableUriPermission(folderUri, Intent.FLAG_GRANT_READ_URI_PERMISSION); }
            catch (Exception ignored) {}
            showHome();
        }
    }

    private String barcodeFormatName(int format) {
        switch (format) {
            case Barcode.FORMAT_QR_CODE: return "QR Code";
            case Barcode.FORMAT_CODE_128: return "Code 128";
            case Barcode.FORMAT_CODE_39: return "Code 39";
            case Barcode.FORMAT_CODE_93: return "Code 93";
            case Barcode.FORMAT_CODABAR: return "Codabar";
            case Barcode.FORMAT_DATA_MATRIX: return "Data Matrix";
            case Barcode.FORMAT_EAN_13: return "EAN-13";
            case Barcode.FORMAT_EAN_8: return "EAN-8";
            case Barcode.FORMAT_ITF: return "ITF";
            case Barcode.FORMAT_UPC_A: return "UPC-A";
            case Barcode.FORMAT_UPC_E: return "UPC-E";
            case Barcode.FORMAT_PDF417: return "PDF417";
            case Barcode.FORMAT_AZTEC: return "Aztec";
            default: return "Other";
        }
    }


    private void pickLogFolder() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, PICK_LOG_FOLDER);
    }

    private void renderQueue() {
        if (queueContainer == null || queueCount == null) return;
        for (ScanEntry entry : scanQueue) {
            if (entry.editor != null) entry.value = entry.editor.getText().toString();
        }
        queueContainer.removeAllViews();
        queueCount.setText("▦  В ТАБЛИЦАТА: " + scanQueue.size());
        LinearLayout table = new LinearLayout(this);
        table.setOrientation(LinearLayout.VERTICAL);
        table.setBackground(shape(Color.WHITE, 0xFFBDD4EC, 8));
        table.setClipToOutline(true);

        LinearLayout headings = new LinearLayout(this);
        headings.setOrientation(LinearLayout.HORIZONTAL);
        headings.setBackgroundColor(0xFF086EC4);
        headings.addView(tableCell("№", 0.45f, true, true));
        headings.addView(tableCell("Дата / час", 1.15f, true, true));
        headings.addView(tableCell("Баркод / номер (редактируем)", 2.0f, true, true));
        headings.addView(tableCell("Изтрий", 0.7f, true, true));
        table.addView(headings);

        if (scanQueue.isEmpty()) {
            TextView empty = text("Тук ще се появява всеки сканиран номер.\nМожеш да редактираш номерата или да изтриеш ненужните редове преди запис.", 14, 0xFF52677E);
            empty.setGravity(android.view.Gravity.CENTER);
            empty.setPadding(dp(12), dp(24), dp(12), dp(24));
            table.addView(empty);
        } else {
            for (int i = 0; i < scanQueue.size(); i++) {
                final ScanEntry entry = scanQueue.get(i);
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                row.setBackgroundColor(i % 2 == 0 ? 0xFFFFFFFF : 0xFFEAF4FF);
                row.addView(tableCell(String.valueOf(i + 1), 0.45f, false, false));
                row.addView(tableCell(entry.date + "\n" + entry.time, 1.15f, false, false));
                LinearLayout editCell = new LinearLayout(this);
                editCell.setPadding(dp(3), dp(4), dp(3), dp(4));
                entry.editor = new EditText(this);
                entry.editor.setSingleLine(true);
                entry.editor.setTextSize(14);
                entry.editor.setTextColor(0xFF102D50);
                entry.editor.setHint("Номер");
                entry.editor.setPadding(dp(8), dp(4), dp(8), dp(4));
                entry.editor.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                entry.editor.setBackground(shape(0xFFF8FBFF, 0xFFB7D4F3, 6));
                entry.editor.setText(entry.value);
                editCell.addView(entry.editor, new LinearLayout.LayoutParams(-1, dp(46)));
                row.addView(editCell, new LinearLayout.LayoutParams(0, -2, 2.0f));
                LinearLayout delCell = new LinearLayout(this);
                delCell.setGravity(android.view.Gravity.CENTER);
                Button delete = coloredButton("✕", 0xFFE83F55, () -> {
                    entry.value = entry.editor.getText().toString();
                    scanQueue.remove(entry);
                    renderQueue();
                });
                delete.setTextSize(14);
                delCell.addView(delete, new LinearLayout.LayoutParams(dp(42), dp(42)));
                row.addView(delCell, new LinearLayout.LayoutParams(0, dp(58), 0.7f));
                table.addView(row);
                View line = new View(this);
                line.setBackgroundColor(0xFFD7E5F3);
                table.addView(line, new LinearLayout.LayoutParams(-1, dp(1)));
            }
        }
        queueContainer.addView(table);
        queueContainer.addView(text("Форматът се запазва автоматично от скенера. Дата и час са от момента на сканиране.", 11, 0xFF5D7188));
    }

    private TextView tableCell(String value, float weight, boolean header, boolean centered) {
        TextView t = text(value, header ? 12 : 12, header ? Color.WHITE : 0xFF163A60);
        t.setGravity(centered ? android.view.Gravity.CENTER : android.view.Gravity.CENTER_VERTICAL);
        if (header) t.setTypeface(null, android.graphics.Typeface.BOLD);
        t.setPadding(dp(4), dp(10), dp(4), dp(10));
        return t;
    }

    private void saveQueuedCodes() {
        if (scanQueue.isEmpty()) {
            Toast.makeText(this, "Таблицата е празна.", Toast.LENGTH_SHORT).show();
            return;
        }
        for (ScanEntry entry : scanQueue) {
            if (entry.editor != null) entry.value = entry.editor.getText().toString().trim();
            if (entry.value.isEmpty()) {
                Toast.makeText(this, "Има празен номер. Попълни го или изтрий реда.", Toast.LENGTH_LONG).show();
                return;
            }
        }
        try {
            File dir = getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS);
            if (dir == null) dir = getFilesDir();
            if (!dir.exists() && !dir.mkdirs()) throw new Exception("Не може да се създаде папка за логовете");
            File txt = new File(dir, "scanned_codes.txt");
            File csvFile = new File(dir, "scanned_codes.csv");
            StringBuilder txtRows = new StringBuilder();
            StringBuilder csvRows = new StringBuilder();
            for (ScanEntry entry : scanQueue) {
                txtRows.append(entry.date).append(" ").append(entry.time).append(" | ")
                        .append(entry.format).append(" | ").append(entry.value).append(System.lineSeparator());
                csvRows.append(csv(entry.date)).append(",").append(csv(entry.time)).append(",")
                        .append(csv(entry.value)).append(",").append(csv(entry.format)).append("\r\n");
            }
            try (FileOutputStream out = new FileOutputStream(txt, true)) {
                out.write(txtRows.toString().getBytes(StandardCharsets.UTF_8));
            }
            boolean newCsv = !csvFile.exists() || csvFile.length() == 0;
            try (FileOutputStream out = new FileOutputStream(csvFile, true)) {
                if (newCsv) {
                    out.write(new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF});
                    out.write("Дата,Час,Баркод,Формат\r\n".getBytes(StandardCharsets.UTF_8));
                }
                out.write(csvRows.toString().getBytes(StandardCharsets.UTF_8));
            }
            int saved = scanQueue.size();
            scanQueue.clear();
            renderQueue();
            Toast.makeText(this, "Записани са " + saved + " реда. Таблицата е изчистена.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Грешка при запис: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String csv(String value) {
        if (value == null) value = "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private void appendToLogFile(String fileName, String line, boolean excelCsv) throws Exception {
        Uri fileUri = null;
        String treeId = DocumentsContract.getTreeDocumentId(logFolderUri);
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(logFolderUri, treeId);
        try (Cursor cursor = getContentResolver().query(childrenUri,
                new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME},
                null, null, null)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (fileName.equals(cursor.getString(1))) {
                        fileUri = DocumentsContract.buildDocumentUriUsingTree(logFolderUri, cursor.getString(0));
                        break;
                    }
                }
            }
        }
        boolean newFile = fileUri == null;
        if (newFile) fileUri = DocumentsContract.createDocument(
                getContentResolver(), logFolderUri, "text/plain", fileName);
        if (fileUri == null) throw new Exception("Не може да се създаде " + fileName);
        try (OutputStream out = getContentResolver().openOutputStream(fileUri, "wa")) {
            if (out == null) throw new Exception("Не може да се отвори " + fileName);
            if (newFile && excelCsv) {
                out.write(new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF});
                out.write("Дата,Час,Баркод,Формат\r\n".getBytes(StandardCharsets.UTF_8));
            }
            out.write(line.getBytes(StandardCharsets.UTF_8));
            out.flush();
        }
    }

    private void pickFolder() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, PICK_FOLDER);
    }


    private void scanFolder(Uri tree) {
        content.removeAllViews();
        content.addView(text("Searching selected folder…", 16, 0xFF14233B));
        ArrayList<Uri> found = new ArrayList<>();
        walk(tree, found, 0);
        content.removeAllViews();
        if (found.isEmpty()) {
            content.addView(text("No .db, .sqlite, .sqlite3 or .db3 files found in this folder and its accessible subfolders.", 15, 0xFF53627A));
            return;
        }
        content.addView(text("Found " + found.size() + " database file(s)", 17, 0xFF14233B));
        for (Uri u : found) {
            String name = displayName(u);
            Button b = button("▣  " + name, () -> showDatabase(u, name));
            content.addView(b);
        }
    }

    private void walk(Uri parent, ArrayList<Uri> found, int depth) {
        if (depth > 12 || found.size() > 500) return;
        try {
            String docId = DocumentsContract.getTreeDocumentId(folderUri);
            if (!parent.equals(folderUri)) docId = DocumentsContract.getDocumentId(parent);
            Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(folderUri, docId);
            try (Cursor c = getContentResolver().query(children,
                    new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                            DocumentsContract.Document.COLUMN_MIME_TYPE}, null, null, null)) {
                if (c == null) return;
                while (c.moveToNext()) {
                    String id = c.getString(0), name = c.getString(1), mime = c.getString(2);
                    Uri child = DocumentsContract.buildDocumentUriUsingTree(folderUri, id);
                    if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) walk(child, found, depth + 1);
                    else if (isDatabase(name)) found.add(child);
                }
            }
        } catch (Exception ignored) {}
    }

    private boolean isDatabase(String n) {
        if (n == null) return false;
        String x = n.toLowerCase(Locale.ROOT);
        return x.endsWith(".db") || x.endsWith(".sqlite") || x.endsWith(".sqlite3") || x.endsWith(".db3");
    }

    private String displayName(Uri u) {
        try (Cursor c = getContentResolver().query(u, new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst()) return c.getString(0);
        } catch (Exception ignored) {}
        return u.getLastPathSegment() == null ? "database" : u.getLastPathSegment();
    }

    private void showDatabase(Uri uri, String name) {
        File copy = null;
        try {
            copy = File.createTempFile("dbview_", ".sqlite", getCacheDir());
            try (InputStream in = getContentResolver().openInputStream(uri);
                 FileOutputStream out = new FileOutputStream(copy)) {
                if (in == null) throw new Exception("Cannot read this file.");
                byte[] buf = new byte[8192]; int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            }
            SQLiteDatabase db = SQLiteDatabase.openDatabase(copy.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
            ArrayList<String> tables = new ArrayList<>();
            try (Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name", null)) {
                while (c.moveToNext()) tables.add(c.getString(0));
            }
            final File dbCopy = copy;
            LinearLayout panel = new LinearLayout(this); panel.setOrientation(LinearLayout.VERTICAL);
            panel.addView(text(name, 22, 0xFF14233B));
            panel.addView(text("Tables: " + tables.size() + " · opened read-only", 14, 0xFF53627A));
            ScrollView sc = new ScrollView(this);
            LinearLayout list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL);
            if (tables.isEmpty()) list.addView(text("No user tables found, or this is not a readable SQLite database.", 14, 0xFF53627A));
            for (String table : tables) list.addView(button("▤  " + table, () -> showTable(db, table)));
            sc.addView(list); panel.addView(sc, new LinearLayout.LayoutParams(-1, dp(330)));
            new AlertDialog.Builder(this).setView(panel).setPositiveButton("Close", (d, w) -> {
                try { db.close(); } catch (Exception ignored) {}
                dbCopy.delete();
            }).show();
        } catch (Exception e) {
            if (copy != null) copy.delete();
            Toast.makeText(this, "Cannot open database: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showTable(SQLiteDatabase db, String table) {
        try {
            Cursor c = db.rawQuery("SELECT * FROM " + quoteIdentifier(table) + " LIMIT 200", null);
            StringBuilder out = new StringBuilder();
            out.append("Table: ").append(table).append("\nShowing up to 200 rows\n\n");
            for (int i = 0; i < c.getColumnCount(); i++) out.append(c.getColumnName(i)).append(i == c.getColumnCount()-1 ? "\n" : "  |  ");
            out.append("────────────────────────\n");
            int rows = 0;
            while (c.moveToNext()) {
                rows++;
                for (int i = 0; i < c.getColumnCount(); i++) {
                    String value;
                    switch (c.getType(i)) {
                        case Cursor.FIELD_TYPE_NULL:
                            value = "NULL";
                            break;
                        case Cursor.FIELD_TYPE_INTEGER:
                            value = Long.toString(c.getLong(i));
                            break;
                        case Cursor.FIELD_TYPE_FLOAT:
                            value = Double.toString(c.getDouble(i));
                            break;
                        case Cursor.FIELD_TYPE_BLOB:
                            byte[] blob = c.getBlob(i);
                            StringBuilder hex = new StringBuilder();
                            int preview = Math.min(blob.length, 16);
                            for (int j = 0; j < preview; j++) {
                                if (j > 0) hex.append(' ');
                                hex.append(String.format(Locale.ROOT, "%02X", blob[j] & 0xFF));
                            }
                            value = "[BLOB: " + blob.length + " bytes" +
                                    (preview > 0 ? "; hex: " + hex + (blob.length > preview ? " …" : "") : "") + "]";
                            break;
                        case Cursor.FIELD_TYPE_STRING:
                        default:
                            value = c.getString(i);
                            if (value != null && value.length() > 180) value = value.substring(0, 180) + "…";
                            break;
                    }
                    out.append(value == null ? "" : value).append(i == c.getColumnCount()-1 ? "\n" : "  |  ");
                }
                out.append("\n");
            }
            out.append("\nRows shown: ").append(rows);
            c.close();
            TextView tv = text(out.toString(), 12, 0xFF14233B);
            tv.setTextIsSelectable(true);
            ScrollView sc = new ScrollView(this); sc.addView(tv);
            new AlertDialog.Builder(this).setTitle(table).setView(sc).setPositiveButton("Close", null).show();
        } catch (Exception e) {
            Toast.makeText(this, "Cannot read table: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String quoteIdentifier(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}

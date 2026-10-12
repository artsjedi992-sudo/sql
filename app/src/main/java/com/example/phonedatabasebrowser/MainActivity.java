package com.example.phonedatabasebrowser;

import android.app.Activity;
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

    private Button button(String label, Runnable action) {
        Button b = new Button(this); b.setText(label);
        b.setAllCaps(false); b.setOnClickListener(v -> action.run());
        return b;
    }

    private void showHome() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFF3F6FB);
        TextView header = text("Database Browser", 25, 0xFF14233B);
        header.setPadding(dp(20), dp(24), dp(20), dp(8));
        root.addView(header);
        root.addView(text("Browse SQLite database files that you choose on this device.", 14, 0xFF53627A));
        root.addView(button("📷 Scan barcode", this::scanBarcode));
        root.addView(button("Choose folder for scan logs (TXT + Excel CSV)", this::pickLogFolder));
        root.addView(text(logFolderUri == null ? "Scan log folder: not selected" : "Logs: scanned_codes.txt and scanned_codes.csv", 13, 0xFF53627A));
        root.addView(button("Choose a folder", this::pickFolder));
        root.addView(text("This app can only see files and folders you grant access to. Android blocks ordinary apps from opening other apps’ private databases.", 13, 0xFF7A4B00));
        ScrollView sc = new ScrollView(this);
        content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        sc.addView(content);
        root.addView(sc, new LinearLayout.LayoutParams(-1, 0, 1));
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
            saveScannedCode(value, format);
            TextView result = text("Format: " + barcodeFormatName(format) + "\n\nValue:\n" + value, 20, 0xFF14233B);
            result.setTextIsSelectable(true);
            new AlertDialog.Builder(this)
                    .setTitle("Decoded barcode")
                    .setView(result)
                    .setPositiveButton("Copy number", (dialog, which) -> {
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        if (clipboard != null) {
                            clipboard.setPrimaryClip(ClipData.newPlainText("Barcode value", value));
                            Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Close", null)
                    .show();
        }
        if (requestCode == PICK_LOG_FOLDER && resultCode == RESULT_OK && data != null) {
            logFolderUri = data.getData();
            try { getContentResolver().takePersistableUriPermission(logFolderUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION); }
            catch (Exception ignored) {}
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

    private void saveScannedCode(String value, int format) {
        if (logFolderUri == null) {
            Toast.makeText(this, "Първо избери папка за логовете.", Toast.LENGTH_LONG).show();
            return;
        }
        Date now = new Date();
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(now);
        String time = new SimpleDateFormat("HH:mm:ss", Locale.ROOT).format(now);
        String formatName = barcodeFormatName(format);
        try {
            appendToLogFile("scanned_codes.txt",
                    date + " " + time + " | " + formatName + " | " + value + System.lineSeparator(), false);
            appendToLogFile("scanned_codes.csv",
                    csv(date) + "," + csv(time) + "," + csv(value) + "," + csv(formatName) + "\r\n", true);
            Toast.makeText(this, "Записано в TXT и Excel CSV.", Toast.LENGTH_SHORT).show();
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

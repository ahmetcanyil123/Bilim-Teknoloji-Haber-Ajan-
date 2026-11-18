package com.antigravity.bilimteknoloji;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.antigravity.bilimteknoloji.engine.IntelligenceEngine;
import com.antigravity.bilimteknoloji.export.ReportExporter;
import com.antigravity.bilimteknoloji.model.CategoryInfo;
import com.antigravity.bilimteknoloji.util.PreferencesHelper;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

/**
 * Ana ekran: Kullanıcıdan 'a', 'b', 'c', 'd', 'e', 'f', 'k', 'y' harfini alır,
 * 30 siteden canlı tarama yapar, 1-5 yıldızla puanlar ve Word (.docx) + PDF (.pdf) üretir.
 */
public class MainActivity extends AppCompatActivity {

    private EditText etLetter;
    private MaterialButton btnRunLetter, btnSettings;
    private MaterialButton btnCatA, btnCatB, btnCatC, btnCatD, btnCatE, btnCatF, btnCatK, btnCatY;
    private MaterialButton btnOpenPdf, btnOpenDocx, btnShareFiles;
    private ProgressBar progressBar;
    private TextView tvStatus;
    private WebView webPreview;
    private LinearLayout layoutExportActions;

    private Map<String, CategoryInfo> categories;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    private ReportExporter.ExportResult lastExport;
    private volatile boolean isRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        categories = CategoryInfo.getAllCategories();

        etLetter = findViewById(R.id.etLetter);
        btnRunLetter = findViewById(R.id.btnRunLetter);
        btnSettings = findViewById(R.id.btnSettings);

        btnCatA = findViewById(R.id.btnCatA);
        btnCatB = findViewById(R.id.btnCatB);
        btnCatC = findViewById(R.id.btnCatC);
        btnCatD = findViewById(R.id.btnCatD);
        btnCatE = findViewById(R.id.btnCatE);
        btnCatF = findViewById(R.id.btnCatF);
        btnCatK = findViewById(R.id.btnCatK);
        btnCatY = findViewById(R.id.btnCatY);

        btnOpenPdf = findViewById(R.id.btnOpenPdf);
        btnOpenDocx = findViewById(R.id.btnOpenDocx);
        btnShareFiles = findViewById(R.id.btnShareFiles);
        progressBar = findViewById(R.id.progressBar);
        tvStatus = findViewById(R.id.tvStatus);
        webPreview = findViewById(R.id.webPreview);
        layoutExportActions = findViewById(R.id.layoutExportActions);

        setupWebView();

        // Buton tıklamaları
        btnCatA.setOnClickListener(v -> startScan("a"));
        btnCatB.setOnClickListener(v -> startScan("b"));
        btnCatC.setOnClickListener(v -> startScan("c"));
        btnCatD.setOnClickListener(v -> startScan("d"));
        btnCatE.setOnClickListener(v -> startScan("e"));
        btnCatF.setOnClickListener(v -> startScan("f"));
        btnCatK.setOnClickListener(v -> startScan("k"));
        btnCatY.setOnClickListener(v -> startScan("y"));

        btnRunLetter.setOnClickListener(v -> triggerFromInput());
        etLetter.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE) {
                triggerFromInput();
                return true;
            }
            return false;
        });

        btnSettings.setOnClickListener(v -> showSettingsDialog());

        btnOpenPdf.setOnClickListener(v -> openFile(lastExport != null ? lastExport.pdfFile : null, "application/pdf"));
        btnOpenDocx.setOnClickListener(v -> openFile(lastExport != null ? lastExport.docxFile : null,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        btnShareFiles.setOnClickListener(v -> shareExportedFiles());
    }

    private void setupWebView() {
        WebSettings settings = webPreview.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        webPreview.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        });

        webPreview.loadDataWithBaseURL(
                null,
                "<html><body style='background:#0f172a;color:#94a3b8;font-family:sans-serif;padding:16px;line-height:1.6;'>"
                        + "<h3 style='color:#f8fafc;margin-top:0;'>🛰️ Hoş Geldiniz</h3>"
                        + "Yukarıdaki harf girişine <b>a, b, c, d, e, f, k, y</b> yazın veya doğrudan kategori butonuna dokunun.<br><br>"
                        + "• <b>[a]</b> Temel ve Popüler Bilim (TR)<br>"
                        + "• <b>[b]</b> Küresel Bilim ve Akademik<br>"
                        + "• <b>[c]</b> Küresel Teknoloji ve Analiz<br>"
                        + "• <b>[d]</b> Girişimcilik ve Teknoloji<br>"
                        + "• <b>[e]</b> Yerel Teknoloji Basını (TR)<br>"
                        + "• <b>[f]</b> Kurumsal Araştırma & Enstitü<br>"
                        + "• <b>[k]</b> Kuantum Teknolojileri (Tüm 30 Sitede)<br>"
                        + "• <b>[y]</b> Yapay Zeka / AI (Tüm 30 Sitede)<br><br>"
                        + "Rapor oluşturulduğunda Word (.docx), PDF ve Web olarak kaydedilir."
                        + "</body></html>",
                "text/html",
                "utf-8",
                null
        );
    }

    private void triggerFromInput() {
        String input = etLetter.getText() != null ? etLetter.getText().toString().trim().toLowerCase(Locale.ROOT) : "";
        if (input.isEmpty()) {
            Toast.makeText(this, "Lütfen bir harf girin (a, b, c, d, e, f, k, y)", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!categories.containsKey(input)) {
            Toast.makeText(this, "Geçersiz harf: '" + input + "'. a, b, c, d, e, f, k, y girin.", Toast.LENGTH_LONG).show();
            return;
        }
        startScan(input);
    }

    private void startScan(String code) {
        if (isRunning) {
            Toast.makeText(this, "Tarama devam ediyor, lütfen bekleyin...", Toast.LENGTH_SHORT).show();
            return;
        }

        final CategoryInfo cat = categories.get(code);
        if (cat == null) return;

        isRunning = true;
        progressBar.setVisibility(View.VISIBLE);
        layoutExportActions.setVisibility(View.GONE);
        tvStatus.setText("[" + code.toUpperCase(Locale.ROOT) + "] " + cat.title + " taranıyor...");

        executor.execute(() -> {
            try {
                IntelligenceEngine.IntelligenceResult scanRes = IntelligenceEngine.runScan(
                        MainActivity.this,
                        cat,
                        httpClient,
                        msg -> mainHandler.post(() -> tvStatus.setText(msg))
                );

                mainHandler.post(() -> tvStatus.setText("Word (.docx) ve PDF üretiliyor..."));
                ReportExporter.ExportResult exportRes = ReportExporter.exportAll(MainActivity.this, scanRes);

                mainHandler.post(() -> {
                    lastExport = exportRes;
                    isRunning = false;
                    progressBar.setVisibility(View.GONE);
                    tvStatus.setText("Tamamlandı: " + cat.title + " (" + scanRes.articles.size() + " madde)");
                    layoutExportActions.setVisibility(View.VISIBLE);

                    webPreview.loadDataWithBaseURL(null, exportRes.htmlContent, "text/html", "utf-8", null);
                    Toast.makeText(MainActivity.this, "Rapor hazırlandı! Word ve PDF oluşturuldu.", Toast.LENGTH_LONG).show();
                });

            } catch (Exception e) {
                mainHandler.post(() -> {
                    isRunning = false;
                    progressBar.setVisibility(View.GONE);
                    tvStatus.setText("Hata oluştu: " + e.getMessage());
                    Toast.makeText(MainActivity.this, "Hata: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void openFile(File file, String mimeType) {
        if (file == null || !file.exists()) {
            Toast.makeText(this, "Dosya bulunamadı.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Bu dosyayı açabilecek bir uygulama bulunamadı: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void shareExportedFiles() {
        if (lastExport == null) return;
        try {
            ArrayList<Uri> uris = new ArrayList<>();
            if (lastExport.pdfFile.exists()) {
                uris.add(FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", lastExport.pdfFile));
            }
            if (lastExport.docxFile.exists()) {
                uris.add(FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", lastExport.docxFile));
            }

            Intent share = new Intent(uris.size() > 1 ? Intent.ACTION_SEND_MULTIPLE : Intent.ACTION_SEND);
            share.setType("*/*");
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (uris.size() > 1) {
                share.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            } else if (!uris.isEmpty()) {
                share.putExtra(Intent.EXTRA_STREAM, uris.get(0));
            }
            share.putExtra(Intent.EXTRA_SUBJECT, "Bilim & Teknoloji İstihbarat Bülteni");
            startActivity(Intent.createChooser(share, "Raporu Paylaş"));
        } catch (Exception e) {
            Toast.makeText(this, "Paylaşım hatası: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showSettingsDialog() {
        View dialogView = getLayoutInflater().inflate(android.R.layout.simple_list_item_1, null);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(40, 20, 40, 10);

        TextView tvKey = new TextView(this);
        tvKey.setText("Gemini API Anahtarı (İsteğe Bağlı):");
        tvKey.setTextColor(0xFFCBD5E1);
        container.addView(tvKey);

        EditText etKey = new EditText(this);
        etKey.setText(PreferencesHelper.getGeminiApiKey(this));
        etKey.setHint("AIzaSy...");
        etKey.setSingleLine(true);
        container.addView(etKey);

        TextView tvKw = new TextView(this);
        tvKw.setText("\nÖzel İlgi Alanları (+⭐ Bonus Kelimeleri):");
        tvKw.setTextColor(0xFFCBD5E1);
        container.addView(tvKw);

        EditText etKw = new EditText(this);
        etKw.setText(String.join(",", PreferencesHelper.getInterestKeywords(this)));
        etKw.setHint("uzay, kuantum, yapay zeka, füzyon...");
        container.addView(etKw);

        new AlertDialog.Builder(this)
                .setTitle("⚙️ Uygulama Ayarları")
                .setView(container)
                .setPositiveButton("Kaydet", (dialog, which) -> {
                    PreferencesHelper.setGeminiApiKey(this, etKey.getText().toString());
                    PreferencesHelper.setInterestKeywords(this, etKw.getText().toString());
                    Toast.makeText(this, "Ayarlar kaydedildi.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("İptal", null)
                .show();
    }
}

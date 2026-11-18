package com.antigravity.bilimteknoloji.export;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import com.antigravity.bilimteknoloji.engine.IntelligenceEngine;
import com.antigravity.bilimteknoloji.model.ArticleItem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Raporları Word (.docx), PDF (.pdf) ve interaktif Web (.html) olarak üretir.
 */
public class ReportExporter {

    public static class ExportResult {
        public final File docxFile;
        public final File pdfFile;
        public final File htmlFile;
        public final String htmlContent;

        public ExportResult(File docxFile, File pdfFile, File htmlFile, String htmlContent) {
            this.docxFile = docxFile;
            this.pdfFile = pdfFile;
            this.htmlFile = htmlFile;
            this.htmlContent = htmlContent;
        }
    }

    public static ExportResult exportAll(
            Context context,
            IntelligenceEngine.IntelligenceResult result
    ) throws IOException {
        File dir = new File(context.getFilesDir(), "reports");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Rapor klasörü oluşturulamadı.");
        }

        String dateIso = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        String baseName = "Istihbarat_Raporu_" + result.categoryCode.toLowerCase(Locale.ROOT) + "_" + dateIso;

        File docxFile = new File(dir, baseName + ".docx");
        File pdfFile = new File(dir, baseName + ".pdf");
        File htmlFile = new File(dir, baseName + ".html");

        // 1. HTML oluştur
        String html = buildHtml(result);
        try (FileOutputStream fos = new FileOutputStream(htmlFile)) {
            fos.write(html.getBytes(StandardCharsets.UTF_8));
        }

        // 2. Word (.docx) oluştur (Saf Java OOXML Zip)
        writeDocx(docxFile, result);

        // 3. PDF (.pdf) oluştur (Android PdfDocument)
        writePdf(pdfFile, result);

        return new ExportResult(docxFile, pdfFile, htmlFile, html);
    }

    // =========================================================================
    // HTML OLUŞTURUCU
    // =========================================================================

    private static String buildHtml(IntelligenceEngine.IntelligenceResult res) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html lang='tr'><head><meta charset='utf-8'>")
                .append("<meta name='viewport' content='width=device-width,initial-scale=1'>")
                .append("<title>").append(esc(res.categoryTitle)).append("</title>")
                .append("<style>")
                .append("body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;background:#0f172a;color:#e2e8f0;margin:0;padding:12px;line-height:1.6;}")
                .append(".meta{background:#1e293b;border-left:4px solid #3b82f6;padding:12px 14px;border-radius:8px;margin-bottom:16px;}")
                .append("h1{font-size:18px;color:#f8fafc;margin:0 0 6px 0;}")
                .append("h2{font-size:15px;color:#93c5fd;margin:18px 0 6px 0;border-bottom:1px solid #334155;padding-bottom:4px;}")
                .append(".summary-box{background:#162032;border:1px solid #334155;border-radius:8px;padding:10px 12px;margin-bottom:18px;font-size:13px;color:#cbd5e1;}")
                .append(".article-card{background:#1e293b;border:1px solid #334155;border-radius:10px;padding:12px;margin-bottom:14px;}")
                .append(".badge-fav{background:#065f46;color:#6ee7b7;padding:2px 6px;border-radius:4px;font-size:11px;font-weight:bold;margin-left:6px;}")
                .append(".stars{color:#f59e0b;font-weight:bold;font-size:14px;}")
                .append(".tags{color:#94a3b8;font-size:11.5px;margin:4px 0 8px 0;}")
                .append(".desc{font-size:13px;color:#f1f5f9;margin-bottom:8px;}")
                .append(".links{font-size:12px;} a{color:#60a5fa;text-decoration:none;font-weight:600;} a:hover{text-decoration:underline;}")
                .append("</style></head><body>");

        sb.append("<div class='meta'>")
                .append("<h1>").append(res.categoryIcon).append(" Bilim & Teknoloji İstihbarat [")
                .append(res.categoryCode.toUpperCase(Locale.ROOT)).append("]</h1>")
                .append("<div><strong>Kategori:</strong> ").append(esc(res.categoryTitle)).append("</div>")
                .append("<div><strong>Rapor Tarihi:</strong> ").append(esc(res.endDateStr)).append("</div>")
                .append("<div><strong>Filtre Başlangıcı:</strong> ").append(esc(res.startDateStr)).append("</div>")
                .append("</div>");

        sb.append("<div class='summary-box'>")
                .append("<strong>📊 Yönetici İstihbarat Özeti:</strong><br>")
                .append(esc(res.executiveSummary))
                .append("</div>");

        for (ArticleItem a : res.articles) {
            String starStr = new String(new char[a.stars]).replace("\0", "⭐");
            sb.append("<div class='article-card'>");
            sb.append("<div class='stars'>").append(starStr);
            if (a.isFavorite) {
                sb.append("<span class='badge-fav'>+⭐ Özel İlgi Alanı</span>");
            }
            sb.append("</div>");
            sb.append("<h2>").append(esc(a.title)).append("</h2>");

            if (!a.topics.isEmpty()) {
                sb.append("<div class='tags'>Etiketler: ").append(esc(String.join(" | ", a.topics))).append("</div>");
            }

            sb.append("<div class='desc'><strong>Hap Özet:</strong> ").append(esc(a.summary)).append("</div>");

            sb.append("<div class='links'>")
                    .append("Kaynak: <a href='").append(esc(a.sourceUrl)).append("' target='_blank'>").append(esc(a.sourceName)).append("</a> | ")
                    .append("<a href='").append(esc(a.articleUrl)).append("' target='_blank'>Doğrudan Habere Git &rarr;</a>")
                    .append("</div>");
            sb.append("</div>");
        }

        sb.append("</body></html>");
        return sb.toString();
    }

    // =========================================================================
    // WORD (.DOCX) OLUŞTURUCU (SAF JAVA OOXML ZIP)
    // =========================================================================

    private static void writeDocx(
            File outFile,
            IntelligenceEngine.IntelligenceResult res
    ) throws IOException {
        String contentTypes = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                + "<Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>"
                + "</Types>";

        String rels = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/>"
                + "</Relationships>";

        StringBuilder doc = new StringBuilder();
        doc.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
                .append("<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">")
                .append("<w:body>");

        // Başlık ve Tarih Bilgisi
        String mainTitle = "Bilim & Teknoloji İstihbarat Bülteni [" + res.categoryCode.toUpperCase(Locale.ROOT) + "]";
        doc.append(docxParagraph(mainTitle, true, 32, "0F172A"));
        doc.append(docxParagraph("Kategori: [" + res.categoryCode.toUpperCase(Locale.ROOT) + "] " + res.categoryTitle, true, 22, "1E3A8A"));
        doc.append(docxParagraph("Tarih: " + res.endDateStr + " | Filtre Başlangıcı: " + res.startDateStr, false, 18, "475569"));
        doc.append(docxParagraph("─────────────────────────────────────────────", false, 16, "94A3B8"));

        // Yönetici İstihbarat Özeti
        doc.append(docxParagraph("Yönetici İstihbarat Özeti (Executive Summary)", true, 24, "1E3A8A"));
        doc.append(docxParagraph(res.executiveSummary, false, 20, "1E293B"));
        doc.append(docxParagraph("─────────────────────────────────────────────", false, 16, "94A3B8"));

        // Haber Maddeleri (5'ten 1'e sıralı)
        for (ArticleItem art : res.articles) {
            String starStr = new String(new char[art.stars]).replace("\0", "⭐");
            String bonus = art.isFavorite ? " [+⭐ İlgi Alanı]" : "";
            doc.append(docxParagraph(starStr + bonus + " " + art.title, true, 24, "0F172A"));
            doc.append(docxParagraph("Kaynak: " + art.sourceName + " (" + art.sourceUrl + ")", false, 18, "2563EB"));
            doc.append(docxParagraph("Haber Linki: " + art.articleUrl, false, 18, "2563EB"));
            if (!art.topics.isEmpty()) {
                doc.append(docxParagraph("Konu Etiketleri: " + String.join(" | ", art.topics), false, 18, "64748B"));
            }
            doc.append(docxParagraph("Hap Özet & Analiz: " + art.summary, false, 20, "1E293B"));
            doc.append(docxParagraph("─────────────────────────────────────────────", false, 16, "CBD5E1"));
        }

        doc.append("<w:sectPr><w:pgSz w:w=\"11906\" w:h=\"16838\"/>")
                .append("<w:pgMar w:top=\"720\" w:right=\"720\" w:bottom=\"720\" w:left=\"720\"/></w:sectPr>")
                .append("</w:body></w:document>");

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outFile))) {
            putZipEntry(zos, "[Content_Types].xml", contentTypes);
            putZipEntry(zos, "_rels/.rels", rels);
            putZipEntry(zos, "word/document.xml", doc.toString());
        }
    }

    private static void putZipEntry(ZipOutputStream zos, String name, String content) throws IOException {
        zos.putNextEntry(new ZipEntry(name));
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private static String docxParagraph(String text, boolean bold, int halfPt, String colorHex) {
        return "<w:p><w:pPr><w:spacing w:after=\"100\"/></w:pPr><w:r><w:rPr>"
                + (bold ? "<w:b/>" : "")
                + "<w:color w:val=\"" + colorHex + "\"/>"
                + "<w:sz w:val=\"" + halfPt + "\"/><w:szCs w:val=\"" + halfPt + "\"/>"
                + "</w:rPr><w:t xml:space=\"preserve\">" + escXml(text) + "</w:t></w:r></w:p>";
    }

    // =========================================================================
    // PDF OLUŞTURUCU (ANDROID NATIVE PDFDOCUMENT)
    // =========================================================================

    private static void writePdf(File outFile, IntelligenceEngine.IntelligenceResult res) throws IOException {
        PdfDocument pdfDoc = new PdfDocument();
        int pageWidth = 595; // A4 standard pt
        int pageHeight = 842;
        int pageNum = 1;

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create();
        PdfDocument.Page page = pdfDoc.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        TextPaint titlePaint = new TextPaint();
        titlePaint.setColor(Color.rgb(15, 23, 42));
        titlePaint.setTextSize(16f);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        TextPaint bodyPaint = new TextPaint();
        bodyPaint.setColor(Color.rgb(30, 41, 59));
        bodyPaint.setTextSize(9.5f);

        TextPaint boldPaint = new TextPaint();
        boldPaint.setColor(Color.rgb(30, 58, 138));
        boldPaint.setTextSize(11f);
        boldPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        TextPaint linkPaint = new TextPaint();
        linkPaint.setColor(Color.rgb(37, 99, 235));
        linkPaint.setTextSize(9f);

        Paint dividerPaint = new Paint();
        dividerPaint.setColor(Color.rgb(203, 213, 225));
        dividerPaint.setStrokeWidth(1f);

        int margin = 36;
        int contentWidth = pageWidth - (margin * 2);
        int y = 45;

        // Başlık
        String header = "Bilim & Teknoloji İstihbarat Bülteni [" + res.categoryCode.toUpperCase(Locale.ROOT) + "]";
        canvas.drawText(header, margin, y, titlePaint);
        y += 20;

        canvas.drawText("Kategori: " + res.categoryTitle + " | Tarih: " + res.endDateStr, margin, y, bodyPaint);
        y += 15;
        canvas.drawLine(margin, y, margin + contentWidth, y, dividerPaint);
        y += 18;

        // Yönetici Özeti
        canvas.drawText("📊 Yönetici İstihbarat Özeti:", margin, y, boldPaint);
        y += 15;
        StaticLayout execLayout = new StaticLayout(res.executiveSummary, bodyPaint, contentWidth,
                Layout.Alignment.ALIGN_NORMAL, 1.2f, 0, false);
        canvas.save();
        canvas.translate(margin, y);
        execLayout.draw(canvas);
        canvas.restore();
        y += execLayout.getHeight() + 16;
        canvas.drawLine(margin, y, margin + contentWidth, y, dividerPaint);
        y += 16;

        // Haberler
        for (ArticleItem a : res.articles) {
            String starStr = new String(new char[a.stars]).replace("\0", "⭐");
            String itemTitle = starStr + (a.isFavorite ? " [+⭐ İlgi]" : "") + " " + a.title;

            StaticLayout titleLayout = new StaticLayout(itemTitle, boldPaint, contentWidth,
                    Layout.Alignment.ALIGN_NORMAL, 1.1f, 0, false);

            StaticLayout descLayout = new StaticLayout(a.summary, bodyPaint, contentWidth,
                    Layout.Alignment.ALIGN_NORMAL, 1.15f, 0, false);

            int needed = titleLayout.getHeight() + descLayout.getHeight() + 50;
            if (y + needed > pageHeight - 40) {
                pdfDoc.finishPage(page);
                pageNum++;
                pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create();
                page = pdfDoc.startPage(pageInfo);
                canvas = page.getCanvas();
                y = 45;
            }

            // Başlık çiz
            canvas.save();
            canvas.translate(margin, y);
            titleLayout.draw(canvas);
            canvas.restore();
            y += titleLayout.getHeight() + 4;

            // Kaynak
            canvas.drawText("Kaynak: " + a.sourceName + " | " + a.articleUrl, margin, y, linkPaint);
            y += 14;

            // Özet
            canvas.save();
            canvas.translate(margin, y);
            descLayout.draw(canvas);
            canvas.restore();
            y += descLayout.getHeight() + 10;

            canvas.drawLine(margin, y, margin + contentWidth, y, dividerPaint);
            y += 12;
        }

        pdfDoc.finishPage(page);

        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            pdfDoc.writeTo(fos);
        } finally {
            pdfDoc.close();
        }
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String escXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }
}

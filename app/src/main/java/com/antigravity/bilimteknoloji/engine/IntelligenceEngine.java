package com.antigravity.bilimteknoloji.engine;

import android.content.Context;

import com.antigravity.bilimteknoloji.model.ArticleItem;
import com.antigravity.bilimteknoloji.model.CategoryInfo;
import com.antigravity.bilimteknoloji.util.PreferencesHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 30 web sitesini canlı tarayan, mükerrer haberleri tekilleştiren,
 * 1-5 yıldızla puanlayıp 5'ten 1'e sıralayan ve ilgi bonusu ekleyen istihbarat motoru.
 */
public class IntelligenceEngine {

    public interface ProgressCallback {
        void onProgress(String message);
    }

    public static class IntelligenceResult {
        public final String categoryCode;
        public final String categoryTitle;
        public final String categoryIcon;
        public final String startDateStr;
        public final String endDateStr;
        public final String executiveSummary;
        public final List<ArticleItem> articles;
        public final String markdownContent;

        public IntelligenceResult(String categoryCode, String categoryTitle, String categoryIcon,
                                  String startDateStr, String endDateStr, String executiveSummary,
                                  List<ArticleItem> articles, String markdownContent) {
            this.categoryCode = categoryCode;
            this.categoryTitle = categoryTitle;
            this.categoryIcon = categoryIcon;
            this.startDateStr = startDateStr;
            this.endDateStr = endDateStr;
            this.executiveSummary = executiveSummary;
            this.articles = articles;
            this.markdownContent = markdownContent;
        }
    }

    public static IntelligenceResult runScan(
            Context context,
            CategoryInfo category,
            OkHttpClient httpClient,
            ProgressCallback callback
    ) throws Exception {

        String startDate = PreferencesHelper.getLastCheckTime(context, category.code);
        String endDate = new SimpleDateFormat("dd.MM.yyyy HH:mm", new Locale("tr", "TR")).format(new Date());
        Set<String> interestKeywords = PreferencesHelper.getInterestKeywords(context);

        if (callback != null) {
            callback.onProgress("[" + category.code.toUpperCase(Locale.ROOT) + "] Canlı haber kaynakları taranıyor...");
        }

        List<ArticleItem> rawArticles = new ArrayList<>();
        Set<String> seenNormalizedTitles = new HashSet<>();

        // 1. Canlı RSS / Atom beslemelerini çek
        for (CategoryInfo.FeedSource feed : category.feeds) {
            if (callback != null) {
                callback.onProgress("Taranıyor: " + feed.name);
            }
            try {
                Request req = new Request.Builder()
                        .url(feed.feedUrl)
                        .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:120.0) AntigravityIstihbarat/1.0")
                        .build();

                try (Response resp = httpClient.newCall(req).execute()) {
                    if (!resp.isSuccessful() || resp.body() == null) continue;
                    String xml = resp.body().string();
                    parseFeedXml(xml, feed.name, rawArticles, seenNormalizedTitles, interestKeywords);
                }
            } catch (Exception e) {
                // Besleme bazlı geçici hataları tolere et
            }
        }

        if (callback != null) {
            callback.onProgress("Haberler analiz ediliyor ve puanlanıyor (" + rawArticles.size() + " madde)...");
        }

        // 2. Yıldız puanlaması (5'ten 1'e sırala)
        Collections.sort(rawArticles, (a, b) -> Integer.compare(b.stars, a.stars));

        // En az 8 - 15 en yetkin haberi seç
        List<ArticleItem> finalArticles = new ArrayList<>();
        int limit = Math.min(rawArticles.size(), 14);
        for (int i = 0; i < limit; i++) {
            finalArticles.add(rawArticles.get(i));
        }

        // 3. Gemini API varsa yapay zeka ile zenginleştir, yoksa akıllı yerel özet oluştur
        String apiKey = PreferencesHelper.getGeminiApiKey(context);
        String executiveSummary;
        if (!apiKey.isEmpty() && !finalArticles.isEmpty()) {
            if (callback != null) {
                callback.onProgress("Gemini API ile istihbarat analizi sentezleniyor...");
            }
            try {
                executiveSummary = callGeminiSummary(apiKey, category, finalArticles, httpClient);
            } catch (Exception e) {
                executiveSummary = generateLocalExecutiveSummary(category, finalArticles);
            }
        } else {
            executiveSummary = generateLocalExecutiveSummary(category, finalArticles);
        }

        // 4. Markdown dokümanını oluştur
        String md = buildMarkdown(category, startDate, endDate, executiveSummary, finalArticles);

        // Son kontrol zamanını kaydet
        PreferencesHelper.setLastCheckTime(context, category.code);

        return new IntelligenceResult(
                category.code,
                category.title,
                category.icon,
                startDate,
                endDate,
                executiveSummary,
                finalArticles,
                md
        );
    }

    private static void parseFeedXml(
            String xml,
            String defaultSource,
            List<ArticleItem> outList,
            Set<String> seenTitles,
            Set<String> interestKeywords
    ) {
        Document doc = Jsoup.parse(xml, "", org.jsoup.parser.Parser.xmlParser());
        Elements items = doc.select("item");
        if (items.isEmpty()) {
            items = doc.select("entry"); // Atom feed
        }

        int count = 0;
        for (Element item : items) {
            String title = cleanText(item.select("title").text());
            if (title.isEmpty()) continue;

            // Mükerrer başlık filtresi
            String norm = normalizeForDedup(title);
            if (seenTitles.contains(norm)) continue;
            seenTitles.add(norm);

            String link = item.select("link").text();
            if (link.isEmpty()) {
                link = item.select("link").attr("href");
            }
            if (link.isEmpty()) {
                link = item.select("guid").text();
            }

            String pubDate = item.select("pubDate").text();
            if (pubDate.isEmpty()) {
                pubDate = item.select("published, updated").text();
            }

            String desc = cleanText(item.select("description, summary, content").text());
            if (desc.length() > 320) {
                desc = desc.substring(0, 317) + "...";
            }

            // Yıldız ve İlgi Analizi
            boolean isFavorite = checkFavorite(title + " " + desc, interestKeywords);
            int stars = evaluateStars(title, desc, isFavorite);

            ArticleItem art = new ArticleItem();
            art.title = title;
            art.sourceName = defaultSource;
            art.sourceUrl = extractDomainUrl(link, defaultSource);
            art.articleUrl = link;
            art.pubDate = pubDate;
            art.summary = desc.isEmpty() ? "Bu gelişmeyle ilgili detaylar kaynak makalede yer almaktadır." : desc;
            art.stars = stars;
            art.isFavorite = isFavorite;
            art.topics = extractTopics(title, desc);

            outList.add(art);
            count++;
            if (count >= 10) break;
        }
    }

    private static int evaluateStars(String title, String desc, boolean isFavorite) {
        String combined = (title + " " + desc).toLowerCase(Locale.ROOT);
        int score = 3; // Varsayılan orta ölçekli/önemli

        // 5 Yıldız Kriterleri: Çığır açıcı, devrim, keşif, nobel, quantum, AGI, ilk kez
        if (combined.contains("nobel") || combined.contains("çığır açıcı") || combined.contains("breakthrough")
                || combined.contains("ilk kez") || combined.contains("tarihi keşif") || combined.contains("devrim")
                || combined.contains("quantum supremacy") || combined.contains("superconductor") || combined.contains("süperiletken")) {
            score = 5;
        }
        // 4 Yıldız: Büyük inovasyon, yeni model, klinik başarı, rekor, fırlatma
        else if (combined.contains("yeni model") || combined.contains("keşfedildi") || combined.contains("başarı")
                || combined.contains("rekor") || combined.contains("derin uzay") || combined.contains("fusion")
                || combined.contains("füzyon") || combined.contains("yapay zeka") || combined.contains("ai")) {
            score = 4;
        }
        // 2 Yıldız: İnceleme, rutin güncelleme, sızıntı
        else if (combined.contains("güncelleme") || combined.contains("fiyat") || combined.contains("özellikleri")
                || combined.contains("iddia") || combined.contains("sızıntı")) {
            score = 2;
        }

        // İlgi bonusu (+⭐)
        if (isFavorite && score < 5) {
            score++;
        }
        return Math.max(1, Math.min(5, score));
    }

    private static boolean checkFavorite(String text, Set<String> interestKeywords) {
        String lower = text.toLowerCase(Locale.ROOT);
        for (String kw : interestKeywords) {
            if (lower.contains(kw)) return true;
        }
        return false;
    }

    private static List<String> extractTopics(String title, String desc) {
        List<String> list = new ArrayList<>();
        String combined = (title + " " + desc).toLowerCase(Locale.ROOT);

        if (combined.contains("kuantum") || combined.contains("quantum") || combined.contains("qubit")) list.add("Kuantum");
        if (combined.contains("yapay zeka") || combined.contains("ai") || combined.contains("llm") || combined.contains("model")) list.add("Yapay Zeka");
        if (combined.contains("uzay") || combined.contains("nasa") || combined.contains("astronomi") || combined.contains("gezegen")) list.add("Uzay & Astronomi");
        if (combined.contains("biyoloji") || combined.contains("mrna") || combined.contains("tıp") || combined.contains("genetik")) list.add("Biyoteknoloji");
        if (combined.contains("çip") || combined.contains("yarı iletken") || combined.contains("donanım") || combined.contains("işlemci")) list.add("Yarı İletken");
        if (combined.contains("robot") || combined.contains("otonom")) list.add("Robotik");
        if (combined.contains("iklim") || combined.contains("enerji") || combined.contains("batarya")) list.add("Enerji & Çevre");

        if (list.isEmpty()) {
            list.add("Bilim & İnovasyon");
        }
        return list;
    }

    private static String extractDomainUrl(String articleUrl, String defaultSource) {
        try {
            java.net.URI uri = new java.net.URI(articleUrl);
            String host = uri.getHost();
            if (host != null && host.length() > 0) {
                return "https://" + host + "/";
            }
        } catch (Exception ignored) {}
        return "https://www.google.com/search?q=" + URLEncoder.encode(defaultSource);
    }

    private static String normalizeForDedup(String title) {
        return title.replaceAll("[^a-zA-Z0-9çÇğĞıİöÖşŞüÜ]", "").toLowerCase(Locale.ROOT);
    }

    private static String cleanText(String input) {
        if (input == null) return "";
        return Jsoup.parse(input).text().trim();
    }

    private static String generateLocalExecutiveSummary(CategoryInfo category, List<ArticleItem> articles) {
        StringBuilder sb = new StringBuilder();
        sb.append(category.title).append(" alanında son dönemde öne çıkan gelişmeler değerlendirildiğinde ");

        int star5Count = 0;
        int favCount = 0;
        for (ArticleItem a : articles) {
            if (a.stars >= 4) star5Count++;
            if (a.isFavorite) favCount++;
        }

        sb.append("kaynaklarda ").append(articles.size()).append(" kritik madde tespit edilmiştir. ")
          .append("Bunlardan ").append(star5Count).append(" tanesi yüksek etkili çığır açıcı inovasyon statüsünde derecelendirilmiş, ")
          .append(favCount).append(" başlık ise özel ilgi alanları filtresiyle eşleşmiştir. ")
          .append("Öne çıkan haberler bilimsel önem derecesine göre 5 yıldızdan 1 yıldıza sıralanmıştır.");
        return sb.toString();
    }

    private static String callGeminiSummary(
            String apiKey,
            CategoryInfo category,
            List<ArticleItem> articles,
            OkHttpClient client
    ) throws IOException {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=" + apiKey;

        StringBuilder articleLines = new StringBuilder();
        for (ArticleItem a : articles) {
            articleLines.append("- ").append(a.title).append(" (Kaynak: ").append(a.sourceName).append(")\n");
            articleLines.append("  Özet: ").append(a.summary).append("\n");
        }

        String prompt = "Sen uzman bir bilim ve teknoloji istihbarat analistisin. "
                + "Aşağıda [" + category.title + "] kategorisinde taranmış en güncel haberler yer almaktadır:\n\n"
                + articleLines.toString() + "\n"
                + "Bu haberlerdeki temel eğilimleri, çığır açıcı atılımları ve sektörel etkileri özetleyen 3-4 paragraflık üst düzey bir Türkçe Yönetici İstihbarat Özeti (Executive Summary) yaz.";

        JsonObject root = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject partObj = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject textPart = new JsonObject();
        textPart.addProperty("text", prompt);
        parts.add(textPart);
        partObj.add("parts", parts);
        contents.add(partObj);
        root.add("contents", contents);

        RequestBody body = RequestBody.create(root.toString(), MediaType.parse("application/json"));
        Request req = new Request.Builder().url(url).post(body).build();

        try (Response resp = client.newCall(req).execute()) {
            if (!resp.isSuccessful() || resp.body() == null) {
                throw new IOException("Gemini HTTP error: " + resp.code());
            }
            String respStr = resp.body().string();
            JsonObject json = JsonParser.parseString(respStr).getAsJsonObject();
            return json.getAsJsonArray("candidates").get(0).getAsJsonObject()
                    .getAsJsonObject("content").getAsJsonArray("parts").get(0).getAsJsonObject()
                    .get("text").getAsString().trim();
        }
    }

    private static String buildMarkdown(
            CategoryInfo category,
            String startDate,
            String endDate,
            String executiveSummary,
            List<ArticleItem> articles
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(category.icon).append(" Bilim & Teknoloji İstihbarat Bülteni [")
                .append(category.code.toUpperCase(Locale.ROOT)).append("]\n\n");
        sb.append("> **Kategori:** `[").append(category.code.toUpperCase(Locale.ROOT)).append("] ")
                .append(category.title).append("`  \n");
        sb.append("> **Rapor Tarihi:** `").append(endDate).append("`  \n");
        sb.append("> **Filtre Başlangıcı:** `").append(startDate).append("`  \n\n");
        sb.append("---\n\n");

        sb.append("### 📊 Yönetici İstihbarat Özeti (Executive Summary)\n");
        sb.append(executiveSummary).append("\n\n---\n\n");

        for (ArticleItem art : articles) {
            String starStr = new String(new char[art.stars]).replace("\0", "⭐");
            String bonus = art.isFavorite ? " [+⭐ Özel İlgi Alanı]" : "";
            sb.append("## ").append(starStr).append(bonus).append(" ").append(art.title).append("\n");
            sb.append("- **Kaynak:** [").append(art.sourceName).append("](").append(art.sourceUrl).append(")\n");
            sb.append("- **Haber Linki:** [Doğrudan Habere Git](").append(art.articleUrl).append(")\n");
            if (!art.topics.isEmpty()) {
                sb.append("- **Konu Etiketleri:** ").append(String.join(" | ", art.topics)).append("\n");
            }
            sb.append("- **Hap Özet & Analiz:** ").append(art.summary).append("\n\n");
            sb.append("---\n\n");
        }

        return sb.toString();
    }
}

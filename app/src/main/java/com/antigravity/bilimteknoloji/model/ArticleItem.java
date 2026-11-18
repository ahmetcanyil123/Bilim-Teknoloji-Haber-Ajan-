package com.antigravity.bilimteknoloji.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Taranan tek bir haber/makale maddesi
 */
public class ArticleItem {
    public String title = "";
    public String sourceName = "";
    public String sourceUrl = "";
    public String articleUrl = "";
    public String pubDate = "";
    public String summary = "";
    public List<String> topics = new ArrayList<>();
    public int stars = 3;              // 1 - 5 yıldız
    public boolean isFavorite = false; // [+⭐ Özel İlgi Alanı]
    public String commandKey = "";

    public ArticleItem() {}

    public ArticleItem(String title, String sourceName, String sourceUrl, String articleUrl,
                       String pubDate, String summary, int stars, boolean isFavorite) {
        this.title = title;
        this.sourceName = sourceName;
        this.sourceUrl = sourceUrl;
        this.articleUrl = articleUrl;
        this.pubDate = pubDate;
        this.summary = summary;
        this.stars = stars;
        this.isFavorite = isFavorite;
    }
}

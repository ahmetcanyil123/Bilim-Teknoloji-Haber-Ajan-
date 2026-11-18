package com.antigravity.bilimteknoloji.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 30 web sitesini ve a, b, c, d, e, f, k, y kategorilerini tanımlar.
 */
public class CategoryInfo {
    public final String code;
    public final String title;
    public final String icon;
    public final String description;
    public final List<FeedSource> feeds = new ArrayList<>();
    public final List<SiteEntry> sites = new ArrayList<>();

    public static class FeedSource {
        public final String name;
        public final String feedUrl;
        public FeedSource(String name, String feedUrl) {
            this.name = name;
            this.feedUrl = feedUrl;
        }
    }

    public static class SiteEntry {
        public final String name;
        public final String url;
        public SiteEntry(String name, String url) {
            this.name = name;
            this.url = url;
        }
    }

    public CategoryInfo(String code, String title, String icon, String description) {
        this.code = code;
        this.title = title;
        this.icon = icon;
        this.description = description;
    }

    public static Map<String, CategoryInfo> getAllCategories() {
        Map<String, CategoryInfo> map = new HashMap<>();

        // [a] Temel ve Popüler Bilim (TR)
        CategoryInfo catA = new CategoryInfo("a", "Temel ve Popüler Bilim (TR)", "🔬",
                "Evrim Ağacı, Sarkaç, TÜBİTAK Bilim Genç, Fizikist");
        catA.sites.add(new SiteEntry("Evrim Ağacı", "https://evrimagaci.org/"));
        catA.sites.add(new SiteEntry("Sarkaç", "https://sarkac.org/"));
        catA.sites.add(new SiteEntry("TÜBİTAK Bilim Genç", "https://bilimgenc.tubitak.gov.tr/"));
        catA.sites.add(new SiteEntry("Fizikist", "https://www.fizikist.com/"));
        catA.feeds.add(new FeedSource("Evrim Ağacı", "https://evrimagaci.org/rss.xml"));
        catA.feeds.add(new FeedSource("Sarkaç", "https://sarkac.org/feed/"));
        catA.feeds.add(new FeedSource("Fizikist", "https://www.fizikist.com/feed/"));
        catA.feeds.add(new FeedSource("TÜBİTAK Bilim Genç & TR Bilim",
                "https://news.google.com/rss/search?q=site:bilimgenc.tubitak.gov.tr+OR+site:evrimagaci.org+OR+site:sarkac.org+OR+site:fizikist.com&hl=tr&gl=TR&ceid=TR:tr"));
        map.put("a", catA);

        // [b] Küresel Bilim ve Akademik
        CategoryInfo catB = new CategoryInfo("b", "Küresel Bilim ve Akademik", "🧬",
                "Science, Scientific American, Phys.org, Quanta, Live Science, EurekAlert!, SN Explores");
        catB.sites.add(new SiteEntry("Science", "https://www.science.org/"));
        catB.sites.add(new SiteEntry("Scientific American", "https://www.scientificamerican.com/"));
        catB.sites.add(new SiteEntry("Phys.org", "https://phys.org/"));
        catB.sites.add(new SiteEntry("Quanta Magazine", "https://www.quantamagazine.org/"));
        catB.sites.add(new SiteEntry("Live Science", "https://www.livescience.com/"));
        catB.sites.add(new SiteEntry("EurekAlert!", "https://www.eurekalert.org/"));
        catB.sites.add(new SiteEntry("Science News Explores", "https://www.snexplores.org/"));
        catB.feeds.add(new FeedSource("Science", "https://www.science.org/rss/news_current.xml"));
        catB.feeds.add(new FeedSource("Phys.org", "https://phys.org/rss-feed/"));
        catB.feeds.add(new FeedSource("Quanta Magazine", "https://www.quantamagazine.org/feed/"));
        catB.feeds.add(new FeedSource("Live Science", "https://www.livescience.com/feeds/all"));
        catB.feeds.add(new FeedSource("Science News Explores", "https://www.snexplores.org/feed"));
        catB.feeds.add(new FeedSource("Scientific American & EurekAlert!",
                "https://news.google.com/rss/search?q=site:scientificamerican.com+OR+site:eurekalert.org+OR+site:science.org+when:7d&hl=en-US&gl=US&ceid=US:en"));
        map.put("b", catB);

        // [c] Küresel Teknoloji ve Analiz
        CategoryInfo catC = new CategoryInfo("c", "Küresel Teknoloji ve Analiz", "💻",
                "MIT Technology Review, TechRadar, Ars Technica, HowStuffWorks");
        catC.sites.add(new SiteEntry("MIT Technology Review", "https://www.technologyreview.com/"));
        catC.sites.add(new SiteEntry("TechRadar", "https://www.techradar.com/"));
        catC.sites.add(new SiteEntry("Ars Technica", "https://arstechnica.com/"));
        catC.sites.add(new SiteEntry("HowStuffWorks", "https://science.howstuffworks.com/"));
        catC.feeds.add(new FeedSource("MIT Technology Review", "https://www.technologyreview.com/feed/"));
        catC.feeds.add(new FeedSource("TechRadar", "https://www.techradar.com/rss"));
        catC.feeds.add(new FeedSource("Ars Technica", "https://feeds.arstechnica.com/arstechnica/index"));
        catC.feeds.add(new FeedSource("HowStuffWorks & Tech Review",
                "https://news.google.com/rss/search?q=site:science.howstuffworks.com+OR+site:technologyreview.com+when:7d&hl=en-US&gl=US&ceid=US:en"));
        map.put("c", catC);

        // [d] Girişimcilik ve Teknoloji
        CategoryInfo catD = new CategoryInfo("d", "Girişimcilik ve Teknoloji", "🚀",
                "TechCrunch, Webrazzi, Pandaily, TechNode, Heise Online");
        catD.sites.add(new SiteEntry("TechCrunch", "https://techcrunch.com/"));
        catD.sites.add(new SiteEntry("Webrazzi", "https://webrazzi.com/"));
        catD.sites.add(new SiteEntry("Pandaily", "https://pandaily.com/"));
        catD.sites.add(new SiteEntry("TechNode", "https://technode.com/"));
        catD.sites.add(new SiteEntry("Heise Online", "https://www.heise.de/"));
        catD.feeds.add(new FeedSource("TechCrunch", "https://techcrunch.com/feed/"));
        catD.feeds.add(new FeedSource("Webrazzi", "https://webrazzi.com/feed/"));
        catD.feeds.add(new FeedSource("Pandaily", "https://pandaily.com/feed/"));
        catD.feeds.add(new FeedSource("TechNode", "https://technode.com/feed/"));
        catD.feeds.add(new FeedSource("Heise Online", "https://www.heise.de/rss/heise-atom.xml"));
        map.put("d", catD);

        // [e] Yerel Teknoloji Basını (TR)
        CategoryInfo catE = new CategoryInfo("e", "Yerel Teknoloji Basını (TR)", "📱",
                "DonanımHaber, ShiftDelete.Net, Webtekno");
        catE.sites.add(new SiteEntry("DonanımHaber", "https://www.donanimhaber.com/"));
        catE.sites.add(new SiteEntry("ShiftDelete.Net", "https://shiftdelete.net/"));
        catE.sites.add(new SiteEntry("Webtekno", "https://www.webtekno.com/"));
        catE.feeds.add(new FeedSource("DonanımHaber", "https://www.donanimhaber.com/rss/tum/"));
        catE.feeds.add(new FeedSource("ShiftDelete.Net", "https://shiftdelete.net/feed"));
        catE.feeds.add(new FeedSource("Webtekno", "https://www.webtekno.com/rss.xml"));
        map.put("e", catE);

        // [f] Kurumsal Araştırma & Enstitü
        CategoryInfo catF = new CategoryInfo("f", "Kurumsal Araştırma & Enstitü", "🏛️",
                "Fraunhofer, CNRS News, Science Japan JST, AIBS, Sciences et Avenir, Psychology Today, Britannica");
        catF.sites.add(new SiteEntry("Fraunhofer-Gesellschaft", "https://www.fraunhofer.de/"));
        catF.sites.add(new SiteEntry("CNRS News", "https://news.cnrs.fr/"));
        catF.sites.add(new SiteEntry("Science Japan (JST)", "https://sj.jst.go.jp/"));
        catF.sites.add(new SiteEntry("AIBS News", "https://www.aibs.org/news/"));
        catF.sites.add(new SiteEntry("Sciences et Avenir", "https://www.sciencesetavenir.fr/"));
        catF.sites.add(new SiteEntry("Psychology Today", "https://www.psychologytoday.com/us"));
        catF.sites.add(new SiteEntry("Encyclopedia Britannica", "https://www.britannica.com/"));
        catF.feeds.add(new FeedSource("Sciences et Avenir", "https://www.sciencesetavenir.fr/rss.xml"));
        catF.feeds.add(new FeedSource("CNRS News", "https://news.cnrs.fr/rss.xml"));
        catF.feeds.add(new FeedSource("Enstitüler (Fraunhofer, CNRS, JST, AIBS)",
                "https://news.google.com/rss/search?q=site:fraunhofer.de+OR+site:news.cnrs.fr+OR+site:sj.jst.go.jp+OR+site:aibs.org+OR+site:sciencesetavenir.fr&hl=en-US&gl=US&ceid=US:en"));
        catF.feeds.add(new FeedSource("İnsan Davranışı & Kültür (Psychology Today, Britannica)",
                "https://news.google.com/rss/search?q=site:psychologytoday.com+OR+site:britannica.com+when:7d&hl=en-US&gl=US&ceid=US:en"));
        map.put("f", catF);

        // [k] Kuantum Teknolojileri (Tüm 30 Sitede)
        CategoryInfo catK = new CategoryInfo("k", "Kuantum Teknolojileri", "⚛️",
                "Kuantum bilgisayarlar, qubit, süperiletkenlik, kriptografi (30 Sitenin Tamamı)");
        catK.feeds.add(new FeedSource("Küresel Bilim Sitelerinde Kuantum",
                "https://news.google.com/rss/search?q=(quantum+OR+qubit+OR+superconductor)+AND+(site:phys.org+OR+site:quantamagazine.org+OR+site:science.org+OR+site:scientificamerican.com+OR+site:livescience.com+OR+site:eurekalert.org)&hl=en-US&gl=US&ceid=US:en"));
        catK.feeds.add(new FeedSource("Teknoloji ve Enstitü Sitelerinde Kuantum",
                "https://news.google.com/rss/search?q=(quantum+OR+qubit)+AND+(site:technologyreview.com+OR+site:arstechnica.com+OR+site:techcrunch.com+OR+site:techradar.com+OR+site:fraunhofer.de+OR+site:news.cnrs.fr+OR+site:sj.jst.go.jp+OR+site:heise.de)&hl=en-US&gl=US&ceid=US:en"));
        catK.feeds.add(new FeedSource("Türkçe Sitelerde Kuantum",
                "https://news.google.com/rss/search?q=(kuantum+OR+qubit+OR+süperiletken)+AND+(site:evrimagaci.org+OR+site:sarkac.org+OR+site:bilimgenc.tubitak.gov.tr+OR+site:fizikist.com+OR+site:donanimhaber.com+OR+site:shiftdelete.net+OR+site:webtekno.com+OR+site:webrazzi.com)&hl=tr&gl=TR&ceid=TR:tr"));
        map.put("k", catK);

        // [y] Yapay Zeka / AI (Tüm 30 Sitede)
        CategoryInfo catY = new CategoryInfo("y", "Yapay Zeka (AI)", "🤖",
                "LLM, AGI, derin öğrenme, AI çipleri, otonom sistemler (30 Sitenin Tamamı)");
        catY.feeds.add(new FeedSource("Küresel Teknoloji Sitelerinde Yapay Zeka",
                "https://news.google.com/rss/search?q=(AI+OR+LLM+OR+AGI+OR+%22artificial+intelligence%22)+AND+(site:technologyreview.com+OR+site:techcrunch.com+OR+site:arstechnica.com+OR+site:techradar.com+OR+site:pandaily.com+OR+site:technode.com+OR+site:heise.de)+when:7d&hl=en-US&gl=US&ceid=US:en"));
        catY.feeds.add(new FeedSource("Küresel Bilim & Enstitü Sitelerinde Yapay Zeka",
                "https://news.google.com/rss/search?q=(AI+OR+%22artificial+intelligence%22+OR+%22neural+network%22+OR+%22deep+learning%22)+AND+(site:science.org+OR+site:scientificamerican.com+OR+site:phys.org+OR+site:quantamagazine.org+OR+site:eurekalert.org+OR+site:fraunhofer.de+OR+site:psychologytoday.com)+when:7d&hl=en-US&gl=US&ceid=US:en"));
        catY.feeds.add(new FeedSource("Türkçe Sitelerde Yapay Zeka",
                "https://news.google.com/rss/search?q=(%22yapay+zeka%22+OR+LLM+OR+ChatGPT+OR+Gemini)+AND+(site:evrimagaci.org+OR+site:sarkac.org+OR+site:bilimgenc.tubitak.gov.tr+OR+site:webrazzi.com+OR+site:donanimhaber.com+OR+site:shiftdelete.net+OR+site:webtekno.com)+when:7d&hl=tr&gl=TR&ceid=TR:tr"));
        map.put("y", catY);

        return map;
    }
}

package com.antigravity.bilimteknoloji.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Kullanıcı ayarları, API anahtarı ve kategori bazlı son kontrol tarihlerini saklar.
 */
public class PreferencesHelper {
    private static final String PREF_NAME = "bilim_teknoloji_prefs";
    private static final String KEY_GEMINI_API_KEY = "gemini_api_key";
    private static final String KEY_INTEREST_KEYWORDS = "interest_keywords";

    private static final String DEFAULT_KEYWORDS = "uzay,kuantum,yapay zeka,füzyon,süperiletken,teleskop,biyoteknoloji,yarı iletken,çip,robotik";

    public static String getGeminiApiKey(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_GEMINI_API_KEY, "");
    }

    public static void setGeminiApiKey(Context context, String apiKey) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_GEMINI_API_KEY, apiKey != null ? apiKey.trim() : "").apply();
    }

    public static Set<String> getInterestKeywords(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String raw = sp.getString(KEY_INTEREST_KEYWORDS, DEFAULT_KEYWORDS);
        Set<String> set = new HashSet<>();
        for (String item : raw.split(",")) {
            String clean = item.trim().toLowerCase(Locale.ROOT);
            if (!clean.isEmpty()) set.add(clean);
        }
        return set;
    }

    public static void setInterestKeywords(Context context, String keywordsCsv) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_INTEREST_KEYWORDS, keywordsCsv != null ? keywordsCsv.trim() : DEFAULT_KEYWORDS).apply();
    }

    public static String getLastCheckTime(Context context, String categoryKey) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String saved = sp.getString("last_check_" + categoryKey.toLowerCase(Locale.ROOT), "");
        if (saved.isEmpty()) {
            // Varsayılan: Son 7 gün öncesi
            long sevenDaysAgo = System.currentTimeMillis() - (7L * 24L * 3600L * 1000L);
            return new SimpleDateFormat("dd.MM.yyyy HH:mm", new Locale("tr", "TR")).format(new Date(sevenDaysAgo));
        }
        return saved;
    }

    public static void setLastCheckTime(Context context, String categoryKey) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String nowStr = new SimpleDateFormat("dd.MM.yyyy HH:mm", new Locale("tr", "TR")).format(new Date());
        sp.edit().putString("last_check_" + categoryKey.toLowerCase(Locale.ROOT), nowStr).apply();
    }
}

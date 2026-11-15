package com.pooyan.namakadetv;

import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Local title matching + explicitly labelled external web search. No site APIs are implied. */
public final class SearchEngine {
    private SearchEngine() {}

    public static String normalize(String input) {
        if (input == null) return "";
        return Normalizer.normalize(input.toLowerCase(Locale.ROOT), Normalizer.Form.NFKC)
                .replace('\u064A','\u06CC').replace('\u0643','\u06A9')
                .replaceAll("[\\u064B-\\u065F\\u0670]", "")
                .replaceAll("[^\\p{L}\\p{N}]+", " ").trim().replaceAll("\\s+", " ");
    }

    public static boolean matches(String query, String title) {
        String q = normalize(query), t = normalize(title);
        if (q.isEmpty() || t.isEmpty()) return false;
        for (String token : q.split(" ")) if (!t.contains(token)) return false;
        return true;
    }

    public static String webSearchUrl(String query, SiteCatalog.Site site) {
        String q = query == null ? "" : query.trim();
        if (q.length() > 100) q = q.substring(0, 100);
        if (q.isEmpty()) return "";
        StringBuilder expression = new StringBuilder(q);
        if (site == null) {
            List<String> terms = new ArrayList<>();
            for (SiteCatalog.Site s : SiteCatalog.all()) {
                if (!SiteCatalog.PERSONAL.equals(s.group)) terms.add("site:" + s.host);
            }
            expression.append(" (").append(String.join(" OR ", terms)).append(')');
        } else {
            expression.append(" site:").append(site.host);
        }
        try {
            return "https://www.bing.com/search?q=" + URLEncoder.encode(expression.toString(), "UTF-8");
        } catch (UnsupportedEncodingException impossible) {
            throw new AssertionError(impossible);
        }
    }
}

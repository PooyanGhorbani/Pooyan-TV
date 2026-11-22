package com.pooyan.namakadetv;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Map;

/** Local-only ranking; missing years, ratings and discovery times are never fabricated. */
public final class CatalogOrdering {
    public static final int NEWEST = 0;
    public static final int TOP_RATED = 1;
    public static final int FIRST_SEEN = 2;
    public static final int ORIGINAL = 3;
    public static final int RELEVANCE = 4;

    private CatalogOrdering() {}

    public static String label(int mode) {
        switch (mode) {
            case TOP_RATED: return "بالاترین امتیاز";
            case FIRST_SEEN: return "تازه شناسایی‌شده";
            case ORIGINAL: return "ترتیب کاتالوگ";
            case RELEVANCE: return "مرتبط‌ترین";
            default: return "جدیدترین انتشار";
        }
    }

    public static List<FarsiPlexTitle> order(List<FarsiPlexTitle> source, int mode,
                                              Map<String, Long> firstSeen, String query) {
        List<FarsiPlexTitle> result = new ArrayList<>();
        if (source == null) return result;
        for (FarsiPlexTitle item : source) if (item != null) result.add(item);
        if (mode == ORIGINAL) return result;
        Map<String, Long> times = firstSeen == null ? Collections.emptyMap() : firstSeen;
        result.sort((a, b) -> {
            int c = 0;
            if (mode == TOP_RATED) {
                c = Double.compare(DiscoveryPolicy.rating(b.rating), DiscoveryPolicy.rating(a.rating));
                if (c == 0) c = Integer.compare(DiscoveryPolicy.year(b.year), DiscoveryPolicy.year(a.year));
            } else if (mode == FIRST_SEEN) {
                c = Long.compare(timestamp(times, b.url), timestamp(times, a.url));
                if (c == 0) c = Integer.compare(DiscoveryPolicy.year(b.year), DiscoveryPolicy.year(a.year));
                if (c == 0) c = Double.compare(DiscoveryPolicy.rating(b.rating), DiscoveryPolicy.rating(a.rating));
            } else if (mode == RELEVANCE) {
                c = Integer.compare(relevance(b, query), relevance(a, query));
                if (c == 0) c = Integer.compare(DiscoveryPolicy.year(b.year), DiscoveryPolicy.year(a.year));
                if (c == 0) c = Double.compare(DiscoveryPolicy.rating(b.rating), DiscoveryPolicy.rating(a.rating));
            } else {
                c = Integer.compare(DiscoveryPolicy.year(b.year), DiscoveryPolicy.year(a.year));
                if (c == 0) c = Double.compare(DiscoveryPolicy.rating(b.rating), DiscoveryPolicy.rating(a.rating));
            }
            return c; // Stable sorting keeps the catalog order for indistinguishable metadata.
        });
        return result;
    }

    public static List<FarsiPlexTitle> topRated(List<FarsiPlexTitle> source) {
        List<FarsiPlexTitle> rated = new ArrayList<>();
        if (source != null) for (FarsiPlexTitle item : source)
            if (item != null && DiscoveryPolicy.rating(item.rating) > 0) rated.add(item);
        return order(rated, TOP_RATED, Collections.emptyMap(), "");
    }

    private static long timestamp(Map<String, Long> times, String url) {
        Long value = times.get(url);
        return value == null ? 0L : value;
    }

    static int relevance(FarsiPlexTitle item, String query) {
        String q = SearchPolicy.normalize(query);
        if (q.isEmpty()) return 0;
        int best = match(SearchPolicy.normalize(item.name), q, 100, 80, 50);
        for (String alias : item.aliases)
            best = Math.max(best, match(SearchPolicy.normalize(alias), q, 95, 75, 45));
        return best;
    }

    private static int match(String value, String query, int exact, int prefix, int contains) {
        if (value.equals(query)) return exact;
        if (value.startsWith(query)) return prefix;
        return value.contains(query) ? contains : 0;
    }
}

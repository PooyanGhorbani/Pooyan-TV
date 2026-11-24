package com.pooyan.namakadetv;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Stable, local, bounded hero selection; no fake metadata or background API calls. */
public final class HomeHeroPolicy {
    public static final int MAX_SLIDES = 8;
    public static final long ROTATION_MS = 8000L;
    private HomeHeroPolicy() {}

    public static List<FarsiPlexTitle> select(List<FarsiPlexTitle> movies, List<FarsiPlexTitle> series) {
        List<FarsiPlexTitle> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int i = 0;
        int max = Math.max(movies == null ? 0 : movies.size(), series == null ? 0 : series.size());
        while (i < max && out.size() < MAX_SLIDES) {
            if (movies != null && i < movies.size()) add(out, seen, movies.get(i));
            if (out.size() < MAX_SLIDES && series != null && i < series.size()) add(out, seen, series.get(i));
            i++;
        }
        return out;
    }

    private static void add(List<FarsiPlexTitle> out, Set<String> seen, FarsiPlexTitle title) {
        if (title != null && title.url != null && !title.url.isEmpty()
                && title.poster != null && !title.poster.isEmpty() && seen.add(title.url)) {
            out.add(title);
        }
    }

    public static int indexAfter(int index, int delta, int count) {
        if (count <= 0) return 0;
        int value = (index + delta) % count;
        return value < 0 ? value + count : value;
    }

    public static String label(int index, int count) {
        if (count <= 0) return "پیشنهادها در حال دریافت‌اند";
        if (count == 1) return "۱ پیشنهاد";
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < count; i++) b.append(i == index ? "● " : "○ ");
        return b.toString().trim() + "    " + (index + 1) + " / " + count + "    ◀ / ▶  پیشنهاد بعدی یا قبلی";
    }
}

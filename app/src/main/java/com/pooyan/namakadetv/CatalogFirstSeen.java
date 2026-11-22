package com.pooyan.namakadetv;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Tracks first discovery on this device. This is NOT the source website's upload date. */
final class CatalogFirstSeen {
    private static final String PREF = "pooyan_catalog_first_seen";
    private static final int LIMIT = 2500;
    private CatalogFirstSeen() {}

    static void record(Context context, List<FarsiPlexTitle> titles) {
        if (titles == null || titles.isEmpty()) return;
        SharedPreferences p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        Map<String, ?> saved = p.getAll();
        SharedPreferences.Editor editor = p.edit();
        long now = System.currentTimeMillis();
        Map<String, Long> all = new HashMap<>();
        for (Map.Entry<String, ?> entry : saved.entrySet())
            if (entry.getValue() instanceof Long) all.put(entry.getKey(), (Long) entry.getValue());
        for (FarsiPlexTitle title : titles) {
            if (title == null || title.url == null || title.url.isEmpty()) continue;
            if (!all.containsKey(title.url)) {
                all.put(title.url, now);
                editor.putLong(title.url, now);
            }
        }
        if (all.size() > LIMIT) {
            List<Map.Entry<String, Long>> ordered = new ArrayList<>(all.entrySet());
            ordered.sort(Comparator.comparingLong(Map.Entry::getValue));
            for (int i = 0; i < ordered.size() - LIMIT; i++) editor.remove(ordered.get(i).getKey());
        }
        editor.apply();
    }

    static Map<String, Long> snapshot(Context context) {
        Map<String, Long> result = new HashMap<>();
        for (Map.Entry<String, ?> entry : context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getAll().entrySet())
            if (entry.getValue() instanceof Long) result.put(entry.getKey(), (Long) entry.getValue());
        return result;
    }
}

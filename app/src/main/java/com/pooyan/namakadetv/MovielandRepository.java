package com.pooyan.namakadetv;

import android.app.Activity;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Reads public catalog pages only. Playback requires a separate approved integration. */
final class MovielandRepository {
    private static final String PREF = "experimental_catalog";
    private final Activity activity;
    private final ExecutorService io = Executors.newFixedThreadPool(2);
    private volatile boolean closed;

    MovielandRepository(Activity activity) { this.activity = activity; }

    boolean enabled() {
        return BuildConfig.DEBUG && activity.getSharedPreferences(PREF, 0)
                .getBoolean("movieland_metadata", false);
    }

    void setEnabled(boolean enabled) {
        activity.getSharedPreferences(PREF, 0).edit().putBoolean("movieland_metadata", enabled).apply();
    }

    private Document page(String path) throws IOException {
        return Jsoup.connect(MovielandCatalog.BASE + path).timeout(6500)
                .maxBodySize(2_000_000).followRedirects(false).get();
    }

    void list(String kind, FarsiPlexRepository.Result callback) {
        load("/" + ("series".equals(kind) ? "series" : "movies"), "", callback);
    }

    void search(String query, FarsiPlexRepository.Result callback) {
        load("/", query, callback);
    }

    private void load(String path, String query, FarsiPlexRepository.Result cb) {
        if (closed || !enabled()) { cb.receive(Collections.emptyList(), null); return; }
        io.execute(() -> {
            List<FarsiPlexTitle> items = new ArrayList<>();
            boolean failed = false;
            for (String route : "/".equals(path) ? new String[]{"/", "/movies", "/series"} : new String[]{path}) {
                if (closed) return;
                try {
                    for (FarsiPlexTitle item : MovielandCatalog.listing(page(route))) {
                        if (query.isEmpty() || SearchPolicy.matches(item, query)) items.add(item);
                    }
                } catch (IOException | RuntimeException e) { failed = true; }
            }
            final String error = failed && items.isEmpty() ? "دریافت بخشی از کاتالوگ ممکن نبود." : null;
            activity.runOnUiThread(() -> {
                if (!closed) cb.receive(enabled() ? items : Collections.emptyList(), enabled() ? error : null);
            });
        });
    }

    void detail(FarsiPlexTitle item, FarsiPlexRepository.DetailResult cb) {
        if (closed || !enabled()) { cb.receive(item, "کاتالوگ آزمایشی غیرفعال است."); return; }
        io.execute(() -> {
            FarsiPlexTitle result;
            String error = null;
            try {
                String path = MovielandCatalog.path(item.url);
                if (path.isEmpty()) throw new IOException("Unsupported catalog path");
                result = MovielandCatalog.details(page(path), item);
            } catch (IOException | RuntimeException e) {
                result = item;
                error = "دریافت اطلاعات عنوان ممکن نبود.";
            }
            FarsiPlexTitle value = result;
            String failure = error;
            activity.runOnUiThread(() -> {
                if (!closed) cb.receive(enabled() ? value : item, enabled() ? failure : "کاتالوگ آزمایشی غیرفعال است.");
            });
        });
    }

    void close() { closed = true; io.shutdownNow(); }
}

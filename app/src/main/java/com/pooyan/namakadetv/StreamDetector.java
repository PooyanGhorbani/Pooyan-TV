package com.pooyan.namakadetv;

import java.net.URI;
import java.util.Locale;

/** Only recognizes explicit media URLs; never extracts DRM keys or decrypts streams. */
public final class StreamDetector {
    private StreamDetector() {}

    public static boolean isDirectMedia(String url) {
        if (!isHttpsUrl(url)) return false;
        String path;
        try { path = new URI(url.trim()).getPath(); } catch (Exception e) { return false; }
        if (path == null) return false;
        String p = path.toLowerCase(Locale.ROOT);
        return p.endsWith(".m3u8") || p.endsWith(".mpd") ||
                p.endsWith(".mp4") || p.endsWith(".m4v") || p.endsWith(".webm");
    }

    public static boolean isHttpsUrl(String url) {
        if (url == null || url.length() > 8192) return false;
        try {
            URI u = new URI(url.trim());
            return "https".equalsIgnoreCase(u.getScheme()) &&
                    u.getHost() != null && !u.getHost().isEmpty() &&
                    u.getRawUserInfo() == null;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static String safeName(String url) {
        if (url == null) return "Video";
        try {
            String path = new URI(url).getPath();
            if (path == null) return "Video";
            String last = path.substring(path.lastIndexOf('/') + 1);
            if (last == null || last.isEmpty()) return "Video";
            return last.length() > 60 ? last.substring(0, 60) + "…" : last;
        } catch (Exception ignored) { return "Video"; }
    }
}

package com.pooyan.namakadetv;

import java.net.URLEncoder;
import java.util.Locale;

/** Stable, encoded request paths for the official Stremio Cinemeta catalogue. */
public final class CatalogPolicy {
    public static final String BASE = "https://v3-cinemeta.strem.io";
    private CatalogPolicy() { }
    public static String catalog(String type, String genre) {
        String t = validType(type);
        if (genre == null || genre.trim().isEmpty()) return BASE + "/catalog/" + t + "/top.json";
        return BASE + "/catalog/" + t + "/top/genre=" + enc(genre.trim()) + ".json";
    }
    public static String search(String type, String query) {
        if (query == null || query.trim().isEmpty()) throw new IllegalArgumentException("Empty query");
        return BASE + "/catalog/" + validType(type) + "/top/search=" + enc(query.trim()) + ".json";
    }
    public static String meta(String type, String id) {
        if (id == null || !id.matches("tt[0-9]{5,12}")) throw new IllegalArgumentException("IMDb ID invalid");
        return BASE + "/meta/" + validType(type) + "/" + id + ".json";
    }
    public static String validType(String type) {
        if (!"movie".equals(type) && !"series".equals(type)) throw new IllegalArgumentException("Unknown catalog type");
        return type;
    }
    public static String enc(String raw) {
        try { return URLEncoder.encode(raw, "UTF-8").replace("+", "%20"); }
        catch (java.io.UnsupportedEncodingException impossible) { throw new IllegalStateException(impossible); }
    }
    public static boolean isImageUrl(String url) {
        if (url == null || !url.startsWith("https://") || url.length() > 1500) return false;
        try {
            java.net.URI u = new java.net.URI(url);
            String host = u.getHost() == null ? "" : u.getHost().toLowerCase(Locale.ROOT);
            return host.equals("images.metahub.space") || host.equals("image.tmdb.org") ||
                    host.equals("m.media-amazon.com") || host.equals("metadata-static.plex.tv") ||
                    host.endsWith(".strem.io");
        } catch (Exception e) { return false; }
    }
}

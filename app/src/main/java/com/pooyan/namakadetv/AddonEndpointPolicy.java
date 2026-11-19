package com.pooyan.namakadetv;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Locale;

/** Strict URL handling for user-supplied, opt-in Stremio add-ons (metadata and playable HTTPS only). */
public final class AddonEndpointPolicy {
    private AddonEndpointPolicy() {}

    public static boolean validManifest(String value) {
        if (value == null || value.length() > 1024) return false;
        try {
            URI uri = new URI(value.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                    || uri.getRawUserInfo() != null || uri.getRawQuery() != null
                    || uri.getRawFragment() != null || (uri.getPort() != -1 && uri.getPort() != 443)) return false;
            String host = uri.getHost().toLowerCase(Locale.ROOT);
            if (host.equals("localhost") || host.endsWith(".local") || host.endsWith(".internal")
                    || host.equals("0.0.0.0") || host.equals("127.0.0.1")
                    || host.startsWith("127.") || host.startsWith("10.")
                    || host.startsWith("192.168.") || host.startsWith("169.254.")
                    || host.startsWith("172.16.") || host.startsWith("172.17.")
                    || host.startsWith("172.18.") || host.startsWith("172.19.")
                    || host.startsWith("172.2") || host.startsWith("172.30.")
                    || host.startsWith("172.31.")) return false;
            String path = uri.getRawPath();
            return path != null && path.endsWith("/manifest.json") && !path.contains("..")
                    && !path.toLowerCase(Locale.ROOT).contains("%2f") && !path.contains("\\");
        } catch (Exception e) { return false; }
    }

    public static String base(String manifest) {
        if (!validManifest(manifest)) return "";
        String value = manifest.trim();
        return value.substring(0, value.length() - "manifest.json".length());
    }

    static String segment(String value) {
        if (value == null || value.length() > 512) return "";
        try { return URLEncoder.encode(value, "UTF-8").replace("+", "%20"); }
        catch (Exception e) { return ""; }
    }

    static String decode(String value) {
        try { return URLDecoder.decode(value, "UTF-8"); }
        catch (Exception e) { return ""; }
    }

    public static String metaUrl(String manifest, String type, String id) {
        String base = base(manifest), value = segment(id);
        if (base.isEmpty() || value.isEmpty() || !("movie".equals(type) || "series".equals(type))) return "";
        return base + "meta/" + type + "/" + value + ".json";
    }

    public static String episodeUrl(String manifest, String id) {
        String base = base(manifest), value = segment(id);
        return base.isEmpty() || value.isEmpty() ? "" : base + "stream/series/" + value + ".json";
    }

    public static String typeFor(String manifest, String url) {
        String tail = tail(manifest, url);
        if (tail.startsWith("meta/movie/") && tail.endsWith(".json")) return "movie";
        if (tail.startsWith("meta/series/") && tail.endsWith(".json")) return "series";
        if (tail.startsWith("stream/series/") && tail.endsWith(".json")) return "episode";
        return "";
    }

    public static String tail(String manifest, String url) {
        String base = base(manifest);
        if (base.isEmpty() || url == null || url.length() > 2048) return "";
        try {
            URI canonical = new URI(base), value = new URI(url);
            if (!"https".equalsIgnoreCase(value.getScheme()) || value.getRawUserInfo() != null
                    || value.getRawQuery() != null || value.getRawFragment() != null
                    || value.getPort() != canonical.getPort()
                    || !canonical.getHost().equalsIgnoreCase(value.getHost())) return "";
            String prefix = canonical.getRawPath(), path = value.getRawPath();
            if (prefix == null || path == null || !path.startsWith(prefix)) return "";
            String tail = path.substring(prefix.length());
            if (tail.contains("/") && tail.indexOf('/') != tail.lastIndexOf('/')) {
                // meta/type/id or stream/series/id: exactly two path separators
                if (tail.indexOf('/', tail.indexOf('/') + 1) != tail.lastIndexOf('/')) return "";
            }
            if (tail.contains("..") || tail.toLowerCase(Locale.ROOT).contains("%2f")) return "";
            return tail;
        } catch (Exception e) { return ""; }
    }

    public static String idFrom(String manifest, String url) {
        String type = typeFor(manifest, url), tail = tail(manifest, url);
        if (type.isEmpty()) return "";
        int start = tail.lastIndexOf('/') + 1;
        return decode(tail.substring(start, tail.length() - ".json".length()));
    }
}

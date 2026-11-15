package com.pooyan.namakadetv;

import java.net.URI;
import java.util.Locale;

/** Unauthenticated thumbnails only, limited to the same curated provider as the page. */
public final class PosterPolicy {
    private PosterPolicy() {}
    public static boolean accept(String page, String poster) {
        if (!StreamDetector.isHttpsUrl(page) || !StreamDetector.isHttpsUrl(poster)) return false;
        if (poster.length() > 2048) return false;
        SiteCatalog.Site site = SiteCatalog.fromUrl(page);
        if (site == null || !SiteCatalog.matches(site, poster)) return false;
        try {
            URI uri = new URI(poster);
            int port = uri.getPort();
            if (port != -1 && port != 443) return false;
            String path = uri.getPath().toLowerCase(Locale.ROOT);
            if (path.endsWith(".svg") || path.endsWith(".svgz")) return false;
            return true;
        } catch (Exception e) { return false; }
    }
}

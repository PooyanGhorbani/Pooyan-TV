package com.pooyan.namakadetv;

/** Decides whether a poster URL may be stored next to a history/bookmark entry. */
public final class PosterPolicy {
    private PosterPolicy() { }
    public static boolean accept(String itemUrl, String posterUrl) {
        return itemUrl != null && posterUrl != null && CatalogPolicy.isImageUrl(posterUrl);
    }
}

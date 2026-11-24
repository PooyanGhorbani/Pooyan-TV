package com.pooyan.namakadetv;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.net.URI;

/**
 * Recognizes only media URLs directly published in public HTML5 <video>/<source> tags.
 * Never requests private endpoints, examines iframes, accesses cookies, or bypasses DRM.
 * A URL here is not proof of licensing or successful playback; the player may fail.
 */
public final class PublicHtml5Media {
    private PublicHtml5Media() {}

    public static String find(Document doc, String pageUrl) {
        if (doc == null || !StreamDetector.isHttpsUrl(pageUrl)) return "";
        for (Element tag : doc.select("video[src], video source[src]")) {
            String candidate = tag.attr("src").trim();
            if (candidate.isEmpty()) continue;
            try {
                URI resolved = new URI(pageUrl).resolve(new URI(candidate));
                String uri=resolved.toString();
                if (StreamDetector.isDirectMedia(uri)) return uri;
            } catch (Exception ignored) {
                // Broken markup or a non-HTTP URI is not a playable source.
            }
        }
        return "";
    }
}

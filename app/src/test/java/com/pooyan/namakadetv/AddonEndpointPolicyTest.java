package com.pooyan.namakadetv;

import org.junit.Test;
import static org.junit.Assert.*;

public final class AddonEndpointPolicyTest {
    private static final String MANIFEST = "https://example.org/customer/manifest.json";

    @Test public void invalidEndpointsRejected() {
        assertFalse(AddonEndpointPolicy.validManifest("http://example.org/manifest.json"));
        assertFalse(AddonEndpointPolicy.validManifest("https://user:pass@example.org/manifest.json"));
        assertFalse(AddonEndpointPolicy.validManifest("https://localhost/manifest.json"));
        assertFalse(AddonEndpointPolicy.validManifest("https://127.0.0.1/manifest.json"));
        assertFalse(AddonEndpointPolicy.validManifest("https://example.org/manifest.json?token=oops"));
        assertFalse(AddonEndpointPolicy.validManifest("https://example.org/api"));
        assertTrue(AddonEndpointPolicy.validManifest(MANIFEST));
    }

    @Test public void metadataAndEpisodeUrlsStayOnConfiguredHostAndBasePath() {
        String movie=AddonEndpointPolicy.metaUrl(MANIFEST,"movie","tt1234567");
        assertEquals("movie",AddonEndpointPolicy.typeFor(MANIFEST,movie));
        assertEquals("tt1234567",AddonEndpointPolicy.idFrom(MANIFEST,movie));
        String episode=AddonEndpointPolicy.episodeUrl(MANIFEST,"tt1234567:2:6");
        assertEquals("episode",AddonEndpointPolicy.typeFor(MANIFEST,episode));
        assertEquals("tt1234567:2:6",AddonEndpointPolicy.idFrom(MANIFEST,episode));
        assertEquals("",AddonEndpointPolicy.typeFor(MANIFEST,"https://evil.org/customer/meta/movie/tt1234567.json"));
        assertEquals("",AddonEndpointPolicy.typeFor(MANIFEST,"https://example.org/meta/movie/tt1234567.json"));
        assertEquals("",AddonEndpointPolicy.typeFor(MANIFEST,movie+"?redirect=1"));
    }
}

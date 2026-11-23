package com.pooyan.namakadetv;

import org.junit.Test;
import static org.junit.Assert.*;

/** Keep the v0.8.6 GitHub Release asset, updater allowlist and version semantics aligned. */
public final class UpdateReleaseNamingTest {
    private static final String BASE = "https://github.com/PooyanGhorbani/Pooyan-TV/releases/download/";
    private static final String TAG = "v0.8.6";
    private static final String APK = "Pooyan-TV-v0.8.6-preview.apk";

    @Test public void publishedPreviewAssetIsAcceptedByUpdater() {
        assertTrue(UpdatePolicy.validAsset(BASE + TAG + "/" + APK, TAG, APK));
    }
    @Test public void tagAndApkNameMustAgree() {
        assertFalse(UpdatePolicy.validAsset(BASE + "v0.8.7/" + APK,
                "v0.8.7", APK));
        assertFalse(UpdatePolicy.validAsset(BASE + TAG + "/Pooyan-TV-v0.8.5-preview.apk",
                TAG, "Pooyan-TV-v0.8.5-preview.apk"));
    }
    @Test public void foreignHostAndQueryAreRejected() {
        assertFalse(UpdatePolicy.validAsset("https://example.com/PooyanGhorbani/Pooyan-TV/releases/download/" + TAG + "/" + APK,
                TAG, APK));
        assertFalse(UpdatePolicy.validAsset(BASE + TAG + "/" + APK + "?token=x", TAG, APK));
    }
    @Test public void onlyStrictlyNewerVersionsAreOffered() {
        assertTrue(UpdatePolicy.newer("v0.8.6", "0.8.5"));
        assertFalse(UpdatePolicy.newer("v0.8.6", "0.8.6"));
        assertFalse(UpdatePolicy.newer("v0.8.5", "0.8.6"));
        assertFalse(UpdatePolicy.newer("0.8.7", "0.8.6"));
    }
    @Test public void digestIsStrictSha256() {
        String hex = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
        assertEquals(hex, UpdatePolicy.sha256("sha256:" + hex.toUpperCase()));
        assertEquals("", UpdatePolicy.sha256("sha256:abc"));
    }
}

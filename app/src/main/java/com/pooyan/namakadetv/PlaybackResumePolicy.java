package com.pooyan.namakadetv;

/** Pure resume calculation for background/foreground playback; easy to unit-test. */
public final class PlaybackResumePolicy {
    private PlaybackResumePolicy() {}
    public static long resumeAfterBackground(long positionMs, long durationMs, boolean ended) {
        if (ended) return 0L;
        long safe = Math.max(0L, positionMs);
        if (durationMs > 0L && durationMs < Long.MAX_VALUE) return Math.min(safe, durationMs);
        return safe;
    }
}

package com.pooyan.namakadetv;

import org.junit.Test;
import static org.junit.Assert.*;

/** Real Android TV viewports: 1080p at 2.0x is 540dp tall, 720p at 1.0x is 720dp. */
public final class HeroViewportRealisticTest {
    @Test public void heroStaysInsideTheClampOnRealTvHeights() {
        for (int dp : new int[]{480, 540, 600, 720, 810, 1080}) {
            int h = HeroViewportPolicy.heroHeight(dp);
            assertTrue("hero " + h + " for " + dp + "dp", h >= 300 && h <= 380);
        }
    }
    @Test public void heroGrowsMonotonicallyWithTheViewport() {
        int previous = 0;
        for (int dp = 400; dp <= 900; dp += 20) {
            int h = HeroViewportPolicy.heroHeight(dp);
            assertTrue(h >= previous);
            previous = h;
        }
    }
}

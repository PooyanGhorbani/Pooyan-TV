package com.pooyan.namakadetv;

/** Sizing for the cinematic home banner: avoid tall hero cards on 720p TVs. */
public final class HeroViewportPolicy {
    private HeroViewportPolicy(){}
    public static int heroHeight(int viewportDp){
        // 300dp accommodates the action row, two title lines and description.
        // At normal TV sizes it occupies approximately half the visible viewport.
        return Math.max(300,Math.min(380,Math.round(viewportDp*0.50f)));
    }
}

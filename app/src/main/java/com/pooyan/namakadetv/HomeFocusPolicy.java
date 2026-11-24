package com.pooyan.namakadetv;

/** Pure lane traversal decisions shared by remote navigation and JVM tests. */
public final class HomeFocusPolicy {
    private HomeFocusPolicy() {}
    public static int nextLane(int current, boolean down, int laneCount) {
        if (current < 0 || current >= laneCount) return -1;
        int next = current + (down ? 1 : -1);
        return next >= 0 && next < laneCount ? next : -1;
    }
    public static int column(int current, int targetCount) {
        if (targetCount <= 0) return -1;
        return Math.max(0, Math.min(current, targetCount - 1));
    }
}

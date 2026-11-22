package com.pooyan.namakadetv;

/** Pure selection logic for reliable TV-remote movement between poster rows. */
public final class TvPosterFocusPolicy {
    private TvPosterFocusPolicy() {}
    public static int neighbor(int index, int count, boolean right) {
        int next = index + (right ? 1 : -1);
        return next >= 0 && next < count ? next : -1;
    }
    public static boolean opensMenuAtRightEdge(int index,int count,boolean right){
        return right&&count>0&&index==count-1;
    }
    public static int column(int index, int childCount) {
        return childCount <= 0 ? -1 : Math.min(Math.max(index, 0), childCount-1);
    }
}

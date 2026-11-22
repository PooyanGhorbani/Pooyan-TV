package com.pooyan.namakadetv;

import org.junit.Test;
import static org.junit.Assert.*;

public final class RemoteNavigationEdgeTest {
    @Test public void rightEdgeNeedsOnePressToReachMenu(){
        assertTrue(TvPosterFocusPolicy.opensMenuAtRightEdge(4,5,true));
        assertFalse(TvPosterFocusPolicy.opensMenuAtRightEdge(3,5,true));
        assertFalse(TvPosterFocusPolicy.opensMenuAtRightEdge(4,5,false));
        assertFalse(TvPosterFocusPolicy.opensMenuAtRightEdge(-1,0,true));
    }
}

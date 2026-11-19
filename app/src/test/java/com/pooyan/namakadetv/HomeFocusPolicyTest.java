package com.pooyan.namakadetv;

import org.junit.Test;
import static org.junit.Assert.*;
public class HomeFocusPolicyTest {
    @Test public void returnsToHeroFromFirstPosterRail() {
        assertEquals(0,HomeFocusPolicy.nextLane(1,false,6));
        assertEquals(1,HomeFocusPolicy.nextLane(0,true,6));
    }
    @Test public void boundsAndColumnAreSafe() {
        assertEquals(-1,HomeFocusPolicy.nextLane(0,false,5));
        assertEquals(-1,HomeFocusPolicy.nextLane(4,true,5));
        assertEquals(1,HomeFocusPolicy.column(3,2));
        assertEquals(0,HomeFocusPolicy.column(-4,2));
    }
}

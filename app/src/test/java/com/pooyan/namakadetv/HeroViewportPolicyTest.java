package com.pooyan.namakadetv;

import org.junit.Test;
import static org.junit.Assert.*;

public final class HeroViewportPolicyTest {
    @Test public void bannerFitsTypical720pAnd1080pScreens(){
        assertEquals(300,HeroViewportPolicy.heroHeight(480));
        assertEquals(360,HeroViewportPolicy.heroHeight(720));
        assertEquals(380,HeroViewportPolicy.heroHeight(1080));
    }
}

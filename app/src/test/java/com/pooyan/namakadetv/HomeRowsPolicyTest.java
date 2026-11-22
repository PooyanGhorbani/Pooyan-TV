package com.pooyan.namakadetv;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public final class HomeRowsPolicyTest {
    @Test public void defaultsAreDeterministic(){
        assertEquals(Arrays.asList("top","recent","favorites","recommend"),HomeRowsPolicy.order(""));
    }
    @Test public void invalidDuplicatedOrOldPreferencesAreRepaired(){
        assertEquals(Arrays.asList("favorites","top","recent","recommend"),
                HomeRowsPolicy.order("favorites,favorites,other,top"));
    }
    @Test public void moveDoesNotDropRows(){
        String next=HomeRowsPolicy.move("top,recent,favorites,recommend",2,-1);
        assertEquals(Arrays.asList("top","favorites","recent","recommend"),HomeRowsPolicy.order(next));
        assertEquals(next,HomeRowsPolicy.move(next,0,-1));
        assertEquals(next,HomeRowsPolicy.move(next,3,1));
    }
}

package com.pooyan.namakadetv;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
/** Positive subtitle delay in media time; pausing freezes it and seeking clears it. */
public final class CueDelayQueue<T> {
 private final java.util.ArrayDeque<Item<T>> queue=new java.util.ArrayDeque<>();
 private static final class Item<T>{long at;T value;Item(long at,T value){this.at=at;this.value=value;}}
 public void add(long position,int delay,T value){if(queue.size()>=128)queue.removeFirst();queue.addLast(new Item<>(position+Math.max(0,Math.min(delay,5000)),value));}
 public T poll(long position){T latest=null;while(!queue.isEmpty()&&queue.peekFirst().at<=position)latest=queue.removeFirst().value;return latest;}
 public void clear(){queue.clear();}
}

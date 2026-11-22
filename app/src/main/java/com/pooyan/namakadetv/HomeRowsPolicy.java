package com.pooyan.namakadetv;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Stable, profile-local ordering for the four optional personalized home rows. */
public final class HomeRowsPolicy {
    public static final String TOP="top", RECENT="recent", FAVORITES="favorites", RECOMMEND="recommend";
    private static final List<String> DEFAULT=Collections.unmodifiableList(
            Arrays.asList(TOP,RECENT,FAVORITES,RECOMMEND));

    private HomeRowsPolicy() {}
    public static List<String> order(String stored){
        List<String> result=new ArrayList<>();
        if(stored!=null){
            for(String key:stored.split(","))if(DEFAULT.contains(key)&&!result.contains(key))result.add(key);
        }
        for(String key:DEFAULT)if(!result.contains(key))result.add(key);
        return result;
    }
    public static String move(String stored,int index,int delta){
        List<String> items=order(stored);
        int next=index+delta;
        if(index>=0&&index<items.size()&&next>=0&&next<items.size())
            Collections.swap(items,index,next);
        return String.join(",",items);
    }
    public static String title(String key){
        switch(key){
            case TOP:return "برترین آثار";
            case RECENT:return "تازه شناسایی‌شده‌ها";
            case FAVORITES:return "فهرست من";
            case RECOMMEND:return "پیشنهاد برای شما";
            default:return "ردیف";
        }
    }
}

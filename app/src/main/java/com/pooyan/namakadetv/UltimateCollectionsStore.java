package com.pooyan.namakadetv;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Profile-private named collections. Stored keys are opaque hashes, not stream URLs. */
public final class UltimateCollectionsStore {
    private final SharedPreferences prefs;
    public UltimateCollectionsStore(Context context){prefs=ProfileStore.data(context,"ultimate_collections");}
    public List<String> names(){
        List<String> out=new ArrayList<>(prefs.getStringSet("names",Collections.emptySet()));
        Collections.sort(out);return out;
    }
    public boolean create(String name){
        name=name==null?"":name.trim();
        if(name.length()<2||name.length()>32||name.contains("\n")||name.contains("/"))return false;
        Set<String> names=new HashSet<>(prefs.getStringSet("names",Collections.emptySet()));
        if(names.size()>=20&&!names.contains(name))return false;
        names.add(name);return prefs.edit().putStringSet("names",names).commit();
    }
    private String key(String name){return "c_"+UltimateIdentity.sha256(name).substring(0,28);}
    public boolean contains(String name,String contentKey){
        if(!names().contains(name))return false;
        return prefs.getStringSet(key(name),Collections.emptySet()).contains(UltimateIdentity.sha256(contentKey));
    }
    public void set(String name,String contentKey,boolean included){
        if(!names().contains(name)||contentKey==null||contentKey.isEmpty())return;
        Set<String> ids=new HashSet<>(prefs.getStringSet(key(name),Collections.emptySet()));
        String hashed=UltimateIdentity.sha256(contentKey);
        if(included){if(ids.size()>=250)return;ids.add(hashed);}else ids.remove(hashed);
        prefs.edit().putStringSet(key(name),ids).apply();
    }
    public void remove(String name){
        Set<String> names=new HashSet<>(prefs.getStringSet("names",Collections.emptySet()));
        names.remove(name);prefs.edit().putStringSet("names",names).remove(key(name)).apply();
    }
}

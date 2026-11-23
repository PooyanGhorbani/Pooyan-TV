package com.pooyan.namakadetv;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

/** Reversible local flags; does not delete original LibraryStore favorites or history. */
public final class UltimateWatchlist {
    private final SharedPreferences prefs;
    public UltimateWatchlist(Context context){prefs=ProfileStore.data(context,"ultimate_watchlist");}
    private String key(String kind,String id){return kind+"_"+UltimateIdentity.sha256(id).substring(0,24);}
    public boolean marked(String kind,String contentId){return prefs.getBoolean(key(kind,contentId),false);}
    public void mark(String kind,String contentId,boolean value){prefs.edit().putBoolean(key(kind,contentId),value).apply();}
    public Set<String> markedIn(String kind,Iterable<FarsiPlexTitle> titles){
        Set<String> out=new HashSet<>();if(titles==null)return out;
        for(FarsiPlexTitle t:titles){String id=UltimateIdentity.contentKey(t);if(marked(kind,id))out.add(id);}return out;
    }
}

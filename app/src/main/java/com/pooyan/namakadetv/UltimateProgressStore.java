package com.pooyan.namakadetv;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.HashSet;
import java.util.Set;

/** Per-profile progress keyed to canonical film/episode identity, not a temporary CDN stream URL. */
public final class UltimateProgressStore {
    public static final class Progress {
        public final long position,duration,updated;
        public final boolean complete;
        Progress(long p,long d,long t,boolean c){position=p;duration=d;updated=t;complete=c;}
    }
    private final SharedPreferences prefs;
    public UltimateProgressStore(Context context){prefs=ProfileStore.data(context,"ultimate_progress");}
    private static String field(String key){return "p_"+UltimateIdentity.sha256(key).substring(0,24);}
    public Progress get(String key){
        if(key==null||key.isEmpty())return null;
        try{JSONObject o=new JSONObject(prefs.getString(field(key),"{}"));
            if(o.length()==0)return null;
            return new Progress(Math.max(0,o.optLong("p")),Math.max(0,o.optLong("d")),o.optLong("at"),o.optBoolean("done"));
        }catch(Exception e){return null;}
    }
    public void save(String key,long position,long duration,boolean complete){
        if(key==null||key.isEmpty()||position<0)return;
        boolean done=complete||duration>0&&position>=duration*.97;
        long now=System.currentTimeMillis();
        try{JSONObject o=new JSONObject().put("p",Math.max(0,position)).put("d",Math.max(0,duration))
             .put("at",now).put("done",done);
            prefs.edit().putString(field(key),o.toString()).apply();
        }catch(Exception ignored){}
    }
    public void clear(String key){if(key!=null&&!key.isEmpty())prefs.edit().remove(field(key)).apply();}
    /** Explicit user request to erase watch history must erase canonical progress too. */
    public void clearAll(){prefs.edit().clear().apply();}
    /** Only already-known identities. No scan of arbitrary SharedPreferences keys. */
    public Set<String> completedKeys(Iterable<FarsiPlexTitle> catalog){
        Set<String> out=new HashSet<>();if(catalog==null)return out;
        for(FarsiPlexTitle t:catalog){String id=UltimateIdentity.contentKey(t);Progress p=get(id);if(p!=null&&p.complete)out.add(id);}
        return out;
    }
}

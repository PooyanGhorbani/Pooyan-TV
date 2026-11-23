package com.pooyan.namakadetv;

import android.content.Context;
import android.content.SharedPreferences;

/** Keeps opaque fingerprints, not playable URLs, tokens, or private headers. */
public final class UltimateSourceStore {
    private final SharedPreferences prefs;
    public UltimateSourceStore(Context context){prefs=ProfileStore.data(context,"ultimate_sources");}
    private String record(String titleKey){return "last_"+UltimateIdentity.sha256(titleKey).substring(0,24);}
    public String lastGood(String titleKey){return prefs.getString(record(titleKey),"");}
    public void success(String titleKey,String url){
        if(titleKey==null||titleKey.isEmpty()||url==null||url.isEmpty())return;
        prefs.edit().putString(record(titleKey),UltimateIdentity.sourceFingerprint(url)).apply();
    }
}

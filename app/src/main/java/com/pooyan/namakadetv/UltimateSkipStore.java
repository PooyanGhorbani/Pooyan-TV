package com.pooyan.namakadetv;

import android.content.Context;
import android.content.SharedPreferences;

/** Explicit user-made intro markers per series. No invented automatic scene detection. */
public final class UltimateSkipStore {
    private final SharedPreferences prefs;
    public UltimateSkipStore(Context context){prefs=ProfileStore.data(context,"ultimate_skip");}
    private String name(String series){return "intro_"+UltimateIdentity.sha256(series).substring(0,24);}
    public void setIntroEnd(String series,long ms){
        if(series!=null&&!series.isEmpty()&&ms>=5000&&ms<=900000)prefs.edit().putLong(name(series),ms).apply();
    }
    public long introEnd(String series){return series==null||series.isEmpty()?0:prefs.getLong(name(series),0);}
    public void forget(String series){if(series!=null)prefs.edit().remove(name(series)).apply();}
}

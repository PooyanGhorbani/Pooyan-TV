package com.pooyan.namakadetv;

import android.content.Context;
import android.content.SharedPreferences;

/** On-device aggregate diagnostics. NEVER store URLs, exceptions, response bodies, or authentication tokens. */
public final class UltimateDiagnostics {
    private final SharedPreferences prefs;
    public UltimateDiagnostics(Context c){prefs=ProfileStore.data(c,"ultimate_diagnostics");}
    public void count(String kind){
        if(!kind.matches("success|network|timeout|access|decoder|http|other"))kind="other";
        prefs.edit().putInt(kind,Math.min(100000,prefs.getInt(kind,0)+1)).apply();
    }
    public String summary(){
        return "پخش موفق: "+prefs.getInt("success",0)+"\nشبکه: "+prefs.getInt("network",0)+
          "\nTimeout: "+prefs.getInt("timeout",0)+"\nمحدودیت دسترسی: "+prefs.getInt("access",0)+
          "\nناسازگاری رمزگشا: "+prefs.getInt("decoder",0)+"\nHTTP: "+prefs.getInt("http",0)+
          "\nسایر خطاها: "+prefs.getInt("other",0);
    }
    public void reset(){prefs.edit().clear().apply();}
}

package com.pooyan.namakadetv;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

/** Family export/import from explicit document picker. Excludes all tokens, media URLs and credentials. */
public final class FamilyBackup {
    private FamilyBackup(){}
    private static final String LIB="namakade_local_library_",DATA="pooyan_farsiplex_v04_",PLAYER="player_preferences_";
    private static SharedPreferences of(Context c,String prefix,String id){return c.getSharedPreferences(prefix+id,0);}
    private static JSONObject playerSettings(SharedPreferences p)throws Exception{
        JSONObject settings=new JSONObject();
        settings.put("autoplay",p.getBoolean("autoplay",true));
        settings.put("countdown",p.getInt("countdown",10));
        settings.put("voice",p.getString("voice","fa-IR"));
        settings.put("subtitle_size",p.getFloat("subtitle_size",1f));
        settings.put("subtitle_delay",p.getInt("subtitle_delay",0));
        settings.put("subtitle_position",p.getFloat("subtitle_position",.14f));
        settings.put("subtitle_style",p.getInt("subtitle_style",0));
        settings.put("subtitle_off",p.getBoolean("subtitle_off",false));
        for(String k:new String[]{"audio","text"}){String value=p.getString(k,"");if(value.length()<24)settings.put(k,value);}
        return settings;
    }
    public static String exportAll(Context context)throws Exception{
        JSONArray profiles=new JSONArray();JSONArray original=new ProfileStore(context).list();
        for(int i=0;i<original.length();i++){
            JSONObject o=original.getJSONObject(i);String id=o.getString("id");
            JSONObject safe=new JSONObject().put("name",o.optString("name").substring(0,Math.min(30,o.optString("name").length())))
                .put("avatar",o.optInt("avatar",0))
                .put("data",new JSONObject(DataTransfer.exportData(of(context,DATA,id),new LibraryStore(of(context,LIB,id)))))
                .put("settings",playerSettings(of(context,PLAYER,id)));
            profiles.put(safe);
        }
        return new JSONObject().put("format","pooyan-family-backup").put("version",1).put("profiles",profiles).toString();
    }
    public static int importAll(Context context,String content)throws Exception{
        if(content==null||content.length()>600000)throw new IllegalArgumentException("فایل خیلی بزرگ است");
        JSONObject bundle=new JSONObject(content);if(!"pooyan-family-backup".equals(bundle.optString("format"))||bundle.optInt("version")!=1)throw new IllegalArgumentException("فرمت پشتیبان ناشناخته");
        JSONArray input=bundle.getJSONArray("profiles");if(input.length()<1||input.length()>8)throw new IllegalArgumentException("تعداد پروفایل نامعتبر");
        ProfileStore registry=new ProfileStore(context);JSONArray existing=registry.list();
        // Never overwrite an existing family. First import only into a fresh, unused installation.
        if(existing.length()!=1||!new LibraryStore(of(context,LIB,"default")).watched().isEmpty()||
           !new LibraryStore(of(context,LIB,"default")).resumeList().isEmpty()||
           !new LibraryStore(of(context,LIB,"default")).bookmarks().isEmpty()||
           !of(context,DATA,"default").getString("favorites","[]").equals("[]"))
            throw new IllegalStateException("برای محافظت از اطلاعات فعلی، بازیابی خانواده فقط روی نصب تازه و بدون تاریخچه انجام می‌شود.");
        // Validate the entire document before changing any data.
        for(int i=0;i<input.length();i++){
            JSONObject o=input.getJSONObject(i);String name=o.getString("name");
            if(name.isEmpty()||name.length()>30||o.optInt("avatar",-1)<0||o.optInt("avatar")>5)throw new IllegalArgumentException("پروفایل نامعتبر");
            JSONObject data=o.getJSONObject("data");
            if(!"pooyan-profile".equals(data.optString("format"))||data.optInt("version")!=1)throw new IllegalArgumentException("اطلاعات پروفایل نامعتبر");
        }
        for(int i=0;i<input.length();i++){
            JSONObject o=input.getJSONObject(i);String id;
            if(i==0){id="default";registry.edit(id,o.getString("name"),o.getInt("avatar"));}
            else {if(!registry.edit(null,o.getString("name"),o.getInt("avatar")))throw new IllegalStateException("ظرفیت پروفایل تمام شد");JSONArray updated=registry.list();id=updated.getJSONObject(updated.length()-1).getString("id");}
            DataTransfer.importData(o.getJSONObject("data").toString(),of(context,DATA,id),new LibraryStore(of(context,LIB,id)));
            JSONObject s=o.getJSONObject("settings");SharedPreferences.Editor edit=of(context,PLAYER,id).edit();
            edit.putBoolean("autoplay",s.optBoolean("autoplay",true));edit.putInt("countdown",Math.max(5,Math.min(20,s.optInt("countdown",10))));
            String voice=s.optString("voice","fa-IR");edit.putString("voice",voice.equals("en-US")?voice:"fa-IR");
            edit.putFloat("subtitle_size",Math.max(.8f,Math.min(1.5f,(float)s.optDouble("subtitle_size",1))));
            edit.putInt("subtitle_delay",Math.max(0,Math.min(5000,s.optInt("subtitle_delay",0))));
            edit.putFloat("subtitle_position",Math.max(.05f,Math.min(.3f,(float)s.optDouble("subtitle_position",.14))));
            edit.putInt("subtitle_style",Math.max(0,Math.min(2,s.optInt("subtitle_style",0))));
            edit.putBoolean("subtitle_off",s.optBoolean("subtitle_off",false));
            for(String k:new String[]{"audio","text"}){String val=s.optString(k,"");if(val.matches("[A-Za-z-]{0,23}"))edit.putString(k,val);}
            edit.apply();
        }
        registry.select("default");return input.length();
    }
}

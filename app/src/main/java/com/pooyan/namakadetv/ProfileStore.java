package com.pooyan.namakadetv;
import android.content.Context;
import android.content.SharedPreferences;
import org.json.*;
/** Local profile registry; no cross-package private-data access. */
public final class ProfileStore {
 private final Context context;private final SharedPreferences prefs;
 public ProfileStore(Context c){context=c.getApplicationContext();prefs=context.getSharedPreferences("family_profiles",0);if(list().length()==0){JSONArray a=new JSONArray();a.put(profile("default","خانواده",0));prefs.edit().putString("profiles",a.toString()).putString("active","default").apply();}}
 private JSONObject profile(String id,String name,int avatar){JSONObject o=new JSONObject();try{o.put("id",id);o.put("name",name);o.put("avatar",Math.max(0,Math.min(avatar,5)));}catch(Exception ignored){}return o;}
 public JSONArray list(){try{return new JSONArray(prefs.getString("profiles","[]"));}catch(Exception e){return new JSONArray();}}
 public String active(){String id=prefs.getString("active","default");JSONArray a=list();for(int i=0;i<a.length();i++)if(id.equals(a.optJSONObject(i).optString("id")))return id;return a.optJSONObject(0).optString("id","default");}
 public void select(String id){JSONArray a=list();for(int i=0;i<a.length();i++)if(id.equals(a.optJSONObject(i).optString("id"))){prefs.edit().putString("active",id).apply();return;}}
 public boolean edit(String id,String name,int avatar){if(name==null||name.trim().isEmpty()||name.length()>30)return false;JSONArray a=list();if(id==null){if(a.length()>=8)return false;a.put(profile(java.util.UUID.randomUUID().toString(),name.trim(),avatar));}else for(int i=0;i<a.length();i++)if(id.equals(a.optJSONObject(i).optString("id")))try{a.put(i,profile(id,name.trim(),avatar));}catch(Exception ignored){}prefs.edit().putString("profiles",a.toString()).apply();return true;}
 public boolean delete(String id){JSONArray a=list(),b=new JSONArray();if(a.length()<=1)return false;for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(!id.equals(o.optString("id")))b.put(o);}if(b.length()==a.length())return false;for(String base:new String[]{"pooyan_farsiplex_v04","namakade_local_library","player_preferences"})context.getSharedPreferences(base+"_"+id,0).edit().clear().apply();prefs.edit().putString("profiles",b.toString()).putString("active",b.optJSONObject(0).optString("id")).apply();return true;}
 public boolean picker(){return prefs.getBoolean("picker",true);}public void picker(boolean enabled){prefs.edit().putBoolean("picker",enabled).apply();}
 public static SharedPreferences data(Context c,String base){String id=new ProfileStore(c).active();SharedPreferences dest=c.getSharedPreferences(base+"_"+id,0);
  if("default".equals(id)&&!dest.getBoolean("migrated",false)){SharedPreferences old=c.getSharedPreferences(base,0);SharedPreferences.Editor e=dest.edit();for(java.util.Map.Entry<String,?> x:old.getAll().entrySet()){Object v=x.getValue();if(v instanceof String)e.putString(x.getKey(),(String)v);else if(v instanceof Boolean)e.putBoolean(x.getKey(),(Boolean)v);else if(v instanceof Integer)e.putInt(x.getKey(),(Integer)v);else if(v instanceof Long)e.putLong(x.getKey(),(Long)v);else if(v instanceof Float)e.putFloat(x.getKey(),(Float)v);}e.putBoolean("migrated",true).apply();}return dest;}
 public static String avatar(int n){return new String[]{"●","★","◆","▲","♥","☀"}[Math.max(0,Math.min(n,5))];}
}

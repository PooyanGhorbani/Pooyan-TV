package com.pooyan.namakadetv;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.util.Arrays;

/** Optional local settings PIN. Does not claim content ratings are age-certified. */
public final class ParentalPin {
    private static final int ITERATIONS=120000;
    private ParentalPin(){}
    private static SharedPreferences prefs(Context c){return c.getSharedPreferences("pooyan_parental_pin",Context.MODE_PRIVATE);}
    public static boolean active(Context c){return !prefs(c).getString("hash","").isEmpty();}
    public static boolean valid(String pin){return pin!=null&&pin.matches("[0-9]{4,8}");}
    private static byte[] derive(String pin,byte[] salt)throws Exception{
        PBEKeySpec spec=new PBEKeySpec(pin.toCharArray(),salt,ITERATIONS,256);
        try{return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).getEncoded();}
        finally{spec.clearPassword();}
    }
    public static boolean set(Context c,String pin){
        if(!valid(pin)||active(c))return false;
        byte[] salt=new byte[16];new SecureRandom().nextBytes(salt);
        try{byte[] key=derive(pin,salt);return prefs(c).edit().putString("salt",Base64.encodeToString(salt,Base64.NO_WRAP))
            .putString("hash",Base64.encodeToString(key,Base64.NO_WRAP)).commit();}catch(Exception e){return false;}
    }
    public static boolean verify(Context c,String pin){
        if(!active(c)||pin==null)return false;
        try{byte[] salt=Base64.decode(prefs(c).getString("salt",""),Base64.DEFAULT);
            byte[] expected=Base64.decode(prefs(c).getString("hash",""),Base64.DEFAULT);
            return MessageDigest.isEqual(expected,derive(pin,salt));}catch(Exception e){return false;}
    }
    public static boolean clear(Context c,String pin){return verify(c,pin)&&prefs(c).edit().clear().commit();}
}

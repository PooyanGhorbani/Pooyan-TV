package com.pooyan.namakadetv;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Locale;

/** Conservative family-only display filter; NOT a certified age-rating or parental-security solution. */
public final class UltimateKidMode {
    private final SharedPreferences prefs;
    public UltimateKidMode(Context context){prefs=ProfileStore.data(context,"ultimate_kids");}
    public boolean enabled(){return prefs.getBoolean("family_only",false);}
    public void enabled(boolean enabled){prefs.edit().putBoolean("family_only",enabled).apply();}
    public boolean visible(FarsiPlexTitle title){
        if(!enabled())return true;
        if(title==null)return false;
        String rating=title.ageRating==null?"":title.ageRating.trim().toUpperCase(Locale.ROOT);
        if(rating.equals("R")||rating.equals("NC-17")||rating.equals("TV-MA")||
           rating.equals("PG-13")||rating.equals("18+")||rating.equals("16+"))return false;
        String g=CatalogIdentity.normalized(title.genre);
        // Deliberately deny untagged and adult genres; this is a coarse display filter.
        boolean family=g.contains("family")||g.contains("children")||g.contains("kids")||g.contains("کودک")||g.contains("خانوادگی");
        boolean adult=g.contains("adult")||g.contains("horror")||g.contains("erotic")||g.contains("بزرگسال")||g.contains("ترسناک");
        boolean certifiedChild=rating.equals("G")||rating.equals("TV-Y")||rating.equals("TV-G");
        return (family||certifiedChild)&&!adult;
    }
}

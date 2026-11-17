package com.pooyan.namakadetv;
/** Labels only capabilities actually reported by Media3. */
public final class TrackPolicy {
 public static String video(int height,int bitrate){return (height>0?height+"p":"کیفیت اعلام نشده")+(bitrate>0?" · "+String.format(java.util.Locale.US,"%.1f Mbps",bitrate/1000000f):"");}
 public static String language(String language,String label){return (language==null||language.trim().isEmpty()?"زبان نامشخص":language)+(label==null||label.trim().isEmpty()?"":" · "+label);}
 public static boolean selectable(boolean supported,int index,int count){return supported&&index>=0&&index<count;}
}

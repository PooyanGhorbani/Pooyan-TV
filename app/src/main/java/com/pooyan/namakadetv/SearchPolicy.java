package com.pooyan.namakadetv;
import java.text.Normalizer;
import java.util.*;
public final class SearchPolicy {
 public static String normalize(String value){if(value==null)return "";String s=Normalizer.normalize(value,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).replace('ي','ی').replace('ك','ک');StringBuilder b=new StringBuilder();for(char c:s.toCharArray()){if(c>='۰'&&c<='۹')c=(char)('0'+c-'۰');if(c>='٠'&&c<='٩')c=(char)('0'+c-'٠');b.append(c);}return b.toString().replaceAll("[\p{P}\p{S}\p{M}\u200c\u200f\u200e]"," ").replaceAll("\s+"," ").trim();}
 public static boolean matches(FarsiPlexTitle item,String query){String q=normalize(query);if(q.isEmpty())return false;if(normalize(item.name).contains(q))return true;for(String alias:item.aliases)if(normalize(alias).contains(q))return true;return false;}
}

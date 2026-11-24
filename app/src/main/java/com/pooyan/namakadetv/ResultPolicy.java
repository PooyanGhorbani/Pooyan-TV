package com.pooyan.namakadetv;
/** Source-independent aggregation and cache deadlines. */
public final class ResultPolicy {
 public static boolean emit(int first,int second,boolean oneDone,boolean twoDone){return first>0||second>0||(oneDone&&twoDone);}
 public static boolean fresh(long now,long created,long ttl){return now>=created&&now-created<ttl;}
}

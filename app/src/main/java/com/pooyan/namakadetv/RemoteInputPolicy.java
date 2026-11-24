package com.pooyan.namakadetv;
/** One activation per complete Select press; a release on a newly focused view never clicks it. */
public final class RemoteInputPolicy {
 private boolean pressed;
 public boolean select(boolean down,boolean up,boolean cancelled,int repeats){
  if(cancelled){pressed=false;return false;}
  if(down){if(repeats==0)pressed=true;return false;}
  if(up){boolean fire=pressed;pressed=false;return fire;}
  return false;
 }
 public static boolean backHides(boolean controlsVisible,boolean playing){return controlsVisible&&playing;}
}

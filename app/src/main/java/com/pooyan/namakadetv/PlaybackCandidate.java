package com.pooyan.namakadetv;

import java.util.*;
public final class PlaybackCandidate {
 public final String url,page;public final int height;public final boolean verified;
 public PlaybackCandidate(String url,String page,int height,boolean verified){this.url=url;this.page=page;this.height=Math.max(0,height);this.verified=verified;}
}

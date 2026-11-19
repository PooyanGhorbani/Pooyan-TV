package com.pooyan.namakadetv;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
public final class PlaybackCandidate {
 public final String url,page;public final int height;public final boolean verified;
 public PlaybackCandidate(String url,String page,int height,boolean verified){this.url=url;this.page=page;this.height=Math.max(0,height);this.verified=verified;}
}

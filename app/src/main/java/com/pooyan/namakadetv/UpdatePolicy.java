package com.pooyan.namakadetv;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Pattern;

/** Immutable, platform-independent validation for GitHub-only updates. */
public final class UpdatePolicy {
    private UpdatePolicy() {}
    public static final String OWNER = "PooyanGhorbani";
    public static final String REPO = "Pooyan-TV";
    public static final String RELEASES_API = "https://api.github.com/repos/PooyanGhorbani/Pooyan-TV/releases?per_page=40";
    public static final int MAX_APK_BYTES = 180 * 1024 * 1024;
    private static final Pattern TAG = Pattern.compile("v(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)");
    private static final Pattern APK = Pattern.compile("Pooyan-TV-v[0-9]+\\.[0-9]+\\.[0-9]+-[A-Za-z0-9._-]+\\.apk");
    public static int compareVersions(String a, String b) {
        int[] aa=parts(a),bb=parts(b);
        if(aa==null||bb==null)throw new IllegalArgumentException("invalid version");
        for(int i=0;i<3;i++)if(aa[i]!=bb[i])return Integer.compare(aa[i],bb[i]);
        return 0;
    }
    private static int[] parts(String s){
        if(s==null||!TAG.matcher(s).matches())return null;
        String[] values=s.substring(1).split("\\.");
        try{int[] v=new int[3];for(int i=0;i<3;i++)v[i]=Integer.parseInt(values[i]);return v;}catch(Exception e){return null;}
    }
    public static boolean newer(String tag,String current) {
        try {return compareVersions(tag,"v"+current)>0;}catch(Exception e){return false;}
    }
    public static boolean validAsset(String raw,String tag,String name){
        try {
            if(tag==null||!TAG.matcher(tag).matches()||name==null||!APK.matcher(name).matches()||!name.startsWith("Pooyan-TV-"+tag+"-"))return false;
            URI u=new URI(raw);
            return "https".equalsIgnoreCase(u.getScheme())&&"github.com".equalsIgnoreCase(u.getHost())&&u.getRawUserInfo()==null&&u.getPort()==-1&&u.getRawQuery()==null&&u.getRawFragment()==null&&
                ("/"+OWNER+"/"+REPO+"/releases/download/"+tag+"/"+name).equals(u.getRawPath());
        }catch(Exception e){return false;}
    }
    /** Only official GitHub delivery hosts; no arbitrary redirects or downgrade. */
    public static boolean allowedDownloadHop(String raw){
        try{URI u=new URI(raw);String host=u.getHost();
            return "https".equalsIgnoreCase(u.getScheme())&&host!=null&&u.getRawUserInfo()==null&&u.getPort()==-1&&
                ("github.com".equalsIgnoreCase(host)||"release-assets.githubusercontent.com".equalsIgnoreCase(host)||"objects.githubusercontent.com".equalsIgnoreCase(host));
        }catch(Exception e){return false;}
    }
    public static String sha256(String digest) {
        if(digest==null)return "";
        String value=digest.toLowerCase(Locale.ROOT);
        if(value.startsWith("sha256:"))value=value.substring(7);
        return value.matches("[0-9a-f]{64}")?value:"";
    }
}

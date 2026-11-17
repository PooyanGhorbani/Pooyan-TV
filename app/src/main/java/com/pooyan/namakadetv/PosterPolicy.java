package com.pooyan.namakadetv;

/** Keeps metadata images limited to the chosen public catalog host. */
public final class PosterPolicy {
    private PosterPolicy(){}
    public static boolean accept(String itemUrl,String posterUrl){
        return !FarsiPlexUrlPolicy.mediaType(itemUrl).isEmpty()
            && !FarsiPlexUrlPolicy.normalize(FarsiPlexUrlPolicy.SITE,posterUrl).isEmpty()
            && posterUrl.toLowerCase(java.util.Locale.ROOT).matches(".*\\.(?:jpg|jpeg|webp|png)/?");
    }
}

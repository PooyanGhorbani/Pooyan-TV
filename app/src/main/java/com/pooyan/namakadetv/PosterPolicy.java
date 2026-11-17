package com.pooyan.namakadetv;

/** Keeps metadata images limited to the chosen public catalog host. */
public final class PosterPolicy {
    private PosterPolicy(){}
    public static boolean accept(String itemUrl,String posterUrl){
        return ImageUrlPolicy.allowedFor(itemUrl,posterUrl);
    }
}

package com.pooyan.namakadetv;

import java.net.URI;
import java.util.Locale;

/** Restrict optional resolver configuration to a public, HTTPS Cloudflare Worker/custom host. */
public final class AiResolverUrlPolicy {
    private AiResolverUrlPolicy() {}
    public static String normalize(String value) {
        if(value==null || value.trim().length()>512)return "";
        try {
            URI uri=new URI(value.trim());
            if(!"https".equalsIgnoreCase(uri.getScheme()) || uri.getUserInfo()!=null || uri.getPort()!=-1)return "";
            String host=uri.getHost();
            if(host==null||host.isEmpty())return "";
            String h=host.toLowerCase(Locale.ROOT);
            if(h.equals("localhost")||h.endsWith(".local")||h.endsWith(".internal") ||h.matches("\\d+(?:\\.\\d+){3}")||h.contains(":"))return "";
            String path=uri.getPath();
            // Only a root worker origin is accepted: no query strings, fragments or surprise paths.
            if(path!=null&&!path.isEmpty()&&!path.equals("/"))return "";
            if(uri.getRawQuery()!=null || uri.getRawFragment()!=null)return "";
            return "https://"+h;
        } catch(Exception e){return "";}
    }
}

package com.pooyan.namakadetv;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
/** Bounded independent media check; never follows redirects or forwards credentials. */
public final class MediaProbe {
    private MediaProbe(){}
    public static boolean valid(String url){
        if(!StreamDetector.isDirectMedia(url))return false;
        HttpURLConnection c=null;
        try{
            c=(HttpURLConnection)new URL(url).openConnection();
            c.setConnectTimeout(5000);c.setReadTimeout(5000);c.setInstanceFollowRedirects(false);
            c.setRequestProperty("Range","bytes=0-4095");
            int code=c.getResponseCode();if(code!=200&&code!=206)return false;
            byte[] bytes=new byte[4096];int n=0,r;
            try(InputStream in=c.getInputStream()){while(n<bytes.length&&(r=in.read(bytes,n,bytes.length-n))>0)n+=r;}
            String text=new String(bytes,0,n,StandardCharsets.UTF_8).trim();
            if(text.startsWith("#EXTM3U")||text.contains("<MPD"))return true;
            if(n>=8&&bytes[4]=='f'&&bytes[5]=='t'&&bytes[6]=='y'&&bytes[7]=='p')return true;
            return n>=4&&(bytes[0]&255)==0x1a&&(bytes[1]&255)==0x45&&(bytes[2]&255)==0xdf&&(bytes[3]&255)==0xa3;
        }catch(Exception e){return false;}finally{if(c!=null)c.disconnect();}
    }
}

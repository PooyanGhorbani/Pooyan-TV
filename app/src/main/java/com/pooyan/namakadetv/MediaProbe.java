package com.pooyan.namakadetv;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
/** Bounded independent media check; never follows redirects or forwards credentials. */
public final class MediaProbe {
    private MediaProbe(){}
    public static final class Result {public final boolean valid;public final SourceHealth.Failure failure;Result(boolean valid,SourceHealth.Failure failure){this.valid=valid;this.failure=failure;}}
    public static boolean valid(String url){return inspect(url).valid;}
    public static Result inspect(String url){
        if(!StreamDetector.isDirectMedia(url))return new Result(false,SourceHealth.Failure.UNSUPPORTED);
        HttpURLConnection c=null;
        try{
            c=(HttpURLConnection)new URL(url).openConnection();
            c.setConnectTimeout(5000);c.setReadTimeout(5000);c.setInstanceFollowRedirects(false);
            c.setRequestProperty("Range","bytes=0-4095");
            int code=c.getResponseCode();if(code!=200&&code!=206)return new Result(false,code==410?SourceHealth.Failure.EXPIRED:code==401||code==402||code==403?SourceHealth.Failure.ACCESS:SourceHealth.Failure.HTTP);
            byte[] bytes=new byte[4096];int n=0,r;
            try(InputStream in=c.getInputStream()){while(n<bytes.length&&(r=in.read(bytes,n,bytes.length-n))>0)n+=r;}
            String text=new String(bytes,0,n,StandardCharsets.UTF_8).trim();
            if(text.startsWith("#EXTM3U")||text.contains("<MPD"))return new Result(true,null);
            if(n>=8&&bytes[4]=='f'&&bytes[5]=='t'&&bytes[6]=='y'&&bytes[7]=='p')return new Result(true,null);
            boolean webm=n>=4&&(bytes[0]&255)==0x1a&&(bytes[1]&255)==0x45&&(bytes[2]&255)==0xdf&&(bytes[3]&255)==0xa3;
            return new Result(webm,webm?null:SourceHealth.Failure.UNSUPPORTED);
        }catch(java.net.SocketTimeoutException e){return new Result(false,SourceHealth.Failure.TIMEOUT);}
        catch(Exception e){return new Result(false,SourceHealth.Failure.NETWORK);}finally{if(c!=null)c.disconnect();}
    }
}

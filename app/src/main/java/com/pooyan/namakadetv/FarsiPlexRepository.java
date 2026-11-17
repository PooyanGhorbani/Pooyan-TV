package com.pooyan.namakadetv;

import android.app.Activity;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** One public metadata source only; no authentication bypasses or third-party catalog sources. */
public final class FarsiPlexRepository {
    public interface Result { void receive(List<FarsiPlexTitle> values,String error); }
    public interface DetailResult { void receive(FarsiPlexTitle value,String error); }
    private final Activity activity;
    private final ExecutorService executor=Executors.newFixedThreadPool(2);
    public FarsiPlexRepository(Activity activity){this.activity=activity;}
    private Document fetch(String url)throws IOException{
        String current=FarsiPlexUrlPolicy.normalize(FarsiPlexUrlPolicy.SITE,url);
        if(current.isEmpty())throw new IOException("آدرس خارج از FarsiPlex است.");
        for(int hop=0;hop<3;hop++){
            Connection.Response response=Jsoup.connect(current)
                .userAgent("PooyanTV/0.4 (Android TV; public catalog client)")
                .timeout(11000).maxBodySize(1_800_000).ignoreHttpErrors(true)
                .followRedirects(false).execute();
            int status=response.statusCode();
            if(status>=300&&status<400){
                String location=response.header("Location");
                String next=FarsiPlexUrlPolicy.normalize(current,location);
                if(next.isEmpty())throw new IOException("هدایت به دامنهٔ ناشناس مسدود شد.");
                current=next;continue;
            }
            if(status!=200)throw new IOException("FarsiPlex: HTTP "+status);
            String contentType=response.contentType();
            if(contentType==null||!contentType.toLowerCase().contains("text/html"))throw new IOException("صفحهٔ HTML دریافت نشد.");
            return response.parse();
        }
        throw new IOException("هدایت‌های بیش از حد");
    }
    public void list(String kind,int page,Result callback){
        executor.execute(()->{
            List<FarsiPlexTitle> items=new ArrayList<>();String error=null;
            try{
                String url=FarsiPlexUrlPolicy.publicList(kind,page);
                items=FarsiPlexParser.listing(fetch(url).outerHtml(),url,kind);
                if(items.isEmpty())error="هیچ عنوانی از سایت دریافت نشد؛ ممکن است ساختار صفحه تغییر کرده باشد.";
            }catch(Exception e){error="ارتباط با FarsiPlex برقرار نشد: "+e.getMessage();}
            List<FarsiPlexTitle> finalItems=items;String finalError=error;
            activity.runOnUiThread(()->callback.receive(finalItems,finalError));
        });
    }
    public void detail(FarsiPlexTitle item,DetailResult callback){
        executor.execute(()->{
            FarsiPlexTitle res=item;String error=null;
            try{
                Document document=fetch(item.url);
                res=FarsiPlexParser.details(document.outerHtml(),item.url,item.kind);
                if(!res.publicStream.isEmpty()&&!MediaProbe.valid(res.publicStream))res.publicStream="";
                if(res.publicStream.isEmpty()&&!"series".equals(item.kind)){
                    FarsiPlexPlayback.Result playback=FarsiPlexPlayback.resolveDetailed(document);
                    res.publicStream=playback.media;res.playbackError=playback.error;
                }
                if(res.poster.isEmpty())res.poster=item.poster;
                if(res.year.isEmpty())res.year=item.year;
            }catch(Exception e){error="دریافت جزئیات ممکن نشد: "+e.getMessage();}
            FarsiPlexTitle result=res;String msg=error;
            activity.runOnUiThread(()->callback.receive(result,msg));
        });
    }
    public void search(String query,Result callback){
        executor.execute(()->{
            List<FarsiPlexTitle> items=new ArrayList<>();String error=null;
            try{
                String url="https://farsiplex.com/?s="+java.net.URLEncoder.encode(query,"UTF-8");
                Connection.Response response=Jsoup.connect(url).followRedirects(false).timeout(11000).maxBodySize(1800000).ignoreHttpErrors(true).execute();
                if(response.statusCode()!=200)throw new IOException("HTTP "+response.statusCode());
                String html=response.body();items.addAll(FarsiPlexParser.listing(html,url,"movie"));items.addAll(FarsiPlexParser.listing(html,url,"series"));
            }catch(Exception e){error="جست‌وجوی سایت انجام نشد؛ دوباره تلاش کنید.";}
            List<FarsiPlexTitle> result=items;String message=error;
            activity.runOnUiThread(()->callback.receive(result,message));
        });
    }
    public void close(){executor.shutdownNow();}
}




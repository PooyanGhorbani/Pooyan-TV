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
/** Optional public catalog adapter. Only explicit public HTML5 media is considered, then independently probed. */
public final class NamakadeRepository {
    private final Activity activity;
    private final ExecutorService executor=Executors.newFixedThreadPool(2);
    public NamakadeRepository(Activity a){activity=a;}
    private Document fetch(String url)throws IOException{
        String current=NamakadeCatalog.safeUrl(url);
        if(current.isEmpty())throw new IOException("مسیر اطلاعات معتبر نیست");
        for(int hop=0;hop<3;hop++){
            Connection.Response r=Jsoup.connect(current).userAgent("Mozilla/5.0 (Android TV; PooyanTV/0.5)")
                .ignoreHttpErrors(true).followRedirects(false).timeout(9000).maxBodySize(1600000).execute();
            int status=r.statusCode();
            if(status>=300&&status<400){
                String location=r.header("Location");
                if(location==null)throw new IOException("هدایت ناقص");
                try{
                    String next=new java.net.URI(current).resolve(location).toString();
                    current=NamakadeCatalog.safeUrl(next);
                }catch(Exception ex){current="";}
                if(current.isEmpty())throw new IOException("هدایت نامعتبر");
                continue;
            }
            if(status!=200||r.contentType()==null||!r.contentType().toLowerCase(java.util.Locale.ROOT).contains("text/html"))
                throw new IOException("منبع فهرست در دسترس نیست");
            return r.parse();
        }
        throw new IOException("هدایت‌های بیش از حد");
    }
    public void list(String kind,int page,FarsiPlexRepository.Result callback){
        executor.execute(()->{
            List<FarsiPlexTitle> titles=new ArrayList<>();String error=null;
            // This source's static catalog pages have no verified pagination contract yet.
            if(page==1)try{String url=NamakadeCatalog.start(kind);titles=NamakadeCatalog.listing(fetch(url).outerHtml(),url,kind);}
            catch(Exception e){error="منبع کمکی موقتاً در دسترس نیست";}
            List<FarsiPlexTitle> r=titles;String msg=error;
            activity.runOnUiThread(()->callback.receive(r,msg));
        });
    }
    public void search(String text,FarsiPlexRepository.Result callback){
        if(text==null||text.trim().isEmpty()){callback.receive(new ArrayList<>(),null);return;}
        final String query=CatalogIdentity.normalized(text);
        final List<FarsiPlexTitle> found=new ArrayList<>();
        final int[] done={0};final String[] firstError={null};
        FarsiPlexRepository.Result join=(items,error)->{
            if(error!=null && firstError[0]==null)firstError[0]=error;
            for(FarsiPlexTitle item:items)if(CatalogIdentity.normalized(item.name).contains(query))found.add(item);
            if(++done[0]==2)callback.receive(found,found.isEmpty()?firstError[0]:null);
        };
        list("movie",1,join);
        list("series",1,join);
    }
    public void detail(FarsiPlexTitle item,FarsiPlexRepository.DetailResult cb){
        executor.execute(()->{
            FarsiPlexTitle title=item;String error=null;
            try{
                title=NamakadeCatalog.details(fetch(item.url).outerHtml(),item.url,item.kind);
                if(title.poster.isEmpty())title.poster=item.poster;
                if(!title.publicStream.isEmpty()&&!MediaProbe.valid(title.publicStream)){
                    title.publicStream="";title.playbackError="سرور پخش، رسانهٔ قابل‌خواندن ارائه نکرد؛ ممکن است لینک منقضی یا دسترسی محدود باشد.";
                }
            }catch(Exception e){error="دریافت جزئیات این عنوان ممکن نشد";}
            FarsiPlexTitle result=title;String msg=error;
            activity.runOnUiThread(()->cb.receive(result,msg));
        });
    }
    public void close(){executor.shutdownNow();}
}

package com.pooyan.namakadetv;

import android.app.Activity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/** Coordinates vetted source adapters while the UI remains provider-neutral. */
public final class UnifiedRepository {
    private boolean closed;
    private final FarsiPlexRepository main;
    private final NamakadeRepository alternate;
    private final Map<String,List<FarsiPlexTitle>> variants=new HashMap<>();
    public UnifiedRepository(Activity activity){
        main=new FarsiPlexRepository(activity);
        alternate=new NamakadeRepository(activity);
    }
    private boolean isSecondary(FarsiPlexTitle item){return item!=null&&!NamakadeCatalog.safeUrl(item.url).isEmpty();}
    private static final class Pending {
        List<FarsiPlexTitle> first=new ArrayList<>(),second=new ArrayList<>();
        String errorOne,errorTwo;
        boolean oneDone,twoDone,delivered;
    }
    private void finished(Pending p,FarsiPlexRepository.Result callback){
        if(closed||!p.oneDone||!p.twoDone||p.delivered)return;
        p.delivered=true;
        Map<String,List<FarsiPlexTitle>> found=new HashMap<>();
        List<FarsiPlexTitle> result=CatalogMerge.join(p.first,p.second,found);
        variants.putAll(found);
        callback.receive(result,result.isEmpty()?(p.errorOne!=null?p.errorOne:p.errorTwo):null);
    }
    public void list(String kind,int page,FarsiPlexRepository.Result callback){
        if(page!=1){main.list(kind,page,callback);return;}
        Pending pending=new Pending();
        main.list(kind,page,(items,error)->{
            pending.first=items;pending.errorOne=error;pending.oneDone=true;finished(pending,callback);
        });
        alternate.list(kind,page,(items,error)->{
            pending.second=items;pending.errorTwo=error;pending.twoDone=true;finished(pending,callback);
        });
    }
    public void search(String query,FarsiPlexRepository.Result callback){
        Pending pending=new Pending();
        main.search(query,(items,error)->{
            pending.first=items;pending.errorOne=error;pending.oneDone=true;finished(pending,callback);
        });
        alternate.search(query,(items,error)->{
            pending.second=items;pending.errorTwo=error;pending.twoDone=true;finished(pending,callback);
        });
    }
    public void detail(FarsiPlexTitle item,FarsiPlexRepository.DetailResult callback){
        FarsiPlexRepository.DetailResult original=(resolved,error)->{
            if(closed)return;
            if(resolved!=null&&!"series".equals(resolved.kind)&&resolved.publicStream.isEmpty()){
                List<FarsiPlexTitle> options=variants.get(item.url);
                if(options!=null&&!options.isEmpty()){
                    tryAlternative(resolved,error,options,0,callback);return;
                }
            }
            callback.receive(resolved,error);
        };
        if(isSecondary(item))alternate.detail(item,original);else main.detail(item,original);
    }
    private void tryAlternative(FarsiPlexTitle primary,String primaryError,List<FarsiPlexTitle> options,int pos,
                                  FarsiPlexRepository.DetailResult result){
        if(pos>=Math.min(options.size(),2)){result.receive(primary,primaryError);return;}
        FarsiPlexTitle candidate=options.get(pos);
        FarsiPlexRepository.DetailResult cb=(found,error)->{
            if(closed)return;
            if(found!=null&&!found.publicStream.isEmpty()){
                primary.publicStream=found.publicStream;
                primary.playbackError="";
                result.receive(primary,null);
            }else tryAlternative(primary,primaryError,options,pos+1,result);
        };
        if(isSecondary(candidate))alternate.detail(candidate,cb);else main.detail(candidate,cb);
    }
    public void close(){closed=true;main.close();alternate.close();}
}

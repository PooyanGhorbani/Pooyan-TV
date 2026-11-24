package com.pooyan.namakadetv;
import org.junit.Test;
import org.junit.Assume;
import org.jsoup.Jsoup;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

/** Opt-in public HTTP checks. No login, JS execution, credentials, or device playback. */
public class LiveSecondarySourceTest {
 private Document fetch(String url)throws Exception{
  for(int i=0;i<3;i++){
   if(NamakadeCatalog.safeUrl(url).isEmpty())throw new IllegalArgumentException("Blocked metadata path");
   Connection.Response r=Jsoup.connect(url).timeout(12000).maxBodySize(1600000).followRedirects(false).ignoreHttpErrors(true).execute();
   if(r.statusCode()>=300&&r.statusCode()<400){url=new URL(new URL(url),r.header("Location")).toString();continue;}
   if(r.statusCode()!=200)throw new java.io.IOException("HTTP "+r.statusCode());
   return r.parse();
  }throw new java.io.IOException("Redirect limit");
 }
 private String image(String url){
  if(!ImageUrlPolicy.allowed(url))return "missing";
  HttpURLConnection c=null;try{c=(HttpURLConnection)new URL(url).openConnection();c.setRequestMethod("HEAD");c.setInstanceFollowRedirects(false);c.setConnectTimeout(7000);c.setReadTimeout(7000);return c.getResponseCode()+":"+c.getContentType();}catch(Exception e){return e.getClass().getSimpleName();}finally{if(c!=null)c.disconnect();}
 }
 private void check(FarsiPlexTitle t,List<String> out)throws Exception{
  String result=t.publicStream.isEmpty()?"no-published-media":MediaProbe.valid(t.publicStream)?"bounded-media-body-verified":"media-probe-failed";
  out.add(t.kind+"\t"+t.url.split("\\?")[0]+"\t"+image(t.poster)+"\t"+t.episodes.size()+"\t"+result);
 }
 @Test public void catalogPostersAndExplicitHtml5Media()throws Exception{
  Assume.assumeTrue("1".equals(System.getenv("POOYAN_LIVE_CHECK")));
  List<String> out=new ArrayList<>();out.add("UTC\t"+java.time.Instant.now());out.add("kind\tpage\tposterHEAD:type\tepisodes\tmedia");
  try{Connection.Response search=Jsoup.connect(NamakadeCatalog.BASE+"/search?page=livesearch&searchField=meymoone").method(Connection.Method.POST).followRedirects(false).timeout(12000).maxBodySize(600000).execute();out.add("public-search\tPOST:/search\t"+search.statusCode()+"\t"+NamakadeCatalog.searchListing(search.body()).size()+"\tnot-a-stream");}catch(Exception e){out.add("public-search\tPOST:/search\t-\t-\t"+e.getClass().getSimpleName());}
  for(String kind:new String[]{"movie","series"}){
   try{
    String url=NamakadeCatalog.start(kind);List<FarsiPlexTitle> list=NamakadeCatalog.listing(fetch(url).outerHtml(),url,kind);
    assertFalse("Public catalog should have entries",list.isEmpty());
    FarsiPlexTitle entry=list.get(0);
    out.add("selected-"+kind+"\t"+entry.url.split("\\?")[0]+"\t-\t-\tmetadata-only");
    out.add("listing-"+kind+"\t"+url+"\t"+image(entry.poster)+"\t"+list.size()+"\tnot-a-stream");
    FarsiPlexTitle detail=NamakadeCatalog.details(fetch(entry.url).outerHtml(),entry.url,kind);check(detail,out);
    if(!detail.episodes.isEmpty()){
     FarsiPlexTitle episode=detail.episodes.get(0);
     FarsiPlexTitle ep=NamakadeCatalog.details(fetch(episode.url).outerHtml(),episode.url,"episode");
     if(ep.poster.isEmpty())ep.poster=episode.poster;check(ep,out);
    }
   }catch(Exception e){out.add(kind+"\tpublic-check\t-\t-\t"+e.getClass().getSimpleName());}
  }
  Path report=Path.of("build/reports/live-secondary.tsv");Files.createDirectories(report.getParent());Files.write(report,out,StandardCharsets.UTF_8);
 }
}

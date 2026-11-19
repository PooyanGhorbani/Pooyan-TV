package com.pooyan.namakadetv;

import org.junit.Test;
import org.junit.Assume;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

/** Opt-in HTTP integration only. Never asserts device decoding or rights to redistribute content. */
public class LivePlaybackTest {
 private Document fetch(String url)throws Exception{
  return Jsoup.connect(url).timeout(15000).maxBodySize(1800000).followRedirects(false).get();
 }
 @Test public void publicCatalogAndNativeDiscovery()throws Exception{
  Assume.assumeTrue("1".equals(System.getenv("POOYAN_LIVE_CHECK")));
  List<String> report=new ArrayList<>();
  report.add("UTC\t"+java.time.Instant.now());
  report.add("kind\tpage\tepisodes\tposterHTTP\tresult\tmediaHEAD\ttype\tlength\tranges");
  List<FarsiPlexTitle> candidates=new ArrayList<>();
  for(String kind:new String[]{"movie","series"}){
   String url=FarsiPlexUrlPolicy.publicList(kind,1);
   List<FarsiPlexTitle> list=FarsiPlexParser.listing(fetch(url).outerHtml(),url,kind);
   assertFalse("Catalog must have entries",list.isEmpty());
   for(FarsiPlexTitle entry:list){
    if(entry.url.contains("/feed/"))continue;
    candidates.add(entry);if(candidates.stream().filter(t->kind.equals(t.kind)).count()==2)break;
   }
  }
  for(FarsiPlexTitle entry:candidates){
   try{
    Document d=fetch(entry.url);
    FarsiPlexTitle detail=FarsiPlexParser.details(d.outerHtml(),entry.url,entry.kind);
    if("series".equals(entry.kind)){
     report.add("series\t"+entry.url+"\t"+detail.episodes.size()+"\t"+poster(detail.poster)+"\tnot-a-stream\t-\t-\t-\t-");
     int n=0;for(FarsiPlexTitle ep:detail.episodes){check(ep,fetch(ep.url),report);if(++n==2)break;}
    }else check(detail,d,report);
   }catch(Exception e){report.add(entry.kind+"\t"+entry.url+"\t-\t-\tHTTP-check-failed:"+e.getClass().getSimpleName()+"\t-\t-\t-\t-");}
  }
  Path output=Path.of("build/reports/live-playback.tsv");Files.createDirectories(output.getParent());
  Files.write(output,report,StandardCharsets.UTF_8);
  report.forEach(System.out::println);
 }
 private int poster(String url){
  if(url.isEmpty())return 0;
  HttpURLConnection c=null;try{c=(HttpURLConnection)new URL(url).openConnection();c.setRequestMethod("HEAD");c.setInstanceFollowRedirects(false);c.setConnectTimeout(7000);c.setReadTimeout(7000);return c.getResponseCode();}catch(Exception e){return -1;}finally{if(c!=null)c.disconnect();}
 }
 private void check(FarsiPlexTitle entry,Document d,List<String> report)throws Exception{
  FarsiPlexTitle detail=FarsiPlexParser.details(d.outerHtml(),entry.url,entry.kind);
  String media=detail.publicStream;
  if(!media.isEmpty()&&!MediaProbe.valid(media))media="";
  if(media.isEmpty())media=FarsiPlexPlayback.resolveDetailed(d).media;
  String fields="-\t-\t-\t-";
  if(!media.isEmpty()){
   HttpURLConnection c=(HttpURLConnection)new URL(media).openConnection();
   try{c.setRequestMethod("HEAD");c.setInstanceFollowRedirects(false);c.setConnectTimeout(7000);c.setReadTimeout(7000);fields=c.getResponseCode()+"\t"+c.getContentType()+"\t"+c.getContentLengthLong()+"\t"+c.getHeaderField("Accept-Ranges");}finally{c.disconnect();}
  }
  report.add(entry.kind+"\t"+entry.url+"\t-\t"+poster(detail.poster)+"\t"+(media.isEmpty()?"unavailable":"range-body-verified")+"\t"+fields);
 }
}

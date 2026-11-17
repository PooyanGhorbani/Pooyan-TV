package com.pooyan.namakadetv;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class FarsiPlexPlaybackTest {
 private Document page(){return Jsoup.parse("<form id='watch-12' action='/play/' method='post'><input name='id' value='12'></form>","https://farsiplex.com/episode/example/");}
 private FarsiPlexPlayback.Reply reply(int code,String url,String location,String html){return new FarsiPlexPlayback.Reply(code,url,location,html,Collections.emptyMap());}
 @Test public void discoversPublishedSignedFileWithoutAiAndPreservesQuery(){
  AtomicInteger count=new AtomicInteger();
  String media="https://pl.farsiplex.buzz/series/example/01.mp4?md5=fixture-only";
  FarsiPlexPlayback.Result result=FarsiPlexPlayback.resolveDetailed(page(),(url,post,id,cookies)->{
   int n=count.incrementAndGet();
   if(n==1){assertTrue(post);assertEquals("12",id);return reply(302,url,"/play-online-video/","");}
   if(n==2)return reply(200,url,null,"<iframe src='/jwplayer/?id=12'></iframe>");
   assertEquals("https://farsiplex.com/jwplayer/?id=12",url);
   return reply(200,url,null,"<script>var jw={\"title\":\"semi; brace } and \\\"quote\",\"file\":\""+media+"\"};</script>");
  },candidate->candidate.equals(media));
  assertEquals(media,result.media);assertEquals("",result.error);assertEquals(3,count.get());
 }
 @Test public void forbiddenStopsWithoutProbing(){
  FarsiPlexPlayback.Result r=FarsiPlexPlayback.resolveDetailed(page(),(u,p,i,c)->reply(403,u,null,""),u->{fail("must not probe");return true;});
  assertEquals("",r.media);assertTrue(r.error.contains("403"));
 }
 @Test public void offsiteRedirectIsNeverRequested(){
  AtomicInteger count=new AtomicInteger();
  FarsiPlexPlayback.Result r=FarsiPlexPlayback.resolveDetailed(page(),(u,p,i,c)->{count.incrementAndGet();return reply(302,u,"https://evil.test/play/","");},u->true);
  assertEquals("",r.media);assertEquals(1,count.get());assertFalse(r.error.isEmpty());
 }
 @Test public void missingFormNeverGuessesId(){
  assertFalse(FarsiPlexPlayback.resolveDetailed(Jsoup.parse("<h1>Poster only</h1>"),(u,p,i,c)->{fail("must not fetch");return null;},u->true).error.isEmpty());
 }
 @Test public void failedProbeDoesNotOfferPlay(){
  FarsiPlexPlayback.Result r=FarsiPlexPlayback.resolveDetailed(page(),(u,p,i,c)->reply(200,u,null,p?"<iframe src='/jwplayer/'></iframe>":"var jw={\"file\":\"https://pl.farsiplex.buzz/test.mp4\"};"),u->false);
  assertEquals("",r.media);assertFalse(r.error.isEmpty());
 }
 @Test public void timeoutHasPersianErrorWithoutSensitiveUrl(){
  FarsiPlexPlayback.Result r=FarsiPlexPlayback.resolveDetailed(page(),(u,p,i,c)->{throw new java.net.SocketTimeoutException("private-url");},u->true);
  assertEquals("",r.media);assertTrue(r.error.contains("مهلت"));assertFalse(r.error.contains("private-url"));
 }
 @Test public void episodeSidebarIgnoredAndSiblingImageAndNumberPreserved(){
  String html="<h1>Show</h1><div id='seasons'><ul><li><img src='/wp-content/uploads/ep.jpg'><div class='numerando'>2 - 3</div><div><a href='/episode/show-03/'>Episode</a></div></li></ul></div><aside><a href='/episode/unrelated/'>Other</a></aside>";
  FarsiPlexTitle t=FarsiPlexParser.details(html,"https://farsiplex.com/tvshow/show/","series");
  assertEquals(1,t.episodes.size());assertEquals("2 - 3 · Episode",t.episodes.get(0).name);assertTrue(t.episodes.get(0).poster.endsWith("ep.jpg"));
 }
 @Test public void episodeUsesOfficialSameSiteCoverWhenOgHostIsNotAllowed(){
  String html="<meta property='og:image' content='https://img.farsiplex.com/poster.jpg'><h1>Episode 1</h1><img class='cover' src='/wp-content/uploads/episode-cover.jpg'>";
  assertEquals("https://farsiplex.com/wp-content/uploads/episode-cover.jpg",FarsiPlexParser.details(html,"https://farsiplex.com/episode/show-01/","episode").poster);
 }
}

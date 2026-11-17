package com.pooyan.namakadetv;
import org.junit.Test;
import static org.junit.Assert.*;
import org.jsoup.Jsoup;
import java.util.List;

public class V051RegressionTest {
 @Test public void overlayLinkGetsSiblingPosterAndCleanTitle(){
  String html="<div class='divPicMask2'><a href='#'><img src='https://media.negahestan.com/m.jpg'></a><div class='mask3'><a href='/iran-1-movies/drama/example'><div class='SSh3'>Example</div><div>Genre: Drama</div><p>Watch Now</p></a></div></div>";
  List<FarsiPlexTitle> list=NamakadeCatalog.listing(html,NamakadeCatalog.start("movie"),"movie");
  assertEquals(1,list.size());assertEquals("Example",list.get(0).name);assertEquals("https://media.negahestan.com/m.jpg",list.get(0).poster);
 }
 @Test public void nameOgMetadataAndExplicitHtml5Only(){
  String html="<meta name='og:title' content='Example | Site'><meta name='og:image' content='https://media.negahestan.com/p.jpg'><video><source src='https://media.negahestan.com/v.mp4?published=fixture'></video><script>var v='https://media.negahestan.com/guess.mp4';</script>";
  FarsiPlexTitle t=NamakadeCatalog.details(html,"https://namakadeh.com/movies/example","movie");
  assertEquals("Example",t.name);assertTrue(t.poster.endsWith("p.jpg"));assertTrue(t.publicStream.endsWith("?published=fixture"));
  assertEquals("",NamakadeCatalog.publishedMedia(Jsoup.parse("<iframe src='https://media.negahestan.com/v.mp4'></iframe>"),t.url));
 }
 @Test public void noUntrustedMediaHostOrAuthenticationPaths(){
  assertEquals("",NamakadeCatalog.safeUrl("https://namakadeh.com/auth/login"));
  assertEquals("",NamakadeCatalog.safeUrl("https://namakadeh.com.evil.test/movies/a"));
  assertEquals("",NamakadeCatalog.publishedMedia(Jsoup.parse("<video src='https://evil.test/v.mp4'></video>"),"https://namakadeh.com/movies/a"));
 }
 @Test public void seriesEpisodesStayWithinSelectedSeries(){
  String html="<ul><li><a href='/series/show/episodes/first'><img src='https://media.negahestan.com/e.jpg'></a><div id='divEpiNo1'>1</div><a href='/series/show/episodes/first'>Play</a></li></ul><a href='/series/other/episodes/first'>Other</a>";
  FarsiPlexTitle t=NamakadeCatalog.details(html,"https://namakadeh.com/serieses/show","series");
  assertEquals(1,t.episodes.size());assertEquals("قسمت 1",t.episodes.get(0).name);assertTrue(t.episodes.get(0).poster.endsWith("e.jpg"));
 }
 @Test public void remoteTraversalEdgesAndPartialRows(){
  assertEquals(1,TvPosterFocusPolicy.neighbor(0,4,true));assertEquals(0,TvPosterFocusPolicy.neighbor(1,4,false));
  assertEquals(-1,TvPosterFocusPolicy.neighbor(3,4,true));assertEquals(-1,TvPosterFocusPolicy.neighbor(0,4,false));
  assertEquals(1,TvPosterFocusPolicy.column(4,2));assertEquals(-1,TvPosterFocusPolicy.column(0,0));
 }
 @Test public void vodSeekIsBoundedAndLiveOrUnknownDurationDisabled(){
  assertEquals(0,PlaybackResumePolicy.seek(5000,-10000,60000));
  assertEquals(60000,PlaybackResumePolicy.seek(59000,10000,60000));
  assertFalse(PlaybackResumePolicy.canSeek(-9223372036854775807L,false,true));
  assertFalse(PlaybackResumePolicy.canSeek(60000,true,true));assertFalse(PlaybackResumePolicy.canSeek(60000,false,false));
  assertTrue(PlaybackResumePolicy.canSeek(60000,false,true));
  assertEquals("01:02:03",PlaybackResumePolicy.time(3723000));assertEquals("--:--",PlaybackResumePolicy.time(-1));
 }
 @Test public void progressIdentitySurvivesFreshMediaSignaturesButSeparatesEpisodes(){
  String page="https://farsiplex.com/episode/show-01/";
  assertEquals(PlaybackResumePolicy.identity("https://pl.farsiplex.buzz/old.mp4?a=1",page),PlaybackResumePolicy.identity("https://pl.farsiplex.buzz/new.mp4?a=2",page));
  assertNotEquals(PlaybackResumePolicy.identity("same",page),PlaybackResumePolicy.identity("same","https://farsiplex.com/episode/show-02/"));
  assertFalse(PlaybackResumePolicy.completed(1000,10000));assertTrue(PlaybackResumePolicy.completed(9800,10000));
  assertFalse(PlaybackResumePolicy.completed(50000,0));assertTrue(PlaybackResumePolicy.completed(599000,600000));
 }
}

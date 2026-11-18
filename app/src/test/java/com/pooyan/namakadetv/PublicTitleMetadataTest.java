package com.pooyan.namakadetv;

import org.junit.Test;
import org.jsoup.Jsoup;
import static org.junit.Assert.*;

public final class PublicTitleMetadataTest {
 @Test public void readsOnlyActuallyPublishedJsonLd(){
  String html="<html><script type='application/ld+json'>"+
   "{\"@type\":\"Movie\",\"aggregateRating\":{\"ratingValue\":7.2,\"bestRating\":10},\"actor\":[{\"name\":\"Actor One\"}],\"duration\":\"PT91M\"}"+
   "</script></html>";
  PublicTitleMetadata.Details v=PublicTitleMetadata.from(Jsoup.parse(html));
  assertEquals("7.2 / 10",v.rating);
  assertEquals("Actor One",v.cast);
  assertEquals("PT91M",v.runtime);
 }
 @Test public void missingDetailsAreNotInvented(){
  PublicTitleMetadata.Details v=PublicTitleMetadata.from(Jsoup.parse("<html><h1>Movie</h1></html>"));
  assertTrue(v.rating.isEmpty());assertTrue(v.cast.isEmpty());assertTrue(v.runtime.isEmpty());
 }
}

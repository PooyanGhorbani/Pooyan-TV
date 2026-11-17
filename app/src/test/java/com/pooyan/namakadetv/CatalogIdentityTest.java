package com.pooyan.namakadetv;
import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;
public class CatalogIdentityTest {
 @Test public void normalizePersianAndPunctuation(){
  assertEquals("میمون مست",CatalogIdentity.normalized("ميمون، مست"));
  assertEquals("har chi to begi",CatalogIdentity.normalized("Har-Chi To Begi"));
 }
 @Test public void deduplicatesRetainingPrimary(){
  FarsiPlexTitle first=new FarsiPlexTitle("https://farsiplex.com/series/meymoone-mast","series","میمون مست");
  FarsiPlexTitle second=new FarsiPlexTitle("https://namakadeh.com/serieses/meymoone-mast","series","میمون، مست");
  Map<String,List<FarsiPlexTitle>> variants=new HashMap<>();
  List<FarsiPlexTitle> out=CatalogMerge.join(Arrays.asList(first),Arrays.asList(second),variants);
  assertEquals(1,out.size());assertEquals(first.url,out.get(0).url);
  assertEquals(second.url,variants.get(first.url).get(0).url);
 }
 @Test public void safeDomainCannotBeSpoofed(){
  assertEquals("",NamakadeCatalog.safeUrl("https://namakadeh.com.evil.test/movies/drama/x"));
  assertEquals("",NamakadeCatalog.safeUrl("http://namakadeh.com/movies"));
  assertEquals("",NamakadeCatalog.safeUrl("https://namakadeh.com:8080/movies"));
 }
 @Test public void categoryPagesAreAllowed(){
  assertEquals("https://namakadeh.com/movies",NamakadeCatalog.safeUrl("https://namakadeh.com/movies"));
 }
}

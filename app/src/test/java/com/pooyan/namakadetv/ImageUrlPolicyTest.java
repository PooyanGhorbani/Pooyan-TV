package com.pooyan.namakadetv;
import org.junit.Test;
import static org.junit.Assert.*;
public class ImageUrlPolicyTest {
 @Test public void blocksUntrustedImageHosts(){
  assertFalse(ImageUrlPolicy.allowed("https://farsiplex.com.evil.test/poster.jpg"));
  assertFalse(ImageUrlPolicy.allowed("http://namakadeh.com/a.jpg"));
  assertFalse(ImageUrlPolicy.allowed("https://127.0.0.1/a.png"));
 }
 @Test public void permitsLimitedTrustedImageCdn(){
  assertTrue(ImageUrlPolicy.allowedFor("https://namakadeh.com/serieses/a","https://media.negahestan.com/a.jpg"));
  assertFalse(ImageUrlPolicy.allowedFor("https://farsiplex.com/movie/a","https://media.negahestan.com/a.jpg"));
 }
}

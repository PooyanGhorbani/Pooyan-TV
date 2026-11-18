package com.pooyan.namakadetv;

import org.junit.Test;
import static org.junit.Assert.*;

public final class UpdatePolicyTest {
 @Test public void comparesSemanticVersionsRatherThanLexical(){
  assertTrue(UpdatePolicy.newer("v0.7.10","0.7.9"));
  assertTrue(UpdatePolicy.newer("v0.8.0","0.7.9"));
  assertFalse(UpdatePolicy.newer("v0.7.0","0.7.0"));
  assertFalse(UpdatePolicy.newer("v1.0.0-beta","0.7.0"));
 }
 @Test public void assetIsOnlyOfficialRepoRelease(){
  String name="Pooyan-TV-v0.8.0-premium-stable.apk";
  assertTrue(UpdatePolicy.validAsset("https://github.com/PooyanGhorbani/Pooyan-TV/releases/download/v0.8.0/"+name,"v0.8.0",name));
  assertFalse(UpdatePolicy.validAsset("https://github.com.evil.test/PooyanGhorbani/Pooyan-TV/releases/download/v0.8.0/"+name,"v0.8.0",name));
  assertFalse(UpdatePolicy.validAsset("https://github.com/PooyanGhorbani/Other/releases/download/v0.8.0/"+name,"v0.8.0",name));
  assertFalse(UpdatePolicy.validAsset("http://github.com/PooyanGhorbani/Pooyan-TV/releases/download/v0.8.0/"+name,"v0.8.0",name));
  assertFalse(UpdatePolicy.validAsset("https://github.com/PooyanGhorbani/Pooyan-TV/releases/download/v0.8.0/"+name+"?fake=1","v0.8.0",name));
 }
 @Test public void rejectsBadDownloadRedirects(){
  assertTrue(UpdatePolicy.allowedDownloadHop("https://release-assets.githubusercontent.com/safe?sig=1"));
  assertFalse(UpdatePolicy.allowedDownloadHop("https://release-assets.githubusercontent.com.evil.org/a"));
  assertFalse(UpdatePolicy.allowedDownloadHop("https://localhost/resource"));
  assertFalse(UpdatePolicy.allowedDownloadHop("http://github.com/test"));
 }
 @Test public void digestMustBeSha256(){
  String value="abcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcd";
  assertEquals(value,UpdatePolicy.sha256("sha256:"+value));
  assertEquals("",UpdatePolicy.sha256("sha1:"+value));
 }
}

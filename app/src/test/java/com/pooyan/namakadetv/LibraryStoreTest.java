package com.pooyan.namakadetv;
import android.content.SharedPreferences;
import org.junit.Test;
import static org.junit.Assert.*;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/** Exercises real LibraryStore JSON persistence across new instances, without Android disk I/O. */
public class LibraryStoreTest {
 static SharedPreferences memory(){
  Map<String,Object> values=new HashMap<>();
  SharedPreferences.Editor editor=(SharedPreferences.Editor)Proxy.newProxyInstance(LibraryStoreTest.class.getClassLoader(),new Class[]{SharedPreferences.Editor.class},(p,m,a)->{
   if(m.getName().startsWith("put"))values.put((String)a[0],a[1]);
   if(m.getName().equals("remove"))values.remove((String)a[0]);
   if(m.getName().equals("commit"))return true;
   if(m.getName().equals("apply"))return null;
   return p;
  });
  return (SharedPreferences)Proxy.newProxyInstance(LibraryStoreTest.class.getClassLoader(),new Class[]{SharedPreferences.class},(p,m,a)->{
   if(m.getName().startsWith("get")&&a!=null&&a.length==2)return values.getOrDefault((String)a[0],a[1]);
   if(m.getName().equals("getAll"))return values;
   if(m.getName().equals("edit"))return editor;
   throw new UnsupportedOperationException(m.getName());
  });
 }
 @Test public void persistsIndependentlyAndRotatedStreamDoesNotDuplicate(){
  SharedPreferences disk=memory();LibraryStore store=new LibraryStore(disk);
  String first="https://farsiplex.com/episode/a/",second="https://farsiplex.com/episode/b/";
  store.savePlayback("A","https://cdn.example/a.mp4?old",first,60000,600000);
  store.savePlayback("B","https://cdn.example/b.mp4",second,120000,600000);
  LibraryStore reopened=new LibraryStore(disk);
  assertEquals(60000,reopened.getResumePosition("https://cdn.example/a.mp4?fresh",first));
  assertEquals(120000,reopened.getResumePosition("https://cdn.example/b.mp4",second));
  reopened.savePlayback("A","https://cdn.example/a.mp4?fresh",first,90000,600000);
  assertEquals(2,reopened.resumeList().size());assertEquals(90000,new LibraryStore(disk).getResumePosition("",first));
 }
 @Test public void completedAndStartOverRemoveOnlyTheirOwnEntry(){
  LibraryStore store=new LibraryStore(memory());String page="https://farsiplex.com/movie/a/";
  store.savePlayback("A","https://cdn.example/a.mp4",page,60000,600000);
  store.savePlayback("B","https://cdn.example/b.mp4","https://farsiplex.com/movie/b/",70000,600000);
  store.savePlayback("A","https://cdn.example/a.mp4",page,599000,600000);
  assertEquals(1,store.resumeList().size());assertEquals("B",store.resumeList().get(0).title);
  store.removePlayback("https://cdn.example/b.mp4","https://farsiplex.com/movie/b/");assertTrue(store.resumeList().isEmpty());
 }
 @Test public void unknownDurationIsNotMistakenForCompleted(){
  LibraryStore store=new LibraryStore(memory());store.savePlayback("A","https://cdn.example/a.mp4","https://farsiplex.com/movie/a/",45000,0);
  assertEquals(45000,store.getResumePosition("","https://farsiplex.com/movie/a/"));
 }
}

package com.pooyan.namakadetv;
import org.junit.Test;
import static org.junit.Assert.*;
public class RemoteInputPolicyTest {
 @Test public void selectActivatesOnceDespiteRepeat(){
  RemoteInputPolicy s=new RemoteInputPolicy();assertFalse(s.select(true,false,false,0));
  assertFalse(s.select(true,false,false,1));assertTrue(s.select(false,true,false,0));
  assertFalse(s.select(false,true,false,0));
 }
 @Test public void cancelledOrStrayReleaseDoesNotActivateNewFocus(){
  RemoteInputPolicy first=new RemoteInputPolicy(),next=new RemoteInputPolicy();
  first.select(true,false,false,0);assertFalse(next.select(false,true,false,0));
  assertFalse(first.select(false,true,true,0));assertFalse(first.select(false,true,false,0));
 }
 @Test public void backHidesPlayingOverlayThenExitsOrExitsPaused(){
  assertTrue(RemoteInputPolicy.backHides(true,true));assertFalse(RemoteInputPolicy.backHides(false,true));
  assertFalse(RemoteInputPolicy.backHides(true,false));
 }
}

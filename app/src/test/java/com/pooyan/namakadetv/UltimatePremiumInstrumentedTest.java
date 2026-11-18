package com.pooyan.namakadetv;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Basic Android TV UI acceptance; does not require internet connectivity. */
@RunWith(AndroidJUnit4.class)
public class UltimatePremiumInstrumentedTest {
    private static TextView find(View v,String text){
        if(v instanceof TextView&&((TextView)v).getText().toString().contains(text))return (TextView)v;
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){
            TextView t=find(g.getChildAt(i),text);if(t!=null)return t;
        }}return null;
    }
    @Test public void homeHasRemoteFocusableSurpriseAndStableNavigation(){
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            scenario.onActivity(a->{
                TextView surprise=find(a.getWindow().getDecorView(),"سورپرایزم کن");
                assertNotNull("Premium discovery control must exist",surprise);
                assertTrue("Discovery button must be remote focusable",surprise.isFocusable());
                assertNotNull("Home navigation must exist",find(a.getWindow().getDecorView(),"خانه"));
            });
        }
    }
}

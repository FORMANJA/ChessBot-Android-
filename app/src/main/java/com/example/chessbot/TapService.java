package com.example.chessbot;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.view.accessibility.AccessibilityEvent;

public class TapService extends AccessibilityService {
    static TapService instance;
    @Override public void onServiceConnected(){ instance=this; }
    @Override public void onAccessibilityEvent(AccessibilityEvent event){}
    @Override public void onInterrupt(){}
    @Override public void onDestroy(){ instance=null; super.onDestroy(); }
    public void tap(float x,float y){
        Path path=new Path(); path.moveTo(x,y);
        GestureDescription.StrokeDescription stroke=new GestureDescription.StrokeDescription(path,0,80);
        dispatchGesture(new GestureDescription.Builder().addStroke(stroke).build(),null,null);
    }
}

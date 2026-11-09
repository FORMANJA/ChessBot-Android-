package com.example.chessbot;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.HardwareBuffer;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Locale;

/**
 * Continuous screen observer. Uses Android 11+ accessibility screenshots, so no 12-second
 * MediaProjection workflow is required. No gesture is dispatched until board detection,
 * opponent identity, and full chess rules are validated.
 */
public final class AutoPlayService extends AccessibilityService {
    private final Handler handler=new Handler(Looper.getMainLooper());
    private boolean running=false, busy=false;
    private String state="Bekliyor";
    private final Runnable tick=new Runnable(){
        @Override public void run(){
            if(!running)return;
            if(!busy)readScreen();
            handler.postDelayed(this,1800);
        }
    };
    @Override protected void onServiceConnected(){
        super.onServiceConnected();
        AccessibilityServiceInfo info=getServiceInfo();
        if(info!=null){info.flags|=AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;setServiceInfo(info);}
        running=true;
        handler.post(tick);
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event){
        if(event==null)return;
        if(event.getPackageName()!=null && !event.getPackageName().toString().contains("chess"))
            state="Chess.com bilgisayar botu ekranı bekleniyor";
    }
    @Override public void onInterrupt(){stopLoop();}
    @Override public void onDestroy(){stopLoop();super.onDestroy();}
    private void stopLoop(){running=false;handler.removeCallbacksAndMessages(null);}
    private void readScreen(){
        AccessibilityNodeInfo root=getRootInActiveWindow();
        if(root==null){publish("Ekran erişimi bekleniyor");return;}
        String pkg=String.valueOf(root.getPackageName());
        root.recycle();
        if(!pkg.contains("chess") && !pkg.contains("chrome")){
            publish("Chess.com ekranı bekleniyor");
            return;
        }
        busy=true;
        try {
            takeScreenshot(0,getMainExecutor(),new TakeScreenshotCallback(){
                @Override public void onSuccess(ScreenshotResult screenshot){
                    try {
                        HardwareBuffer buffer=screenshot.getHardwareBuffer();
                        Bitmap raw=Bitmap.wrapHardwareBuffer(buffer,screenshot.getColorSpace());
                        if(raw==null){publish("Görüntü çözümlenemedi");return;}
                        Bitmap bitmap=raw.copy(Bitmap.Config.ARGB_8888,false);
                        raw.recycle();
                        buffer.close();
                        File folder=getExternalFilesDir(null);
                        if(folder==null){bitmap.recycle();publish("Dosya alanı yok");return;}
                        File current=new File(folder,"board_capture.png");
                        File previous=new File(folder,"board_previous.png");
                        if(current.exists())java.nio.file.Files.copy(current.toPath(),previous.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        try(FileOutputStream out=new FileOutputStream(current)){bitmap.compress(Bitmap.CompressFormat.PNG,90,out);}
                        bitmap.recycle();
                        if(previous.exists()&&getSharedPreferences("board",MODE_PRIVATE).contains("left")){
                            android.content.SharedPreferences pref=getSharedPreferences("board",MODE_PRIVATE);
                            BoardDiff.Result diff=BoardDiff.analyze(previous,current,
                                pref.getFloat("left",0),pref.getFloat("top",0),
                                pref.getFloat("right",0),pref.getFloat("bottom",0));
                            publish("Ekran otomatik izleniyor. Hamle uygulama güvenlik doğrulaması bekliyor.\n"+diff.summary);
                        } else publish("Ekran otomatik izleniyor; tahta kalibrasyonu gerekiyor");
                    }catch(Exception e){publish("Görüntü işleme hatası: "+e.getClass().getSimpleName());}
                    finally {busy=false;}
                }
                @Override public void onFailure(int errorCode){busy=false;publish("Ekran yakalama hatası: "+errorCode);}
            });
        }catch(Exception e){busy=false;publish("Erişim hatası: "+e.getClass().getSimpleName());}
    }
    private void publish(String message){
        state=message;
        getSharedPreferences("autoplay",MODE_PRIVATE).edit().putString("status",message).apply();
    }
}

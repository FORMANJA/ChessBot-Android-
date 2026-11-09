package com.example.chessbot;

import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.*;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

public class CaptureService extends Service {
    public static final String ACTION_DONE = "com.example.chessbot.CAPTURE_DONE";
    private Handler handler = new Handler(Looper.getMainLooper());
    private MediaProjection projection;
    private ImageReader reader;
    private VirtualDisplay display;
    private boolean completed = false;

    @Override public android.os.IBinder onBind(Intent i) { return null; }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationChannel channel = new NotificationChannel("capture", "ChessBot capture", NotificationManager.IMPORTANCE_LOW);
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        Notification n = new Notification.Builder(this,"capture")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("ChessBot: ekran yakalama")
            .setContentText("12 saniye sonra tek kare alınacak. Chess.com'a geç.")
            .build();
        startForeground(7, n, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        if(intent == null) { finish("İzin verisi bulunamadı"); return START_NOT_STICKY; }
        try {
            int resultCode = intent.getIntExtra("resultCode", Activity.RESULT_CANCELED);
            Intent data = intent.getParcelableExtra("data");
            MediaProjectionManager mgr = (MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
            projection = mgr.getMediaProjection(resultCode, data);
            if(projection == null) { finish("Ekran yakalama başlatılamadı"); return START_NOT_STICKY; }
            projection.registerCallback(new MediaProjection.Callback() {
                @Override public void onStop() { if(!completed) finish("Ekran paylaşımı durduruldu"); }
            }, handler);
            DisplayMetrics metrics = new DisplayMetrics();
            ((WindowManager)getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getRealMetrics(metrics);
            int w=metrics.widthPixels, h=metrics.heightPixels;
            reader = ImageReader.newInstance(w,h,android.graphics.PixelFormat.RGBA_8888,2);
            display = projection.createVirtualDisplay("ChessBotCapture",w,h,metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader.getSurface(),null,handler);
            handler.postDelayed(() -> capture(), 12000);
        } catch(Exception e) { finish("Yakalama hatası: "+e.getClass().getSimpleName()+": "+e.getMessage()); }
        return START_NOT_STICKY;
    }
    private void capture() {
        if(completed) return;
        try {
            Image img=reader.acquireLatestImage();
            if(img==null) { handler.postDelayed(() -> captureRetry(), 500); return; }
            writeImage(img);
        } catch(Exception e) { finish("Kayıt hatası: "+e.getMessage()); }
    }
    private void captureRetry() {
        if(completed) return;
        try {
            Image img=reader.acquireLatestImage();
            if(img==null) { finish("Ekran görüntüsü alınamadı"); return; }
            writeImage(img);
        } catch(Exception e) { finish("Kayıt hatası: "+e.getMessage()); }
    }
    private void writeImage(Image img) throws Exception {
        Image.Plane plane=img.getPlanes()[0];
        ByteBuffer buffer=plane.getBuffer();
        int w=img.getWidth(), h=img.getHeight();
        int pixelStride=plane.getPixelStride(), rowStride=plane.getRowStride();
        Bitmap padded=Bitmap.createBitmap(w+(rowStride-pixelStride*w)/pixelStride,h,Bitmap.Config.ARGB_8888);
        padded.copyPixelsFromBuffer(buffer);
        Bitmap cropped=Bitmap.createBitmap(padded,0,0,w,h);
        File out=new File(getExternalFilesDir(null),"board_capture.png");
        File prev=new File(getExternalFilesDir(null),"board_previous.png");
        if(out.exists()) { java.nio.file.Files.copy(out.toPath(),prev.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING); }
        try(FileOutputStream stream=new FileOutputStream(out)) { cropped.compress(Bitmap.CompressFormat.PNG,100,stream); }
        img.close(); padded.recycle(); cropped.recycle();
        finish("Kaydedildi: "+w+"×"+h+". Önceki görüntü saklandı.");
    }
    private void finish(String message) {
        if(completed) return;
        completed=true;
        getSharedPreferences("capture",MODE_PRIVATE).edit().putString("status",message).apply();
        sendBroadcast(new Intent(ACTION_DONE).setPackage(getPackageName()));
        if(display!=null) display.release();
        if(reader!=null) reader.close();
        if(projection!=null) projection.stop();
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }
}

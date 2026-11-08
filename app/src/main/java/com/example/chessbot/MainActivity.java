package com.example.chessbot;
import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private TextView status;
    @Override public void onCreate(Bundle b){super.onCreate(b);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(36,60,36,20);
        status=new TextView(this); status.setText("ChessBot Lab v0.1\nİzin ve dokunma altyapısı. Otomatik oyun henüz uygulanmadı."); root.addView(status);
        Button access=new Button(this); access.setText("Erişilebilirlik iznini aç"); root.addView(access);
        access.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        Button capture=new Button(this); capture.setText("Ekran yakalama iznini iste"); root.addView(capture);
        capture.setOnClickListener(v->{MediaProjectionManager m=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);startActivityForResult(m.createScreenCaptureIntent(),10);});
        Button test=new Button(this); test.setText("Uygulama içinde test dokunuşu"); root.addView(test);
        test.setOnClickListener(v->{if(TapService.instance==null){status.setText("Önce erişilebilirlik servisini etkinleştir.");return;} TapService.instance.tap(test.getX()+test.getWidth()/2f,test.getY()+test.getHeight()/2f+root.getTop());status.setText("Test dokunuşu gönderildi.");});
        setContentView(root);
    }
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==10)status.setText(result==RESULT_OK?"Ekran yakalama izni verildi. Yakalama motoru sonraki sürümde eklenecek.":"Ekran yakalama izni reddedildi.");}
}

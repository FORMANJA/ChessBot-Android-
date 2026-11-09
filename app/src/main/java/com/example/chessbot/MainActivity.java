package com.example.chessbot;

import android.app.Activity;
import android.content.*;
import android.graphics.*;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.widget.*;
import android.view.*;
import java.io.File;

public class MainActivity extends Activity {
    private TextView status;
    private BoardView boardView;
    private final int REQUEST_CAPTURE=42;
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20,24,20,16);
        TextView title=new TextView(this);
        title.setText("ChessBot Lab V0.4 — Kare Farkı Analizi");
        title.setTextSize(19);
        root.addView(title);
        status=new TextView(this);
        status.setText("Chess.com bilgisayar botunu aç. Yakalama iznini verince 12 saniye içinde oyuna geç. Sonra buraya dön.");
        root.addView(status);
        Button capture=new Button(this);
        capture.setText("12 saniye sonra ekranı yakala");
        root.addView(capture);
        capture.setOnClickListener(v -> {
            MediaProjectionManager m=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
            startActivityForResult(m.createScreenCaptureIntent(),REQUEST_CAPTURE);
        });
        Button reload=new Button(this);
        reload.setText("Son görüntüyü yükle");
        root.addView(reload);
        reload.setOnClickListener(v -> reloadImage());
        Button compare=new Button(this);
        compare.setText("Önceki ve son görüntüyü karşılaştır");
        root.addView(compare);
        compare.setOnClickListener(v -> {
            File folder=getExternalFilesDir(null);
            File previous=new File(folder,"board_previous.png");
            File current=new File(folder,"board_capture.png");
            if(!previous.exists()||!current.exists()) {
                status.setText("Karşılaştırmak için iki ayrı ekran yakalaması yap.");
                return;
            }
            android.content.SharedPreferences pref=getSharedPreferences("board",MODE_PRIVATE);
            if(!pref.contains("left")) {status.setText("Önce tahtanın köşelerini seç.");return;}
            status.setText(BoardDiff.compare(previous,current,
                pref.getFloat("left",0),pref.getFloat("top",0),
                pref.getFloat("right",0),pref.getFloat("bottom",0)));
        });
        TextView guide=new TextView(this);
        guide.setText("Görüntü açılınca tahtanın SOL ÜST ve SAĞ ALT köşelerine sırayla dokun. Kırmızı 8×8 ızgara görünür.");
        root.addView(guide);
        boardView=new BoardView(this);
        root.addView(boardView,new LinearLayout.LayoutParams(-1,0,1));
        Button reset=new Button(this);
        reset.setText("Köşeleri yeniden seç");
        root.addView(reset);
        reset.setOnClickListener(v -> boardView.reset());
        setContentView(root);
        reloadImage();
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request!=REQUEST_CAPTURE) return;
        if(result!=RESULT_OK || data==null) {status.setText("Ekran yakalama izni reddedildi.");return;}
        Intent service=new Intent(this,CaptureService.class);
        service.putExtra("resultCode",result);
        service.putExtra("data",data);
        startForegroundService(service);
        status.setText("İzin verildi. Şimdi Chess.com bot oyununa geç; 12 saniye sonra ekran yakalanacak.");
    }
    private void reloadImage() {
        String state=getSharedPreferences("capture",MODE_PRIVATE).getString("status","Henüz ekran yakalanmadı.");
        status.setText(state);
        File file=new File(getExternalFilesDir(null),"board_capture.png");
        if(!file.exists()) return;
        Bitmap bmp=BitmapFactory.decodeFile(file.getAbsolutePath());
        if(bmp!=null) boardView.setBitmap(bmp);
    }
    private class BoardView extends View {
        private Bitmap bitmap;
        private Paint p=new Paint(3);
        private int clicks=0;
        private float x1,y1,x2,y2;
        BoardView(Context c){super(c);}
        void setBitmap(Bitmap b){bitmap=b;clicks=0;invalidate();}
        void reset(){clicks=0;invalidate();}
        private RectF dest() {
            if(bitmap==null) return new RectF();
            float scale=Math.min(getWidth()/(float)bitmap.getWidth(),getHeight()/(float)bitmap.getHeight());
            float w=bitmap.getWidth()*scale,h=bitmap.getHeight()*scale;
            return new RectF((getWidth()-w)/2f,(getHeight()-h)/2f,(getWidth()+w)/2f,(getHeight()+h)/2f);
        }
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            if(bitmap==null)return;
            RectF d=dest();
            p.setColor(Color.WHITE);c.drawBitmap(bitmap,null,d,p);
            if(clicks==0)return;
            p.setColor(Color.RED);p.setStrokeWidth(3);
            float ax=d.left+x1*d.width()/bitmap.getWidth(), ay=d.top+y1*d.height()/bitmap.getHeight();
            c.drawCircle(ax,ay,8,p);
            if(clicks<2)return;
            float bx=d.left+x2*d.width()/bitmap.getWidth(), by=d.top+y2*d.height()/bitmap.getHeight();
            for(int i=0;i<=8;i++){
                float x=ax+(bx-ax)*i/8f,y=ay+(by-ay)*i/8f;
                c.drawLine(x,ay,x,by,p);c.drawLine(ax,y,bx,y,p);
            }
        }
        @Override public boolean onTouchEvent(android.view.MotionEvent e){
            if(e.getAction()!=MotionEvent.ACTION_UP||bitmap==null)return true;
            RectF d=dest();
            if(!d.contains(e.getX(),e.getY()))return true;
            float x=(e.getX()-d.left)*bitmap.getWidth()/d.width();
            float y=(e.getY()-d.top)*bitmap.getHeight()/d.height();
            if(clicks==0||clicks>=2){x1=x;y1=y;clicks=1;}
            else{
                x2=x;y2=y;clicks=2;
                if(x2<=x1||y2<=y1){clicks=0;status.setText("Önce sol üst, sonra sağ alt köşeyi seç.");}
                else {
                    getSharedPreferences("board",MODE_PRIVATE).edit().putFloat("left",x1).putFloat("top",y1)
                        .putFloat("right",x2).putFloat("bottom",y2).apply();
                    status.setText("Tahta sınırları: "+(int)x1+","+(int)y1+" → "+(int)x2+","+(int)y2);
                }
            }
            invalidate();return true;
        }
    }
}

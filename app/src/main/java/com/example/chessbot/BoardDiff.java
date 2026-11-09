package com.example.chessbot;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.File;
import java.util.Arrays;
import java.util.Locale;

/** Compare two screenshots of the same calibrated board. Does not identify pieces. */
public final class BoardDiff {
    public static String compare(File previous, File current, float left, float top, float right, float bottom) {\n        return analyze(previous,current,left,top,right,bottom).summary;\n    }\n    public static final class Result {\n        public final String summary; public final double[] scores;\n        Result(String summary,double[] scores){this.summary=summary;this.scores=scores;}\n    }\n    public static Result analyze(File previous, File current, float left, float top, float right, float bottom) {
        Bitmap a=BitmapFactory.decodeFile(previous.getAbsolutePath());
        Bitmap b=BitmapFactory.decodeFile(current.getAbsolutePath());
        if(a==null||b==null)return new Result("Görüntüler okunamadı.",null);
        try {
            if(a.getWidth()!=b.getWidth()||a.getHeight()!=b.getHeight()) return new Result("Ekran boyutu değişti; karşılaştırma yapılamaz.",null);
            if(right<=left||bottom<=top)return new Result("Önce tahtayı kalibre et.",null);
            double[] scores=new double[64];
            for(int row=0;row<8;row++) for(int col=0;col<8;col++){
                double sum=0;int count=0;
                // Inner square excludes grid lines and coordinate labels.
                for(int yi=2;yi<=8;yi++)for(int xi=2;xi<=8;xi++){
                    float fx=(col+(xi/10f))/8f, fy=(row+(yi/10f))/8f;
                    int x=Math.min(a.getWidth()-1,Math.max(0,Math.round(left+(right-left)*fx)));
                    int y=Math.min(a.getHeight()-1,Math.max(0,Math.round(top+(bottom-top)*fy)));
                    int p=a.getPixel(x,y),q=b.getPixel(x,y);
                    int dr=android.graphics.Color.red(p)-android.graphics.Color.red(q);
                    int dg=android.graphics.Color.green(p)-android.graphics.Color.green(q);
                    int db=android.graphics.Color.blue(p)-android.graphics.Color.blue(q);
                    sum+=Math.sqrt(dr*dr+dg*dg+db*db);count++;
                }
                scores[row*8+col]=sum/count;
            }
            Integer[] indices=new Integer[64];for(int i=0;i<64;i++)indices[i]=i;
            Arrays.sort(indices,(x,y)->Double.compare(scores[y],scores[x]));
            StringBuilder out=new StringBuilder("Değişen kare adayları (en yüksekten):\n");
            for(int k=0;k<8;k++){
                int i=indices[k],row=i/8,col=i%8;
                out.append(String.format(Locale.US,"%c%d: %.1f", 'a'+col,8-row,scores[i]));
                if(k<7)out.append("   ");
                if(k==3)out.append("\n");
            }
            out.append("\n\nBu yalnızca görüntü farkıdır; hamle veya taş türü kesin olarak belirlenmez. Animasyon, vurgular ve tema değişimi sonucu etkiler.");
            return new Result(out.toString(),scores);
        } finally {a.recycle();b.recycle();}
    }
}

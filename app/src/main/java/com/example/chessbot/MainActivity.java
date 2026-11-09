package com.example.chessbot;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private TextView status;
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(36, 60, 36, 20);
        status = new TextView(this);
        status.setText("ChessBot Lab v0.2\nGüvenli kurulum testi. Erişilebilirlik ve otomatik dokunma yok. Ekran yakalama izni isteyebilirsin.");
        root.addView(status);
        Button capture = new Button(this);
        capture.setText("Ekran yakalama iznini test et");
        root.addView(capture);
        capture.setOnClickListener(v -> {
            MediaProjectionManager m = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            startActivityForResult(m.createScreenCaptureIntent(), 10);
        });
        setContentView(root);
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == 10) status.setText(result == RESULT_OK
            ? "İzin verildi. Bu sürüm görüntü kaydetmez ve ekranı izlemez."
            : "Ekran yakalama izni verilmedi.");
    }
}

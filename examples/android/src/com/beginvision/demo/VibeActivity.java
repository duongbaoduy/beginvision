package com.beginvision.demo;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.http.conn.util.InetAddressUtils;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.PictureCallback;
import android.graphics.Bitmap;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.media.AudioRecord;
import android.os.Bundle;
import android.os.Looper;
import android.os.Handler;
import android.util.*;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class VibeActivity extends Activity
        implements CameraView.CameraReadyCallback, OverlayView.UpdateDoneCallback
{
    public static String TAG="BV";
    
    private ReentrantLock previewLock = new ReentrantLock();
    private CameraView cameraView = null;
    private OverlayView overlayView = null;
    private Bitmap  resultBitmap = null;    

    boolean beginDemo = false;

    //
    //  Activiity's event handler
    //
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // application setting
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);    

        // load and setup GUI
        super.onCreate(savedInstanceState);
        setContentView(R.layout.vibe);

        TextView tv = (TextView)findViewById(R.id.tv_message);
        tv.setText("This a VIBE (VIsual Background Extractor) demo");
        Button btn = (Button)findViewById(R.id.btn_control);
        btn.setOnClickListener(controlAction);

        // init camera
        initCamera();
   }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onPause() {      
        super.onPause();

        if ( cameraView != null) {
            previewLock.lock();
            cameraView.StopPreview();
            previewLock.unlock();
        }

        //finish();
        System.exit(0);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    //
    //  Interface implementation
    //
    public void onCameraReady() {
        cameraView.StopPreview();
        cameraView.setupCamera(640, 480, 4, previewCb);
        resultBitmap = Bitmap.createBitmap(cameraView.Width(), cameraView.Height(), Bitmap.Config.ARGB_8888);
        cameraView.StartPreview();
    }

    public void onUpdateDone() {
        previewLock.unlock(); 
    }

    //
    //  Internal help functions
    //
    private void initCamera() {
        SurfaceView cameraSurface = (SurfaceView)findViewById(R.id.surface_camera);
        cameraView = new CameraView(cameraSurface);        
        cameraView.setCameraReadyCallback(this);

        overlayView = (OverlayView)findViewById(R.id.surface_overlay);
        //overlayView_.setOnTouchListener(this);
        overlayView.setUpdateDoneCallback(this);
    }
     
    //
    //  Internal help class and object definment
    //
    private PreviewCallback previewCb = new PreviewCallback() {
        public void onPreviewFrame(byte[] yuvFrame, Camera c) {
            processNewFrame(yuvFrame, c);
        }
    };

    private void processNewFrame(final byte[] yuvFrame, final Camera c) {
        if ( beginDemo == false) {
            c.addCallbackBuffer(yuvFrame);
            return;    
        }

        if ( previewLock.isLocked() ) {
            c.addCallbackBuffer(yuvFrame);
        }
        
        previewLock.lock(); 
        new Thread(new Runnable() {
                    public void run() {
                        NativeAgent.updatePictureForResult("VIBE", yuvFrame, resultBitmap, cameraView.Width(), cameraView.Height());
                        c.addCallbackBuffer(yuvFrame);
                        new Handler(Looper.getMainLooper()).post( resultAction );
                    }
                }).start();
    }

    private OnClickListener controlAction = new OnClickListener() {
        @Override
        public void onClick(View v) {
            new Handler(Looper.getMainLooper()).postDelayed( beginAction, 2000); 
        }   
    };
    
    private Runnable beginAction = new Runnable() {
        public void run() {
            beginDemo = true;
        }
    };

    private Runnable resultAction = new Runnable() {
        private int count = 0;
        @Override 
        public void run() {
            count++;
            if ( (count % 3) == 0) {
                overlayView.DrawResult(resultBitmap);
            } else {
                previewLock.unlock(); 
            }
        }
    };
}

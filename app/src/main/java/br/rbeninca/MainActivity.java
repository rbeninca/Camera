package br.rbeninca;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity implements  View.OnClickListener{
    private ListenableFuture<ProcessCameraProvider> listenableFuture;
    ImageButton buttonFoto,buttonFlip,buttonVideo;
    PreviewView previewView;
    String [] permissions= new String [] {
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
    };
    boolean recording;

    CameraController cameraController;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonFoto=findViewById(R.id.imageButtonFoto);
        buttonFlip=findViewById(R.id.imageButtonflip);
        buttonVideo=findViewById(R.id.imageButtonVideo);
        previewView=findViewById(R.id.previewView);


        buttonFoto.setOnClickListener(this);
        buttonFlip.setOnClickListener(this);
        buttonVideo.setOnClickListener(this);
        buttonVideo.setImageResource(R.drawable.icon_video_camera_rec);
        recording=false;

        Log.d("LogOncreate",Long.toString(Thread.currentThread().getId())+"numThreads:"+Thread.activeCount());
        checkPermissoes();
        cameraController=new CameraController(this,previewView);



    }
    @Override
    public void onClick(View view){
        switch (view.getId()){
            case R.id.imageButtonFoto:
                try {
                    Log.d("LogCapturaFotoClick",Long.toString(Thread.currentThread().getId()));
                    cameraController.captureFoto();
                } catch ( IOException exception){
                    exception.printStackTrace();
                }
                break;
            case R.id.imageButtonflip:
                cameraController.inicializar_provider_cameraX();
                Toast.makeText(this,"flip",Toast.LENGTH_LONG).show();
                break;
            case R.id.imageButtonVideo:
                if (!recording){
                    buttonVideo.setImageResource(R.drawable.icon_video_camera_stop);
                    recording=true;
                    cameraController.recordVideo();
                } else {
                    buttonVideo.setImageResource(R.drawable.icon_video_camera_rec);
                    recording=false;
                    cameraController.stopRecording();
                }
                break;
        }
    }


    public  void  checkPermissoes(){
        //Checando as permissões foram concedidas ou as solicitando ao usuário
        for (String permission: permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) !=PackageManager.PERMISSION_GRANTED)
            {
                requestPermissions(permissions,PackageManager.PERMISSION_GRANTED);
            }
        }
    }

}
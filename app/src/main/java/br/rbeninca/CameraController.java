package br.rbeninca;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.core.VideoCapture;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CameraController {

    private ListenableFuture<ProcessCameraProvider> listenableFuture;

    //Casos de uso do cameraX
    private Preview previewUseCase;
    private ImageCapture imageCaptureUseCase;
    private VideoCapture videoCaptureUsecase;

    int currentCamera = 0;
    Context mContext;
    PreviewView mPreviewView;

    public CameraController(Context context, PreviewView previewView) {
        mContext = context;
        mPreviewView = previewView;
        listenableFuture = ProcessCameraProvider.getInstance(mContext);
        inicializar_provider_cameraX();
    }


    public void inicializar_provider_cameraX() {
        //        cameraProviderListenableFuture.addListener(()->{
//            try {
//                ProcessCameraProvider cameraProvider = cameraProviderListenableFuture.get();
//                startCameraX(cameraProvider);
//            }catch (ExecutionException e){
//                e.printStackTrace();
//            }catch (InterruptedException e){
//                e.printStackTrace();
//            }
//        }, getExecutor());
        currentThreadDebug();
        listenableFuture.addListener(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ProcessCameraProvider cameraProvider = listenableFuture.get();
                            inicializar_cameraX(cameraProvider);
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                ,
                getExecutor()
        );
    }

    private Executor getExecutor() {
        //Retorne um Executor que executará tarefas enfileiradas no thread principal
        // usado para despachar chamadas para componentes do aplicativo (atividades, serviços, etc).
        return ContextCompat.getMainExecutor(mContext);
    }

    @SuppressLint("RestrictedApi")
    public void inicializar_cameraX(ProcessCameraProvider cameraProvider) {
        //Desviculando todas ocorrencias de associações feitas com caso de uso do cameraX
        cameraProvider.unbindAll();
        Log.d("LogStartCamera", Long.toString(Thread.currentThread().getId()));
        //Definindo  Camera Selector, ou seja, esconlhendo a camera;
        if (currentCamera == CameraSelector.LENS_FACING_BACK) {
            currentCamera = CameraSelector.LENS_FACING_FRONT;
        } else {
            currentCamera = CameraSelector.LENS_FACING_BACK;
        }
        CameraSelector cameraSelector;
        cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(currentCamera)
                .build();

        //Definindo  use case Preview ;
        previewUseCase = new Preview.Builder().build();
        previewUseCase.setSurfaceProvider(mPreviewView.getSurfaceProvider());

        //Definido  use case Image capture;
        imageCaptureUseCase = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build();
        // Video capture use case
        videoCaptureUsecase = new VideoCapture.Builder()
                .setVideoFrameRate(30)
                .build();


        //associando os multplios objetos locais denidos localmente ao
        // ciclo de vida do camera X informando o contexto e os seguintes casos de uso
        // Preview  e Image Capture

        Camera camera = cameraProvider.bindToLifecycle(
                (LifecycleOwner) mContext,
                cameraSelector,
                previewUseCase,
                imageCaptureUseCase,
                videoCaptureUsecase
        );


        //Apos associar o caso de uso ao lifecycle do cameraX então podemos associar o
        // fluxo de preview gerado ao componente Preview que definidos no contexto.
        previewUseCase.setSurfaceProvider(
                mPreviewView.getSurfaceProvider()
        );
    }

    public void captureFoto() throws IOException {
        //Definindo o  arquivo da foto, para isso criamos uma função que gera o nome
        File photoFile = new File(getFilePath());

        //Utilizamos o caso de uso definido no contexto paara tirar a boto ao pressionar
        // o botão, observe ue passamos um caminho para foto ser salvo,(Conforme documentação),
        // e passamos também um contexto do fluxo da thread da aplicação para e um callback para cclo de vida.

        imageCaptureUseCase.takePicture(

                new ImageCapture.OutputFileOptions.Builder(photoFile).build(),
                getExecutor(),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        //Log.d("File","file saved on"+outputFileResults.getSavedUri().getPath());
                        //currentThreadDebug();
                        Toast.makeText(mContext, "Foto salva " + photoFile.getPath(), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.d("takePicture", photoFile.getPath());
                        Log.e("takePicture", exception.getMessage(), exception.getCause());
                        Toast.makeText(mContext, "Erro ao salvar foto" + exception.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private String getFilePath() {
        //caso opte por usar local diferente para armazenar tem que fericair se exite o diretório
        File photoDir = new File("/sdcard/DCIM");
        if (!photoDir.exists()) {
            photoDir.mkdir();
        }


        //Recuperando contexto da aplicação para adquirir pastas destino
        ContextWrapper contextWrapper = new ContextWrapper(mContext);
       // File filePhoto = contextWrapper.getExternalFilesDir(Environment.DIRECTORY_DCIM);
        File filePhoto = contextWrapper.getExternalFilesDir(MediaStore.Images.Media.EXTERNAL_CONTENT_URI.getPath());

        //Criando nome do arquivo com base na data e hora atual.

        Date date = new Date();
        String timestamp = String.valueOf(date.getTime());
        //Criando  um arquivo temporario
        //return  filePhoto+"/"+timestamp+".jpg";
        Log.d("File",MediaStore.Images.Media.EXTERNAL_CONTENT_URI.getPath());
        File file = new File(filePhoto, timestamp + ".jpg");
        return file.getPath();


    }

    public void currentThreadDebug() {
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        Integer i = 0;
        Log.d("LogThread:", "Start-----------------------------------------------------------------");
        for (Thread thread : threadSet) {
            Log.d("LogThread:", i.toString() + "nameThread:" + thread.getName().toString() + " id: " + thread.getId() + " print: " + thread.toString());
            i++;
        }
        Log.d("LogThread:", "-----------------");
        Thread thread = Thread.currentThread();
        Log.d("LogThread Corrente", "nameThread:" + thread.getName().toString() + " id: " + thread.getId() + " print: " + thread.toString());
        Log.d("LogThread:", "End-----------------------------------------------------------------");

    }

    @SuppressLint("RestrictedApi")
    public void recordVideo() {
        if (videoCaptureUsecase != null) {

            long timestamp = System.currentTimeMillis();

            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, timestamp);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
            try {
                if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                videoCaptureUsecase.startRecording(
                        new VideoCapture.OutputFileOptions.Builder(
                                mContext.getContentResolver(),
                                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                contentValues
                        ).build(),
                        getExecutor(),
                        new VideoCapture.OnVideoSavedCallback() {
                            @Override
                            public void onVideoSaved(@NonNull VideoCapture.OutputFileResults outputFileResults) {
                                Log.d("File","file saved on"+outputFileResults.getSavedUri());
                                Toast.makeText(mContext, "Vídeo Gravado com Sucesso."+outputFileResults.getSavedUri(), Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(int videoCaptureError, @NonNull String message, Throwable cause) {
                                Toast.makeText(mContext, "Error saving video: " + message, Toast.LENGTH_SHORT).show();
                            }
                        }
                );


            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressLint("RestrictedApi")
    public void stopRecording(){
        videoCaptureUsecase.stopRecording();
    }
}
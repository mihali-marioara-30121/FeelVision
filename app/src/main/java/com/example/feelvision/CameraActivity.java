package com.example.feelvision;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

public class CameraActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2{
    private static final String TAG = "MainActivity";
    private Mat mRgba;
    private CameraBridgeViewBase mOpenCvCameraView;
    private int mCameraId = 0;
    private FacialExpressionRecognition facialExpressionRecognition;
    private FrameLayout parentFrameLayout;
    private TextToSpeech textToSpeech;
    private SpeechRecognizer speechRecognizer;

    private final BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS) {
                Log.i(TAG, "OpenCv Is loaded");
                mOpenCvCameraView.enableView();
            }
            super.onManagerConnected(status);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        int MY_PERMISSIONS_REQUEST_CAMERA = 0;

        if (ContextCompat.checkSelfPermission(CameraActivity.this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(CameraActivity.this, new String[] {Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
        }

        setContentView(R.layout.activity_camera);
        mOpenCvCameraView = findViewById(R.id.frame_Surface);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        parentFrameLayout = findViewById(R.id.parent_layout);

        try {
            int INPUT_SIZE = 48;
            String modelFileName = "model_v4.tflite";
            facialExpressionRecognition = new FacialExpressionRecognition(getAssets(),CameraActivity.this,
                    modelFileName, INPUT_SIZE);
        }
        catch (IOException e){
            e.printStackTrace();
        }

        initializeTextToSpeech();
        initializeSpeechRecognizer();
        setupSpeechRecognitionOnTouchListener();
    }
    private void initializeTextToSpeech() {
        textToSpeech = new TextToSpeech(this, i -> {
            textToSpeech.setLanguage(Locale.ENGLISH);
                if(mCameraId == 0){
                    speak("Back camera is open!");
                } else {
                    speak("Frontal camera is open!");
                }
        });
    }
    private void initializeSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new SpeechRecognitionListener());
    }

    private void startListening() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        speechRecognizer.startListening(intent);
    }

    private class SpeechRecognitionListener implements RecognitionListener {
        @Override
        public void onResults(Bundle results) {
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            processResult( matches.get(0));
        }

        @Override
        public void onReadyForSpeech(Bundle params) {}
        @Override
        public void onBeginningOfSpeech() {}
        @Override
        public void onRmsChanged(float rmsdB) {}
        @Override
        public void onBufferReceived(byte[] buffer) {}
        @Override
        public void onEndOfSpeech() {}
        @Override
        public void onError(int error) {}
        @Override
        public void onPartialResults(Bundle partialResults) {}
        @Override
        public void onEvent(int eventType, Bundle params) {}
    }

    private void processResult(String command) {
        command = command.toLowerCase();
        if (command.contains("flip")) {
            swapCamera();
            if(mCameraId == 0){
                speak("Back camera is open!");
            } else {
                speak("Frontal camera is open!");
            }
        }
        if(command.contains("back")){
            openMainActivity();
        }
        if (command.contains("instruction")) {
            speak("Go back to main page to hear instructions!");
        }
        if (command.contains("start")) {
            speak("Camera is already opened!");
        }
        Toast.makeText(CameraActivity.this, "Recognized Command: " + command, Toast.LENGTH_SHORT).show();
    }

    private void speak(String message) {
        textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    public void setupSpeechRecognitionOnTouchListener() {
        parentFrameLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        startListening();
                        break;

                    case MotionEvent.ACTION_UP:
                        speechRecognizer.stopListening();
                        break;
                }
                return true;
            }
        });
    }
   private void swapCamera(){
        mCameraId = mCameraId^1; //basic not operation
       mOpenCvCameraView.disableView();
       mOpenCvCameraView.setCameraIndex(mCameraId);
       mOpenCvCameraView.enableView();
   }

    @Override
    protected void onResume() {
        super.onResume();
        if (OpenCVLoader.initDebug()) {
            Log.d(TAG,"Opencv initialization is done");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        else {
            Log.d(TAG,"Opencv is not loaded. try again");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0,this, mLoaderCallback);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mOpenCvCameraView !=null){
            mOpenCvCameraView.disableView();
        }
    }

    public void onDestroy(){
        super.onDestroy();

        if (mOpenCvCameraView != null){
            mOpenCvCameraView.disableView();
        }
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }

    public void onCameraViewStarted(int width ,int height){
        mRgba = new Mat(height,width, CvType.CV_8UC4);
    }
    public void onCameraViewStopped(){
        mRgba.release();
    }
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame){
        mRgba = inputFrame.rgba();

        if(mCameraId == 1){
            Core.flip(mRgba, mRgba, 1); //rotate by 180
        }
        mRgba = facialExpressionRecognition.recognizeImage(mRgba);

        return mRgba;
    }

    private void openMainActivity() {
        Intent intent = new Intent(CameraActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

}
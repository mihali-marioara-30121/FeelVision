package com.example.feelvision;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import android.widget.Toast;

import org.opencv.android.OpenCVLoader;

import java.util.ArrayList;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {

    private TextToSpeech textToSpeech;
    private SpeechRecognizer speechRecognizer;
    private ConstraintLayout constraintLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (OpenCVLoader.initDebug()) {
            Log.i("LOADED", "OpenCV loaded successfully");
        } else {
            Log.e("LOADED", "OpenCV initialization failed!");
            Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG).show();
            return;
        }
        constraintLayout = findViewById(R.id.parent_layout);

        checkAudioPermissions();
        initializeTextToSpeech();
        initializeSpeechRecognizer();
        setupSpeechRecognitionOnTouchListener();
    }

    private void checkAudioPermissions() {
        int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 0;
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.RECORD_AUDIO}, MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
        }
    }

    private void initializeTextToSpeech() {
        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener(){

            @Override
            public void onInit(int i) {
                if(textToSpeech.getEngines().size() == 0) {
                    Toast.makeText(MainActivity.this, "There is no TextToSpeech engine on your device!",
                            Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    textToSpeech.setLanguage(Locale.ENGLISH);
                    speak("Hello! Tap the screen and say 'Start' to open camera or 'Instructions to hear all the possible commands!");
                }
            }
        });
    }

    private void initializeSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new SpeechRecognitionListener());
    }

    private void startListening() {
        textToSpeech.stop();
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
        if (command.contains("start")) {
            openCameraActivity();
        }

        if (command.contains("back")) {
            speak("You are already in the main page. Say 'Start' to open camera");
        }

        if (command.contains("flip")) {
            speak("You are in the main page now. Say 'Start' to open camera");
        }

        if (command.contains("instruction")) {
            speak("Hello! Welcome to FeelVision! " + "Keep your finger on the screen while saying a command then release it to execute! " + "Say 'start' to open camera. " +
                    "Say 'flip' to switch between back and frontal camera. " +
                    "Say 'back' to return to the main page. ");
        }

        Toast.makeText(MainActivity.this, "Recognized Command: " + command, Toast.LENGTH_SHORT).show();
    }

    private void speak(String message) {
        textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    public void setupSpeechRecognitionOnTouchListener() {
        constraintLayout.setOnTouchListener(new View.OnTouchListener() {
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

    private void openCameraActivity() {
        Intent intent = new Intent(MainActivity.this, CameraActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }


    @Override
    protected void onDestroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}
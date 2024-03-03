package com.example.imagepro;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;

import java.util.ArrayList;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {

    private TextToSpeech textToSpeech;
    private SpeechRecognizer speechRecognizer;

    static {
        if(OpenCVLoader.initDebug()){
            Log.d("MainActivity: ","Opencv is loaded");
        }
        else {
            Log.d("MainActivity: ","Opencv failed to load");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button camera_button = findViewById(R.id.camera_button);

        checkAudioPermissions();
        camera_button.setOnClickListener(v -> openCameraActivity());
        initializeTextToSpeech();
        initializeSpeechRecognizer();
    }

    private void checkAudioPermissions() {
        int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 0;
        // Check if the record audio permission is not granted
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_DENIED){
            // Request the record audio permission
            ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.RECORD_AUDIO}, MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
        }
    }

    private void startListening() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        speechRecognizer.startListening(intent);
    }

    private void openCameraActivity() {
        Intent intent = new Intent(MainActivity.this, CameraActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private void initializeSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new SpeechRecognitionListener());
    }

    private void processResult(String command) {
        command = command.toLowerCase();
        if (command.contains("start")){
            openCameraActivity();
            speak("CameraActivity is open!");
        } else {
            startListening();
        }
        // Display the recognized command in a Toast message
        Toast.makeText(MainActivity.this, "Recognized Command: " + command, Toast.LENGTH_SHORT).show();
    }

    private void initializeTextToSpeech() {
        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener(){

            @Override
            public void onInit(int i) {
                if(textToSpeech.getEngines().size() == 0){ // no engine are installed
                    Toast.makeText(MainActivity.this, "There is no TextToSpeech engine on your device!",
                             Toast.LENGTH_LONG).show();
                    finish();
                } else{
                    textToSpeech.setLanguage(Locale.ENGLISH);
                    speak("Hello! I am ready. Say START to open Camera!");
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            startListening();
                        }
                    }, 4000);
                }
            }
        });
    }

    private void speak(String message) {
        textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
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

    @Override
    protected void onPause() {
        super.onPause();
        textToSpeech.shutdown();
    }

    private class SpeechRecognitionListener implements RecognitionListener {
        @Override
        public void onResults(Bundle results) {
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                processResult( matches.get(0));
        }

        // Implement other required methods with empty bodies
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
}
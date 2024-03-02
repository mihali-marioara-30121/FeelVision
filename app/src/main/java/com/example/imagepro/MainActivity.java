package com.example.imagepro;

import static android.Manifest.permission.RECORD_AUDIO;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import android.os.Bundle;


public class MainActivity extends AppCompatActivity {

    private TextToSpeech textToSpeech;
    private SpeechRecognizer speechRecognizer;
    private View rootView;

    private TextView textView;

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

        int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 0;
// Check if the record audio permission is not granted
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_DENIED){
            // Request the record audio permission
            ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.RECORD_AUDIO}, MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
        } else {
            initializeSpeechRecognizer();
        }

        Button camera_button = findViewById(R.id.camera_button);

        rootView = getWindow().getDecorView().getRootView(); // Store the root view
        textView = findViewById(R.id.text_view);
        camera_button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                openCameraActivity();
            }
        });

        initializeTextToSpeech();
        initializeSpeechRecognizer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Start speech recognition when the activity is resumed
        startListening(rootView);
    }

    private void startListening(View view) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        speechRecognizer.startListening(intent);
    }

    private void openCameraActivity() {
        Intent intent = new Intent(MainActivity.this,CameraActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private void initializeSpeechRecognizer() {
        // Initialize SpeechRecognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new SpeechRecognitionListener());

    }

    private void processResult(String command) {
        textView.setText(command);
        command = command.toLowerCase();
        if(command.contains("start")){
            openCameraActivity();
            speak("CameraActivity is open!");
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
                }
            }
        });
    }

    private void speak(String message) {
        if(Build.VERSION.SDK_INT >= 21){
           textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
        } else {
           textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null);
        }
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
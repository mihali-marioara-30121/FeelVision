package com.example.feelvision;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Locale;

public class FacialExpressionRecognition {
    private final Interpreter interpreter;
    private final int INPUT_SIZE;
    private TextToSpeech textToSpeech;
    private CascadeClassifier cascadeClassifier;
    private Context classContext;
    private String lastEmotion = "";
    private long lastTimestamp = System.currentTimeMillis();

    public FacialExpressionRecognition(AssetManager assetManager, Context context, String modelPath, int inputSize) throws IOException {
        INPUT_SIZE = inputSize;
        classContext = context;
        initializeTextToSpeech();
        GpuDelegate gpuDelegate = new GpuDelegate();

        Interpreter.Options options = new Interpreter.Options();
        options.addDelegate(gpuDelegate);
        options.setNumThreads(4);
        interpreter = new Interpreter(loadModelFile(assetManager, modelPath), options);

        Log.d("facial_Expression","Model is loaded");

        // load haar cascade classifier
        try {
            InputStream inputStream = context.getResources().openRawResource(R.raw.haarcascade_frontalface_alt);
            File cascadeDir = context.getDir("cascade",Context.MODE_PRIVATE);
            File cascadeFile = new File(cascadeDir,"haarcascade_frontalface_alt");
            FileOutputStream outputStream = new FileOutputStream(cascadeFile);

            byte[] buffer = new byte[4096];
            int byteRead;
            while ((byteRead = inputStream.read(buffer)) != -1){
                outputStream.write(buffer,0, byteRead);
            }

            inputStream.close();
            outputStream.close();

            cascadeClassifier = new CascadeClassifier(cascadeFile.getAbsolutePath());
            Log.d("facial_Expression","Classifier is loaded");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Mat recognizeImage(Mat mat_image) {
        Core.flip(mat_image.t(),mat_image,1);
        Mat grayscaleImage = new Mat();
        Imgproc.cvtColor(mat_image,grayscaleImage, Imgproc.COLOR_RGBA2GRAY);

        int height = grayscaleImage.height();
        int absoluteFaceSize = (int) (height * 0.1);

        MatOfRect faces = new MatOfRect();

        if(cascadeClassifier != null) {
            // detect face in frame
            cascadeClassifier.detectMultiScale(grayscaleImage, faces,1.1,2,2,
                    new Size(absoluteFaceSize, absoluteFaceSize), new Size());
        }

        Rect[] faceArray = faces.toArray();
        if (faceArray.length == 0) {
            alertUserToFindFace();
        }

        for (int i=0; i<faceArray.length; i++) {
            lastTimestamp = System.currentTimeMillis();
            Imgproc.rectangle(mat_image,faceArray[i].tl(),faceArray[i].br(),new Scalar(0,255,0,255),4);

                        // starting x coordinate       starting y coordinate
            Rect roi = new Rect((int)faceArray[i].tl().x,(int)faceArray[i].tl().y,
                    ((int)faceArray[i].br().x)-(int)(faceArray[i].tl().x), //width  of the roi
                    ((int)faceArray[i].br().y)-(int)(faceArray[i].tl().y)); //height of the roi

            Mat cropped_rgba = new Mat(mat_image,roi);

            Bitmap bitmap;
            bitmap = Bitmap.createBitmap(cropped_rgba.cols(), cropped_rgba.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(cropped_rgba, bitmap);
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap,48,48,false);
            ByteBuffer byteBuffer = convertBitmapToByteBuffer(scaledBitmap);

            int numClasses = 7;
            float[][] emotion = new float[1][numClasses];
            //predict emotion
            interpreter.run(byteBuffer, emotion);

            String emotionString = getEmotion(emotion);
            speak(emotionString);
            Imgproc.putText(mat_image,emotionString,
                    new Point((int)faceArray[i].tl().x + 10,(int)faceArray[i].tl().y - 10),
                    2,4,new Scalar(237,9,9,150),4);
        }

        Core.flip(mat_image.t(), mat_image,0);
        return mat_image;
    }

    private void alertUserToFindFace() {
        long currentTimestamp = System.currentTimeMillis();
        if (currentTimestamp - lastTimestamp < 10000) {
            return;
        }

        if (!textToSpeech.isSpeaking()) {
            lastTimestamp = System.currentTimeMillis();
            lastEmotion = "undetected face";
            textToSpeech.speak("No face detected, please point the camera towards a person", TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private int argmax(float[] array) {
        int maxIndex = 0;
        for (int i = 1; i < array.length; i++) {
            if (array[i] > array[maxIndex]) {
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    // 0: Angry, 1: Disgust, 2: Fear, 3:Happy, 4: Sad, 5: Neutral , 6: Surprise
    public String getEmotion(float[][] emotion){
        String emotionString = "";
        switch (argmax(emotion[0])) {
            case 3:
                emotionString = "Happy";
                break;
            case 4:
                emotionString = "Neutral";
                break;
            case 6:
                emotionString = "Surprise";
                break;
        }
        return emotionString;
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap scaledBitmap) {
        ByteBuffer byteBuffer;
        int size_image = INPUT_SIZE;

        byteBuffer = ByteBuffer.allocateDirect(4 * size_image * size_image * 3);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues=new int[size_image*size_image];
        scaledBitmap.getPixels(intValues,0,scaledBitmap.getWidth(),0,0,scaledBitmap.getWidth(),scaledBitmap.getHeight());
        int pixel = 0;

        for(int i = 0; i < size_image; ++i){
            for(int j = 0; j < size_image; ++j){
                final int val = intValues[pixel++];
                //convert image from 0-255 to 0-1
                byteBuffer.putFloat((((val>>16)&0xFF))/255.0f); //extracts the red value
                byteBuffer.putFloat((((val>>8)&0xFF))/255.0f); //extracts the green value
                byteBuffer.putFloat(((val & 0xFF))/255.0f); //extracts the blue value
            }
        }
        return byteBuffer;
    }


    private MappedByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException {
        AssetFileDescriptor assetFileDescriptor = assetManager.openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(assetFileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = assetFileDescriptor.getStartOffset();
        long declaredLength = assetFileDescriptor.getDeclaredLength();

        return  fileChannel.map(FileChannel.MapMode.READ_ONLY,startOffset,declaredLength);
    }

    private void initializeTextToSpeech() {
        textToSpeech = new TextToSpeech(classContext, i -> {
            textToSpeech.setLanguage(Locale.ENGLISH);
        });
    }
    private void speak(String message) {
        if (message.isEmpty())
            return;

        if (!textToSpeech.isSpeaking() && !lastEmotion.equals(message)) {
            textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
            lastEmotion = message;
        }
    }
}

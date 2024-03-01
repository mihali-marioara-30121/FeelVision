package com.example.imagepro;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
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
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class FacialExpressionRecognition {
    private final Interpreter interpreter;
    private final int INPUT_SIZE;
    private CascadeClassifier cascadeClassifier; // used for face detection

    public FacialExpressionRecognition(AssetManager assetManager, Context context, String modelPath, int inputSize) throws IOException {
        INPUT_SIZE = inputSize;
        GpuDelegate gpuDelegate = new GpuDelegate();

        Interpreter.Options options = new Interpreter.Options();
        options.addDelegate(gpuDelegate);
        options.setNumThreads(4); // set this according to your phone
        // this will load model weight to interpreter
        interpreter = new Interpreter(loadModelFile(assetManager, modelPath), options);

        Log.d("facial_Expression","Model is loaded");

        // load haar cascade classifier
        try {
            // define input stream to read classifier
            InputStream inputStream = context.getResources().openRawResource(R.raw.haarcascade_frontalface_alt);
            // create a folder
            File cascadeDir = context.getDir("cascade",Context.MODE_PRIVATE);
            // now create a new file in that folder
            File cascadeFile = new File(cascadeDir,"haarcascade_frontalface_alt");
            // now define output stream to transfer data to file we created
            FileOutputStream outputStream = new FileOutputStream(cascadeFile);
            // now create buffer to store byte
            byte[] buffer = new byte[4096];
            int byteRead;
            // read bytes in while loop
            // when it read -1 that means no data to read
            while ((byteRead = inputStream.read(buffer)) != -1){
                // writing on mCascade file
                outputStream.write(buffer,0, byteRead);
            }

            // close input and output stream
            inputStream.close();
            outputStream.close();

            cascadeClassifier = new CascadeClassifier(cascadeFile.getAbsolutePath());
            // if cascade file is loaded print
            Log.d("facial_Expression","Classifier is loaded");
            // cropped frame is then pass through interpreter which will return facial expression/emotion
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    public Mat recognizeImage(Mat mat_image){
        // rotate image by 90 degree before predicting
        Core.flip(mat_image.t(),mat_image,1);
        // convert mat_image to gray scale image
        Mat grayscaleImage = new Mat();
        Imgproc.cvtColor(mat_image,grayscaleImage, Imgproc.COLOR_RGBA2GRAY);

        int height = grayscaleImage.height();

        // define minimum height of face in original image
        // below this size no face in original image will show
        int absoluteFaceSize = (int) (height * 0.1);
        // now create MatOfRect to store face (Matrix of rectangle)
        MatOfRect faces = new MatOfRect();
        // check if cascadeClassifier is loaded or not
        if(cascadeClassifier != null){
            // detect face in frame
            cascadeClassifier.detectMultiScale(grayscaleImage, faces,1.1,2,2,
                    new Size(absoluteFaceSize, absoluteFaceSize), new Size());
                    // minimum size
        }

        // now convert it to array
        Rect[] faceArray = faces.toArray(); // Array of rectangles
        // loop through each face
        for (int i=0; i<faceArray.length; i++){
            // if you want to draw rectangle around face
            //                input/output starting point ending point        color   R  G  B  alpha    thickness
            Imgproc.rectangle(mat_image,faceArray[i].tl(),faceArray[i].br(),new Scalar(0,255,0,255),2);
            // now crop face from original frame and grayscaleImage
            // roi = region of interest
                        // starting x coordinate       starting y coordinate
            Rect roi = new Rect((int)faceArray[i].tl().x,(int)faceArray[i].tl().y,
                    ((int)faceArray[i].br().x)-(int)(faceArray[i].tl().x), //width  of the roi
                    ((int)faceArray[i].br().y)-(int)(faceArray[i].tl().y)); //height of the roi

            Mat cropped_rgba = new Mat(mat_image,roi);
            // now convert cropped_rgba to bitmap
            Bitmap bitmap;
            bitmap=Bitmap.createBitmap(cropped_rgba.cols(), cropped_rgba.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(cropped_rgba, bitmap);
            // resize bitmap to (48,48)
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap,48,48,false);
            // now convert scaledBitmap to byteBuffer
            ByteBuffer byteBuffer = convertBitmapToByteBuffer(scaledBitmap);
            int numClasses = 7;
            // now create an object to hold output
            float[][] emotion = new float[1][numClasses];
            //now predict with bytebuffer as an input and emotion as an output
            interpreter.run(byteBuffer, emotion);
            // if emotion is recognize print value of it

            // define float value of emotion
            float emotionValue = (float) Array.get(Array.get(emotion,0),0);
            Log.d("facial_expression","Output:  "+ emotionValue);
            // create a function that return text emotion
            //String emotion_s = findEmotionByValue(emotionValue);
            String emotion_s = getEmotion(emotion);
            // now put text on original frame(mat_image)
            //             input/output    text: Angry (2.934234)
            Imgproc.putText(mat_image,emotion_s+" ("+emotionValue+")",
                    new Point((int)faceArray[i].tl().x+10,(int)faceArray[i].tl().y+20),
                    1,1.5,new Scalar(0,0,255,150),2);
            //      use to scale text      color     R G  B  alpha    thickness
        }

        // rotate mat_image -90 degree after prediction
        Core.flip(mat_image.t(), mat_image,0);
        return mat_image;
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
    public String getEmotion(float[][] emotion){
        String emotion_s = "";
        switch (argmax(emotion[0])) {
            case 0:
                emotion_s = "Angry";
                break;
            case 1:
                emotion_s = "Disgust";
                break;
            case 2:
                emotion_s = "Fear";
                break;
            case 3:
                emotion_s = "Happy";
                break;
            case 4:
                emotion_s = "Sad";
                break;
            case 5:
                emotion_s = "Surprise";
                break;
            case 6:
                emotion_s = "Neutral";
                break;
        }
        return emotion_s;
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap scaledBitmap) {
        ByteBuffer byteBuffer;
        int size_image = INPUT_SIZE;

        byteBuffer = ByteBuffer.allocateDirect(4 * size_image * size_image * 3);
        // 4 is multiplied for float input
        // 3 is multiplied for rgb
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues=new int[size_image*size_image];
        scaledBitmap.getPixels(intValues,0,scaledBitmap.getWidth(),0,0,scaledBitmap.getWidth(),scaledBitmap.getHeight());
        int pixel = 0;
        for(int i = 0; i < size_image; ++i){
            for(int j = 0; j < size_image; ++j){
                final int val = intValues[pixel++];
                // now put float value to bytebuffer
                // scale image to convert image from 0-255 to 0-1
                byteBuffer.putFloat((((val>>16)&0xFF))/255.0f); //extracts the red value
                byteBuffer.putFloat((((val>>8)&0xFF))/255.0f); //extracts the green value
                byteBuffer.putFloat(((val & 0xFF))/255.0f); //extracts the blue value
            }
        }
        return byteBuffer;
    }


    private MappedByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException{
        // this will give description of file
        AssetFileDescriptor assetFileDescriptor = assetManager.openFd(modelPath);
        // create a inputsteam to read file
        FileInputStream inputStream = new FileInputStream(assetFileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();

        long startOffset = assetFileDescriptor.getStartOffset();
        long declaredLength = assetFileDescriptor.getDeclaredLength();
        return  fileChannel.map(FileChannel.MapMode.READ_ONLY,startOffset,declaredLength);
    }
}

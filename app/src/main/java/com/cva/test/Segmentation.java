package com.cva.test;



import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.provider.ContactsContract;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.google.android.gms.common.util.ArrayUtils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.common.FirebaseMLException;
import com.google.firebase.ml.common.modeldownload.FirebaseLocalModel;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager;
import com.google.firebase.ml.common.modeldownload.FirebaseRemoteModel;
import com.google.firebase.ml.custom.FirebaseModelDataType;
import com.google.firebase.ml.custom.FirebaseModelInputOutputOptions;
import com.google.firebase.ml.custom.FirebaseModelInputs;
import com.google.firebase.ml.custom.FirebaseModelInterpreter;
import com.google.firebase.ml.custom.FirebaseModelOptions;
import com.google.firebase.ml.custom.FirebaseModelOutputs;


import java.io.DataInputStream;

import java.util.Random;
import java.util.Scanner;

public class Segmentation {


    FirebaseModelInterpreter firebaseInterpreter;
    FirebaseModelInputOutputOptions inputOutputOptions;
    Bitmap segImage =Bitmap.createBitmap(160, 160, Bitmap.Config.ARGB_8888);
    DataInputStream filestream;
    Random rand = new Random();
    int[] mSegmentColors = new int[21];
    int[][] mSegmentBits = new int[160][160];

    private static final float IMAGE_MEAN = 128.0f;
    private static final float IMAGE_STD = 128.0f;



    void init_model(){

        FirebaseModelDownloadConditions.Builder conditionsBuilder =
                new FirebaseModelDownloadConditions.Builder().requireWifi();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Enable advanced conditions on Android Nougat and newer.
            conditionsBuilder = conditionsBuilder
                    .requireCharging()
                    .requireDeviceIdle();
        }
        FirebaseModelDownloadConditions conditions = conditionsBuilder.build();

// Build a remote model source object by specifying the name you assigned the model
// when you uploaded it in the Firebase console.
        FirebaseRemoteModel cloudSource = new FirebaseRemoteModel.Builder("unet_mixed_dataset")
                .enableModelUpdates(true)
                .setInitialDownloadConditions(conditions)
                .setUpdatesDownloadConditions(conditions)
                .build();
        FirebaseModelManager.getInstance().registerRemoteModel(cloudSource);

        FirebaseLocalModel localSource =
                new FirebaseLocalModel.Builder("my_local_model")  // Assign a name to this model
                        .setAssetFilePath("model.tflite")
                        .build();
        FirebaseModelManager.getInstance().registerLocalModel(localSource);
        FirebaseModelOptions options = new FirebaseModelOptions.Builder().
                setRemoteModelName("unet_mixed_dataset")
                .setLocalModelName("my_local_model")
                .build();
        try {
             firebaseInterpreter =
                    FirebaseModelInterpreter.getInstance(options);

           inputOutputOptions =
                    new FirebaseModelInputOutputOptions.Builder()
                            .setInputFormat(0, FirebaseModelDataType.FLOAT32, new int[]{1, 160, 160, 1})
                            .setOutputFormat(0, FirebaseModelDataType.FLOAT32, new int[]{1,  160, 160, 1})
                            .build();
        } catch (FirebaseMLException e) {
            e.printStackTrace();
        }


        FirebaseModelManager.getInstance().downloadRemoteModelIfNeeded(cloudSource)
                .addOnSuccessListener(
                        new OnSuccessListener<Void>() {

                            @Override
                            public void onSuccess(Void aVoid) {

                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Model couldn’t be downloaded or other internal error.
                                // ...
                            }
                        });

        for (int i = 0; i < 21; i++) {
            if (i == 0) {
                mSegmentColors[i] = Color.TRANSPARENT;
            } else {
                mSegmentColors[i] = Color.rgb(
                        (int)(255 * rand.nextFloat()),
                        (int)(255 * rand.nextFloat()),
                        (int)(255 * rand.nextFloat()));
            }
        }
    }

    float[][][][] transform_image(Bitmap image) {
        Bitmap scaledImage = Bitmap.createScaledBitmap(image, 160, 160, true);
        float[][][][] input = new float[1][160][160][1];
        int batchNum = 0;
        for (int x = 0; x < 160; x++) {
            for (int y = 0; y < 160; y++) {
                int pixel = scaledImage.getPixel(x, y);
                // Normalize the model inputs
                // to do : verify if the model inputs need to be normalized or not
                input[batchNum][x][y][0] = ((Color.red(pixel) - 127) + (Color.green(pixel) - 127) + (Color.blue(pixel) - 127))/ 128.0f;
            }
        }
        return input;
    }
    
    Bitmap segment(final Bitmap image, final ImageView imageview){
        init_model();
        try {
            FirebaseModelInputs inputs = new FirebaseModelInputs.Builder()
                    .add(transform_image(image))
                    .build();

            firebaseInterpreter.run(inputs, inputOutputOptions)
                    .addOnSuccessListener(
                            new OnSuccessListener<FirebaseModelOutputs>() {
                                @Override
                                public void onSuccess(FirebaseModelOutputs result) {

                                    float[][][] output = result.getOutput(0);
                                    float[][] mSegmentsBits = new float[160][160];
                                    for(int i=0; i< 160; i++){
                                        for(int j=0; j< 160; j++){
                                            if(output[0][i][j] > 0.5){
                                            mSegmentsBits[i][j] = Color.rgb(0,0,255);
                                            }
                                            else{
                                                mSegmentsBits[i][j]=Color.rgb(165,42,42);
                                            }

                                        }

                                    }
                                    segImage = bitmapFromArray(mSegmentBits);
                                    imageview.setImageBitmap(segImage);

                                }
                            })
                    .addOnFailureListener(
                            new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    e.printStackTrace();
                                }
                            });
        } catch (FirebaseMLException e) {
            e.printStackTrace();
        }
        return segImage;
    }


    // the equivalent of  colormap[label] in numpy
    int[][] labelToColorImage(float[][] label){

        int[][] colorMap = pascalColorMapFromFile();
        int[][] labelColorMap = new int[label.length][label[0].length];
        for(int i=0; i< 3; i++){
            for(int j=0; j< label[0].length; j++){

                labelColorMap[i][j] = colorMap[i][(int)label[i][j]];
            }
        }
        return labelColorMap;
    }


    void setFileStream(DataInputStream filestream){
        this.filestream = filestream;
    }

    int[][] pascalColorMapFromFile(){
        Scanner sc = null;
            sc = new Scanner(filestream);

        int rows = 3;
        int columns = 256;
        int [][] myArray = new int[rows][columns];
        while(sc.hasNextLine()) {
            String row = sc.nextLine();
            for (int i=0; i< myArray.length; i++) {
                String[] line = row.trim().split(" ");
                for (int j=0; j< line.length; j++) {
                    myArray[i][j] = Integer.parseInt(line[j]);
                }
            }
        }

        return myArray;
    }

    // ported from google's python  deeplab demo to generate a label colormap for Pascal VOC segmentation
    // to do: refactor
    int[][] PascalColorMap(){

        int[][] colorMap = new int[256][3];
        int[] ind = new int[256];
        for(int i=0; i<256; i++){
            ind[i]=i;
        }

        for(int shift=0; shift<8; shift++){
            for(int channel=0; channel<3; channel++){
            for(int i=0; i< 256; i++){
                colorMap[i][channel] |= ((ind[i] >> channel) & 1) << shift;
            }

            }
            for(int i=0; i<256; i++){
                ind[i] >>= 3;
            }
        }

    return transpose(colorMap);
    }

    int[][] transpose(int[][] matrix){


        int[][] transpose = new int[matrix[0].length][matrix.length];
        if(matrix.length > 0){
            for(int i=0; i<matrix.length; i++){
                for(int j=0; j<matrix[0].length; j++){
                    transpose[j][i] = matrix[i][j];
                }
            }
        }
        return transpose;
    }

    void debugArray(float[][] array) {
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[0].length; j++) {
                System.out.print(array[i][j]);
                System.out.print(' ');
            }
            System.out.println(' ');
        }
    }

    void debugArray(int[][] array) {
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[0].length; j++) {
                System.out.print(array[i][j]);
                System.out.print(' ');
            }
            System.out.println(' ');
        }
    }

    public  Bitmap bitmapFromArray(int[][] pixels2d){
        int width = pixels2d.length;
        int height = pixels2d[0].length;
        int[] pixels = new int[width * height];
        int pixelsIndex = 0;
        for (int i = 0; i < width; i++)
        {
            for (int j = 0; j < height; j++)
            {
                pixels[pixelsIndex] = mSegmentColors[pixels2d[i][j]];
                pixelsIndex ++;
            }
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
    }



}

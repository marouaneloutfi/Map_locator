package com.cva.test;


import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.DataInputStream;
import java.io.IOException;


public class MainActivity extends AppCompatActivity {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    Button camerabutton, findbutton;
    ImageView image;
    boolean has_image = false;
    Segmentation segmentation = new Segmentation();
    Bitmap imageBitmap;
    GraphicOverlay graphic_overlay;
    AssetManager assetManager;
    AssetFileDescriptor descriptor = null;
    Bitmap segImage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        addListenerOnCamera();
        addListenerOnFind();


    }




    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            imageBitmap = (Bitmap) extras.get("data");
            image.setImageBitmap(imageBitmap);
            imageBitmap = flipImage(rotateImage(imageBitmap, 90));


        }
    }

    public void addListenerOnCamera(){
        this.
        image = (ImageView) findViewById(R.id.map_slice);
        camerabutton= (Button) findViewById(R.id.camera_btn);

       camerabutton.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               has_image = true;
               dispatchTakePictureIntent();
           }
       });

    }


    public void addListenerOnFind(){
        findbutton = (Button) findViewById(R.id.find_btn);
        findbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(has_image) {

                    try {
                        DataInputStream fileStream = new DataInputStream(getAssets().open(String.format("pascal_array.txt")));
                        segmentation.setFileStream(fileStream);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    segImage = segmentation.segment(imageBitmap,image);
                    image.setImageBitmap(segImage);

                }
                else{
                    System.out.println("Take a picture first");
                }
            }
        });
    }



    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }

    public static Bitmap flipImage(Bitmap source){
        Matrix matrix = new Matrix();
        float centreX = source.getWidth()/ 2f;
        float centerY = source.getHeight()/2f;
        matrix.postScale(-1,1,centreX,centerY);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }




}


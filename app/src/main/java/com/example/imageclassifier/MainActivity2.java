package com.example.imageclassifier;

import androidx.annotation.DrawableRes;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;


public class MainActivity2 extends AppCompatActivity {

    private TextView textView;
    private ImageView imageView;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        Intent intent = getIntent();
        String classID = (String) intent.getSerializableExtra("class");

        textView = (TextView) findViewById(R.id.textView_2);
        this.writeTextView(textView, classID);

        imageView = (ImageView) findViewById(R.id.imageView_2);
        imageView.setImageURI(Uri.parse("android.resource://"+getPackageName()+"/drawable/"+classID));
    }

    private void  writeTextView(TextView textView, String id){
        String text = "";
        try {
            InputStream inputStream = getAssets().open("class_info_txt/" + id + ".txt");
            int size = inputStream.available();
            byte[] buffer = new byte[size];

            inputStream.read(buffer);
            inputStream.close();

            text = new String(buffer);

        }catch (IOException exception){
            exception.printStackTrace();
        }

        textView.setText(text);
    }
}
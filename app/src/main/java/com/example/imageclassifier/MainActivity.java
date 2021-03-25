package com.example.imageclassifier;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.example.imageclassifier.classifier.ImageClassifier;

import org.tensorflow.lite.support.common.FileUtil;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private ImageView imageView;
    private ListView listView;
    private Button buttonTakePicture;
    private ImageClassifier imageClassifier;

    private static final int CAMERA_PERMISSION_REQUEST = 1000;
    private static final int CAMERA_REQUEST = 10001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("+++++++++++ FLAG 1: MainActivity - onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Window w = getWindow();
        w.setTitle("Teste APP");

        initializeUIElements();
    }

    private void initializeUIElements() {
        System.out.println("+++++++++++ FLAG 2: MainActivity - initializeUIElements");
        imageView = findViewById(R.id.ImageView_canvas);
        listView = findViewById(R.id.listView_accuracy);
        buttonTakePicture = findViewById(R.id.button_camera);

        // Chamada da classe de classificação da imagem
        try {
            //add path
            MappedByteBuffer classifierModel = FileUtil.loadMappedFile(this, "model.tflite");
            List<String> labels = FileUtil.loadLabels(this, "labels.txt");

            imageClassifier = new ImageClassifier(classifierModel, labels);
        } catch (IOException e) {
            Log.e("imageClassifier Error", "ERROR: " + e);
        }

        buttonTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("+++++++++++ FLAG 3: MainActivity - initializeUIElements - onClick");
                if (hasPermission()) {
                    openCamera();
                } else {
                    requestPermission();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        System.out.println("+++++++++++ FLAG 4: MainActivity - onActivityResult");
        // Utilizando a imagem da foto
        if (requestCode == CAMERA_REQUEST) {
            // bitmap da imagem
            Bitmap photo = (Bitmap) Objects.requireNonNull(Objects.requireNonNull(data).getExtras()).get("data");
            imageView.setImageBitmap(photo);

            // Aqui o bitmap da imagem é passado para fazer a classificação
            List<ImageClassifier.Recognition> predicitons = imageClassifier.recognizeImage(
                    photo, 0);


            final List<String> predicitonsList = new ArrayList<>();
            for (ImageClassifier.Recognition recog : predicitons) {
                predicitonsList.add("Classe: " + recog.getName() + "  |  " + "Acurácia: " + recog.getConfidence());
            }

            ArrayAdapter<String> predictionsAdapter = new ArrayAdapter<>(
                    this, R.layout.support_simple_spinner_dropdown_item, predicitonsList);
            listView.setAdapter(predictionsAdapter);

            // ListView - CLICK LISTENER
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                // position -> mostra a possição onde esta sendo clicado.
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    System.out.println("+++++++++++ FLAG 5: MainActivity - onActivityResult - onItemClick");
                    //Toast.makeText(MainActivity.this, "Classe: "+ predicitons.get(position).getName(), Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(MainActivity.this, MainActivity2.class);
                    intent.putExtra("class", predicitons.get(position).getName());
                    startActivity(intent);
                }
            });
        }
        super.onActivityResult(requestCode, resultCode, data);
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        System.out.println("+++++++++++ FLAG 6: MainActivity - onRequestPermissionsResult");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (hasAllPermissions(grantResults)) {
                openCamera();
            } else {
                requestPermission();
            }
        }
    }

    private boolean hasAllPermissions(int[] grantResults) {
        System.out.println("+++++++++++ FLAG 7: MainActivity - hasAllPermissions");
        for (int result : grantResults) {
            if (result == PackageManager.PERMISSION_DENIED)
                return false;
        }
        return true;
    }

    private void requestPermission() {
        System.out.println("+++++++++++ FLAG 8: MainActivity - requestPermission");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                Toast.makeText(this, "Permissão da câmera requerida", Toast.LENGTH_SHORT).show();
            }
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
        }
    }

    private void openCamera() {
        System.out.println("+++++++++++ FLAG 9: MainActivity - openCamera");
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, CAMERA_REQUEST);
    }

    private boolean hasPermission() {
        System.out.println("+++++++++++ FLAG 10: MainActivity - hasPermission");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }
}
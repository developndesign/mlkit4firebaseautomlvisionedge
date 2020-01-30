package com.developndesign.firebaseautomlvisionedge;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.ml.common.FirebaseMLException;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.automl.FirebaseAutoMLLocalModel;
import com.google.firebase.ml.vision.automl.FirebaseAutoMLRemoteModel;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler;
import com.google.firebase.ml.vision.label.FirebaseVisionOnDeviceAutoMLImageLabelerOptions;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    FirebaseAutoMLRemoteModel remoteModel; // For loading the model remotely
    FirebaseVisionImageLabeler labeler; //For running the image labeler
    FirebaseVisionOnDeviceAutoMLImageLabelerOptions.Builder optionsBuilder; // Which option is use to run the labeler local or remotely
    ProgressDialog progressDialog; //Show the progress dialog while model is downloading...
    FirebaseModelDownloadConditions conditions; //Conditions to download the model
    FirebaseVisionImage image; // preparing the input image
    TextView textView; // Displaying the label for the input image
    Button button; // To select the image from device
    ImageView imageView; //To display the selected image
    private FirebaseAutoMLLocalModel localModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.text);
        button = findViewById(R.id.selectImage);
        imageView = findViewById(R.id.image);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fromRemoteModel();
            }
        });
    }

    private void setLabelerFromLocalModel(Uri uri) {
        localModel = new FirebaseAutoMLLocalModel.Builder()
                .setAssetFilePath("manifest.json")
                .build();
        try {
            FirebaseVisionOnDeviceAutoMLImageLabelerOptions options =
                    new FirebaseVisionOnDeviceAutoMLImageLabelerOptions.Builder(localModel)
                            .setConfidenceThreshold(0.0f)
                            .build();
            labeler = FirebaseVision.getInstance().getOnDeviceAutoMLImageLabeler(options);
            image = FirebaseVisionImage.fromFilePath(MainActivity.this, uri);
            processImageLabeler(labeler, image);
        } catch (FirebaseMLException | IOException e) {
            // ...
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                if (result != null) {
                    Uri uri = result.getUri(); //path of image in phone
                    imageView.setImageURI(uri); //set image in imageview
                    textView.setText(""); //so that previous text don't get append with new one
                    setLabelerFromLocalModel(uri);
                } else
                    progressDialog.cancel();
            } else
                progressDialog.cancel();
        }
    }

    private void setLabelerFromRemoteLabel(final Uri uri) {
        FirebaseModelManager.getInstance().isModelDownloaded(remoteModel)
                .addOnSuccessListener(new OnSuccessListener<Boolean>() {
                    @Override
                    public void onSuccess(Boolean isDownloaded) {
                        if (isDownloaded) {
                            optionsBuilder = new FirebaseVisionOnDeviceAutoMLImageLabelerOptions.Builder(remoteModel);
                            FirebaseVisionOnDeviceAutoMLImageLabelerOptions options = optionsBuilder
                                    .setConfidenceThreshold(0.0f)
                                    .build();
                            try {
                                labeler = FirebaseVision.getInstance().getOnDeviceAutoMLImageLabeler(options);
                                image = FirebaseVisionImage.fromFilePath(MainActivity.this, uri);
                                processImageLabeler(labeler, image);
                            } catch (FirebaseMLException | IOException exception) {
                                // Error.
                            }
                        }
                    }
                });
    }

    private void processImageLabeler(FirebaseVisionImageLabeler labeler, FirebaseVisionImage image) {
        labeler.processImage(image).addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionImageLabel>>() {
            @Override
            public void onSuccess(List<FirebaseVisionImageLabel> labels) {
                progressDialog.cancel();
                for (FirebaseVisionImageLabel label : labels) {
                    String eachlabel = label.getText().toUpperCase();
                    float confidence = label.getConfidence();
                    textView.append(eachlabel + " - " + ("" + confidence * 100).subSequence(0, 4) + "%" + "\n\n");
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, "Something went wrong! " + e, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fromRemoteModel() {
        showPrgressBar();                                         /* model name*/
        remoteModel = new FirebaseAutoMLRemoteModel.Builder("Flowers_2020123213435").build();
        conditions = new FirebaseModelDownloadConditions.Builder().requireWifi().build();
        //first download the model
        FirebaseModelManager.getInstance().download(remoteModel, conditions)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        CropImage.activity().start(MainActivity.this); // open image crop activity
                    }
                });
    }
    private void showPrgressBar() {
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMessage("Please Wait...");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();
    }
}
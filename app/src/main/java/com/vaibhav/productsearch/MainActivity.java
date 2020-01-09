package com.vaibhav.productsearch;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    int MY_CAMERA_REQUEST_CODE = 1;
    ArrayList<String> sentenceList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final EditText mainUserSearchEditText = findViewById(R.id.mainUserSearch);
        final EditText userEditSearchText = findViewById(R.id.userEditSearchText);
        final TextView identifiedTextLabel = findViewById(R.id.identifiedTextLabel);
        final TextSwitcher textSwitcher = findViewById(R.id.hintTextSwitcher);
        textSwitcher.setInAnimation(getApplicationContext(), android.R.anim.fade_in);
        textSwitcher.setOutAnimation(getApplicationContext(), android.R.anim.fade_out);

        final Handler handler = new Handler();
        final String[] switchText = {"1. Click capture", "2. Take a photo", "3. Detected text will appear here", "4. Select the text", "5. Edit the text if required", "6. Click search button on top right"};
        final Runnable runnable = new Runnable() {
            int i = 0;
            @Override
            public void run() {
                if(i < switchText.length){
                    textSwitcher.setText(switchText[i]);
                    i++;
                }
                else {
                    i=0;
                }
                handler.postDelayed(this, 2500);
            }
        };
        handler.postDelayed(runnable, 1000);

        Button mainButton = findViewById(R.id.mainButton);
        mainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Empty both the edit texts when new snap is taken
                mainUserSearchEditText.setText("");
                userEditSearchText.setText("");
                if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, MY_CAMERA_REQUEST_CODE);
                }
                else{
                    //Stop the thread
                    handler.removeCallbacks(runnable);
                    handler.postDelayed(runnable, 0);
                    textSwitcher.setVisibility(View.GONE);
                    identifiedTextLabel.setText(getString(R.string.selectCardViewHeading));
                    sentenceList.clear();
                    int REQUEST_IMAGE_CAPTURE = 1;
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    intent.resolveActivity(getPackageManager());
                    startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
                }
            }
        });

        userEditSearchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mainUserSearchEditText.setText(s);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        FloatingActionButton floatingActionButton = findViewById(R.id.searchFAB);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String searchQuery = userEditSearchText.getText().toString();
                String finalUrl = "https://www.amazon.in/s?k=" + searchQuery;
                Intent openInBrowserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl));
                startActivity(openInBrowserIntent);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(), "Allowed!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "Denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        try{
            Bundle extras = data.getExtras();
            Bitmap bitmap = (Bitmap) extras.get("data");
            Toast.makeText(getApplicationContext(), "Captured", Toast.LENGTH_LONG).show();

            if(bitmap != null){
                FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
                FirebaseVisionTextRecognizer textRecognizer = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
                textRecognizer.processImage(image).addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                    @Override
                    public void onSuccess(FirebaseVisionText firebaseVisionText) {
                        for(FirebaseVisionText.TextBlock textBlock : firebaseVisionText.getTextBlocks()){
                            String textToSet = textBlock.getText();
                            sentenceList.add(textToSet.replace("\n", " ").toLowerCase());
                        }
                        RecyclerView recyclerView = findViewById(R.id.mainRecyclerView);
                        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.VERTICAL, false);
                        recyclerView.setLayoutManager(linearLayoutManager);
                        RecyclerViewAdapter adapter = new RecyclerViewAdapter(getApplicationContext() ,sentenceList);
                        recyclerView.setAdapter(adapter);
                        int animationResource = R.anim.layout_recycler_view_animation;
                        LayoutAnimationController animation = AnimationUtils.loadLayoutAnimation(getApplicationContext(), animationResource);
                        recyclerView.setLayoutAnimation(animation);

                        //If data is changed we want to re-run the animation
                        recyclerView.getAdapter().notifyDataSetChanged();
                        recyclerView.scheduleLayoutAnimation();
                    }
                });
            }
        }
        catch (Exception e){
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Failed to captured", Toast.LENGTH_LONG).show();
        }
    }

    public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>{
        private ArrayList<String> sentenceListRV;
        private Context saveContext;
        private RecyclerViewAdapter(Context context, ArrayList<String> sentences){
            saveContext = context;
            sentenceListRV = sentences;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_cardview, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
            holder.cardTextView.setText(sentenceListRV.get(position));
            holder.cardTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EditText userEditSearchText = findViewById(R.id.userEditSearchText);
                    String text = userEditSearchText.getText().toString();
                    text += " " + sentenceListRV.get(position);
                    userEditSearchText.setText(text);
                }
            });
        }

        @Override
        public int getItemCount() {
            return sentenceListRV.size();
        }

        private class ViewHolder extends RecyclerView.ViewHolder{
            TextView cardTextView;
            private ViewHolder(View view){
                super(view);
                cardTextView = view.findViewById(R.id.cardTextView);
            }
        }
    }
}

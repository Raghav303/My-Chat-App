package com.example.chatapp.Activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.chatapp.Models.User;
import com.example.chatapp.R;
import com.example.chatapp.databinding.ActivityUpdateProfileBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;

public class UpdateProfile extends AppCompatActivity {

    User user;
    boolean flag=true;
    FirebaseAuth auth;
    FirebaseStorage storage;
    FirebaseDatabase database;
    ActivityUpdateProfileBinding binding;
    Uri selectedImage;
    Uri  prevImage;
    String prevName;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        flag=true;
        super.onCreate(savedInstanceState);
        binding =  ActivityUpdateProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        database = FirebaseDatabase.getInstance();

        getSupportActionBar().setTitle("Update Profile");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        String uid = auth.getUid();
       database.getReference().child("users").child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if(flag) {
                    user = snapshot.getValue(User.class);
                    if (!user.equals(null)) {
                        prevImage = Uri.parse(user.getProfileImage());
                        prevName = user.getName();


                        Glide.with(UpdateProfile.this).load(user.getProfileImage())
                                .placeholder(R.drawable.avatar)//default agar load na hui
                                .into(binding.imageView);

                        binding.nameBox.setText(user.getName());
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


        binding.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, 45);
            }
        });

        binding.continueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String name = binding.nameBox.getText().toString();

                if(name.isEmpty()) {
                    binding.nameBox.setError("Please type a name");
                    return;
                }

                //Toast.makeText(UpdateProfile.this,name+" "+prevName,Toast.LENGTH_SHORT).show();
                if(name.equals( prevName) && ( selectedImage == null || selectedImage.equals(prevImage))) {
                   // Toast.makeText(UpdateProfile.this,"Same Data",Toast.LENGTH_SHORT).show();


                }


              else  if(selectedImage == null)
                {
                    HashMap<String, Object> obj = new HashMap<>();
                    obj.put("name",name);
                    database.getReference().child("users")
                            .child(FirebaseAuth.getInstance().getUid())
                            .updateChildren(obj).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {

                        }
                    });

                }

                else{
                StorageReference reference = storage.getReference().child("Profiles").child(auth.getUid());
                reference.putFile(selectedImage).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if(task.isSuccessful()) {
                            reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    String imageUrl = uri.toString();

                                 /*   String uid = auth.getUid();
                                    String phone = auth.getCurrentUser().getPhoneNumber();
                                    String name = binding.nameBox.getText().toString();

                                    User user = new User(uid, name, phone, imageUrl);*/
                                    HashMap<String, Object> obj = new HashMap<>();
                                    obj.put("profileImage", imageUrl);
                                    obj.put("name",name);
                                    database.getReference().child("users")
                                            .child(FirebaseAuth.getInstance().getUid())
                                            .updateChildren(obj).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                           // Toast.makeText(UpdateProfile.this,"Name and Image UpdateSuccesful",Toast.LENGTH_SHORT).show();
                                         //   database.getReference().child("users").child(uid).addValueEventListener(null);

                                        }
                                    });

                                }
                            });
                        }
                    }
                });
            }     flag=false;
                 Toast.makeText(UpdateProfile.this,"Update Succesful",Toast.LENGTH_SHORT).show();

        }
}
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(data.getData()!= null && requestCode == 45 && resultCode == RESULT_OK)
        {
            selectedImage = data.getData();
            binding.imageView.setImageURI(data.getData());
        }
    }
}